import { login, api, db } from './lib.mjs';
const sleep = ms => new Promise(r => setTimeout(r, ms));

const T = {};
for (const n of ['cs1', 'audit1', 'admin', 'install1', 'install2', 'disp1', 'cs2']) T[n] = (await login(n)).token;

console.log('=== A. 已完成工单的非法操作 ===');
const doneWo = '2098424591407243266'; // 上一轮已 COMPLETED
for (const [op, path] of [['再接单', `/api/work-orders/${doneWo}/accept`], ['再开始', `/api/work-orders/${doneWo}/start`], ['再完成', `/api/work-orders/${doneWo}/complete`]]) {
  const r = await api(T.install1, 'POST', path);
  console.log(`  ${op}: http=${r.http} code=${r.code} msg=${JSON.stringify(r.msg || '')}`);
}

console.log('\n=== B. 已完成订单的非法操作 ===');
const doneOrder = '2098424590002151427';
const r1 = await api(T.audit1, 'POST', `/api/orders/${doneOrder}/audit`, { pass: true, remark: 'x' });
console.log(`  再审核: http=${r1.http} code=${r1.code} msg=${JSON.stringify(r1.msg || '')}`);
const r2 = await api(T.cs1, 'POST', `/api/orders/${doneOrder}/cancel`, { reason: 'x' });
console.log(`  再取消: http=${r2.http} code=${r2.code} msg=${JSON.stringify(r2.msg || '')}`);

console.log('\n=== C. 越权操作测试（核心）===');
// 新建订单 → 审核 → 自动派单
const cr = await api(T.cs1, 'POST', '/api/orders', { customerId: 2, packageCode: 'PKG_100M', installAddress: '北京市海淀区越权测试1号' });
const oid = cr.data;
await api(T.audit1, 'POST', `/api/orders/${oid}/audit`, { pass: true, remark: '越权测试' });
let wo = null;
for (let i = 0; i < 12 && !wo; i++) { await sleep(700); wo = (await api(T.admin, 'GET', `/api/work-orders/by-order/${oid}`)).data; }
console.log(`  新工单 ${wo.id} 状态=${wo.status} 负责人=${wo.installerName}(id=${wo.installerId})`);

const ownerName = { 6: 'install1', 7: 'install2', 8: 'install3', 9: 'install4', 10: 'install5' }[wo.installerId];
const otherName = wo.installerId === '6' ? 'install2' : 'install1';
console.log(`  工单负责人=${ownerName}，尝试用【非负责人 ${otherName}】接单...`);
const hijack = await api(T[otherName], 'POST', `/api/work-orders/${wo.id}/accept`);
console.log(`  ▶ 非负责人接单结果: http=${hijack.http} code=${hijack.code} msg=${JSON.stringify(hijack.msg || hijack.data?.status || '')}`);
const d = db(`SELECT id, status, installer_id, accept_time FROM work_order WHERE id=${wo.id}`);
console.log(`  ▶ DB 实际: ${d[0]?.join(' | ')}`);
if (hijack.code === 0) console.log('  🔴 越权成功：非负责人可以接单他人工单！');

console.log('\n=== D. 待派发工单直接完成（跳过接单/施工）===');
// 建一个 PENDING 工单：客服创建订单但不派单 → 需人工建 PENDING。改测：对 DISPATCHED 工单直接 complete
console.log(`  当前工单状态=${(await api(T.admin,'GET',`/api/work-orders/by-order/${oid}`)).data?.status}，直接 complete:`, );
const skipR = await api(T[otherName], 'POST', `/api/work-orders/${wo.id}/complete`);
console.log(`  ▶ DISPATCHED 直接完成: http=${skipR.http} code=${skipR.code} msg=${JSON.stringify(skipR.msg || '')}`);
const d2 = db(`SELECT status FROM work_order WHERE id=${wo.id}`);
console.log(`  ▶ DB: ${d2[0]?.[0]}`);
