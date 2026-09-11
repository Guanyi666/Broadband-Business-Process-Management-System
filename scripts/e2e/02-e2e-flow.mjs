import { login, api, db } from './lib.mjs';

const sleep = ms => new Promise(r => setTimeout(r, ms));
const installerName = id => ({ 6: 'install1', 7: 'install2', 8: 'install3', 9: 'install4', 10: 'install5' }[id] || `user${id}`);

const t = {};
for (const n of ['cs1', 'audit1', 'admin']) t[n] = (await login(n)).token;
console.log('登录 OK\n');

// ---------- 1. 客服创建订单 ----------
const createR = await api(t.cs1, 'POST', '/api/orders', {
  customerId: 1, packageCode: 'PKG_300M', installAddress: '北京市朝阳区测试链路1号',
  appointmentTime: '2026-09-13T10:00:00', contactPhone: '13900000001', remark: 'E2E测试'
});
console.log('[1] 创建订单 (cs1):', createR.http, createR.code === 0 ? 'OK id=' + createR.data : JSON.stringify(createR));
const orderId = createR.data;
if (!orderId) process.exit(1);

let d = db(`SELECT id,status,cs_id,package_code,install_address FROM broadband_order WHERE id=${orderId}`);
console.log('    DB:', d[0]?.join(' | '));

// ---------- 2. 审核通过 ----------
const auditR = await api(t.audit1, 'POST', `/api/orders/${orderId}/audit`, { pass: true, remark: 'E2E审核通过' });
console.log('\n[2] 审核通过 (audit1):', auditR.http, JSON.stringify(auditR.msg || auditR.code));
await sleep(2500); // 等异步自动派单
d = db(`SELECT id,status,auditor_id FROM broadband_order WHERE id=${orderId}`);
console.log('    DB 订单:', d[0]?.join(' | '));

// ---------- 3. 自动派单结果（异步事件，轮询等待） ----------
let wo = null, woR = null;
for (let i = 0; i < 12 && !wo; i++) {
  await sleep(700);
  woR = await api(t.admin, 'GET', `/api/work-orders/by-order/${orderId}`);
  wo = woR.data;
}
console.log('\n[3] 自动派单 (事件异步):', wo ? `工单id=${wo.id} 状态=${wo.status} 装维=${wo.installerName}(id=${wo.installerId})` : `未生成工单! http=${woR?.http} msg=${woR?.msg}`);
if (!wo) { console.log('❌ 派单失败，终止'); process.exit(1); }
d = db(`SELECT id,status,installer_id,dispatch_time FROM work_order WHERE order_id=${orderId}`);
console.log('    DB 工单:', d.map(r => r.join(' | ')).join(' ; '));
d = db(`SELECT status FROM broadband_order WHERE id=${orderId}`);
console.log('    订单状态:', d[0]?.[0]);

// ---------- 4. 装维接单 ----------
const instUser = installerName(wo.installerId);
const tInst = (await login(instUser)).token;
const acceptR = await api(tInst, 'POST', `/api/work-orders/${wo.id}/accept`);
console.log(`\n[4] 接单 (${instUser}):`, acceptR.http, JSON.stringify(acceptR.msg || acceptR.data?.status));
d = db(`SELECT status,accept_time FROM work_order WHERE id=${wo.id}`);
console.log('    DB:', d[0]?.join(' | '));

// ---------- 5. 开始施工 ----------
const startR = await api(tInst, 'POST', `/api/work-orders/${wo.id}/start`);
console.log(`\n[5] 开始施工 (${instUser}):`, startR.http, JSON.stringify(startR.msg || startR.data?.status));
d = db(`SELECT status,start_time FROM work_order WHERE id=${wo.id}`);
console.log('    DB:', d[0]?.join(' | '));
d = db(`SELECT status FROM broadband_order WHERE id=${orderId}`);
console.log('    订单状态:', d[0]?.[0]);

// ---------- 6. 完成工单 ----------
const compR = await api(tInst, 'POST', `/api/work-orders/${wo.id}/complete`);
console.log(`\n[6] 完成工单 (${instUser}):`, compR.http, JSON.stringify(compR.msg || compR.code));
d = db(`SELECT status,finish_time FROM work_order WHERE id=${wo.id}`);
console.log('    DB 工单:', d[0]?.join(' | '));
d = db(`SELECT status,completed_time FROM broadband_order WHERE id=${orderId}`);
console.log('    DB 订单:', d[0]?.join(' | '));

// ---------- 7. 时间线 ----------
const tl = await api(t.admin, 'GET', `/api/work-orders/${wo.id}`);
console.log('\n[7] 工单时间线:');
for (const e of (tl.data?.timeline || [])) console.log(`    ${e.eventTime} | ${e.fromStatus ?? '-'}→${e.toStatus ?? '-'} | ${e.operatorName}(${e.operatorRole}) | ${e.remark || ''}`);
const otl = await api(t.cs1, 'GET', `/api/orders/${orderId}/timeline`);
console.log('    订单时间线:', (otl.data || []).map(e => `${e.eventType}(${e.operatorName || '-'})`).join(' → '));

console.log(`\n=== 测试订单 orderId=${orderId} 工单id=${wo.id} 装维=${instUser} ===`);
