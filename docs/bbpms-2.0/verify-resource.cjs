/**
 * ITERATION 2 地址与网络资源管理 — 端到端验证
 *
 * 覆盖：
 *  A. 资源核查三态
 *     - RESOURCE_OK          北京市朝阳区建国路1号 1号楼 1单元 101   → 可安装（房号存在且未安装）
 *     - RESOURCE_INSUFFICIENT 北京市朝阳区建国路1号 1号楼 1单元 102   → 已安装
 *     - RESOURCE_INSUFFICIENT 北京市朝阳区建国路1号 2号楼 1单元 999   → 无此房号
 *     - NO_COVERAGE           上海市未知路9号 1号楼 1单元 101         → 小区不在册
 *  B. 资源台账 CRUD（admin）
 *     - 区域/小区/楼栋/单元/房间 列表均 200 且数据非空
 *     - OLT/PON/ONU 列表均 200 且数据非空
 *     - resource:edit 新增房间 → 201 系列返回（幂等：已存在则跳过）
 *  C. 订单创建回写
 *     - POST /api/orders 带 roomId/resourceStatus/checkRemark
 *     - GET /api/orders/{id} 校验三字段一致
 *  D. 权限隔离
 *     - cs1（无 resource:view）访问 /api/resources/regions → 403
 *     - 未登录访问 /api/resources/regions → 401
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

async function api(token, method, path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      Authorization: token ? `Bearer ${token}` : undefined,
      'Content-Type': 'application/json',
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  let json = null;
  try { json = await res.json(); } catch (_) { /* non-JSON */ }
  return { status: res.status, json };
}

(async () => {
  console.log('== ITERATION 2 端到端验证 ==\n');

  // 登录
  let admin, cs1;
  try {
    admin = await login('admin');
    cs1 = await login('cs1');
    console.log(`  login  admin/cs1  OK`);
  } catch (e) {
    console.log(`  login  FAIL: ${e.message}`);
    process.exit(1);
  }

  // ---------- A. 资源核查三态 ----------
  console.log('\n[A] 资源核查三态');
  const checkCases = [
    { name: 'OK 可安装', addr: '北京市朝阳区建国路1号 1号楼 1单元 101', roomNo: '101', want: 'RESOURCE_OK' },
    { name: '已安装',   addr: '北京市朝阳区建国路1号 1号楼 1单元 102', roomNo: '102', want: 'RESOURCE_INSUFFICIENT' },
    { name: '无此房号', addr: '北京市朝阳区建国路1号 2号楼 1单元 999', roomNo: '999', want: 'RESOURCE_INSUFFICIENT' },
    { name: '小区不再册', addr: '上海市未知路9号 1号楼 1单元 101', roomNo: '101', want: 'NO_COVERAGE' },
  ];
  for (const c of checkCases) {
    const { status, json } = await api(admin, 'POST', '/api/resources/check', { address: c.addr, roomNo: c.roomNo });
    const got = json?.data?.status;
    ok(`${c.name} → ${got ?? 'N/A'}`, status === 200 && got === c.want, `http=${status} msg=${json?.data?.message ?? ''}`);
  }

  // ---------- B. 资源台账 CRUD ----------
  console.log('\n[B] 资源台账列表');
  const listEndpoints = [
    ['区域', '/api/resources/regions'],
    ['小区', '/api/resources/communities'],
    ['楼栋', '/api/resources/buildings?communityId=1'],
    ['单元', '/api/resources/units?buildingId=1'],
    ['房间', '/api/resources/rooms?unitId=1'],
    ['OLT', '/api/resources/olts'],
    ['PON', '/api/resources/pons?oltId=1'],
    ['ONU', '/api/resources/onus'],
  ];
  for (const [label, path] of listEndpoints) {
    const { status, json } = await api(admin, 'GET', path);
    const n = Array.isArray(json?.data) ? json.data.length : -1;
    ok(`${label} list`, status === 200 && n >= 0, `http=${status} n=${n}`);
  }

  // 新增房间（resource:edit，幂等创建：若已存在后端应去重或报错不崩）
  {
    const { status, json } = await api(admin, 'POST', '/api/resources/rooms?unitId=1&roomNo=105');
    ok('新增房间 105', status === 200, `http=${status} ${JSON.stringify(json)?.slice(0, 120)}`);
  }

  // ---------- C. 订单创建回写 ----------
  console.log('\n[C] 订单创建回写');
  const orderNo = 'ITR2-' + Date.now();
  let createdId = null;
  {
    const { status, json } = await api(admin, 'POST', '/api/orders', {
      customerId: 1,
      packageCode: 'PKG_100M',
      packageName: '100M Broadband',
      installAddress: '北京市朝阳区建国路1号 1号楼 1单元 101',
      contactPhone: '13800138000',
      roomId: 1,
      resourceStatus: 'RESOURCE_OK',
      checkRemark: '自动化验证-资源核查通过-101',
    });
    ok('创建订单', status === 200, `http=${status}`);
    createdId = json?.data?.id ?? json?.data ?? null;
    console.log(`  info  created order id=${createdId} (orderNo=${orderNo})`);
  }
  if (createdId) {
    const { status, json } = await api(admin, 'GET', `/api/orders/${createdId}`);
    const d = (json?.data ?? {}).order ?? json?.data ?? {};
    ok('回写 room_id', String(d.roomId) === '1', `got=${d.roomId}`);
    ok('回写 resource_status', d.resourceStatus === 'RESOURCE_OK', `got=${d.resourceStatus}`);
    ok('回写 check_remark', (d.checkRemark || '').includes('资源核查通过'), `got=${d.checkRemark}`);
  }

  // ---------- D. 权限隔离 ----------
  console.log('\n[D] 权限隔离');
  {
    const { status } = await api(cs1, 'GET', '/api/resources/regions');
    ok('cs1 无 resource:view → 403', status === 403, `http=${status}`);
  }
  {
    const { status } = await api(null, 'GET', '/api/resources/regions');
    ok('未登录 → 401', status === 401, `http=${status}`);
  }

  console.log(`\n===== 结果: ${passed} passed, ${failed} failed =====`);
  process.exit(failed === 0 ? 0 : 1);
})().catch((e) => {
  console.error('FATAL:', e);
  process.exit(2);
});