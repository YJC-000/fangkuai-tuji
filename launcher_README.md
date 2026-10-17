# 方块突击 V3.3.0 电脑版启动器

## 说明
电脑版不再使用 HTML 内置菜单，改用本启动器（Python tkinter 原生窗口）。

## 运行
```bash
python launcher.py
```

## 打包成 exe（Windows）
```bash
pip install pyinstaller
pyinstaller --onefile --windowed --name "方块突击" --icon=icon.ico launcher.py
```

## 功能
- 4 个游戏模式：生存、团队死斗、无尽狂潮、联机
- 原生设置页：灵敏度、画质、天气、音量
- 暖色 FlClash 风格 UI
- 自动启动本地 HTTP 服务器，浏览器打开即玩
