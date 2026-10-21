// 方块突击 - 自建 PeerJS 信令服务器
// 部署到 Render.com 免费套餐，用于游戏联机的 P2P 握手
const { PeerServer } = require('peer');

const PORT = process.env.PORT || 10000;
const server = PeerServer({
  port: PORT,
  path: '/',
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

server.on('connection', (client) => {
  console.log('[' + new Date().toISOString() + '] 客户端已连接: ' + client.getId());
});

server.on('disconnect', (client) => {
  console.log('[' + new Date().toISOString() + '] 客户端断开: ' + client.getId());
});

server.on('error', (err) => {
  console.error('服务器错误:', err);
});

console.log('方块突击信令服务器已启动，端口: ' + PORT);
