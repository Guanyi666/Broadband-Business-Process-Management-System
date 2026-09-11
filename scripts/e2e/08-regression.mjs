import { login, api, db } from './lib.mjs';
const sleep = ms => new Promise(r => setTimeout(r, ms));
const instName = id => ({ 6: 'install1', 7: 'install2', 8: 'install3', 9: 'install4', 10: 'install5' }[id] || '?');

const T = {};
for (const n of ['cs1', 'audit1', 'admin', 'install1', 'install2']) T[n] = (await login(n)).token;

console.log('=== 回归1：端到端链路（验证订单状态全程同步）===');
const cr = await api(T.cs1, 'POST', '/api/orders', { customerId: 1, packageCode: 'PKG_300M', installAddress: '北京市朝阳区回归验证1号' });
const oid = cr.data;
console.log(`创建订单 ${oid}`);
await api(T.audit1, 'POST', `/api/orders/${oid}/audit`, { pass: true, remark: '回归' });
let wo = null;
for (let i = 0; i < 12 && !wo; i++) { await sleep(700); wo = (await api(T.admin, 'GET', `/api/work-orders/by-order/${oid}`)).data; }
const snap = async (tag) => {
  const o = db(`SELECT status FROM broadband_order WHERE id=${oid}`)[0]?.[0];
  const w = db(`SELECT status FROM work_order WHERE id=${wo.id}`)[0]?.[0];
  console.log(`  [${tag}] 订单=${o} | 工单=${w}`);
};
await snap('审核+自动派单后');
const owner = instName(wo.installerId);
const tOwner = (await login(owner)).token;
await api(tOwner, 'POST', `/api/work-orders/${wo.id}/accept`); await snap('接单后');
await api(tOwner, 'POST', `/api/work-orders/${wo.id}/start`); await snap('开始施工后');
await api(tOwner, 'POST', `/api/work-orders/${wo.id}/complete`); await snap('完工后');
const ot = db(`SELECT from_status, to_status FROM order_audit_log WHERE order_id=${oid} ORDER BY id`);
console.log('  订单状态链:', ot.map(r => `${r[0] || 'NULL'}→${r[1]}`).join(' → '));

console.log('\n=== 回归2：越权修复验证（非负责人接单必须被拒）===');
const cr2 = await api(T.cs1, 'POST', '/api/orders', { customerId: 2, packageCode: 'PKG_100M', installAddress: '北京市海淀区回归越权2号' });
const oid2 = cr2.data;
await api(T.audit1, 'POST', `/api/orders/${oid2}/audit`, { pass: true, remark: '回归越权' });
let wo2 = null;
for (let i = 0; i < 12 && !wo2; i++) { await sleep(700); wo2 = (await api(T.admin, 'GET', `/api/work-orders/by-order/${oid2}`)).data; }
const owner2 = instName(wo2.installerId);
const other2 = wo2.installerId === '6' ? 'install2' : 'install1';
console.log(`  工单${wo2.id} 负责人=${owner2}，非负责人 ${other2} 尝试接单：`);
const hj = await api(T[other2], 'POST', `/api/work-orders/${wo2.id}/accept`);
console.log(`  ▶ http=${hj.http} code=${hj.code} msg=${JSON.stringify(hj.msg || '')}`);
const st = db(`SELECT status FROM work_order WHERE id=${wo2.id}`)[0]?.[0];
console.log(`  ▶ 工单状态=${st}（应仍为 DISPATCHED）${hj.code !== 0 && st === 'DISPATCHED' ? ' ✅ 越权已阻断' : ' ❌ 仍可越权'}`);
// 负责人本人应能接单
const ok = await api(T[owner2], 'POST', `/api/work-orders/${wo2.id}/accept`);
console.log(`  ▶ 负责人本人接单: code=${ok.code} ${ok.code === 0 ? '✅' : JSON.stringify(ok.msg)}`);

console.log('\n=== 回归3：不存在的接口应返回 404 ===');
const nf = await api(T.admin, 'GET', '/api/not-exist-endpoint');
console.log(`  http=${nf.http} code=${nf.code} msg=${JSON.stringify(nf.msg || '')}`);
const nf2 = await api(T.admin, 'GET', '/api/resources/page');
console.log(`  错误路径 http=${nf2.http} code=${nf2.code} ${nf2.http === 404 ? '✅ 已返回404' : '❌'}`);
