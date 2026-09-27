#!/data/data/com.termux/files/usr/bin/bash
# 方块突击 · 手机主机版 —— 局域网开服
# 用法：bash start-lan.sh
cd "$(dirname "$0")"

# 如果服务器没在跑，就先在后台启动
if ! node -e "require('http').get({host:'127.0.0.1',port:8080,path:'/',timeout:800},r=>process.exit(0)).on('error',()=>process.exit(1))" 2>/dev/null; then
  echo "[1/2] 正在启动本地服务器..."
  node server.js &
  sleep 2
fi

echo "[2/2] 你的开服二维码和网址："
node qrlan.js
echo ""
echo "提示：朋友手机连同一个WiFi/热点，扫码或打开上面的网址就能进游戏。"
echo "想停止服务器：按 Ctrl+C 两次。"
