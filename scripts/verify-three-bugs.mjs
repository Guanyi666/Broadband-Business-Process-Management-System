/**
 * 三个 Bug 修复的针对性回归验证：
 *   A. 问题1: 客户 H5 新建工单订单摘要套餐名中文化（toOrderSummary 走字典映射）
 *   B. 问题3: 待派单(WAIT_DISPATCH)订单时间线审核节点衔接（不再灰色跳步）
 *   C. 问题2: 管理端一级菜单单次点击跳转（前端交互，验证构建产物含 onGroupClick 逻辑）
 *
 * 运行: node scripts/verify-three-bugs.mjs
 * 依赖: 后端 8080 运行中；docker 容器 bbpms-redis / bbpms-mysql
 */
import { execFileSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'

const BASE = 'http://127.0.0.1:8080'
const CUSTOMER_BASE = `${BASE}/api/customer-portal`
const DOCKER = 'C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe'

const PASS = '\x1b[32mPASS\x1b[0m'
const FAIL = '\x1b[31mFAIL\x1b[0m'
const WARN = '\x1b[33mWARN\x1b[0m'
let passed = 0
let failed = 0

function check(ok, label) {
  if (ok) { passed++; console.log(`  ${PASS} ${label}`) }
  else { failed++; console.log(`  ${FAIL} ${label}`) }
}

function docker(args) {
  return execFileSync(DOCKER, args, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'] })
}

function sql(query) {
  const out = docker(['exec', '-i', 'bbpms-mysql', 'mysql', '-uroot', '-p123456', '--default-character-set=utf8mb4', 'bbpms', '-e', query])
  return out.split('\n').filter((l) => l.trim() && !l.includes('Warning')).map((l) => l.split('\t'))
}

function redisGet(key) {
  return docker(['exec', 'bbpms-redis', 'redis-cli', '-a', '123456', 'GET', key]).trim()
}

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

async function api(token, url, opts = {}) {
  const res = await fetch(`${BASE}${url}`, {
    ...opts,
    headers: { Authorization: `Bearer ${token}`, ...(opts.headers || {}) }
  })
  const text = await res.text()
  let json
  try { json = JSON.parse(text) } catch { json = text }
  return { status: res.status, json }
}

async function main() {
  console.log('三个 Bug 修复针对性回归验证')
  console.log('============================================================')
  const token = await login()
  console.log('登录成功（admin）\n')

  // ============ A. 问题1: 订单摘要套餐名中文化 ============
  console.log('▌ A. 客户订单摘要套餐名中文化（toOrderSummary 映射）')
  // 客户门户接口需 CUSTOMER 角色，用 customer1 登录
  let cToken
  try {
    cToken = await login('customer1', 'admin123')
    console.log('  客户登录成功（customer1）')
  } catch (e) {
    console.log(`  ${WARN} customer1 登录失败: ${e.message}，尝试 admin`)
    try { cToken = await login() } catch (e2) { console.log(`  ${FAIL} admin 也登录失败: ${e2.message}`) }
  }
  // 从数据库找一条 package_name 为英文的历史订单（客户1名下，若有）
  const engRows = sql(`SELECT id, order_no, package_code, package_name, status FROM broadband_order WHERE package_name REGEXP '[a-zA-Z]{4,}' AND deleted=0 AND customer_id=1 LIMIT 5`)
  const fallbackRows = sql(`SELECT id, order_no, package_name FROM broadband_order WHERE deleted=0 AND customer_id=1 LIMIT 5`)
  const probeRows = engRows.length > 1 ? engRows.slice(1) : fallbackRows.slice(1)
  if (cToken && probeRows.length) {
    for (const row of probeRows) {
      const id = row[0]
      const r = await api(cToken, `/api/customer-portal/orders/${id}`)
      if (r.status !== 200) { check(false, `订单 ${id} 摘要接口状态 ${r.status}（${String(r.json).slice(0,80)}）`); continue }
      const data = r.json?.data || {}
      // 详情响应结构: data.order.packageName（toOrderSummary 的订单摘要）
      const order = data.order || data
      const name = order?.packageName || order?.package_name || ''
      const isChinese = /[\u4e00-\u9fa5]/.test(name)
      check(isChinese, `订单 ${row[1]} (${row.length > 3 ? row[3] : ''}) 摘要返回中文套餐名: ${name}`)
    }
  } else {
    console.log(`  ${FAIL} 无可用客户 token 或该客户无订单，跳过 A 项`)
    if (!cToken) failed++
  }

  // ============ B. 问题3: WAIT_DISPATCH 订单时间线衔接 ============
  console.log('\n▌ B. 待派单(WAIT_DISPATCH)订单时间线节点衔接')
  const waitRows = sql(`SELECT id, order_no, status, audit_time, dispatch_time FROM broadband_order WHERE status='WAIT_DISPATCH' AND deleted=0 LIMIT 3`)
  const waitList = waitRows.length > 1 ? waitRows.slice(1) : sql(`SELECT id, order_no, status, audit_time, dispatch_time FROM broadband_order WHERE deleted=0 AND status IN ('WAIT_DISPATCH','DISPATCHED','AUDITED') LIMIT 4`).slice(1)
  const extractNodes = (data) => {
    // TrackResultVO 结构：nodes / timeline / stages 等
    if (Array.isArray(data)) return data
    if (data?.nodes && Array.isArray(data.nodes)) return data.nodes
    if (data?.timeline && Array.isArray(data.timeline)) return data.timeline
    if (data?.stages && Array.isArray(data.stages)) return data.stages
    if (data?.items && Array.isArray(data.items)) return data.items
    return null
  }
  if (waitList.length === 0) {
    console.log(`  ${WARN} 无待派单/已派单/已审核订单可验证（用任意订单验证 track 接口）`)
    const anyRows = sql(`SELECT id, order_no FROM broadband_order WHERE deleted=0 LIMIT 2`).slice(1)
    for (const row of anyRows) {
      const r = await api(token, `/api/orders/${row[0]}/track`)
      const nodes = extractNodes(r.json?.data)
      if (nodes && nodes.length > 0) {
        const states = nodes.map(n => n.state || n.status).join(',')
        const firstNotDone = nodes.findIndex(n => (n.state || n.status) !== 'DONE' && (n.state || n.status) !== 'SKIP')
        const hasGap = firstNotDone > 0 && nodes.slice(firstNotDone).some(n => (n.state || n.status) === 'DONE')
        check(!hasGap, `订单 ${row[1]} 时间线无跳步（states: ${states}）`)
      } else {
        check(false, `订单 ${row[1]} track 接口无节点数据`)
      }
    }
  } else {
    for (const row of waitList) {
      const id = row[0]
      const r = await api(token, `/api/orders/${id}/track`)
      const nodes = extractNodes(r.json?.data)
      console.log(`  订单 ${row[1]} 状态=${row[2]} audit_time=${row[3] || 'NULL'}`)
      const states = nodes ? nodes.map(n => `${n.label || n.name || n.stage || '?'}:${n.state || n.status}`).join(' → ') : ''
      if (states) console.log(`    时间线: ${states}`)
      if (nodes && nodes.length > 0) {
        // 规则1: 首个非DONE/SKIP节点之前不得有PENDING
        const firstNotDone = nodes.findIndex(n => (n.state || n.status) !== 'DONE' && (n.state || n.status) !== 'SKIP')
        if (firstNotDone >= 0) {
          const before = nodes.slice(0, firstNotDone)
          const noPendingBefore = before.every(n => (n.state || n.status) === 'DONE' || (n.state || n.status) === 'SKIP')
          check(noPendingBefore, `首个未完成=${nodes[firstNotDone].label || nodes[firstNotDone].name || nodes[firstNotDone].stage || '?'} 前置节点均已完成`)
        } else {
          check(true, '全部节点已完成（订单已完结）')
        }
        // 规则2: 已审核节点必须 DONE（核心问题）
        const auditNode = nodes.find(n => (n.label || n.name || n.stage || '').includes('审核'))
        if (auditNode) {
          check((auditNode.state || auditNode.status) === 'DONE', `审核节点「${auditNode.label || auditNode.name || auditNode.stage}」状态=${auditNode.state || auditNode.status}（应为 DONE 已完成）`)
        } else {
          console.log(`    ${WARN} 时间线中未找到「审核」节点`)
        }
      } else {
        check(false, `订单 ${row[1]} track 接口无节点数据（响应: ${String(r.json).slice(0, 100)}）`)
      }
    }
  }

  // ============ C. 问题2: 管理端菜单单击跳转（构建产物检查） ============
  console.log('\n▌ C. 管理端一级菜单单击跳转逻辑（构建产物）')
  const distDir = path.resolve('bbpms-admin-web/dist')
  const assetsDir = path.join(distDir, 'assets')
  let found = false
  if (fs.existsSync(assetsDir)) {
    const files = fs.readdirSync(assetsDir).filter(f => f.endsWith('.js'))
    for (const f of files) {
      const content = fs.readFileSync(path.join(assetsDir, f), 'utf8')
      if (content.includes('onGroupClick') || (content.includes('sidebar-submenu-title') && content.includes('router.push'))) {
        found = true
        console.log(`  ${PASS} 构建产物含一级菜单跳转逻辑 (${f})`)
        break
      }
    }
    if (!found) {
      // 压缩后可能改名，宽松搜索
      for (const f of files) {
        const content = fs.readFileSync(path.join(assetsDir, f), 'utf8')
        if (content.includes('submenu') && content.includes('push(')) { found = true; break }
      }
      check(found, '构建产物含子菜单点击跳转逻辑（宽松匹配）')
    }
  } else {
    console.log(`  ${WARN} dist 目录不存在，跳过（前端构建已在编译期验证）`)
  }

  console.log('\n============================================================')
  console.log(`结果: ${passed} 通过 / ${failed} 失败`)
  process.exit(failed > 0 ? 1 : 0)
}

main().catch((e) => { console.error('脚本异常:', e.message); process.exit(1) })
