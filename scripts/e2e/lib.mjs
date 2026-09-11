import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

export const BASE = 'http://localhost:8080';
const MYSQL_ARGS = ['exec', 'bbpms-mysql', 'mysql', '--default-character-set=utf8mb4',
  '-ubbpms_app', '-pbbpms_pwd_2026', '-N', 'bbpms'];

/** RSA 加密登录，返回 token */
export async function login(username, password = 'admin123') {
  const pk = await (await fetch(BASE + '/api/auth/public-key')).json();
  const key = crypto.createPublicKey({ key: Buffer.from(pk.data, 'base64'), format: 'der', type: 'spki' });
  const enc = crypto.publicEncrypt({ key, padding: crypto.constants.RSA_PKCS1_PADDING }, Buffer.from(password));
  const r = await fetch(BASE + '/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: enc.toString('base64') })
  });
  const j = await r.json();
  return { token: j.data?.token || j.data?.accessToken, code: j.code, msg: j.msg, raw: j.data };
}

/** 通用 API 调用 */
export async function api(token, method, path, body, params) {
  let url = BASE + path;
  if (params && Object.keys(params).length) {
    const qs = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== null) qs.append(k, String(v));
    url += '?' + qs.toString();
  }
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const r = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  let j = null;
  const text = await r.text();
  // 雪花 ID 超出 JS 安全整数（2^53），原生 JSON.parse 会丢精度 —— 把 16 位以上整数转为字符串
  const safeText = text.replace(/([:,\[]\s*)(-?\d{16,})/g, '$1"$2"');
  try { j = JSON.parse(safeText); } catch { j = { raw: text }; }
  return { http: r.status, ...j };
}

/** 直接查数据库（返回行数组，以 | 分隔） */
export function db(sql) {
  const out = execFileSync('docker', [...MYSQL_ARGS, '-e', sql], { encoding: 'utf-8' });
  return out.trim().split('\n').filter(Boolean).map(l => l.split('\t'));
}

/** 打印表 */
export function show(rows) {
  if (!rows.length) return '(空)';
  return rows.map(r => r.join(' | ')).join('\n');
}

export const accounts = {
  admin: 'admin', cs1: 'cs1', cs2: 'cs2', audit1: 'audit1', audit2: 'audit2',
  disp1: 'disp1', disp2: 'disp2', disp3: 'disp3', disp4: 'disp4',
  install1: 'install1', install2: 'install2', install3: 'install3',
  install4: 'install4', install5: 'install5', customer1: 'customer1'
};
