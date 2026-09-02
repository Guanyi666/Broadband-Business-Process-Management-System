// cs1 (CUSTOMER_SERVICE) attempts admin-only endpoints → expect 403
const BASE = 'http://localhost:8080/api'
import crypto from 'node:crypto'

async function login(u) {
  const pk = await (await fetch(BASE + '/auth/public-key')).json()
  const enc = crypto.publicEncrypt(
    { key: `-----BEGIN PUBLIC KEY-----\n${pk.data}\n-----END PUBLIC KEY-----`, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from('admin123')
  ).toString('base64')
  const r = await (await fetch(BASE + '/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: enc }) })).json()
  return r.data.accessToken
}

const cs = await login('cs1')
const admin = await login('admin')

async function call(path, token, method = 'GET') {
  const res = await fetch(BASE + path, { method, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } })
  return { status: res.status, body: await res.json() }
}

let pass = 0, fail = 0
const check = (name, cond, detail) => { cond ? (pass++, console.log('PASS |', name, '|', detail)) : (fail++, console.log('FAIL |', name, '|', detail)) }

// cs1 has no system:user:view → 403
const u1 = await call('/users/page?pageNum=1&pageSize=10', cs)
check('cs1 GET /api/users/page → 403', u1.status === 403, `HTTP ${u1.status} code=${u1.body.code}`)
// cs1 has no order:audit → 403.
// NOTE: must send a VALID body — Spring MVC resolves+validates @RequestBody
// (→ 400 on empty body) BEFORE the @PreAuthorize AOP check runs.
const u2 = await fetch(BASE + '/orders/999/audit', {
  method: 'POST',
  headers: { Authorization: `Bearer ${cs}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ pass: true, remark: 'perm test' })
})
const u2body = await u2.json()
check('cs1 POST /api/orders/999/audit → 403', u2.status === 403, `HTTP ${u2.status} code=${u2body.code}`)
// admin CAN access users
const u3 = await call('/users/page?pageNum=1&pageSize=5', admin)
check('admin GET /api/users/page → 200', u3.status === 200 && u3.body.code === 0, `HTTP ${u3.status} total=${u3.body.data?.total}`)
// install1 (INSTALLER) cannot access users page either
const inst = await login('install1')
const u4 = await call('/users/page?pageNum=1&pageSize=5', inst)
check('install1 GET /api/users/page → 403', u4.status === 403, `HTTP ${u4.status} code=${u4.body.code}`)
// install1 CAN access own work orders
const u5 = await call('/work-orders/my', inst)
check('install1 GET /api/work-orders/my → 200', u5.status === 200 && u5.body.code === 0, `HTTP ${u5.status}`)

console.log(`\n===== ${pass}/${pass + fail} PASSED =====`)
process.exit(fail ? 1 : 0)
