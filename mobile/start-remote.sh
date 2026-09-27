#!/data/data/com.termux/files/usr/bin/bash
# 方块突击 · 手机主机版 —— 远程开服（不同网络也能连）
# 用法：bash start-remote.sh
cd "$(dirname "$0")"

echo "正在启动远程联机（约 5~15 秒出公网网址）..."
node remote-run.js
echo ""
echo "提示：把上面的 https://xxx.trycloudflare.com 网址发给朋友，任何网络都能打开。"
echo "停止：按 Ctrl+C。"
