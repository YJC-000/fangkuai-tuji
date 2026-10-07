/* ============================================================
   方块突击 · 局域网联机服务器
   ------------------------------------------------------------
   使用方法（傻瓜版）：
   1. 确保电脑已安装 Node.js（安装过就跳过）
   2. 在这个文件夹打开命令行（在文件夹地址栏输入 cmd 回车）
   3. 输入：npm install ws    （只需第一次，装依赖）
   4. 输入：node server.js    （启动服务器）
   5. 记下窗口里打印的「局域网访问」地址，例如：
        http://192.168.1.100:8080
   6. 你自己和朋友的电脑（必须在同一个 WiFi/局域网），
      在浏览器打开这个地址，点「联机模式」就能一起玩
   ------------------------------------------------------------
   提示：第一次启动如果 Windows 弹出防火墙询问，
        请勾选「专用网络」并点允许，否则别人连不上。
   ============================================================ */

const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');

let WebSocket;
try {
  WebSocket = require('ws');
} catch (e) {
  console.log('缺少 ws 依赖，请先运行：npm install ws');
  process.exit(1);
}

// 终端二维码（可选依赖，没装也不影响运行）
let QRCODE = null;
try { QRCODE = require('qrcode-terminal'); } catch (e) { QRCODE = null; }

const PORT = 8080;
const SPAWNS = [[-8, 30], [8, 30], [0, 35], [-12, 25], [12, 25]]; // 出生点
let nextId = 1;

// ===== 【地图随机化】与前端同一套地图生成算法（同种子 → 服务器与所有玩家看到同一张图）=====
// 【V3.0】地图大小：单机/合作 600，双人对战 150（两人不需要那么大）
const MAP_SIZE = 600;
function mulberry32(a){return function(){a|=0;a=a+0x6D2B79F5|0;var t=Math.imul(a^a>>>15,1|a);t=t+Math.imul(t^t>>>7,61|t)^t;return((t^t>>>14)>>>0)/4294967296;};}
const MAP_COLORS=[0x707a88,0x9c7a4d,0x5d5348,0x4a6fa5,0x7a5a8a,0x5a8a6a,0x8a6a5a];
const NET_SPAWN_PTS=[[-8,30],[8,30],[0,35],[-12,25],[12,25]];
function mapGenSpec(seed, size){
  const R=mulberry32(seed);
  const half=size/2;
  const boxes=[];
  for(let i=0;i<4;i++){
    let x=0,z=0,ok=false;
    for(let t=0;t<20&&!ok;t++){
      x=(R()*2-1)*40; z=(R()*2-1)*40;
      ok=Math.hypot(x,z)>10;
      for(const s of NET_SPAWN_PTS) if(Math.hypot(x-s[0],z-s[1])<6) ok=false;
    }
    boxes.push({type:'box',x,z,w:1.6+R()*2.4,h:0.9+R()*0.5,d:1.6+R()*2.4,color:MAP_COLORS[(R()*MAP_COLORS.length)|0]});
  }
  // 【V3.0 掩体加多】大图32个、小图18个；形状=方块/圆柱/四棱锥
  for(let i=0;i<(size>=300?32:18);i++){
    let a=R()*Math.PI*2, r=half*0.25+R()*half*0.6;
    let x=Math.cos(a)*r, z=Math.sin(a)*r;
    for(let t=0;t<15;t++){
      let bad=false;
      for(const b of boxes){ if(Math.hypot(b.x-x,b.z-z)<5) bad=true; }
      if(!bad)break;
      a=R()*Math.PI*2; r=half*0.25+R()*half*0.6; x=Math.cos(a)*r; z=Math.sin(a)*r;
    }
    const shR=R();
    if(shR<0.6) boxes.push({type:'box',x,z,w:1.5+R()*3,h:1+R()*2.2,d:1.5+R()*3,color:MAP_COLORS[(R()*MAP_COLORS.length)|0]});
    else if(shR<0.85) boxes.push({type:'cyl',x,z,r:0.9+R()*1.6,h:1.2+R()*2.2,color:MAP_COLORS[(R()*MAP_COLORS.length)|0]});
    else boxes.push({type:'tetra',x,z,r:1.0+R()*1.2,h:1+R()*1.6,color:MAP_COLORS[(R()*MAP_COLORS.length)|0]});
  }
  const buildings=[]; const bpos=[];
  // 【V3.0】大图 3 栋楼房 + 2 座高塔，小图 2 栋楼房
  const placeBuilding=()=>{
    let cx=0,cz=0;
    for(let t=0;t<30;t++){
      const a=R()*Math.PI*2, r=half*0.45+R()*half*0.32;
      cx=Math.cos(a)*r; cz=Math.sin(a)*r;
      let ok=Math.hypot(cx,cz)>half*0.4;
      for(const p of bpos) if(Math.hypot(cx-p[0],cz-p[1])<55) ok=false;
      for(const b of boxes) if(Math.hypot(b.x-cx,b.z-cz)<13) ok=false;
      if(ok)break;
    }
    bpos.push([cx,cz]); return {cx,cz};
  };
  const nBox=size>=300?3:2, nTower=size>=300?2:0;
  for(let i=0;i<nBox;i++){ const p=placeBuilding(); buildings.push({cx:p.cx,cz:p.cz,w:18,d:14,h:9,color:0x4a5568,tower:false}); }
  for(let i=0;i<nTower;i++){ const p=placeBuilding(); buildings.push({cx:p.cx,cz:p.cz,w:8,d:8,h:16,color:0x556070,tower:true}); }
  return {boxes,buildings};
}
// 建筑内全部实体块（与前端完全一致，联机两边碰撞一致）
function buildingSpec(bd){
  const {cx,cz,w,d,h}=bd; const t=1, W2=w/2, D2=d/2, doorW=3.4, doorH=4.5;
  const out=[]; const box=(x,z,w2,d2,y2,h2)=>out.push({x,z,w:w2,d:d2,y:y2,h:h2});
  box(cx-(W2+doorW/2)/2, cz-D2, W2-doorW/2, t, 0, h);
  box(cx+(W2+doorW/2)/2, cz-D2, W2-doorW/2, t, 0, h);
  box(cx, cz-D2, doorW, t, doorH, h-doorH);
  box(cx, cz+D2, w, t, 0, h);
  box(cx-W2, cz, t, d, 0, h);
  box(cx+W2, cz, t, d, 0, h);
  const platX0=cx+0.65, platX1=cx+7.15; // 【碰撞修复】走廊平台 x 带：避开楼梯爬升通道
  box((platX0+platX1)/2, cz, platX1-platX0, d-2, 3, 0.5);
  box((platX0+platX1)/2, cz-5.5, platX1-platX0, 1, 6, 0.5); // 二层平台前段
  box((platX0+platX1)/2, cz+4.05, platX1-platX0, 3.9, 6, 0.5); // 二层平台后段
  box(cx, cz-6.4, w, 1.2, 9, 1);  // 楼顶板后段（留出楼梯段3 通道）
  box(cx, cz-0.925, w, 6.55, 9, 1); // 楼顶板前段左（给段3让出通道）
  box(cx, cz+5.325, w, 3.35, 9, 1); // 楼顶板前段右
  const stairW=1.6, stepH=0.5, stepD=1.3, N=7; // 【碰撞修复】7 级：楼梯顶与平台/楼顶顶面齐平
  let x0=cx-W2+1.2, z0=cz-D2+2;
  for(let i=1;i<=N;i++) box(x0+(i-1)*stepD+stepD/2, z0, stairW, stepD, (i-1)*stepH, stepH); // 【碰撞修复】独立0.5m台阶
  x0=cx+W2-2.2; z0=cz-D2+2;
  for(let i=1;i<=6;i++) box(x0, z0+(i-1)*stepD+stepD/2, stairW, stepD, 3.5+(i-1)*stepH, stepH);
  z0=cz+3.0; x0=cx+W2-2.2; // 楼梯段3：起点z=段2顶附近，爬完段2直接转-x
  for(let i=1;i<=N;i++) box(x0-(i-1)*stepD-stepD/2, z0, stairW, stepD, 6.5+(i-1)*stepH, stepH);
  return out;
}
// 【V3.0 新建筑】高塔：8x8、高16m，折返楼梯登顶（与前端 towerSpec 完全一致）
function towerSpec(bd){
  const {cx,cz,w,d,h}=bd; const W2=w/2,D2=d/2;
  const out=[]; const box=(x,z,w2,d2,y2,h2)=>out.push({x,z,w:w2,d:d2,y:y2,h:h2});
  box(cx-3.75,cz-D2,0.5,1,0,h);
  box(cx+1.75,cz-D2,3.5,1,0,h);
  box(cx-2.0,cz-D2,3.0,1,4.5,h-4.5);
  box(cx,cz+D2,w,1,0,h);
  box(cx-W2,cz,1,d,0,h);
  box(cx+W2,cz,1,d,0,h);
  const zF=cz-1.8,zB=cz+1.8,sw=2.4,sd=0.85;
  const flight=(z0,dir,y0)=>{ for(let i=1;i<=8;i++){ const x=cx+dir*(-2.8+(i-1)*sd+sd/2); box(x,z0,sd,sw,y0+(i-1)*0.5,0.5); } };
  flight(zF,1,0);    box(cx+3.0,cz,1.4,6.6,3.5,0.5);
  flight(zB,-1,4);   box(cx-3.0,cz,1.4,6.6,7.5,0.5);
  flight(zF,1,8);    box(cx+3.0,cz,1.4,6.6,11.5,0.5);
  flight(zB,-1,12);
  box(cx,cz,w-1,d-1,16,0.5);
  box(cx,cz-D2+0.4,w-1.2,0.6,16.5,1.2); box(cx,cz+D2-0.4,w-1.2,0.6,16.5,1.2);
  box(cx-W2+0.4,cz,0.6,d-1.2,16.5,1.2); box(cx+W2-0.4,cz,0.6,d-1.2,16.5,1.2);
  return out;
}
// 由种子生成全部障碍的 x/z AABB，服务器用来给物品找合法位置
function buildMapAABBs(seed, size){
  const spec=mapGenSpec(seed, size);
  const half=size/2;
  const obs=[]; // 带 y 高度：既能给物品找位置，也能给服务器托管的怪物/红球做碰撞
  const push=(x,z,w,d,y,h)=>obs.push({min:{x:x-w/2,y:y,z:z-d/2},max:{x:x+w/2,y:y+h,z:z+d/2}});
  push(0,-half, size, 20, 0, 6); push(0, half, size, 20, 0, 6);
  push(-half,0, 20, size, 0, 6); push( half,0, 20, size, 0, 6);
  for(const b of spec.boxes){
    if(b.type==='cyl'||b.type==='tetra') push(b.x,b.z, b.r*2, b.r*2, 0, b.h);
    else push(b.x,b.z, b.w, b.d, 0, b.h);
  }
  const inner=[];
  for(const bd of spec.buildings){
    const sp=bd.tower?towerSpec(bd):buildingSpec(bd);
    for(const s of sp) push(s.x,s.z,s.w,s.d, s.y, s.h);
    inner.push({min:{x:bd.cx-bd.w/2+1,z:bd.cz-bd.d/2+1},max:{x:bd.cx+bd.w/2-1,z:bd.cz+bd.d/2-1}});
  }
  return {obs,inner};
}
// 物品摆放合法性：避开障碍物外扩1.2、楼内、中心出生区、已有物品6格
function canPlaceItem(x,z,obs,inner,items){
  if(Math.hypot(x,z)<24)return false;
  for(const o of obs) if(x>o.min.x-1.2&&x<o.max.x+1.2&&z>o.min.z-1.2&&z<o.max.z+1.2) return false;
  for(const ib of inner) if(x>ib.min.x&&x<ib.max.x&&z>ib.min.z&&z<ib.max.z) return false;
  for(const it of items) if(Math.hypot(it.x-x,it.z-z)<6) return false;
  return true;
}
const clients = new Map(); // id -> {ws, name, state, room}

// ===== 【新增·房间系统】房间表：房间号 -> {set:玩家id集合, seed:地图种子, items:物品表, ...} =====
// 【地图随机化+拾取系统】每个房间额外保存：地图种子 seed、物品表 items、障碍物避让数据 obs/inner
const rooms = new Map();
// ===== 【新增·房间系统】只向“同一个房间”的玩家广播（原来的 broadcast 是全服广播，保留不动）=====
function broadcastRoom(msg, room, except) {
  const r = rooms.get(room);
  if (!r) return;
  const s = JSON.stringify(msg);
  for (const oid of r.set) {
    if (oid === except) continue;
    const oc = clients.get(oid);
    if (oc && oc.ws.readyState === 1) oc.ws.send(s);
  }
}

// 选取真实的局域网 IP：跳过 VMware/VirtualBox/Hyper-V/WSL/Docker 等虚拟网卡
// 以及 169.254.x.x 这类没联网时的自动私有地址，优先返回真实物理网卡（WLAN/以太网）
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
      return i.address; // 真实物理网卡优先
    }
  }
  return virtualFallback || '127.0.0.1';
}

// 托管游戏网页：局域网内其他电脑直接用浏览器访问就能拿到游戏
// 【PWA支持】按文件扩展名返回正确的 Content-Type（Service Worker 等要求正确 MIME）
const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml'
};
const server = http.createServer((req, res) => {
  let url;
  try { url = decodeURIComponent(req.url.split('?')[0]); } catch (e) { url = '/'; }
  // 【V3.0 局域网自动发现】/api/rooms 返回房间列表（房号/人数/是否有密码），供手机端搜索局域网房间
  if (url === '/api/rooms') {
    const list = [];
    for (const [room, r] of rooms) {
      list.push({ room, count: r.set.size, hasPwd: !!r.password, coop: !!r.coop, size: r.size });
    }
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
    res.end(JSON.stringify({ rooms: list }));
    return;
  }
  if (url === '/') url = '/枪战.HTML';
  const file = path.join(__dirname, url);
  if (!file.startsWith(__dirname) || !fs.existsSync(file)) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Not Found');
    return;
  }
  const type = MIME[path.extname(url).toLowerCase()] || 'application/octet-stream';
  // 【V3.0 缓存修复】no-store：WebView2/浏览器不缓存游戏页面，改版后必显示新内容
  res.writeHead(200, { 'Content-Type': type, 'Cache-Control': 'no-store' });
  fs.createReadStream(file).pipe(res);
});

const wss = new WebSocket.Server({ server });

function broadcast(msg, except) {
  const s = JSON.stringify(msg);
  for (const [id, c] of clients) {
    if (id !== except && c.ws.readyState === 1) c.ws.send(s);
  }
}

wss.on('connection', (ws) => {
  const id = nextId++;
  const spawn = SPAWNS[(id - 1) % SPAWNS.length];
  clients.set(id, { ws, name: '玩家' + id, state: null, spawn });

  ws.on('message', (raw) => {
    let m;
    try { m = JSON.parse(raw); } catch (e) { return; }
    if (!m || typeof m !== 'object') return;
    const c = clients.get(id);
    if (!c) return;

    switch (m.type) {
      case 'join': {
        c.name = (typeof m.name === 'string' ? m.name : '').slice(0, 12).trim() || ('玩家' + id);

        // ===== 【新增·房间系统】开始：校验房间号，区分“创建房间 / 加入房间” =====
        // ===== 【V2.0 房间密码】创建房间时接收密码存入房间表；加入房间时校验密码 =====
        let room = (typeof m.room === 'string' ? m.room : '').trim().slice(0, 16);
        if (!room) room = 'default'; // 不带房间号的旧客户端进默认房间，保持向后兼容
        const pwd = typeof m.password === 'string' ? m.password.trim() : '';
        let roomSet = rooms.get(room);
        if (room !== 'default') {
          if (m.create) {
            // 创建房间：房间号已存在则报错，提示玩家改为“加入”
            if (roomSet) {
              ws.send(JSON.stringify({ type: 'roomerror', reason: '房间号「' + room + '」已存在，请直接点「加入房间」，或换一个房间号创建' }));
              return;
            }
          } else {
            // 加入房间：房间不存在则报错（对应需求：房间不存在时给页面文字提示）
            if (!roomSet) {
              ws.send(JSON.stringify({ type: 'roomerror', reason: '房间号「' + room + '」不存在，请让朋友先「创建房间」，或核对房间号是否输错' }));
              return;
            }
            // 【V2.0 房间密码】加入有密码的房间必须密码正确，否则拒绝进入
            if (roomSet.password && pwd !== roomSet.password) {
              ws.send(JSON.stringify({ type: 'roomerror', reason: '房间密码错误，请核对密码后再加入' }));
              return;
            }
          }
        }
        if (!roomSet) {
          // 【新增·合作模式】创建房间：合作房=双人打怪(大图400)，对战房=双人对战(小图220)
          const isCoop = m.coop === true;
          const roomSize = isCoop ? 600 : (room === 'default' ? 600 : 150); // 【V3.0】合作/默认 600，双人对战房 150
          roomSet = { set: new Set(), seed: (Math.random() * 1e9) | 0, items: new Map(), itemSeq: 1, obs: null, inner: null,
                      coop: isCoop, size: roomSize, monsters: isCoop || (room !== 'default'), // 【对战刷怪】对战房也有怪物（开局6只越刷越快）
                      password: pwd || null, // 【V2.0 房间密码】创建房间时保存密码（不填则为无密码房）
                      kills: {}, scoreLimit: 30, finished: false, // 【V3.0 团队死斗】击杀计分，先到30杀获胜
                      coopEnemies: new Map(), coopBullets: [], enemySeq: 0, ballSeq: 0, spawnT: 8, spawnGap: 8 }; // 【持续刷怪】开局8秒后自动补怪，间隔越刷越短 // 【合作模式】服务器托管的怪物/红球
          const mm = buildMapAABBs(roomSet.seed, roomSet.size);
          roomSet.obs = mm.obs; roomSet.inner = mm.inner;
          rooms.set(room, roomSet);
          // 【物品加量V2.1】建房后立即补一批物品，开局就有弹药捡（不用等 4 秒刷新）
          refillRoom(roomSet, room);
          if (roomSet.monsters) for (let i = 0; i < 6; i++) spawnCoopEnemy(roomSet); // 【持续刷怪】合作房/对战房开局预生成6只，之后自动越刷越快
        }
        if (c.room && rooms.has(c.room)) rooms.get(c.room).set.delete(id); // 同一连接换房间时先退出旧房间
        c.room = room;
        roomSet.set.add(id);
        // ===== 【新增·房间系统】结束 =====

        const others = [];
        for (const [oid, oc] of clients) {
          if (oid === id) continue;
          if (oc.room !== c.room) continue; // 【新增·房间系统】只列出同一房间的玩家
          others.push({
            id: oid, name: oc.name,
            pos: oc.state ? oc.state.pos : [0, 0, 30],
            hp: oc.state ? oc.state.hp : 100,
            alive: oc.state ? oc.state.alive : true
          });
        }
        // 【地图随机化+拾取系统】welcome 下发地图种子与当前物品列表，新玩家与全房间同步
        const itemList = [];
        for (const [iid, it] of roomSet.items) itemList.push({ id: iid, itype: it.itype, x: it.x, z: it.z });
        ws.send(JSON.stringify({ type: 'welcome', id, spawn, room: c.room, players: others, mapSeed: roomSet.seed, items: itemList, coop: roomSet.coop, monsters: roomSet.monsters, mapSize: roomSet.size })); // 【合作模式】告知房间类型/是否刷怪与地图大小
        // 【新增·房间系统】下面两条只广播给同房间：新增玩家 + 文字通知
        broadcastRoom({ type: 'join', id, name: c.name, pos: [0, 0, 30], hp: 100, alive: true }, c.room, id);
        broadcastRoom({ type: 'notice', text: c.name + ' 加入了房间（当前 ' + roomSet.size + ' 人）' }, c.room, id);
        break;
      }
      case 'state': {
        if (!c.room) break; // 【新增·房间系统】还没进房间的状态不转发
        // 【合作模式】记录倒地标记（怪物AI不会瞄准倒地的人）
        c.state = { pos: m.pos, yaw: m.yaw, pitch: m.pitch, hp: m.hp, alive: m.alive !== false, downed: m.downed === true };
        broadcastRoom({ // 【新增·房间系统】由全服 broadcast 改为同房间 broadcastRoom
          type: 'state', id,
          pos: m.pos, yaw: m.yaw || 0, pitch: m.pitch || 0,
          hp: m.hp, alive: c.state.alive, downed: c.state.downed
        }, c.room, id);
        break;
      }
      case 'hit': {
        const t = clients.get(m.target);
        // 【新增·房间系统】只能打到同一房间的玩家
        if (t && t.room === c.room && t.ws.readyState === 1) {
          t.ws.send(JSON.stringify({ type: 'hit', from: id, dmg: m.dmg, head: !!m.head }));
        }
        break;
      }
      case 'respawn': {
        if (!c.room) break; // 【新增·房间系统】
        const pos = Array.isArray(m.pos) && m.pos.length === 3 ? m.pos : [0, 0, 30];
        c.state = { pos: pos, yaw: 0, pitch: 0, hp: 100, alive: true };
        ws.send(JSON.stringify({ type: 'respawn', id, pos: pos }));
        broadcastRoom({ type: 'respawn', id, pos: pos }, c.room, id); // 【新增·房间系统】同房间
        break;
      }
      case 'died': {
        // 死者上报击杀者，服务器只给击杀者发一次击杀确认
        const killer = clients.get(m.killer);
        // 【新增·房间系统】击杀者必须在同一房间
        if (killer && killer.room === c.room && killer.ws.readyState === 1) {
          killer.ws.send(JSON.stringify({ type: 'kill', victim: id }));
          // 【V3.0 团队死斗】对战房累计击杀，先到 30 杀一方获胜，广播结束
          const r = rooms.get(c.room);
          if (r && !r.coop && !r.finished) {
            r.kills[m.killer] = (r.kills[m.killer] || 0) + 1;
            if (r.kills[m.killer] >= r.scoreLimit) {
              r.finished = true;
              broadcastRoom({ type: 'tdm_end', winner: m.killer, winnerName: killer.name || '玩家' }, c.room);
            }
          }
        }
        break;
      }
      case 'pickup': {
        // 【拾取系统】玩家上报捡到物品：校验存在 → 从房间物品表删除 → 广播给全房间（含自己，本地已移除无副作用）
        if (!c.room) break;
        const r = rooms.get(c.room);
        if (!r || !r.items.has(m.id)) break;
        r.items.delete(m.id);
        broadcastRoom({ type: 'item_pickup', id: m.id }, c.room);
        break;
      }
      case 'damage_enemy': {
        // 【合作模式】玩家打服务器托管的怪物：统一结算血量，击杀由持续刷怪计时补充
        if (!c.room) break;
        const r = rooms.get(c.room);
        if (!r || !r.monsters) break;
        const e = r.coopEnemies.get(m.id);
        if (!e) break;
        e.hp -= m.dmg || 0;
        if (e.hp <= 0) {
          r.coopEnemies.delete(m.id);
          ws.send(JSON.stringify({ type: 'coop_kill', killer: id }));
        }
        break;
      }
      case 'revive': {
        // 【合作模式】队友走到倒地者身边碰满 5 秒后发起救援：目标起身，血量只有 10
        if (!c.room) break;
        const r = rooms.get(c.room);
        if (!r || !r.coop) break;
        const target = clients.get(m.target);
        if (!target || target.room !== c.room || target.ws.readyState !== 1) break;
        target.ws.send(JSON.stringify({ type: 'revived', hp: 10 }));
        break;
      }
      case 'grenade_explode': {
        // 【V3.0】玩家手雷爆炸：转发给同房间其他人播爆炸特效（伤害由各端 hit 消息结算）
        if (!c.room) break;
        broadcastRoom({ type: 'grenade_explode', x: m.x, y: m.y, z: m.z }, c.room, id);
        break;
      }
      case 'weather': {
        // 【V3.0 天气同步】某玩家切换天气：广播给同房间其他人（手机端也能同步天气）
        if (!c.room) break;
        if (typeof m.weather !== 'string') break;
        broadcastRoom({ type: 'weather', weather: m.weather }, c.room, id);
        break;
      }
    }
  });

  ws.on('close', () => {
    // ===== 【新增·房间系统】离开房间：通知同房其他玩家，房间空了就删除 =====
    const leaving = clients.get(id);
    if (leaving && leaving.room) {
      const r = rooms.get(leaving.room);
      if (r) {
        r.set.delete(id);
        broadcastRoom({ type: 'leave', id }, leaving.room);
        broadcastRoom({ type: 'notice', text: (leaving.name || ('玩家' + id)) + ' 离开了房间（剩余 ' + r.set.size + ' 人）' }, leaving.room);
        if (r.set.size === 0) rooms.delete(leaving.room); // 房间空了连同物品一起清掉
      }
    }
    clients.delete(id);
  });
  ws.on('error', () => {});
});

// ===== 【拾取系统】每个有人的房间定时补货，场上保持 16 个物品（位置避开障碍物/楼内/出生区）=====
function refillRoom(r, room){
  // 【物品加量V2.1】场上保持 16 个物品，弹药/医疗包更容易捡到
  while (r.items.size < 16) {
    const rnd = (Math.random() * 3) | 0;
    const itype = rnd === 0 ? 'rifle_ammo' : (rnd === 1 ? 'sniper_ammo' : 'medkit');
    const half = r.size / 2 - 12;
    let placed = false;
    for (let t = 0; t < 30; t++) {
      const x = (Math.random() * 2 - 1) * half, z = (Math.random() * 2 - 1) * half;
      const arr = []; for (const it of r.items.values()) arr.push(it);
      if (canPlaceItem(x, z, r.obs, r.inner, arr)) {
        const id = 'I' + (r.itemSeq++);
        r.items.set(id, { itype, x, z });
        broadcastRoom({ type: 'item_add', id, itype, x, z }, room);
        placed = true; break;
      }
    }
    if (!placed) break; // 实在找不到合法位置就等下一轮
  }
}
function refillAllRooms(){
  for (const [room, r] of rooms) {
    if (r.set.size === 0) continue;
    refillRoom(r, room);
  }
}
setInterval(refillAllRooms, 4000);

// ===== 【新增·合作模式】服务器托管怪物：AI移动/射击/子弹命中/广播（每100ms）=====
// 双人合作打怪时，怪物由服务器统一管理，两人看到的永远是同一批怪物
function spawnCoopEnemy(r){
  const half = r.size / 2 - 10;
  let x = 0, z = 0;
  for (let t = 0; t < 30; t++) {
    const a = Math.random() * Math.PI * 2, rr = half * 0.5 + Math.random() * half * 0.4;
    x = Math.cos(a) * rr; z = Math.sin(a) * rr;
    let bad = false;
    for (const ib of r.inner) { if (x > ib.min.x && x < ib.max.x && z > ib.min.z && z < ib.max.z) { bad = true; break; } }
    if (!bad) break;
  }
  const id = ++r.enemySeq;
  r.coopEnemies.set(id, { x, z, hp: 100, speed: 2.2 + Math.random() * 1.3, shootT: 1.5 + Math.random() * 2 });
}
function coopTick(){
  for (const [room, r] of rooms) {
    if (!r.monsters || r.set.size === 0) continue; // 【对战刷怪】合作房/对战房都托管怪物
    // 【持续刷怪】开局 6 只，之后不管杀不杀，每隔 spawnGap 秒自动补 1 只；
    // 每补一只间隔缩短（*0.93），越到后面出怪越快，最快 1.5 秒一只（上限 30 只）
    r.spawnT -= 0.1;
    if (r.spawnT <= 0) {
      if (r.coopEnemies.size < 30) spawnCoopEnemy(r);
      r.spawnGap = Math.max(1.5, r.spawnGap * 0.93);
      r.spawnT = r.spawnGap;
    }
    // 收集房间内存活的玩家（倒地的人不被瞄准，等队友救）
    const players = [];
    for (const oid of r.set) {
      const oc = clients.get(oid);
      if (oc && oc.state && oc.state.pos && oc.state.hp > 0 && oc.state.downed !== true) players.push(oc);
    }
    // 怪物 AI：追踪最近的玩家，靠太近会后退；会撞障碍物
    for (const [eid, e] of r.coopEnemies) {
      let t = null, td = 1e18;
      for (const p of players) {
        const dx = p.state.pos[0] - e.x, dz = p.state.pos[2] - e.z;
        const d2 = dx * dx + dz * dz;
        if (d2 < td) { td = d2; t = p; }
      }
      if (!t) continue;
      const dx = t.state.pos[0] - e.x, dz = t.state.pos[2] - e.z;
      const dist = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
      let mx = dx / dist, mz = dz / dist;
      if (dist < 7) { mx = -mx; mz = -mz; } // 太近后退
      e.x += mx * e.speed * 0.1; e.z += mz * e.speed * 0.1;
      for (const o of r.obs) { // 低矮障碍(顶<1.9)才挡怪物，平台/楼板不挡
        if (e.x + 0.5 > o.min.x && e.x - 0.5 < o.max.x && e.z + 0.5 > o.min.z && e.z - 0.5 < o.max.z && o.min.y < 1.9) {
          const px1 = o.max.x + 0.5 - e.x, px2 = e.x - (o.min.x - 0.5);
          const pz1 = o.max.z + 0.5 - e.z, pz2 = e.z - (o.min.z - 0.5);
          const mm = Math.min(px1, px2, pz1, pz2);
          if (mm === px1) e.x = o.max.x + 0.5; else if (mm === px2) e.x = o.min.x - 0.5;
          else if (mm === pz1) e.z = o.max.z + 0.5; else e.z = o.min.z - 0.5;
        }
      }
      const half = r.size / 2 - 2;
      if (e.x < -half) e.x = -half; if (e.x > half) e.x = half;
      if (e.z < -half) e.z = -half; if (e.z > half) e.z = half;
      // 射击：朝目标发射红球（25 伤害 ×4 击杀）
      e.shootT -= 0.1;
      if (e.shootT <= 0 && dist < 42) {
        const bx = e.x, by = 1.5, bz = e.z;
        let vx = t.state.pos[0] - bx, vy = (t.state.pos[1] || 0) + 1.2 - by, vz = t.state.pos[2] - bz;
        const len = Math.max(0.001, Math.hypot(vx, vy, vz));
        vx = vx / len * 16; vy = vy / len * 16; vz = vz / len * 16;
        r.coopBullets.push({ id: ++r.ballSeq, x: bx, y: by, z: bz, vx: vx, vy: vy, vz: vz, life: 5 });
        e.shootT = 1.7 + Math.random() * 1.6;
      }
    }
    // 红球移动 + 撞障碍 + 命中玩家
    for (let i = r.coopBullets.length - 1; i >= 0; i--) {
      const b = r.coopBullets[i];
      b.life -= 0.1;
      let dead = b.life <= 0 || b.y <= 0.02;
      // 【碰撞修复V2.1】细分步进移动（每帧拆成 8 小步），防止红球太快直接穿过薄墙
      const steps = 8;
      const sx = b.vx * 0.1 / steps, sy = b.vy * 0.1 / steps, sz = b.vz * 0.1 / steps;
      for (let s = 0; s < steps && !dead; s++) {
        b.x += sx; b.y += sy; b.z += sz;
        // 【碰撞修复V2.1】加 y 轴检测：楼板/走廊平台只有在红球真正碰到它（y 在板厚范围内）时才挡；
        // 之前漏了 y 轴，红球一飞进楼的 x/z 投影就被判撞墙，导致躲在楼里/楼后的玩家永远打不到
        for (const o of r.obs) {
          if (b.x > o.min.x && b.x < o.max.x && b.y > o.min.y && b.y < o.max.y && b.z > o.min.z && b.z < o.max.z) { dead = true; break; }
        }
      }
      if (!dead) {
        for (const p of players) {
          const py = p.state.pos[1] || 0;
          const cy = Math.max(py + 0.25, Math.min(b.y, py + 1.55));
          const dx = b.x - p.state.pos[0], dy = b.y - cy, dz = b.z - p.state.pos[2];
          if (dx * dx + dy * dy + dz * dz < 0.25) {
            dead = true;
            p.ws.send(JSON.stringify({ type: 'hit', from: 'enemy', dmg: 25 }));
            break;
          }
        }
      }
      if (dead) r.coopBullets.splice(i, 1);
    }
    // 广播本帧怪物与红球状态（100ms 一帧）
    const es = [], bs = [];
    for (const [eid, e] of r.coopEnemies) es.push({ id: eid, x: Math.round(e.x * 100) / 100, z: Math.round(e.z * 100) / 100, hp: e.hp });
    for (const b of r.coopBullets) bs.push({ id: b.id, x: Math.round(b.x * 100) / 100, y: Math.round(b.y * 100) / 100, z: Math.round(b.z * 100) / 100, vx: b.vx, vy: b.vy, vz: b.vz });
    broadcastRoom({ type: 'coop_state', enemies: es, bullets: bs }, room);
  }
}
setInterval(coopTick, 100);

server.listen(PORT, '0.0.0.0', () => {
  const ip = getLANIP();
  const lanUrl = 'http://' + ip + ':' + PORT;
  console.log('==================================================');
  console.log('  方块突击 · 局域网联机服务器已启动');
  console.log('  本机访问：   http://127.0.0.1:' + PORT);
  console.log('  局域网访问： ' + lanUrl);
  console.log('  >>> 手机/平板连同一个WiFi，用相机或浏览器扫下面二维码直接进：');
  console.log('==================================================');
  if (QRCODE) {
    QRCODE.generate(lanUrl, { small: false }, (q) => console.log(q));
  } else {
    console.log('  （未安装二维码库，手机请手动输入上面的「局域网访问」地址）');
  }
  console.log('==================================================');
  console.log('  注意：地址里的数字（' + ip + '）是电脑当前IP，换WiFi/重启后可能变化，');
  console.log('  每次以本窗口显示的为准。关闭本窗口即停止服务器。');
  console.log('==================================================');
});
