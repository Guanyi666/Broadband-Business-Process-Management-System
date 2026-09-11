import { login, api, db } from './lib.mjs';

const T = {};
for (const n of ['admin', 'cs1', 'cs2', 'audit1', 'disp1', 'disp2', 'install1', 'install2', 'customer1']) T[n] = (await login(n)).token;

console.log('=== 数据权限隔离测试 ===\n');
console.log('账号'.padEnd(10) + '订单可见'.padEnd(10) + '其中自己创建'.padEnd(14) + '工单可见'.padEnd(10) + '说明');
console.log('-'.repeat(80));

const totals = db(`SELECT (SELECT COUNT(*) FROM broadband_order WHERE deleted=0), (SELECT COUNT(*) FROM work_order WHERE deleted=0)`);
const [allOrders, allWos] = totals[0];
console.log(`[基准] 全库订单=${allOrders} 工单=${allWos}\n`);

for (const name of ['admin', 'cs1', 'cs2', 'audit1', 'disp1', 'disp2', 'install1', 'install2']) {
  const og = await api(T[name], 'GET', '/api/orders/page', undefined, { pageNum: 1, pageSize: 100 });
  const wg = await api(T[name], 'GET', '/api/work-orders/page', undefined, { pageNum: 1, pageSize: 100 });
  const oCnt = og.data?.total ?? `err(${og.code})`;
  const wCnt = wg.data?.total ?? `err(${wg.code})`;
  // 自己创建的订单数
  const own = db(`SELECT COUNT(*) FROM broadband_order WHERE deleted=0 AND create_by=(SELECT id FROM sys_user WHERE username='${name}')`);
  console.log(String(name).padEnd(10) + String(oCnt).padEnd(10) + String(own[0][0]).padEnd(14) + String(wCnt).padEnd(10) + (og.code === 0 ? '' : '订单接口:' + og.msg));
}

console.log('\n=== 装维 /my 队列（应只含自己的工单）===');
for (const name of ['install1', 'install2']) {
  const my = await api(T[name], 'GET', '/api/work-orders/my');
  const list = my.data?.records || [];
  const ids = [...new Set(list.map(w => w.installerId))];
  console.log(`${name.padEnd(9)} 工单数=${my.data?.total} 涉及负责人id=${JSON.stringify(ids)} ${ids.every(i => true) ? '' : ''}`);
}

console.log('\n=== 客户门户数据隔离（customer1 只能看自己的订单）===');
const po = await api(T.customer1, 'GET', '/api/customer-portal/orders');
const pol = po.data?.records || po.data || [];
console.log(`customer1 门户订单数=${Array.isArray(pol) ? pol.length : po.data?.total ?? 'err'} code=${po.code}`);
const custId = db(`SELECT id FROM sys_user WHERE username='customer1'`)[0]?.[0];
console.log(`(customer1 绑定 customerId=${db(`SELECT customer_id FROM customer_user_binding WHERE user_id=${custId}`)[0]?.[0]})`);
