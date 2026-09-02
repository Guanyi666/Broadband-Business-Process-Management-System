// BBPMS Phase 5 — leave linkage + SLA timeout E2E
// Scenario A: installer on approved leave is excluded from dispatch candidates.
//             (Seeds an active APPROVED leave row directly in MySQL: the API
//              requires 4h advance notice, so a via-API leave can never be
//              "currently active" — the exclusion filter is what we verify.)
// Scenario B: DISPATCHED work order not accepted within SLA window is
//             auto-cancelled (run with --bbpms.workorder.sla.accept-timeout-minutes=1).
import crypto from 'node:crypto'
import { execSync } from 'node:child_process'

const BASE = 'http://localhost:8080/api'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const results = []
function record(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}${detail ? ' | ' + detail : ''}`)
}

function sql(q) {
  return execSync(`docker exec bbpms-mysql mysql -ubbpms_app -pbbpms_pwd_2026 bbpms -e "${q}"`, { encoding: 'utf8' })
}

async function login(username, password = 'admin123') {
  const pk = await (await fetch(BASE + '/auth/public-key')).json()
  const enc = crypto.publicEncrypt(
    { key: `-----BEGIN PUBLIC KEY-----\n${pk.data}\n-----END PUBLIC KEY-----`, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(password)
  ).toString('base64')
  const r = await fetch(BASE + '/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: enc })
  })
  const j = await r.json()
  if (j.code !== 0) throw new Error(`login ${username} failed: ${j.code} ${j.msg}`)
  return { token: j.data.accessToken, user: j.data.user }
}

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return { status: res.status, json: await res.json() }
}

async function scenarioA() {
  console.log('\n--- Scenario A: leave linkage (candidate exclusion) ---')
  const disp = await login('disp1')

  // 1. seed an ACTIVE approved leave for install2 (userId=7) — single-line SQL
  //    (execSync on Windows breaks on multi-line -e strings)
  sql(`INSERT INTO lv_leave_request (applicant_id, leave_type, start_at, end_at, total_hours, reason, status, current_level, required_level, applied_at, deleted, version) VALUES (7, 'PERSONAL', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR), 6.00, 'E2E active leave', 'APPROVED', 1, 1, NOW(), 0, 0)`)
  record('seeded active APPROVED leave for install2', true, '')

  // 2. dispatch candidates must exclude install2 (userId=7)
  await sleep(1000)
  const cand = await api('/dispatch/candidates', { token: disp.token })
  const ids = (cand.json.data || []).map((c) => c.userId ?? c.installerId)
  record('dispatch candidates exclude install2 (on leave)', !ids.includes(7), `candidates=${JSON.stringify(ids)}`)

  // 3. cleanup
  sql(`DELETE FROM lv_leave_request WHERE applicant_id=7 AND reason='E2E active leave'`)
  record('cleanup: seeded leave row removed', true, '')
}

async function scenarioB() {
  console.log('\n--- Scenario B: SLA accept-timeout auto-cancel ---')
  const cs = await login('cs1')
  const audit = await login('audit1')

  // fresh customer (unique phone) + order + audit pass → work order DISPATCHED
  const phone = '137' + String(Date.now()).slice(-8)
  const cust = await api('/customers', {
    method: 'POST', token: cs.token,
    body: { name: 'SLA客户', phone, idCardNo: '110101199002022345', address: '北京市海淀区中关村大街1号', province: '北京', city: '北京市', district: '海淀区' }
  })
  const ord = await api('/orders', {
    method: 'POST', token: cs.token,
    body: { customerId: cust.json.data, packageCode: 'FIBER_300M', installAddress: '北京市海淀区中关村大街1号-202', contactPhone: phone, remark: 'SLA超时验证' }
  })
  const orderId = ord.json.data
  record('order created (SLA scenario)', ord.status === 200 && ord.json.code === 0, `orderId=${orderId}`)
  const aud = await api(`/orders/${orderId}/audit`, { method: 'POST', token: audit.token, body: { pass: true, remark: 'SLA场景审核' } })
  record('audit pass (SLA scenario)', aud.status === 200 && aud.json.code === 0, `HTTP ${aud.status}`)

  let woId = null
  for (let i = 0; i < 20; i++) {
    const wo = await api(`/work-orders/by-order/${orderId}`, { token: audit.token })
    if (wo.json.code === 0 && wo.json.data?.id) { woId = wo.json.data.id; break }
    await sleep(500)
  }
  record('work order created (DISPATCHED, unaccepted)', !!woId, `workOrderId=${woId}`)
  if (!woId) return

  // Do NOT accept. The engine is verified (Phase 10); the app runs the
  // default 30min timeout, so backdate dispatch_time 31min in SQL (SA-P2-008
  // approved approach — config-driven alternative would need a restart) and
  // wait for the next SLA scan (interval 30s, initialDelay 30s).
  sql(`UPDATE work_order SET dispatch_time = DATE_SUB(NOW(), INTERVAL 31 MINUTE) WHERE id = ${woId}`)
  let finalStatus = null
  for (let i = 0; i < 10; i++) {
    await sleep(10_000)
    const wo = await api(`/work-orders/${woId}`, { token: audit.token })
    finalStatus = wo.json.data?.status
    if (finalStatus === 'AUTO_CANCELLED') break
  }
  record('SLA auto-cancelled unaccepted work order', finalStatus === 'AUTO_CANCELLED', `status=${finalStatus}`)
}

async function main() {
  await scenarioA()
  await scenarioB()
  const failed = results.filter((r) => !r.ok)
  console.log(`\n===== ${results.length - failed.length}/${results.length} PASSED =====`)
  process.exit(failed.length ? 1 : 0)
}

main().catch((e) => { console.error('SCRIPT ERROR:', e.message); process.exit(2) })
