import { login, api, db } from './lib.mjs';
const sleep = ms => new Promise(r => setTimeout(r, ms));

const T = {};
for (const n of ['admin', 'cs1', 'audit1', 'disp1', 'install1', 'install4', 'customer1']) T[n] = (await login(n)).token;

console.log('=== A. 客户自助下单流程（PENDING_CS_CONFIRM 能否流转）===');
const co = await api(T.customer1, 'POST', '/api/customer-portal/orders', {
  packageCode: 'PKG_100M', installAddress: '北京市朝阳区建国路1号',
  appointmentTime: '2026-09-14 14:00:00', contactPhone: '13900000001', remark: '客户自助测试'
});
console.log('  下单:', co.http, JSON.stringify(co.msg || ('orderId=' + co.data)));
let custOrder = null;
if (co.code === 0) {
  custOrder = co.data?.id || co.data;
  console.log('  DB:', db(`SELECT id,status,source,cs_id FROM broadband_order WHERE id=${custOrder}`)[0]?.join(' | '));
  // 客服尝试确认
  const cf = await api(T.cs1, 'POST', `/api/orders/${custOrder}/audit`, { pass: true, remark: '客服确认' });
  console.log('  客服确认:', cf.http, JSON.stringify(cf.msg || cf.code));
  console.log('  DB 确认后:', db(`SELECT status FROM broadband_order WHERE id=${custOrder}`)[0]?.[0]);
}

console.log('\n=== B. 改派测试 ===');
const dispResult = await api(T.disp1, 'POST', '/api/dispatch/manual', { orderId: custOrder, installerId: 6, reason: 'E2E手动派单' });
console.log('  手动派单(disp1→install1):', dispResult.http, JSON.stringify(dispResult.msg || ('wo=' + dispResult.data?.workOrderId)));
let wo = dispResult.data?.workOrderId;
if (wo) {
  console.log('  DB:', db(`SELECT id,status,installer_id FROM work_order WHERE id=${wo}`)[0]?.join(' | '));
  const re = await api(T.disp1, 'POST', `/api/dispatch/${wo}/reassign`, { newInstallerId: 7, reason: '改派测试' });
  console.log('  改派(→install2):', re.http, JSON.stringify(re.msg || re.code));
  console.log('  DB 改派后:', db(`SELECT id,status,installer_id FROM work_order WHERE id=${wo}`)[0]?.join(' | '));
}

console.log('\n=== C. 其他模块冒烟（GET 接口可访问性）===');
const smoke = [
  ['dashboard', '/api/dashboard/overview'],
  ['用户列表', '/api/users/page?pageNum=1&pageSize=5'],
  ['角色列表', '/api/roles'],
  ['菜单树', '/api/menus/tree'],
  ['部门树', '/api/depts/tree'],
  ['操作日志', '/api/logs/operation/page?pageNum=1&pageSize=5'],
  ['登录日志', '/api/logs/login/page?pageNum=1&pageSize=5'],
  ['通知消息', '/api/notify/messages/page?pageNum=1&pageSize=5'],
  ['通知模板', '/api/notify/templates'],
  ['资源-小区', '/api/resources/communities'],
  ['装维列表', '/api/installers/page?pageNum=1&pageSize=5'],
  ['派单规则', '/api/dispatch/rules/active'],
  ['考勤-我的', '/api/attendance/my'],
  ['考勤-今日', '/api/attendance/today'],
];
for (const [name, path] of smoke) {
  const r = await api(T.admin, 'GET', path);
  console.log(`  ${name.padEnd(10)} http=${r.http} code=${r.code} ${r.code === 0 ? 'OK' : JSON.stringify(r.msg || '')}`);
}
