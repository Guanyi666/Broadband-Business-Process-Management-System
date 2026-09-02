// BBPMS Phase 6 — exception / edge-case E2E
// State-machine rejections, idempotency, param validation, concurrent dispatch.
import crypto from 'node:crypto'

const BASE = 'http://localhost:8080/api'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const results = []
function record(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}${detail ? ' | ' + detail : ''}`)
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

async function setupOrder() {
  // create customer + order + audit pass + work order, return ids
  const cs = await login('cs1')
  const audit = await login('audit1')
  const phone = '136' + String(Date.now()).slice(-8)
  const cust = await api('/customers', {
    method: 'POST', token: cs.token,
    body: { name: '异常场景客户', phone, idCardNo: '110101199003033456', address: '北京市西城区金融街1号', province: '北京', city: '北京市', district: '西城区' }
  })
  const ord = await api('/orders', {
    method: 'POST', token: cs.token,
    body: { customerId: cust.json.data, packageCode: 'FIBER_500M', installAddress: '北京市西城区金融街1号-301', contactPhone: phone, remark: '异常场景测试' }
  })
  const orderId = ord.json.data
  await api(`/orders/${orderId}/audit`, { method: 'POST', token: audit.token, body: { pass: true, remark: '异常场景审核' } })
  let woId = null
  for (let i = 0; i < 20; i++) {
    const wo = await api(`/work-orders/by-order/${orderId}`, { token: audit.token })
    if (wo.json.code === 0 && wo.json.data?.id) { woId = wo.json.data.id; break }
    await sleep(500)
  }
  return { cs, audit, orderId, woId }
}

async function main() {
  // 0. param validation
  const cs = await login('cs1')
  const noCust = await api('/orders', { method: 'POST', token: cs.token, body: { packageCode: 'FIBER_100M', installAddress: 'x' } })
  record('create order without customerId → 400', noCust.status === 400 && noCust.json.code === 400, `HTTP ${noCust.status} code=${noCust.json.code}`)
  const ghost = await api('/orders/999999999/audit', { method: 'POST', token: cs.token, body: { pass: true } })
  record('audit nonexistent order → business error (not 500)', ghost.status !== 500, `HTTP ${ghost.status} code=${ghost.json.code}`)

  // 1. customer idempotency (same phone → same id)
  const phone = '135' + String(Date.now()).slice(-8)
  const body = { name: '幂等客户', phone, idCardNo: '110101199004044567', address: 'a', province: '北京', city: '北京市', district: '朝阳区' }
  const c1 = await api('/customers', { method: 'POST', token: cs.token, body })
  const c2 = await api('/customers', { method: 'POST', token: cs.token, body })
  record('customer upsert idempotent (same phone → same id)', c1.json.data === c2.json.data && !!c1.json.data, `id=${c1.json.data}`)

  // 2. state machine rejections
  const { audit, orderId, woId } = await setupOrder()
  if (!woId) { console.log('STOP: setup failed'); return }
  const inst = await login('install1')
  await api('/installers/location', { method: 'POST', token: inst.token, body: { userId: inst.user.id, lat: 39.9, lng: 116.4, onDuty: 1 } })
  const disp = await login('disp1')
  await api(`/dispatch/auto?orderId=${orderId}`, { method: 'POST', token: disp.token })

  // 2a. start before accept → rejected (DISPATCHED → IN_PROGRESS not in state machine)
  const earlyStart = await api(`/work-orders/${woId}/start`, { method: 'POST', token: inst.token })
  record('start without accept → rejected', earlyStart.json.code !== 0 && earlyStart.status !== 500, `HTTP ${earlyStart.status} code=${earlyStart.json.code}`)

  // 2b. double accept → second rejected
  const a1 = await api(`/work-orders/${woId}/accept`, { method: 'POST', token: inst.token })
  const a2 = await api(`/work-orders/${woId}/accept`, { method: 'POST', token: inst.token })
  record('accept OK', a1.json.code === 0 && a1.json.data?.status === 'ACCEPTED', `status=${a1.json.data?.status}`)
  record('double accept → rejected', a2.json.code !== 0 && a2.status !== 500, `HTTP ${a2.status} code=${a2.json.code}`)

  // 2c. reject audit on an already-audited order
  const reAudit = await api(`/orders/${orderId}/audit`, { method: 'POST', token: audit.token, body: { pass: false, remark: '第二次审核' } })
  record('audit an already-audited order → rejected', reAudit.json.code !== 0 && reAudit.status !== 500, `HTTP ${reAudit.status} code=${reAudit.json.code}`)

  // 2d. full chain then complete twice
  await api(`/work-orders/${woId}/start`, { method: 'POST', token: inst.token })
  for (let i = 1; i <= 3; i++) {
    await api(`/install/${woId}/photos`, { method: 'POST', token: inst.token, body: { url: `http://mock.local/ex${i}.jpg`, objectKey: `ex/${woId}/${i}.jpg` } })
  }
  const cp1 = await api(`/install/${woId}/complete`, { method: 'POST', token: inst.token, body: { orderId, lat: 39.9, lng: 116.4, distance: 300, remark: '异常场景完工' } })
  const cp2 = await api(`/install/${woId}/complete`, { method: 'POST', token: inst.token, body: { orderId, lat: 39.9, lng: 116.4, distance: 300, remark: '重复完工' } })
  record('complete OK', cp1.json.code === 0, `HTTP ${cp1.status} code=${cp1.json.code}`)
  record('complete an already-completed work order → rejected', cp2.json.code !== 0 && cp2.status !== 500, `HTTP ${cp2.status} code=${cp2.json.code}`)
  const woAfter = await api(`/work-orders/${woId}`, { token: audit.token })
  record('work order still COMPLETED after rejected double-complete', woAfter.json.data?.status === 'COMPLETED', `status=${woAfter.json.data?.status}`)

  // 3. concurrent dispatch of the same order — both must not corrupt state
  const phone2 = '134' + String(Date.now()).slice(-8)
  const cust2 = await api('/customers', { method: 'POST', token: cs.token, body: { name: '并发客户', phone: phone2, idCardNo: '110101199005055678', address: 'b', province: '北京', city: '北京市', district: '东城区' } })
  const ord2 = await api('/orders', { method: 'POST', token: cs.token, body: { customerId: cust2.json.data, packageCode: 'FIBER_200M', installAddress: 'b-101', contactPhone: phone2 } })
  const orderId2 = ord2.json.data
  await api(`/orders/${orderId2}/audit`, { method: 'POST', token: audit.token, body: { pass: true, remark: '并发场景审核' } })
  let woId2 = null
  for (let i = 0; i < 20; i++) {
    const wo = await api(`/work-orders/by-order/${orderId2}`, { token: audit.token })
    if (wo.json.code === 0 && wo.json.data?.id) { woId2 = wo.json.data.id; break }
    await sleep(500)
  }
  const d1p = api(`/dispatch/auto?orderId=${orderId2}`, { method: 'POST', token: disp.token })
  const d2p = api(`/dispatch/auto?orderId=${orderId2}`, { method: 'POST', token: disp.token })
  const [r1, r2] = await Promise.all([d1p, d2p])
  const wins = [r1, r2].filter((r) => r.json.code === 0)
  record('concurrent dispatch: ≥1 succeeds', wins.length >= 1, `r1=${r1.json.code} r2=${r2.json.code}`)
  record('concurrent dispatch: no 500', r1.status !== 500 && r2.status !== 500, `r1=${r1.status} r2=${r2.status}`)
  const wo2 = await api(`/work-orders/${woId2}`, { token: audit.token })
  record('order state still consistent after concurrent dispatch', ['DISPATCHED', 'ACCEPTED'].includes(wo2.json.data?.status), `status=${wo2.json.data?.status}`)

  // 4. token: logout revocation already covered in auth_e2e; add: garbage token on business API → 401
  const badTok = await fetch(BASE + '/work-orders/my?pageNum=1&pageSize=5', { headers: { Authorization: 'Bearer garbage.token.here' } })
  record('garbage token on business API → 401', badTok.status === 401, `HTTP ${badTok.status}`)

  const failed = results.filter((r) => !r.ok)
  console.log(`\n===== ${results.length - failed.length}/${results.length} PASSED =====`)
  process.exit(failed.length ? 1 : 0)
}

main().catch((e) => { console.error('SCRIPT ERROR:', e.message); process.exit(2) })
