# 方块突击 - 自建联机信令服务器

## 为什么需要这个？
PeerJS 官方公共服务器（0.peerjs.com 等）在国内不稳定/已失效，
游戏联机需要一个自己的信令服务器来让两个设备互相找到对方。

## 一键部署到 Render.com（免费）

1. 把这个 peer-server 文件夹推到你的 GitHub 仓库
2. 打开 https://dashboard.render.com/
3. 点击 "New" → "Web Service"
4. 选择你的 GitHub 仓库
5. Render 会自动读取 render.yaml，点击 "Apply"
6. 等待部署完成（约2-3分钟），得到一个 URL，例如：
   https://fangkuai-peer-server.onrender.com

## 部署后
把得到的服务器URL填入游戏的"联机设置"→"信令服务器地址"中。

## 注意
- Render 免费套餐 15 分钟无请求会休眠，下次访问需要约 30 秒唤醒
- 唤醒后联机功能正常
- 完全免费，不需要信用卡
