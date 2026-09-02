// BBPMS Phase 4 — Auth chain E2E (login → me → menus → refresh → logout → 401/403)
import crypto from 'node:crypto'

const BASE = 'http://localhost:8080/api/auth'

async function api(path, { method = 'GET', body, token, raw = false } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })
  if (raw) return { status: res.status, text: await res.text() }
  return { status: res.status, json: await res.json() }
}

function encryptPassword(pubKeyBase64, password) {
  // Backend returns the raw DER/base64 public key; wrap into PEM for Node.
  const pem = `-----BEGIN PUBLIC KEY-----\n${pubKeyBase64}\n-----END PUBLIC KEY-----`
  return crypto.publicEncrypt(
    { key: pem, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(password, 'utf8')
  ).toString('base64')
}

const results = []
function record(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}${detail ? ' | ' + detail : ''}`)
}

async function loginAs(username, password = 'admin123') {
  const pk = await api('/public-key')
  if (pk.status !== 200) throw new Error(`public-key HTTP ${pk.status}`)
  const pubKey = pk.json.data
  const enc = encryptPassword(pubKey, password)
  const resp = await api('/login', { method: 'POST', body: { username, password: enc } })
  return resp
}

async function main() {
  // 0. public key (no token)
  const pk = await api('/public-key')
  record('GET /api/auth/public-key (anon)', pk.status === 200 && typeof pk.json.data === 'string' && pk.json.data.length > 100, `HTTP ${pk.status}`)

  // 1. login as admin
  const adminLogin = await loginAs('admin')
  const ok1 = adminLogin.status === 200 && adminLogin.json.code === 0 && adminLogin.json.data?.accessToken && adminLogin.json.data?.refreshToken
  record('POST /api/auth/login admin', ok1, `HTTP ${adminLogin.status} code=${adminLogin.json.code}`)
  if (!ok1) return
  const { accessToken, refreshToken, token, user } = adminLogin.json.data
  record('login resp has token alias + user', !!token && !!user, `token=${!!token} user=${JSON.stringify(user)}`)
  record('login resp user.roles', Array.isArray(user?.roles) && user.roles.includes('SUPER_ADMIN'), `roles=${JSON.stringify(user?.roles)}`)

  // 2. /me
  const me = await api('/me', { token: accessToken })
  record('GET /api/auth/me', me.status === 200 && me.json.data?.id, `username=${me.json.data?.username}`)

  // 3. /menus
  const menus = await api('/menus', { token: accessToken })
  record('GET /api/auth/menus', menus.status === 200 && Array.isArray(menus.json.data), `count=${menus.json.data?.length}`)

  // 4. no token → 401
  const noToken = await api('/me')
  record('GET /api/auth/me without token → 401', noToken.status === 401, `HTTP ${noToken.status} code=${noToken.json?.code}`)

  // 5. bad token → 401
  const badToken = await api('/me', { token: 'garbage.jwt.token' })
  record('GET /api/auth/me with bad token → 401', badToken.status === 401, `HTTP ${badToken.status}`)

  // 6. refresh rotation
  const r1 = await api(`/refresh?refreshToken=${encodeURIComponent(refreshToken)}`, { method: 'POST' })
  const ok6 = r1.status === 200 && r1.json.code === 0 && r1.json.data?.accessToken
  record('POST /api/auth/refresh (first use)', ok6, `HTTP ${r1.status}`)
  const rotatedRefresh = r1.json.data?.refreshToken

  // 7. refresh replay → must fail
  const r2 = await api(`/refresh?refreshToken=${encodeURIComponent(refreshToken)}`, { method: 'POST' })
  record('POST /api/auth/refresh (replay old) → rejected', r2.status === 401 || r2.json?.code !== 0, `HTTP ${r2.status} code=${r2.json?.code}`)

  // 8. new access token works, old one revoked?
  const me2 = await api('/me', { token: r1.json.data.accessToken })
  record('GET /api/auth/me with rotated access', me2.status === 200, `HTTP ${me2.status}`)
  const meOld = await api('/me', { token: accessToken })
  record('old access after rotation (may still work until expiry)', meOld.status === 200 || meOld.status === 401, `HTTP ${meOld.status}`)

  // 9. logout revokes
  const lo = await api('/logout', { method: 'POST', token: r1.json.data.accessToken })
  record('POST /api/auth/logout', lo.status === 200 && lo.json.code === 0, `HTTP ${lo.status}`)
  const meAfterLogout = await api('/me', { token: r1.json.data.accessToken })
  record('GET /api/auth/me after logout → 401', meAfterLogout.status === 401, `HTTP ${meAfterLogout.status} code=${meAfterLogout.json?.code}`)

  // 10. per-role login: cs1 / audit1 / disp1 / install1
  for (const u of ['cs1', 'audit1', 'disp1', 'install1']) {
    const l = await loginAs(u)
    const ok = l.status === 200 && l.json.code === 0
    record(`POST /api/auth/login ${u}`, ok, `HTTP ${l.status} roles=${JSON.stringify(l.json.data?.user?.roles)}`)
  }

  // 11. wrong password
  const bad = await loginAs('admin', 'wrongpass')
  record('login wrong password → rejected', bad.status === 401 || bad.json?.code !== 0, `HTTP ${bad.status} code=${bad.json?.code} msg=${bad.json?.msg}`)

  // 12. nonexistent user
  const nope = await loginAs('ghost')
  record('login ghost user → rejected', nope.status === 401 || nope.json?.code !== 0, `HTTP ${nope.status} code=${nope.json?.code}`)

  const failed = results.filter((r) => !r.ok)
  console.log(`\n===== ${results.length - failed.length}/${results.length} PASSED =====`)
  process.exit(failed.length ? 1 : 0)
}

main().catch((e) => {
  console.error('SCRIPT ERROR:', e.message)
  process.exit(2)
})
