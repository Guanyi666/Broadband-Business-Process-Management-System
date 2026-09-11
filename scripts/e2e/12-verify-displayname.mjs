import { login, api } from './lib.mjs';

console.log('=== 登录返回的显示名（应为中文姓名）===');
for (const u of ['install1', 'install2', 'admin', 'cs1', 'audit1']) {
  const r = await login(u);
  console.log(`  ${u.padEnd(9)} → name=${JSON.stringify(r.raw?.user?.name)} username=${JSON.stringify(r.raw?.user?.username)}`);
}

console.log('\n=== /api/auth/me 显示名 ===');
for (const u of ['install1', 'admin']) {
  const lg = await login(u);
  const me = await api(lg.token, 'GET', '/api/auth/me');
  console.log(`  ${u.padEnd(9)} → name=${JSON.stringify(me.data?.name)} realName检查`);
}
