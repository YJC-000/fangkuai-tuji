# -*- coding: utf-8 -*-
# 把 dist_dir（游戏完整目录）压缩成 dist_selfextract.zip（保留目录结构），供绿色版引导器内嵌。
# 用法：先 dotnet publish 生成 dist_dir，再运行本脚本。
import zipfile, os

BASE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(BASE, 'dist_dir')
OUT = os.path.join(BASE, 'dist_selfextract.zip')

if os.path.exists(OUT): os.remove(OUT)
with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(SRC):
        for fn in files:
            full = os.path.join(root, fn)
            rel = os.path.relpath(full, SRC)
            z.write(full, rel)
print('packed:', OUT, os.path.getsize(OUT))
