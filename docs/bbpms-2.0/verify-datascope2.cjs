/**
 * P0-3 数据权限端到端验证 v2 — 按真实权限矩阵划分
 *
 * 接口权限（P0-2 已加固）：
 *   /api/orders/page      → order:view   （admin/cs/audit 有）
 *   /api/work-orders/page → workorder:view（admin/disp/audit/installer 有）
 *
 * 账号（密码 admin123，role 决定 data_scope，dept 决定归属）：
 *   admin   SUPER_ADMIN      scope=1 ALL          (dept1)
 *   cs1     CUSTOMER_SERVICE scope=4 SELF         (dept2)
 *   audit1  AUDITOR          scope=4 SELF         (dept2)
 *   disp1   DISPATCHER       scope=4 SELF         (dept2)
 *   disp2   DISPATCHER_DEPT  scope=2 DEPT         (dept3)
 *   audit2  AUDITOR_CHILD    scope=3 DEPT_AND_CHILD (dept3)
 *   disp4   DISPATCHER_DEPT  scope=2 DEPT         (dept5 ⊂ dept3)
 *
 * 订单数据 create_by：2010→12(dept3) 2020→11(dept3) 2030→13(dept4) 2040→14(dept5)
 * 工单数据 create_by：2010→12 2020→11 2030→13 2040→14（同订单）
 * 老数据 create_by=NULL（种子）= 设计上人人可见（逃生口，防回归）
 *
 * 期望：
 *   订单页(admin)    → 2010/2020/2030/2040 全见
 *   订单页(audit2)   → 2010/2020/2040 可见，2030 不可见（dept4 平行）
 *   订单页(cs1)      → 2010-2040 全不可见（SELF: create_by!=2 且非 NULL）
 *   订单页(audit1)   → 2010-2040 全不可见（SELF）
 *   工单页(admin)    → 全见
 *   工单页(disp2)    → 2010/2020 可见，2030/2040 不可见
 *   工单页(disp4)    → 2040 可见，2010/2020/2030 不可见（dept5 递归点）
 *   工单页(audit2)   → 2010/2020/2040 可见，2030 不可见
 *   工单页(disp1)    → 2010-2040 全不可见（SELF dept2）
 */
const crypto = require('node:crypto');
const BASE = 'http://localhost:8080';

async function getPublicKey() {
  const res = await fetch(`${BASE}/api/auth/public-key`);
  const json = await res.json();
  const b64 = json.data;
  if (!b64) throw new Error(`public-key failed: ${JSON.stringify(json).slice(0, 300)}`);
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
  if (json.code !== 0 && json.code !== 200) {
    throw new Error(`login ${username} failed: ${JSON.stringify(json)}`);
  }
  const token = json.data?.accessToken || json.data?.token;
  if (!token) throw new Error(`login ${username}: no token in ${JSON.stringify(json).slice(0, 300)}`);
  return token;
}

async function fetchPage(token, path, nameField) {
  const res = await fetch(`${BASE}${path}?page=1&size=100`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (res.status === 403) return { error: '403', names: [], raw: [] };
  const json = await res.json();
  if (json.code !== 0 && json.code !== 200) {
    throw new Error(`${path} failed: ${JSON.stringify(json).slice(0, 300)}`);
  }
  const records = json.data?.records ?? json.data?.list ?? [];
  return { error: null, names: records.map((r) => r[nameField]), raw: records };
}

const ORD = 'BBDEMO2026', WOD = 'WODEMO2026';
const O2010 = 'BBDEMO20260201', O2020 = 'BBDEMO20260202', O2030 = 'BBDEMO20260203', O2040 = 'BBDEMO20260204';
const W2010 = 'WODEMO20260201', W2020 = 'WODEMO20260202', W2030 = 'WODEMO20260203', W2040 = 'WODEMO20260204';

const cases = [
  // 订单页（order:view）
  { name: '订单页 admin   (ALL,  scope=1)',               user: 'admin',  path: '/api/orders/page', f: 'orderNo', mustSee: [O2010, O2020, O2030, O2040], mustNotSee: [] },
  { name: '订单页 audit2  (CHILD, scope=3)',              user: 'audit2', path: '/api/orders/page', f: 'orderNo', mustSee: [O2010, O2020, O2040], mustNotSee: [O2030] },
  { name: '订单页 cs1     (SELF,  scope=4)',              user: 'cs1',    path: '/api/orders/page', f: 'orderNo', mustSee: [], mustNotSee: [O2010, O2020, O2030, O2040] },
  { name: '订单页 audit1  (SELF,  scope=4)',              user: 'audit1', path: '/api/orders/page', f: 'orderNo', mustSee: [], mustNotSee: [O2010, O2020, O2030, O2040] },
  // 工单页（workorder:view）
  { name: '工单页 admin   (ALL,  scope=1)',               user: 'admin',  path: '/api/work-orders/page', f: 'workNo', mustSee: [W2010, W2020, W2030, W2040], mustNotSee: [] },
  { name: '工单页 disp2   (DEPT dept3,   scope=2)',       user: 'disp2',  path: '/api/work-orders/page', f: 'workNo', mustSee: [W2010, W2020], mustNotSee: [W2030, W2040] },
  { name: '工单页 disp4   (DEPT dept5,   scope=2)',       user: 'disp4',  path: '/api/work-orders/page', f: 'workNo', mustSee: [W2040], mustNotSee: [W2010, W2020, W2030] },
  // audit2(AUDITOR_CHILD) 无 workorder:view（P0-2 权限设计：审核员不看工单列表）
  // → 预期 403，验证此权限隔离不被数据权限改动破坏
  { name: '工单页 audit2  (权限隔离保持, 预期403)',       user: 'audit2', path: '/api/work-orders/page', f: 'workNo', expect403: true },
  { name: '工单页 disp1   (SELF dept2,   scope=4)',       user: 'disp1',  path: '/api/work-orders/page', f: 'workNo', mustSee: [], mustNotSee: [W2010, W2020, W2030, W2040] },
];

(async () => {
  let pass = 0, fail = 0;
  for (const c of cases) {
    try {
      const token = await login(c.user);
      const { error, names } = await fetchPage(token, c.path, c.f);
      if (c.expect403) {
        const ok = error === '403';
        console.log(`${ok ? 'PASS' : 'FAIL'}  ${c.name}`);
        console.log(`      actual=${error ?? '200(' + names.length + '条)'} expect=403`);
        ok ? pass++ : fail++;
        continue;
      }
      if (error) {
        console.log(`FAIL  ${c.name}: ${error} (接口权限不足或异常)`);
        fail++;
        continue;
      }
      const joined = names.join(' ');
      const missing = c.mustSee.filter((x) => !joined.includes(x));
      const leaked = c.mustNotSee.filter((x) => joined.includes(x));
      const ok = missing.length === 0 && leaked.length === 0;
      console.log(`${ok ? 'PASS' : 'FAIL'}  ${c.name}`);
      console.log(`      count=${names.length} missing=[${missing.join(',')}] leaked=[${leaked.join(',')}]`);
      ok ? pass++ : fail++;
    } catch (e) {
      console.log(`ERROR ${c.name}: ${e.message}`);
      fail++;
    }
  }
  console.log(`\nRESULT: ${pass}/${pass + fail} passed`);
  process.exit(fail ? 1 : 0);
})();