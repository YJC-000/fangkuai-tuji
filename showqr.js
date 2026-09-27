/* ============================================================
   二维码 + 剪贴板小工具（给两个启动器共用）
   用法：
     node showqr.js <网址>      -> 打印该网址的二维码，并把网址复制到剪贴板
     node showqr.js --copy      -> 只把局域网地址 http://IP:8080 复制到剪贴板
   ============================================================ */
const { spawn } = require('child_process');
const os = require('os');

let QRCODE = null;
try { QRCODE = require('qrcode-terminal'); } catch (e) {}

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
  try {
    const c = spawn('clip');
    c.stdin.write(text);
    c.stdin.end();
    return true;
  } catch (e) { return false; }
}

const arg = process.argv[2];
if (arg === '--copy') {
  const url = 'http://' + getLANIP() + ':8080';
  copyToClipboard(url);
  console.log('[已复制到剪贴板] ' + url);
} else if (arg) {
  console.log('==================================================');
  console.log('  访问地址：' + arg);
  console.log('  手机扫码直接进：');
  if (QRCODE) QRCODE.generate(arg, { small: false }, (q) => console.log(q));
  if (copyToClipboard(arg)) console.log('  [已复制到剪贴板，可直接粘贴发给朋友]');
  console.log('==================================================');
} else {
  console.log('用法：node showqr.js <网址>  或  node showqr.js --copy');
}
