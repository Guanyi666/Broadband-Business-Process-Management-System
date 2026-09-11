import { login, api } from './lib.mjs';

const cs1 = (await login('cs1')).token;
const admin = (await login('admin')).token;
const disp1 = (await login('disp1')).token;
const orderId = '2098424149193383938';

console.log('=== 按订单查工单 /api/work-orders/by-order/{id} 权限验证 ===');
for (const [name, tk] of [['cs1', cs1], ['disp1', disp1], ['admin', admin]]) {
  const r = await api(tk, 'GET', `/api/work-orders/by-order/${orderId}`);
  console.log(`${name.padEnd(6)} http=${r.http} code=${r.code} msg=${r.msg || ''} data=${r.data ? (r.data.workNo + '/' + r.data.status + '/装维' + r.data.installerId) : 'null'}`);
}

console.log('\n=== 工单详情 /api/work-orders/{id} ===');
const woId = '2098424151311507458';
for (const [name, tk] of [['cs1', cs1], ['install4', (await login('install4')).token], ['admin', admin]]) {
  const r = await api(tk, 'GET', `/api/work-orders/${woId}`);
  console.log(`${name.padEnd(8)} http=${r.http} code=${r.code} msg=${r.msg || ''} status=${r.data?.status || '-'}`);
}
