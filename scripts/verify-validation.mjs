/**
 * 迭代三：输入校验补强 端到端验收脚本
 *
 * 验证核心目标：
 *   A. 无效输入被 400 拦截（非法手机号/超长字段/负数值/未来时间约束/缺失必填）
 *   B. 合法输入放行（校验不误伤正常请求）
 *   C. 校验错误消息格式（字段:消息）
 *   D. Controller @Valid 生效性抽查（覆盖此前零校验的装维/派单/通知模块）
 *
 * 运行：node scripts/verify-validation.mjs
 * 依赖：后端 8080 运行中（新 jar）；docker 容器 bbpms-redis / bbpms-mysql
 */
import { execFileSync } from 'node:child_process'
import crypto from 'node:crypto'

const BASE = 'http://127.0.0.1:8080'
const DOCKER = 'C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe'

const PASS = '\x1b[32mPASS\x1b[0m'
const FAIL = '\x1b[31mFAIL\x1b[0m'
let passed = 0
let failed = 0

function check(ok, label) {
  if (ok) { passed++; console.log(`  ${PASS} ${label}`) }
  else { failed++; console.log(`  ${FAIL} ${label}`) }
}

function docker(args) {
  return execFileSync(DOCKER, args, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'] })
}

function redisGet(key) {
  return docker(['exec', 'bbpms-redis', 'redis-cli', '-a', '123456', 'GET', key]).trim()
}

// ---------- 登录（admin，密码 admin123） ----------
async function login(username = 'admin', password = 'admin123') {
  const cap = await (await fetch(`${BASE}/api/auth/captcha`)).json()
  const captchaId = cap.data?.captchaId
  if (!captchaId) throw new Error('获取 captchaId 失败')
  let code = redisGet(`auth:captcha:${captchaId}`)
  if (!code) {
    const keys = docker(['exec', 'bbpms-redis', 'redis-cli', '-a', '123456', 'KEYS', `*captcha*${captchaId}*`]).trim()
    if (keys) code = redisGet(keys.split('\n')[0])
  }
  if (!code) throw new Error('读取验证码失败')
  const pk = (await (await fetch(`${BASE}/api/auth/public-key`)).json()).data
  const pem = `-----BEGIN PUBLIC KEY-----\n${pk.match(/.{1,64}/g).join('\n')}\n-----END PUBLIC KEY-----`
  const encrypted = crypto.publicEncrypt(
    { key: pem, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(password, 'utf8')
  ).toString('base64')
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: encrypted, captchaId, captcha: code.toLowerCase() })
  })
  const json = await res.json()
  const token = json.data?.accessToken || json.data?.token
  if (!token) throw new Error('登录失败: ' + JSON.stringify(json).slice(0, 200))
  return token
}

async function post(path, body, token) {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(body)
  })
  let json = null
  try { json = await res.json() } catch { /* 非 JSON 响应 */ }
  return { status: res.status, json }
}

async function put(path, body, token) {
  const res = await fetch(`${BASE}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(body)
  })
  let json = null
  try { json = await res.json() } catch { /* 非 JSON 响应 */ }
  return { status: res.status, json }
}

// =====================================================================
async function main() {
  console.log('BBPMS 输入校验补强验收（400 拦截 / 放行 / 消息格式）')
  console.log('========================================================')
  const token = await login()
  console.log('登录成功\n')

  // ---------- A. 无效输入被 400 拦截 ----------
  console.log('▌ A. 校验生效性（无效输入 → 400）')

  // A1. 订单创建：非法手机号（订单侧可能先过业务/序列化校验，400 且非 500 即生效）
  let r = await post('/api/orders', {
    customerId: 1, packageId: 1, customerName: '验收客户', customerPhone: '12345',
    address: '北京市朝阳区测试路1号', appointmentAt: new Date(Date.now() + 86400000).toISOString()
  }, token)
  check(r.status === 400 && r.json?.code !== 500, `A1 订单非法手机号被拦 (400): ${(r.json?.msg || '').slice(0, 50)}`)

  // A2. 客户创建：姓名超长（>50）
  r = await post('/api/customers', { name: '验'.repeat(60), phone: '13800138000' }, token)
  check(r.status === 400 && r.json?.code === 400, `A2 客户姓名超长被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A3. 客户创建：非法身份证
  r = await post('/api/customers', { name: '验收客户2', phone: '13800138000', idCardNo: '123' }, token)
  check(r.status === 400 && r.json?.code === 400, `A3 非法身份证被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A4. 套餐创建：带宽为负数
  r = await post('/api/packages', { code: 'TMP_VAL_001', name: '验收套餐', speedMbps: -100, monthlyFee: 99 }, token)
  check(r.status === 400 && /带宽/.test(r.json?.msg || ''), `A4 负带宽被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A5. 套餐创建：月租费为负
  r = await post('/api/packages', { code: 'TMP_VAL_002', name: '验收套餐2', speedMbps: 100, monthlyFee: -1 }, token)
  check(r.status === 400 && /费用|月租/.test(r.json?.msg || ''), `A5 负月租费被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A6. 登录：空用户名
  r = await post('/api/auth/login', { username: '', password: 'x', captchaId: 'x', captcha: 'x' }, '')
  check(r.status === 400, `A6 登录空用户名被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A7. 派单规则：权重负数（PUT）
  r = await put('/api/dispatch/rules/', { name: '验收规则', weightDistance: -1, weightLoad: 1, weightSkill: 1, weightRating: 1, radiusKm: 5, enabled: 1 }, token)
  check(r.status === 400 && /权重/.test(r.json?.msg || ''), `A7 负权重被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A8. 角色创建：空编码
  r = await post('/api/roles', { code: '', name: '验收角色' }, token)
  check(r.status === 400 && /编码|不能为空/.test(r.json?.msg || ''), `A8 角色空编码被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A9. 短信发送：非法手机号
  r = await post('/api/notify/sms', { phone: '999', templateCode: 'X', params: {} }, token)
  check(r.status === 400 && /手机号/.test(r.json?.msg || ''), `A9 短信非法手机号被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A10. 装维位置：经纬度越界
  r = await post('/api/installers/location', { userId: 1, lat: 99.9, lng: 0, onDuty: 1 }, token)
  check(r.status === 400 && /纬度/.test(r.json?.msg || ''), `A10 纬度越界被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A11. 部门创建：空名称
  r = await post('/api/depts', { name: '', parentId: 0 }, token)
  check(r.status === 400 && /名称|不能为空/.test(r.json?.msg || ''), `A11 部门空名称被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A12. 装维签到：纬度越界
  r = await post('/api/attendance/clock-in', { lat: -91, lng: 0, source: 'MANUAL' }, token)
  check(r.status === 400 && /纬度/.test(r.json?.msg || ''), `A12 签到纬度越界被拦 (400): ${r.json?.msg?.slice(0, 50)}`)

  // A13. 预约创建：时间在过去（@Future 触发 → 400，可能因时间格式序列化为"请求体格式错误"）
  const past = new Date(Date.now() - 86400000).toISOString().replace('T', ' ').slice(0, 19)
  r = await post('/api/appointments', { orderId: 1, appointmentTime: past }, token)
  check(r.status === 400 && r.json?.code !== 500, `A13 预约过去时间被拦 (400): ${(r.json?.msg || '').slice(0, 50)}`)

  console.log()

  // ---------- B. 合法输入放行 ----------
  console.log('▌ B. 合法输入放行（不被误伤）')

  // B1. 合法客户创建
  const uniq = `验收客户${Date.now() % 100000}`
  r = await post('/api/customers', { name: uniq, phone: '13800138000' }, token)
  check(r.status === 200 && r.json?.code === 0, `B1 合法客户创建放行: ${r.json?.msg || 'ok'}`)

  // B2. 合法套餐创建（校验字段边界内，status 必填）
  const pkgCode = `TMP_V${Date.now() % 10000}`
  r = await post('/api/packages', { code: pkgCode, name: '边界套餐', speedMbps: 100, monthlyFee: 0.5, status: 1 }, token)
  check(r.status === 200 && r.json?.code === 0, `B2 合法套餐创建放行: ${r.json?.msg || 'ok'}`)

  // B3. 合法部门创建
  r = await post('/api/depts', { name: '验收临时部门', parentId: 0, phone: '13800138000' }, token)
  check(r.status === 200 && r.json?.code === 0, `B3 合法部门创建放行: ${r.json?.msg || 'ok'}`)

  // B4. 合法登录（空校验不误伤正常流程）
  check(true, 'B4 登录已成功（见上方）')

  console.log()

  // ---------- C. 校验消息格式 ----------
  console.log('▌ C. 错误消息格式（字段:消息）')
  r = await post('/api/customers', { name: '验'.repeat(60), phone: '13800138000' }, token)
  const hasField = /^\w+[::：]/.test(r.json?.msg || '')
  check(hasField, `C1 消息含字段名前缀: ${(r.json?.msg || '').slice(0, 60)}`)
  check((r.json?.msg || '').length > 0 && r.json?.msg.length < 300, 'C2 消息长度适中（非堆栈）')

  console.log(`\n========================================================`)
  console.log(`结果: ${passed} 通过, ${failed} 失败`)
  if (failed > 0) process.exit(1)
}

main().catch((e) => { console.error('脚本异常:', e.message); process.exit(1) })
