import { login, api, accounts } from './lib.mjs';

console.log('=== 阶段4：测试账号登录与菜单权限 ===\n');
const results = [];
for (const [name, username] of Object.entries(accounts)) {
  const lg = await login(username);
  if (!lg.token) {
    results.push({ name, ok: false, role: '-', menus: 0, note: `登录失败: ${lg.msg || lg.code}` });
    continue;
  }
  const me = await api(lg.token, 'GET', '/api/auth/me');
  const menus = await api(lg.token, 'GET', '/api/auth/menus');
  const roleList = me.data?.roles || me.data?.roleCodes || [];
  const menuCount = Array.isArray(menus.data) ? menus.data.length : (menus.data?.length ?? '-');
  results.push({ name, ok: true, role: JSON.stringify(roleList), menus: menuCount, note: me.data?.userType ?? '' });
}

console.log('账号'.padEnd(12) + '登录'.padEnd(6) + '角色'.padEnd(34) + '菜单数');
console.log('-'.repeat(70));
for (const r of results) {
  console.log(String(r.name).padEnd(12) + (r.ok ? 'OK' : 'FAIL').padEnd(6) + String(r.role).padEnd(34) + r.menus + (r.ok ? '' : '  ' + r.note));
}
const failed = results.filter(r => !r.ok);
console.log(`\n登录成功 ${results.length - failed.length}/${results.length}`);
