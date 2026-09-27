/* ============================================================
   局域网一键脚本（给「方块突击启动器.exe」调用）
   node qrlan.js            -> 输出 ASCII 二维码（给命令行用）
   node qrlan.js --json     -> 输出二维码矩阵（给启动器exe画图用）
   两种模式都会把网址复制到剪贴板
   ============================================================ */
const { spawn } = require('child_process');
const os = require('os');

let QRCODE = null;
try { QRCODE = require('qrcode-terminal'); } catch (e) {}
const QRCode = require('./node_modules/qrcode-terminal/vendor/QRCode');

// 和 server.js 相同的挑选逻辑：跳过 VMware 等虚拟网卡，返回真实局域网 IP
const VIRTUAL_NIC_RE = /vmware|vmnet|virtualbox|vethernet|hyper-?v|docker|wsl|loopback|pseudo|tap-?adapter|tunnel|vpn|bluetooth|teredo|isatap|6to4|本地连接\*|以太网\*/i;
function getLANIP() {
  const ifs = os.networkInterfaces();
  let virtualFallback = null;
  for (const name in ifs) {
    const isVirtual = VIRTUAL_NIC_RE.test(name);
    for (const i of ifs[name]) {
      if (i.family !== 'IPv4' || i.internal) continue;
      if (i.address.startsWith('169.254.')) continue;
      if (isVirtual) { if (!virtualFallback) virtualFallback = i.address; continue; }
      return i.address;
    }
  }
  return virtualFallback || '127.0.0.1';
}

// 复制文字到剪贴板（用 Windows 自带的 clip）
function copyToClipboard(text) {
  try { const c = spawn('clip'); c.stdin.write(text); c.stdin.end(); } catch (e) {}
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

const url = 'http://' + getLANIP() + ':8080';
copyToClipboard(url);

if (process.argv.includes('--json')) {
  // 给 exe 用：输出矩阵
  const { n, rows } = matrixRows(url);
  console.log('__QRURL__:' + url);
  console.log('__QRSIZE__:' + n);
  for (const row of rows) console.log('__QRROW__:' + row);
  console.log('__QREND__');
} else {
  // 给命令行用：ASCII 二维码
  console.log('__URL__:' + url);
  if (QRCODE) QRCODE.generate(url, { small: false }, (q) => console.log(q));
}
