/* ============================================================
   远程联机一键启动（给「启动远程联机.bat」调用）
   自动完成：
     1) 检查并启动本地游戏服务器（node server.js）
     2) 启动 cloudflared 公网隧道
     3) 等到公网网址后，打印二维码并把网址复制到剪贴板
     4) 失败自动重试最多6次；保持运行，关窗口即停止
   ============================================================ */
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const http = require('http');

let QRCODE = null;
try { QRCODE = require('qrcode-terminal'); } catch (e) {}
const QRCode = require('./node_modules/qrcode-terminal/vendor/QRCode');

const LOG = path.join(__dirname, '_tunnel.log');   // 隧道日志（临时文件）
// 公网网址的样子：排除 api.trycloudflare.com（那是 Cloudflare 接口地址，不是你的网址）
const URL_RE = /https:\/\/(?!api\.)[a-z0-9-]+\.trycloudflare\.com/;
const TUN = path.join(__dirname, 'cloudflared.exe');
const MAX_TRIES = 6;
// 输出模式：默认ansi（终端彩色）| --ascii（纯文本）| --json（给启动器exe画图用）
const MODE = process.argv.includes('--json') ? 'json' : (process.argv.includes('--ascii') ? 'ascii' : 'ansi');

// 复制文字到剪贴板（用 Windows 自带的 clip）
function copyToClipboard(text) {
  try { const c = spawn('clip'); c.stdin.write(text); c.stdin.end(); } catch (e) {}
}

// 把 qrcode-terminal 的 ANSI 背景色方块转成纯文本方块，方便在窗口里显示
function ansiToBlocks(s) {
  let out = '', white = false;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (c === '\n') { out += '\n'; continue; } // 保留换行，否则二维码会挤成一条
    if (c === '\x1b') {
      const m = s.indexOf('m', i);
      const code = s.slice(i + 1, m);
      i = m;
      white = code.includes('47'); // 白底 = 数据块
      continue;
    }
    out += (white ? '█' : ' ');
  }
  return out;
}

// 生成二维码 0/1 矩阵
function matrixRows(url) {
  const q = new QRCode(-1, 1); // 容错级别 L
  q.addData(url);
  q.make();
  const n = q.getModuleCount();
  const rows = [];
  for (let r = 0; r < n; r++) {
    let s = '';
    for (let c = 0; c < n; c++) s += q.isDark(r, c) ? '1' : '0';
    rows.push(s);
  }
  return { n, rows };
}

// 打印二维码 + 复制网址
function showQR(url) {
  copyToClipboard(url);
  console.log('__URL__:' + url);
  if (MODE === 'json') {
    // 给 exe 用：输出矩阵，由 exe 画成标准方形二维码
    const { n, rows } = matrixRows(url);
    console.log('__QRSIZE__:' + n);
    for (const row of rows) console.log('__QRROW__:' + row);
    console.log('__QREND__');
    console.log('  公网网址已复制到剪贴板，可直接粘贴发给朋友。');
    return;
  }
  console.log('');
  console.log('══════════════════════════════════════════════════');
  console.log('  你的公网网址（发给朋友，任何网络都能打开）：');
  console.log('  ' + url);
  console.log('  手机扫码直接进：');
  if (QRCODE) QRCODE.generate(url, { small: false }, (q) => console.log(MODE === 'ascii' ? ansiToBlocks(q) : q));
  console.log('  [已复制到剪贴板，可直接粘贴发给朋友]');
  console.log('══════════════════════════════════════════════════');
  console.log('');
  console.log('  隧道运行中。关闭启动器窗口 = 停止公网访问。');
}

// 检查本地 8080 服务器是否在运行
function ensureLocalServer(cb) {
  const rq = http.get({ host: '127.0.0.1', port: 8080, path: '/', timeout: 1200 }, (res) => { rq.destroy(); cb(true); });
  rq.on('error', () => cb(false));
  rq.on('timeout', () => { rq.destroy(); cb(false); });
}

let starting = false;
function startTunnel() {
  if (starting) return;
  starting = true;
  console.log('[2/2] 正在连接 Cloudflare 公网隧道（约5~15秒）...');
  try { fs.unlinkSync(LOG); } catch (e) {}
  const fd = fs.openSync(LOG, 'w');
  const tun = spawn(TUN, ['tunnel', '--url', 'http://localhost:8080', '--no-autoupdate'],
    { cwd: __dirname, stdio: ['ignore', fd, fd] });
  let tries = 0;
  const poll = setInterval(() => {
    let data = '';
    try { data = fs.readFileSync(LOG, 'utf8'); } catch (e) {}
    const m = data.match(URL_RE);
    if (m) {
      clearInterval(poll);
      try { fs.closeSync(fd); } catch (e) {}
      starting = false;
      showQR(m[0]);
      // 之后如果隧道意外断开，提示用户
      setInterval(() => {
        if (tun.exitCode !== null) {
          console.log('[提示] 公网隧道已断开，请关闭本窗口后重新双击启动。');
          process.exit(0);
        }
      }, 3000);
      return;
    }
    if (tun.exitCode !== null) {
      clearInterval(poll);
      try { fs.closeSync(fd); } catch (e) {}
      starting = false;
      tries++;
      if (tries >= MAX_TRIES) {
        console.log('[失败] 连续' + MAX_TRIES + '次连不上 Cloudflare，请检查网络后重新双击。');
        process.exit(1);
      }
      console.log('[提示] 隧道请求超时/失败，自动重试第 ' + tries + '/' + MAX_TRIES + ' 次...');
      setTimeout(startTunnel, 2000);
    }
  }, 1500);
}

console.log('==============================================');
console.log('  远程联机启动器');
console.log('==============================================');
console.log('[1/2] 检查本地游戏服务器...');
ensureLocalServer((ok) => {
  if (!ok) {
    console.log('      未运行，正在后台启动...');
    spawn('node', ['server.js'], { cwd: __dirname, detached: true, stdio: 'ignore' }).unref();
    setTimeout(startTunnel, 2500);
  } else {
    console.log('      已在运行，跳过。');
    startTunnel();
  }
});
