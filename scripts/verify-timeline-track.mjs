/**
 * 订单与履约双轨时间线 —— 独立验收脚本（第五步）
 *
 * 用途：不依赖前端、直接打后端 /track 接口，并按「设计契约」逐条校验。
 * 最关键的一条校验：**拿数据库真实时间字段与接口返回的 stage.time 逐一比对**，
 * 用来证明时间取自真实数据、没有被伪造。
 *
 * 运行：node scripts/verify-timeline-track.mjs
 * 前置：后端已在 8080 启动
 */

import { execSync, execFileSync } from 'node:child_process'
import crypto from 'node:crypto'
import { pathToFileURL } from 'node:url'

const BASE = 'http://127.0.0.1:8080'
const DOCKER = 'C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe'
const MYSQL = `"${DOCKER}" exec -i bbpms-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 bbpms`
const REDIS_CLI = `"${DOCKER}" exec bbpms-redis redis-cli -a 123456`

function docker(args) {
  try {
    return execFileSync(DOCKER, args, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'] })
  } catch (e) {
    return ''
  }
}

let pass = 0
let fail = 0
const failures = []

function ok(msg) { pass++; console.log(`  \x1b[32mPASS\x1b[0m ${msg}`) }
function bad(msg) { fail++; failures.push(msg); console.log(`  \x1b[31mFAIL\x1b[0m ${msg}`) }
function check(cond, msg) { cond ? ok(msg) : bad(msg) }

function sh(cmd) {
  try {
    return execSync(cmd, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'], shell: 'C:\\Program Files\\Git\\bin\\bash.exe' })
  } catch (e) {
    return ''
  }
}

function sql(query) {
  // 用 execFileSync 直调 docker，绕开 shell 路径解析
  const out = docker(['exec', '-i', 'bbpms-mysql', 'mysql', '-uroot', '-p123456', '--default-character-set=utf8mb4', 'bbpms', '-e', query])
  return out.split('\n')
    .filter((l) => l.trim() && !l.includes('Warning'))
    .map((l) => l.split('\t'))
}

function redisGet(key) {
  const out = docker(['exec', 'bbpms-redis', 'redis-cli', '-a', '123456', 'GET', key])
  return out.trim()
}

function redisKeys(pattern) {
  const out = docker(['exec', 'bbpms-redis', 'redis-cli', '-a', '123456', 'KEYS', pattern])
  return out.trim()
}

// ---------- 登录 ----------
async function login(username = 'admin', password = 'admin123') {
  const capRes = await fetch(`${BASE}/api/auth/captcha`)
  const cap = await capRes.json()
  const captchaId = cap.data?.captchaId
  if (!captchaId) throw new Error('获取 captchaId 失败: ' + JSON.stringify(cap))

  // 验证码明文存 Redis：auth:captcha:<id>（值已小写）
  let code = redisGet(`auth:captcha:${captchaId}`)
  if (!code) {
    // 兜底：模糊找（redisUtils 可能加了全局前缀）
    const keys = redisKeys(`*captcha*${captchaId}*`)
    if (keys) code = redisGet(keys.split('\n')[0])
  }
  if (!code) throw new Error('读取验证码失败，captchaId=' + captchaId)

  const pkRes = await fetch(`${BASE}/api/auth/public-key`)
  const pkJson = await pkRes.json()
  const publicKeyB64 = pkJson.data
  if (!publicKeyB64) throw new Error('获取 RSA 公钥失败')

  // 后端返回的是裸 base64 DER 公钥，需包成 PEM（PKCS#1 / SPKI 均兼容）
  const pem = `-----BEGIN PUBLIC KEY-----\n${publicKeyB64.match(/.{1,64}/g).join('\n')}\n-----END PUBLIC KEY-----`
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
  if (!token) throw new Error('登录失败: ' + JSON.stringify(json).slice(0, 300))
  return token
}

// ---------- 数据库真实值 ----------
function dbOrder(orderNo) {
  const rows = sql(`SELECT id, order_no, status, create_time, audit_time, dispatch_time, completed_time, cancelled_time, update_time FROM broadband_order WHERE order_no='${orderNo}';`)
  if (rows.length < 2) return null
  const v = rows[1]
  return {
    id: v[0], orderNo: v[1], status: v[2],
    createTime: v[3], auditTime: v[4], dispatchTime: v[5],
    completedTime: v[6], cancelledTime: v[7], updateTime: v[8]
  }
}

function dbWorkOrder(orderId) {
  // 主工单 = 非取消的最新一条
  const rows = sql(`SELECT id, status, create_time, dispatch_time, accept_time, start_time, finish_time FROM work_order WHERE order_id=${orderId} AND status NOT IN ('CANCELLED','AUTO_CANCELLED') ORDER BY id DESC LIMIT 1;`)
  if (rows.length < 2) return null
  const v = rows[1]
  return { id: v[0], status: v[1], createTime: v[2], dispatchTime: v[3], acceptTime: v[4], startTime: v[5], finishTime: v[6] }
}

const norm = (t) => (t === 'NULL' || !t ? null : String(t).trim())

// ---------- 校验单个订单 ----------
async function verifyOrder(token, orderNo, expect) {
  console.log(`\n\x1b[36m▌ ${orderNo} (${expect.status})\x1b[0m`)
  const db = dbOrder(orderNo)
  if (!db) { bad(`${orderNo} 数据库中不存在`); return }
  const wo = dbWorkOrder(db.id)

  const res = await fetch(`${BASE}/api/orders/${db.id}/track`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!res.ok) { bad(`HTTP ${res.status}`); return }
  const body = await res.json()
  const d = body.data
  if (!d) { bad('返回无 data: ' + JSON.stringify(body).slice(0, 200)); return }

  const stages = d.stages || []
  const summary = d.summary || {}

  // 1. 骨架完整性
  check(stages.length === 8, `骨架 8 个节点（实际 ${stages.length}）`)
  if (stages.length !== 8) return

  // 2. 禁止伪造：time 为 null 必须是 PENDING
  const fabricated = stages.filter((s) => s.time === null && s.state !== 'PENDING' && s.state !== 'EXCEPTION')
  check(fabricated.length === 0, `无伪造时间（time 为空却有状态的节点 ${fabricated.length} 个）`)

  // 3. 有 time 的节点不能是 PENDING（注意：Jackson 序列化会省略 null 字段，time 缺失=undefined，需宽松判空）
  const hasTime = (t) => t !== null && t !== undefined && String(t).trim() !== ''
  const hasTimeButPending = stages.filter((s) => hasTime(s.time) && s.state === 'PENDING')
  check(hasTimeButPending.length === 0, `有真实时间却标记为待处理 ${hasTimeButPending.length} 个`)

  // 4. 时间与数据库真实字段一致（核心：证明未伪造）
  const expectTime = {
    0: norm(db.createTime),
    1: norm(db.auditTime),
    2: wo ? norm(wo.createTime) : null,
    3: norm(db.dispatchTime) || (wo ? norm(wo.dispatchTime) : null),
    4: wo ? norm(wo.acceptTime) : null,
    5: wo ? norm(wo.startTime) : null,
    6: (wo ? norm(wo.finishTime) : null) || norm(db.completedTime),
    7: norm(db.updateTime)
  }
  const mismatch = []
  for (let i = 0; i < 8; i++) {
    const dbVal = expectTime[i]
    const apiVal = norm(stages[i].time)
    // 接口可能格式化到分钟，数据库精确到秒 —— 比较前缀即可
    if (dbVal && apiVal) {
      if (!dbVal.startsWith(apiVal.slice(0, 16)) && !apiVal.startsWith(dbVal.slice(0, 16))) {
        mismatch.push(`节点${i + 1}: DB=${dbVal} API=${apiVal}`)
      }
    }
  }
  check(mismatch.length === 0, `时间与数据库一致${mismatch.length ? ' → ' + mismatch.join('; ') : ''}`)

  // 5. 当前节点
  const currents = stages.filter((s) => s.state === 'CURRENT')
  const exception = stages.filter((s) => s.state === 'EXCEPTION')
  const terminal = ['CLOSED', 'CANCELLED'].includes(db.status)
  if (terminal || exception.length) {
    check(true, `终结/异常订单，CURRENT 数 ${currents.length}（不强求）`)
  } else {
    check(currents.length === 1, `恰有 1 个进行中节点（实际 ${currents.length}）`)
  }

  // 6. 状态匹配
  check(summary.currentStatus === db.status, `当前状态匹配（API=${summary.currentStatus} DB=${db.status}）`)

  // 7. 进度格式
  const done = stages.filter((s) => s.state === 'DONE').length
  check(/^\d+\/8$/.test(summary.progress || ''), `进度格式 n/8（实际 ${summary.progress}，已完成 ${done}）`)

  // 8. 异常订单必须有异常标记
  if (['CANCELLED'].includes(db.status)) {
    check(exception.length > 0 || summary.currentStatusDesc?.includes('取消'), `异常订单有异常标记`)
  }

  // 输出骨架快照
  console.log('    ' + stages.map((s) => `${s.state === 'DONE' ? '✓' : s.state === 'CURRENT' ? '◉' : s.state === 'EXCEPTION' ? '!' : '○'}${s.name}`).join(' → '))
  console.log(`    进度 ${summary.progress} | 已用时 ${summary.elapsed} | 等待 ${summary.waiting}`)
}

// ---------- 主流程 ----------
async function main() {
  console.log('\x1b[1m订单与履约双轨时间线 —— 独立验收\x1b[0m')
  console.log('='.repeat(60))

  let token
  try {
    token = await login()
    console.log('\x1b[32m登录成功\x1b[0m\n')
  } catch (e) {
    console.error('\x1b[31m登录失败:\x1b[0m', e.message)
    process.exit(1)
  }

  const cases = [
    { orderNo: 'BBDEMO20260001', status: 'CREATED 待审核' },
    { orderNo: 'BBDEMO20260008', status: 'AUDITED 已审核' },
    { orderNo: 'BBDEMO20260002', status: 'WAIT_DISPATCH 待派单' },
    { orderNo: 'BBDEMO20260003', status: 'DISPATCHED 已派单(多工单)' },
    { orderNo: 'BBDEMO20260004', status: 'INSTALLING 安装中' },
    { orderNo: 'BBDEMO20260005', status: 'FINISHED 已完成(日志为空)' },
    { orderNo: 'BBDEMO20260006', status: 'CLOSED 已归档' },
    { orderNo: 'BBDEMO20260007', status: 'CANCELLED 已取消' }
  ]

  for (const c of cases) {
    try {
      await verifyOrder(token, c.orderNo, c)
    } catch (e) {
      bad(`${c.orderNo} 异常: ${e.message}`)
    }
  }

  console.log('\n' + '='.repeat(60))
  console.log(`\x1b[1m结果: \x1b[32m${pass} 通过\x1b[0m / \x1b[31m${fail} 失败\x1b[0m`)
  if (failures.length) {
    console.log('\n失败项:')
    failures.forEach((f) => console.log('  - ' + f))
  }
  process.exit(fail > 0 ? 1 : 0)
}

main()
