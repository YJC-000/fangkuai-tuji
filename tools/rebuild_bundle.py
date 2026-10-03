# -*- coding: utf-8 -*-
# 重建 exe 内嵌资源包 _bundle.zip（VERSION 变化时 exe 会自动重新解压更新）
# 用法：把 _game.zip（完整游戏目录，含 node.exe/cloudflared.exe 等）放在本项目根目录，运行本脚本。
import zipfile, os, shutil, glob

BASE = os.path.dirname(os.path.abspath(__file__))       # 脚本所在目录 = 项目根目录
SRC = os.path.join(BASE, '_game.zip')                   # 上次打包的完整游戏目录压缩包
OUT = os.path.join(BASE, '_bundle.zip')
STAGE = os.path.join(BASE, '_stage_bundle')
VERSION = '20261002C'                                   # 版本号变化 → exe 检测到后重新解压

if os.path.exists(STAGE): shutil.rmtree(STAGE)
os.makedirs(STAGE)
with zipfile.ZipFile(SRC) as z:
    z.extractall(STAGE)

# 覆盖最新前端/服务端/图标
shutil.copy2(os.path.join(BASE, '枪战.HTML'), os.path.join(STAGE, '枪战.HTML'))
shutil.copy2(os.path.join(BASE, 'server.js'), os.path.join(STAGE, 'server.js'))
for f in glob.glob(os.path.join(BASE, 'icon*.*')):
    shutil.copy2(f, os.path.join(STAGE, os.path.basename(f)))

# 写版本文件
with open(os.path.join(STAGE, 'VERSION'), 'w', encoding='utf-8') as f:
    f.write(VERSION)

# 重新打包
if os.path.exists(OUT): os.remove(OUT)
with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(STAGE):
        for fn in files:
            full = os.path.join(root, fn)
            rel = os.path.relpath(full, STAGE)
            z.write(full, rel)
shutil.rmtree(STAGE)
print('bundle rebuilt:', OUT, os.path.getsize(OUT), 'VERSION=' + VERSION)
