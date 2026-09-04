/**
 * Dashboard 权限门控验证（0904 修复）
 *
 * 验证目标：
 *  A. 装维账号（install1，有 dashboard:view 但无 dispatch:view / workorder:sla:view /
 *     installer:view / attendance:view-all / leave:approve*）访问 Dashboard 相关接口：
 *     - 有权限接口（overview/trend/dispatch-duration/workorders page）→ 200
 *     - 无权限接口（dispatch/stats、work-orders/expiring、installers/online、
 *       attendance/on-duty、leave/pending-approvals）→ 403（前端应据此隐藏模块，不发请求）
 *  B. 有权限账号（disp1 调度员）访问同一批接口 → 全部 200（回归）
 *
 * 前端行为（代码层面验证）：useDashboard 每个模块带 requirePerm 门控，
 *   无权限 → permitted=false → 不发请求 → index.vue 隐藏对应面板；catch 403 → 静默降级。
 */
const crypto = require('node:crypto');
const BASE = 'http://localhost:8080';

let passed = 0, failed = 0;
function ok(name, cond, extra = '') {
  if (cond) { passed++; console.log(`  PASS  ${name}${extra ? '  ' + extra : ''}`); }
  else { failed++; console.log(`  FAIL  ${name}${extra ? '  ' + extra : ''}`); }
}

async function getPublicKey() {
  const res = await fetch(`${BASE}/api/auth/public-key`);
  const json = await res.json();
  const b64 = json.data;
  if (!b64) throw new Error(`public-key failed: ${JSON.stringify(json).slice(0, 200)}`);
  return crypto.createPublicKey({ key: Buffer.from(b64, 'base64'), format: 'der', type: 'spki' });
}

async function login(username, password = 'admin123') {
  const pubKey = await getPublicKey();
  const enc = crypto.publicEncrypt({ key: pubKey, padding: crypto.constants.RSA_PKCS1_PADDING }, Buffer.from(password, 'utf8'));
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: enc.toString('base64') }),
  });
  const json = await res.json();
  if (json.code !== 0 && json.code !== 200) throw new Error(`login ${username} failed: ${JSON.stringify(json).slice(0, 200)}`);
  const token = json.data?.accessToken || json.data?.token;
  if (!token) throw new Error(`login ${username}: no token`);
  return token;
}

async function hit(token, path) {
  const res = await fetch(`${BASE}${path}`, { headers: { Authorization: `Bearer ${token}` } });
  return res.status;
}

(async () => {
  console.log('== Dashboard 权限门控验证 ==\n');

  // 登录：install1（装维/无 dispatch/sla/counts 权限）、disp1（调度员/有权限）
  let install1, disp1;
  try {
    install1 = await login('install1');
    disp1 = await login('disp1');
    console.log('  login install1/disp1 OK');
  } catch (e) {
    console.log(`  login FAIL: ${e.message}`);
    process.exit(1);
  }

  // ---------- A. 装维账号：Dashboard 各模块接口 ----------
  console.log('\n[A] 装维账号 install1（应 200 的看板数据接口 + 403 的无权限模块接口）');

  // 有 dashboard:view 的模块 → 200
  const allowedEndpoints = [
    ['今日态势/趋势/时效（dashboard:view）', '/api/dashboard/overview?days=7'],
    ['趋势（dashboard:view）', '/api/dashboard/trend?days=7'],
    ['派单时效（dashboard:view）', '/api/dashboard/dispatch-duration?days=7'],
    ['最新工单动态（workorder:view）', '/api/work-orders/page?pageNum=1&pageSize=8']
  ];
  for (const [label, path] of allowedEndpoints) {
    const s = await hit(install1, path);
    ok(`install1 ${label} → ${s}`, s === 200, `http=${s}`);
  }

  // 无权限模块 → 403（前端据此隐藏，不发请求/静默）
  const deniedEndpoints = [
    ['派单策略构成+评分（dispatch:view）', '/api/dispatch/stats?days=7'],
    ['SLA 临期预警（workorder:sla:view）', '/api/work-orders/expiring?minutes=30'],
    ['在线装维（installer:view）', '/api/installers/online'],
    ['在岗人员（attendance:view-all）', '/api/attendance/on-duty'],
    ['待审批请假（leave:approve*）', '/api/leave/pending-approvals']
  ];
  for (const [label, path] of deniedEndpoints) {
    const s = await hit(install1, path);
    ok(`install1 ${label} → ${s}（期望403）`, s === 403, `http=${s}`);
  }

  // ---------- B. 调度员账号回归 ----------
  console.log('\n[B] 调度员账号 disp1（应有全权限，全部 200）');
  const allEndpoints = [
    ['overview', '/api/dashboard/overview?days=7'],
    ['trend', '/api/dashboard/trend?days=7'],
    ['dispatch-duration', '/api/dashboard/dispatch-duration?days=7'],
    ['workorders page', '/api/work-orders/page?pageNum=1&pageSize=8'],
    ['dispatch stats', '/api/dispatch/stats?days=7'],
    ['work-orders expiring', '/api/work-orders/expiring?minutes=30'],
    ['installers online', '/api/installers/online'],
    ['attendance on-duty', '/api/attendance/on-duty'],
    ['leave pending-approvals', '/api/leave/pending-approvals']
  ];
  for (const [label, path] of allEndpoints) {
    const s = await hit(disp1, path);
    ok(`disp1 ${label} → ${s}`, s === 200, `http=${s}`);
  }

  // ---------- C. 装维账号权限码构成确认（perms 快照） ----------
  console.log('\n[C] 权限码快照（install1 应含 dashboard:view / workorder:view，不含 dispatch:view 等）');
  {
    const res = await fetch(`${BASE}/api/auth/me`, { headers: { Authorization: `Bearer ${install1}` } });
    const json = await res.json();
    const perms = json.data?.permissions ?? json.data?.perms ?? [];
    const roles = json.data?.roles ?? [];
    ok('install1 含 dashboard:view', perms.includes('dashboard:view'));
    ok('install1 含 workorder:view', perms.includes('workorder:view'));
    ok('install1 不含 dispatch:view', !perms.includes('dispatch:view'));
    ok('install1 不含 workorder:sla:view', !perms.includes('workorder:sla:view'));
    console.log(`  info  roles=${JSON.stringify(roles)} permsCount=${perms.length}`);
  }

  console.log(`\n===== 结果: ${passed} passed, ${failed} failed =====`);
  process.exit(failed === 0 ? 0 : 1);
})().catch((e) => {
  console.error('FATAL:', e);
  process.exit(2);
});