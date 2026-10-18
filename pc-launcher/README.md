# 方块突击 · 电脑版启动器（C# WinForms · V3.4.0）

原生 Windows 启动器（非 HTML 外壳），暖色 FlClash 风格，左侧导航 + 右侧内容布局。
启动后在后台用纯 C# `HttpListener` 托管游戏，**不弹命令提示符**，游戏资源（`枪战.HTML`、`three.min.js`、`peerjs.min.js`）以内嵌 `game.zip` 打进单个 exe，单文件即可玩。

## 功能

- **生存模式 / 团队死斗 / 无尽狂潮**：本地单人
- **远程联机（P2P 直连，与手机版互通）**：
  - 创建房间：自动生成 4 位房间号，可选密码；房间信息（房间号、主机名、是否有密码、合作/对战、人数）发布到静态托管 API（crudcrud）
  - 房间号占用检测
  - 房间列表：实时刷新，显示锁标识与「合作/对战」模式标签
  - 随机进入：一键加入任意无密码房间
  - 关闭启动器时自动从托管网站删除自己创建的房间
  - 游戏内用 **PeerJS + WebRTC DataChannel 直连**（主机 Peer ID = `fk-tuji-<房间号>`，PeerJS 公共云做信令，STUN 用 miwifi/qq/google），电脑端与手机 APK 走同一套 P2P，无需自建 WebSocket 服务器；内置网络抖动自动重连、断线重连、幽灵占用重试
- **游戏设置**：鼠标/触摸/手柄灵敏度、天气（默认/早晨/夜晚/永久早晨/永久夜晚）、画质、主音量、手柄校准、控制方式，内容超出可滚动
- 窗口图标内嵌，无白色边框，启动无黑窗

## 从源码构建

环境：Windows 10/11（自带 .NET Framework 4.8）、.NET SDK 6+（用于 `dotnet build`）。

1. 把仓库根目录的 `枪战.HTML`、`three.min.js`、`peerjs.min.js` 三个文件打包成 `game.zip`。
   注意中文文件名必须用 .NET `ZipFile`（不要用 `Compress-Archive`，会导致中文名乱码）：

   ```powershell
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   $stage = "gamestage"; New-Item -ItemType Directory -Force $stage | Out-Null
   Copy-Item ..\枪战.HTML, ..\three.min.js, ..\peerjs.min.js $stage -Force
   if (Test-Path game.zip) { Remove-Item game.zip -Force }
   [System.IO.Compression.ZipFile]::CreateFromDirectory((Resolve-Path $stage), (Join-Path (Get-Location) 'game.zip'), 'Optical', $false)
   ```

2. 编译：

   ```powershell
   dotnet build launcher_v340.csproj -c Release -o dist
   ```

   产物 `dist\方块突击V340.exe`（约 380 KB，内嵌全部资源）即可单独运行。

## 文件

- `Program.cs`：启动器全部代码（UI、自绘控件、本地服务器、房间 API）
- `launcher_v340.csproj`：工程文件（net48 / WinForms / 内嵌 game.zip + launcher.ico）
- `launcher.ico`：程序与窗口图标
- `game.zip`：游戏资源（构建时按上面步骤生成）

## 房间 API

使用免费匿名的 crudcrud：

```
https://crudcrud.com/api/<endpoint>/rooms
```

房间字段：`roomId`(房间号)、`name`(主机昵称)、`hasPwd`(bool)、`players`(int)、`mode`(4=合作/6=对战)、`ts`(unix 秒)。
endpoint 常量同时写在电脑版 `Program.cs`、手机版 Kotlin 与 `枪战.HTML` 中，更换时三处需同步。
