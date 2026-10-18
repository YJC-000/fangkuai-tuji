# 方块突击 🎮

3D 第一人称方块枪战小游戏。前端 Three.js，提供**电脑版原生启动器**和**安卓 APK**，也能直接用浏览器打开 `枪战.HTML` 玩。

## ⬇️ 下载 V3.4.0

| 平台 | 文件 | 说明 |
|---|---|---|
| Windows 电脑 | [方块突击V3.4.0 电脑版.exe](releases/fangkuai-tuji-v340-pc.exe) | 单文件，双击即玩，无需安装；C# 原生启动器，启动不弹命令窗 |
| 安卓手机 | [方块突击V3.4.0 安卓版.apk](releases/fangkuai-tuji-v340.apk) | 横屏原生 UI，GitHub Actions 自动构建 |

> 电脑版 exe 已把游戏资源内嵌进单文件，只下载这一个文件即可；首次启动会在本机起一个小服务器托管游戏。

## ✨ V3.4.0 更新

- **电脑版改用 C# WinForms 原生启动器**（不再是 HTML 外壳），暖色 FlClash 风格：大圆角卡片、顺滑曲线、左侧导航 + 右侧内容，更适合电脑宽屏鼠标操作
- **远程联机房间系统**：一人开一个房间，房间（房间号/密码/主机名/合作或对战/人数）发布到静态托管网站
  - 自动生成房间号、**自动检测房间号是否被占用**
  - 房间列表实时刷新、**有密码的房间显示锁**、合作/对战标签
  - **随机进入**：一键加入任意无密码房间
  - 关闭启动器时自动删除自己创建的房间
  - 手机版与电脑版共用同一个房间托管服务，房间互通
- 游戏设置页：鼠标/触摸/手柄灵敏度、天气、画质、主音量、手柄校准、控制方式，内容超出可滚动

## 🎮 玩法

- **生存模式**：开局 6 只怪物，之后不管杀不杀，自动越刷越快（最快 1.5 秒一只，上限 30 只）
- **团队死斗**：小图双人 PvP，先达到击杀数获胜
- **无尽狂潮**：怪物持续不断，刷新越来越快
- **远程联机**：合作打怪（倒地 5 秒可被队友救起，复活 10 血）或对战
- 地图随机：障碍物、楼房位置/大小/颜色随机；可进楼爬楼梯上到楼顶打高处
- 拾取系统：弹药箱（步枪弹/狙击弹）、医疗包、手雷（近距离伤害高、圆形倒计时）

## 🎯 操作

| 操作 | 按键 |
|---|---|
| 移动 | WASD |
| 转视角 | 鼠标（右摇杆，手柄） |
| 开枪 | 左键 / RT |
| 瞄准 | 右键 / LT |
| 跳跃 | 空格 / A |
| 换弹 | R |
| 手雷 | 数字键 5，长按右键瞄准，松手可投 |
| 切武器 | 1~5 / LB、RB |

手机端横屏自动显示虚拟摇杆和按钮，可在设置里调整灵敏度、校准手柄。

## 🛠️ 从源码构建

### 电脑版（Windows）

环境：Windows 10/11（自带 .NET Framework 4.8）、.NET SDK 6+。

```powershell
# 把游戏资源打包成 game.zip 放进 pc-launcher 目录
Compress-Archive -Path 枪战.HTML, three.min.js, qrcode.min.js -DestinationPath pc-launcher\game.zip -Force
# 编译
dotnet build pc-launcher\launcher_v340.csproj -c Release -o pc-launcher\dist
```

产物 `pc-launcher\dist\方块突击V340.exe` 单文件即可运行。源码见 [`pc-launcher/`](pc-launcher/)。

### 安卓版

直接用 GitHub Actions 推送代码后自动构建 APK（工作流见 `.github/workflows/build-apk.yml`），产物在 Actions 构建结果和 `releases/` 目录。原生界面源码（Kotlin）在 `android/`。

## 📁 文件说明

| 文件 / 目录 | 说明 |
|---|---|
| `枪战.HTML` | 游戏前端（Three.js 单文件，含全部玩法） |
| `pc-launcher/` | 电脑版 C# WinForms 启动器源码（V3.4.0） |
| `android/` | 安卓版 Kotlin 原生 UI + Capacitor 工程 |
| `releases/` | 已打包的电脑版 exe 与各版本安卓 APK |
| `.github/workflows/` | 自动构建 APK 的工作流 |
| `server.js` | 旧版 Node.js 联机服务器（兼容保留） |

## 🔗 房间托管

房间列表使用免费匿名静态托管服务（crudcrud）存储房间元数据；同一 endpoint 常量写在电脑版 `pc-launcher/Program.cs` 和安卓版 `NetActivity.kt` 中，更换服务时两处需同步。
