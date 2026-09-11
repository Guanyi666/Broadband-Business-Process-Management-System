import { login } from './lib.mjs';
const BASE = 'http://localhost:8080';

const t = (await login('admin')).token;

// 直接看原始 JSON 文本（不做任何大整数处理）
async function raw(path) {
  const r = await fetch(BASE + path, { headers: { Authorization: 'Bearer ' + t } });
  const text = await r.text();
  return text;
}

const s1 = await raw('/api/work-orders/page?pageNum=1&pageSize=2');
console.log('=== 工单列表原始 JSON（截取前 400 字符）===');
console.log(s1.slice(0, 400));

const s2 = await raw('/api/orders/page?pageNum=1&pageSize=2');
console.log('\n=== 订单列表原始 JSON（截取前 400 字符）===');
console.log(s2.slice(0, 400));

// 精确判断 id 是否带引号
const m1 = s1.match(/"id"\s*:\s*("[^"]+"|\d+)/);
const m2 = s2.match(/"id"\s*:\s*("[^"]+"|\d+)/);
console.log('\n=== 结论 ===');
console.log('工单 id 原始形式:', m1 ? m1[1] : '未找到', m1 && m1[1].startsWith('"') ? '→ 字符串 ✅' : '→ 数字 ❌(精度风险)');
console.log('订单 id 原始形式:', m2 ? m2[1] : '未找到', m2 && m2[1].startsWith('"') ? '→ 字符串 ✅' : '→ 数字 ❌(精度风险)');
const mt = s1.match(/"total"\s*:\s*("[^"]+"|\d+)/);
console.log('total 原始形式:', mt ? mt[1] : '未找到');
