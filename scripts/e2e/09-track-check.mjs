import { login, api } from './lib.mjs';

const t = (await login('customer1')).token;

for (const oid of ['2040', '1002', '1005', '1099']) {
  const r = await api(t, 'GET', `/api/customer-portal/orders/${oid}/track`);
  if (r.code !== 0) { console.log(`订单 ${oid}: code=${r.code} ${r.msg}`); continue; }
  const d = r.data;
  console.log(`\n=== 订单 ${oid} (${d.status} / ${d.statusLabel}) 进度 ${d.progress} terminal=${d.terminal} ===`);
  for (const s of d.stages || []) {
    console.log(`  ${String(s.name).padEnd(8)} state=${String(s.state).padEnd(10)} time=${s.time || '-'}`);
  }
}
