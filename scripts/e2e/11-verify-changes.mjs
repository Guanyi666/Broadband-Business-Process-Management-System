import { login, api, db } from './lib.mjs';

const ct = (await login('customer1')).token;
const at = (await login('admin')).token;

console.log('=== 1. 资源校验降级验证（用明显未覆盖的地址下单）===');
const r = await api(ct, 'POST', '/api/customer-portal/orders', {
  packageCode: 'PKG_100M',
  installAddress: '北京市测试区未覆盖路999号',
  contactPhone: '13900000001',
  remark: '验证资源校验降级'
});
console.log('  下单结果: http=' + r.http + ' code=' + r.code + ' msg=' + JSON.stringify(r.msg || ''));
const newId = r.data;
if (newId) {
  const d = db(`SELECT id, status, install_address, resource_status, check_remark FROM broadband_order WHERE id=${newId}`);
  console.log('  DB: ' + (d[0] || []).join(' | '));
  console.log('  ✅ 未覆盖地址仍可下单（校验结果仅记录，不拦截）');
} else {
  console.log('  ❌ 仍被拦截: ' + r.msg);
}

console.log('\n=== 2. 菜单中已无「装维地图」===');
const menus = await api(at, 'GET', '/api/auth/menus');
const s = JSON.stringify(menus.data);
console.log('  菜单含"地图": ' + (s.includes('地图') ? '❌ 仍存在' : '✅ 已移除'));
console.log('  菜单含 /installer/map: ' + (s.includes('/installer/map') ? '❌ 仍存在' : '✅ 已移除'));

console.log('\n=== 3. 客户订单 track（履约进度）数据仍正常返回 ===');
const tr = await api(ct, 'GET', `/api/customer-portal/orders/${newId}/track`);
console.log('  track code=' + tr.code + ' 节点数=' + (tr.data?.stages?.length || 0) + ' 进度=' + tr.data?.progress);

// 清理本次验证订单
if (newId) {
  db(`DELETE FROM order_audit_log WHERE order_id=${newId}`);
  db(`DELETE FROM appointment WHERE order_id=${newId}`);
  db(`DELETE FROM broadband_order WHERE id=${newId}`);
  console.log('\n(验证订单已清理)');
}
