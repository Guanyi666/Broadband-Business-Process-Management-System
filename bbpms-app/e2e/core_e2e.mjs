// BBPMS Phase 5 — core business chain E2E
// cs1 creates customer+order → audit1 approves → work order auto-created →
// installer goes online → disp1 auto-dispatches → installer accepts/starts/
// arrives/info/photos/signature/complete → BSS mock → verify final states.
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

async function main() {
  // 1. logins
  const cs = await login('cs1')
  const audit = await login('audit1')
  const disp = await login('disp1')
  const installers = {}
  for (const u of ['install1', 'install2', 'install3', 'install4', 'install5']) {
    const l = await login(u)
    installers[l.user.id] = { username: u, token: l.token }
  }
  record('5 role logins + 5 installer logins', true, `installer ids: ${Object.keys(installers).join(',')}`)

  // 2. create customer (upsert by phone → idempotent)
  const phone = '139' + String(Date.now()).slice(-8)
  const cust = await api('/customers', {
    method: 'POST', token: cs.token,
    body: { name: 'E2E客户', phone, idCardNo: '110101199001011234', address: '北京市朝阳区望京SOHO T3', province: '北京', city: '北京市', district: '朝阳区' }
  })
  const customerId = cust.json.data
  record('cs1 POST /api/customers → customerId', cust.status === 200 && cust.json.code === 0 && customerId, `customerId=${customerId}`)

  // 3. create order
  const ord = await api('/orders', {
    method: 'POST', token: cs.token,
    body: { customerId, packageCode: 'FIBER_100M', packageName: '光纤宽带100M', installAddress: '北京市朝阳区望京SOHO T3-1502', expectedInstallDate: '2026-09-03T10:00:00', appointmentTime: '2026-09-02T14:00:00', contactPhone: phone, remark: 'E2E链路测试订单' }
  })
  const orderId = ord.json.data
  record('cs1 POST /api/orders → orderId', ord.status === 200 && ord.json.code === 0 && orderId, `orderId=${orderId}`)

  // 4. installer goes online FIRST (writes installers:active ZSET) — the audit
  // dispatch listener creates the work order on audit only when candidates exist,
  // so going online must precede audit (SA-P2-004: WO creation is owned by dispatch).
  const installerId = Number(Object.keys(installers)[0])
  const loc = await api('/installers/location', {
    method: 'POST', token: installers[installerId].token,
    body: { userId: installerId, lat: 39.9087, lng: 116.3975, onDuty: 1 }
  })
  record(`install1 POST /installers/location (online)`, loc.status === 200 && loc.json.code === 0, `HTTP ${loc.status}`)

  // 5. audit pass → @Async listener auto-dispatches to the online installer
  const aud = await api(`/orders/${orderId}/audit`, {
    method: 'POST', token: audit.token, body: { pass: true, remark: 'E2E审核通过' }
  })
  record('audit1 POST /orders/{id}/audit pass', aud.status === 200 && aud.json.code === 0, `HTTP ${aud.status} code=${aud.json.code}`)

  // 6. poll for auto-created work order (@Async dispatch listener, ≤10s)
  let workOrderId = null
  for (let i = 0; i < 20; i++) {
    const wo = await api(`/work-orders/by-order/${orderId}`, { token: audit.token })
    if (wo.json.code === 0 && wo.json.data?.id) { workOrderId = wo.json.data.id; break }
    await sleep(500)
  }
  record('work order auto-created after audit (≤10s)', !!workOrderId, `workOrderId=${workOrderId}`)
  if (!workOrderId) { console.log('STOP: no work order'); return }

  // 7. auto dispatch (idempotent short-circuit — returns the same work order)
  const dispRes = await api(`/dispatch/auto?orderId=${orderId}`, { method: 'POST', token: disp.token })
  const d = dispRes.json.data || {}
  record('disp1 POST /dispatch/auto → workOrderId+installerId', dispRes.status === 200 && dispRes.json.code === 0 && d.workOrderId, `workOrderId=${d.workOrderId} installer=${d.installerName}(${d.installerId}) score=${d.score}`)
  const pickedId = d.installerId
  const picked = installers[pickedId]
  record('dispatched installer is a known seeded installer', !!picked, `username=${picked?.username}`)
  if (!picked) { console.log('STOP: unknown installer'); return }

  // 8. accept → start
  const acc = await api(`/work-orders/${workOrderId}/accept`, { method: 'POST', token: picked.token })
  record(`${picked.username} POST /work-orders/{id}/accept`, acc.status === 200 && acc.json.code === 0 && acc.json.data?.status === 'ACCEPTED', `status=${acc.json.data?.status}`)
  const st = await api(`/work-orders/${workOrderId}/start`, { method: 'POST', token: picked.token })
  record(`${picked.username} POST /work-orders/{id}/start`, st.status === 200 && st.json.code === 0 && st.json.data?.status === 'IN_PROGRESS', `status=${st.json.data?.status}`)

  // 9. arrive + info
  const ar = await api(`/install/${workOrderId}/arrive`, { method: 'POST', token: picked.token, body: { lat: 39.9087, lng: 116.3975, address: '望京SOHO T3-1502' } })
  record(`${picked.username} POST /install/{id}/arrive`, ar.status === 200 && ar.json.code === 0, `HTTP ${ar.status}`)
  const inf = await api(`/install/${workOrderId}/info`, { method: 'POST', token: picked.token, body: { onuMac: 'AA:BB:CC:DD:EE:FF', onuSn: 'SN-E2E-0001', oltPort: '0/1/1', signal: -18.5 } })
  record(`${picked.username} POST /install/{id}/info`, inf.status === 200 && inf.json.code === 0, `HTTP ${inf.status}`)

  // 10. photos ×3 (min count = 3)
  let photoOk = true
  for (let i = 1; i <= 3; i++) {
    const ph = await api(`/install/${workOrderId}/photos`, { method: 'POST', token: picked.token, body: { url: `http://mock.local/e2e/photo${i}.jpg`, objectKey: `e2e/${workOrderId}/photo${i}.jpg` } })
    if (ph.status !== 200 || ph.json.code !== 0) photoOk = false
  }
  record(`${picked.username} POST /install/{id}/photos ×3`, photoOk, '3 uploads')

  // 11. signature
  const sg = await api(`/install/${workOrderId}/signature`, { method: 'POST', token: picked.token, body: { customerName: 'E2E客户', dataUrl: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', objectKey: `e2e/${workOrderId}/sign.png` } })
  record(`${picked.username} POST /install/{id}/signature`, sg.status === 200 && sg.json.code === 0, `HTTP ${sg.status}`)

  // 12. complete (BSS mock fires)
  const cp = await api(`/install/${workOrderId}/complete`, {
    method: 'POST', token: picked.token,
    body: { orderId, lat: 39.9087, lng: 116.3975, distance: 520, remark: 'E2E完工' }
  })
  record(`${picked.username} POST /install/{id}/complete`, cp.status === 200 && cp.json.code === 0, `HTTP ${cp.status} code=${cp.json.code}`)

  // 13. final states
  const wo = await api(`/work-orders/${workOrderId}`, { token: audit.token })
  record('work order final status = COMPLETED', wo.json.data?.status === 'COMPLETED', `status=${wo.json.data?.status}`)
  const order = await api(`/orders/${orderId}`, { token: audit.token })
  // OrderDetailVO is nested: { order, customer, appointment, timeline }
  record('order final status = FINISHED', order.json.data?.order?.status === 'FINISHED', `status=${order.json.data?.order?.status}`)
  // audit1 lacks install:view; admin is the verification account
  const admin = await login('admin')
  const ir = await api(`/install/by-work-order/${workOrderId}`, { token: admin.token })
  record('install record COMPLETED + photos/signature persisted', ir.json.data?.status === 'COMPLETED' && ir.json.data?.signatureUrl, `status=${ir.json.data?.status} sig=${!!ir.json.data?.signatureUrl}`)
  const dr = await api(`/dispatch/records/page?pageNum=1&pageSize=5`, { token: disp.token })
  const foundDr = (dr.json.data?.records || []).some((x) => x.workOrderId === workOrderId)
  record('dispatch_record persisted', dr.status === 200 && foundDr, `records=${(dr.json.data?.records || []).length}`)

  const failed = results.filter((r) => !r.ok)
  console.log(`\n===== ${results.length - failed.length}/${results.length} PASSED =====`)
  console.log(`orderId=${orderId} workOrderId=${workOrderId} installer=${picked.username}`)
  process.exit(failed.length ? 1 : 0)
}

main().catch((e) => { console.error('SCRIPT ERROR:', e.message); process.exit(2) })
