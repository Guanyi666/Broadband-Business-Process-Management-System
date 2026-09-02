// BBPMS Phase 8 — API contract smoke: new/patched endpoints (installers, customers, roles, dispatch)
import crypto from 'node:crypto'

const BASE = 'http://localhost:8080/api'

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return { status: res.status, json: await res.json() }
}

function encryptPassword(pubKeyBase64, password) {
  const pem = `-----BEGIN PUBLIC KEY-----\n${pubKeyBase64}\n-----END PUBLIC KEY-----`
  return crypto.publicEncrypt({ key: pem, padding: crypto.constants.RSA_PKCS1_PADDING }, Buffer.from(password, 'utf8')).toString('base64')
}

const results = []
function record(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}${detail ? ' | ' + detail : ''}`)
}

async function loginAs(username, password = 'admin123') {
  const pk = await api('/auth/public-key')
  const enc = encryptPassword(pk.json.data, password)
  return api('/auth/login', { method: 'POST', body: { username, password: enc } })
}

async function main() {
  const login = await loginAs('admin')
  if (login.status !== 200 || login.json.code !== 0) {
    record('login', false, `HTTP ${login.status} ${JSON.stringify(login.json)}`)
    return
  }
  const token = login.json.data.accessToken
  const h = { token }

  // 1. GET /installers/page
  const page = await api('/installers/page?pageNum=1&pageSize=5', h)
  const rows = page.json?.data?.records || []
  record('GET /installers/page', page.json.code === 0 && Array.isArray(rows) && rows.length > 0, `total=${page.json.data.total} first=${JSON.stringify(rows[0] ? { id: rows[0].userId, name: rows[0].realName, onDuty: rows[0].onDuty } : null)}`)

  // 2. GET /installers/locations
  const locs = await api('/installers/locations', h)
  record('GET /installers/locations', locs.json.code === 0 && Array.isArray(locs.json.data), `count=${locs.json.data?.length}`)

  // 3. GET /installers/{id}/profile (first row from the page above)
  const firstId = rows[0]?.userId
  const prof = await api(`/installers/${firstId}/profile`, h)
  record('GET /installers/{id}/profile', prof.json.code === 0 && String(prof.json.data?.userId) === String(firstId) && prof.json.data?.username, `id=${firstId} username=${prof.json.data?.username} rating=${prof.json.data?.rating}`)

  // 4. GET /customers/search (Phase 5 e2e created "E2E客户" customers)
  const cust = await api('/customers/search?keyword=E2E&limit=5', h)
  record('GET /customers/search', cust.json.code === 0 && Array.isArray(cust.json.data) && cust.json.data.length > 0, `hits=${cust.json.data?.length}`)

  // 5. GET /customers/page (was empty placeholder — now real pagination)
  const cpage = await api('/customers/page?pageNum=1&pageSize=10', h)
  record('GET /customers/page (real data)', cpage.json.code === 0 && cpage.json.data.total > 0 && cpage.json.data.records.length > 0, `total=${cpage.json.data.total} rows=${cpage.json.data.records.length}`)

  // 6. GET /roles/page
  const rpage = await api('/roles/page?pageNum=1&pageSize=10', h)
  record('GET /roles/page', rpage.json.code === 0 && rpage.json.data.records.length >= 5, `total=${rpage.json.data.total} codes=${(rpage.json.data.records || []).map(r => r.code).join(',')}`)

  // 7. GET /dispatch/candidates?orderId= — contract: query param orderId (any existing order)
  const orders = await api('/orders/page?pageNum=1&pageSize=5', h)
  const anyOrder = orders.json?.data?.records?.[0]
  const cands = await api(`/dispatch/candidates?orderId=${anyOrder?.id || 0}&limit=5`, h)
  record('GET /dispatch/candidates?orderId=', cands.json.code === 0 && Array.isArray(cands.json.data), `order=${anyOrder?.id} code=${cands.json.code} count=${cands.json.data?.length}`)

  // 8. GET /dispatch/records/page
  const recs = await api('/dispatch/records/page?pageNum=1&pageSize=5', h)
  record('GET /dispatch/records/page', recs.json.code === 0, `total=${recs.json.data?.total}`)

  const failed = results.filter(r => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} passed`)
  process.exit(failed.length ? 1 : 0)
}

main().catch((e) => { console.error('FATAL', e); process.exit(1) })
