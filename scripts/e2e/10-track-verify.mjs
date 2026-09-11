import { login, api, db } from './lib.mjs';

/** 连续性校验：第一个非 DONE 节点之后，不允许再出现 DONE */
function checkContinuous(stages) {
  let blocked = false;
  for (const s of stages) {
    if (s.state !== 'DONE') blocked = true;
    else if (blocked) return { ok: false, bad: s.name };
  }
  return { ok: true };
}

const ct = (await login('customer1')).token;
const list = await api(ct, 'GET', '/api/customer-portal/orders');
const orders = list.data?.records || list.data || [];
console.log(`customer1 可见订单 ${orders.length} 条\n`);

let pass = 0, fail = 0;
for (const o of orders) {
  const r = await api(ct, 'GET', `/api/customer-portal/orders/${o.id}/track`);
  if (r.code !== 0) { console.log(`订单 ${o.id}: ${r.msg}`); continue; }
  const d = r.data;
  const c = checkContinuous(d.stages || []);
  const chain = (d.stages || []).map(s => `${s.name}:${s.state}`).join(' | ');
  console.log(`订单${d.orderNo} [${d.statusLabel}] ${d.progress} → ${chain}`);
  if (c.ok) pass++; else { fail++; console.log(`  ❌ 违反连续性：${c.bad} 在前序未完成时显示 DONE`); }
}
console.log(`\n连续性校验：通过 ${pass}，失败 ${fail}`);

console.log('\n=== 管理端履约轨道对照（admin 视角，含已完成订单）===');
const at = (await login('admin')).token;
const wos = db(`SELECT id, order_id, status FROM work_order WHERE deleted=0 ORDER BY id LIMIT 12`);
for (const [wid, oid, st] of wos) {
  const r = await api(at, 'GET', `/api/work-orders/${wid}/track`);
  if (r.code !== 0) { console.log(`WO${wid}(${st}): ${r.msg || r.code}`); continue; }
  const d = r.data;
  const c = checkContinuous(d.stages || []);
  const chain = (d.stages || []).map(s => `${s.name}:${s.state}`).join(' | ');
  console.log(`WO${wid}(${st}) → ${chain} ${c.ok ? '✅' : '❌' + c.bad}`);
}
