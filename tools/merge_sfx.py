# -*- coding: utf-8 -*-
# 把引导器 exe 与游戏压缩包合并成单文件绿色版：
# 结构 = 引导器字节 + dist_selfextract.zip + [8字节 zip长度]
# 用法：先 dotnet publish LauncherSFX 得到 sfx_stub_out\方块突击绿色版.exe，再运行本脚本。
import struct, os

BASE = os.path.dirname(os.path.abspath(__file__))
STUB = os.path.join(BASE, 'sfx_stub_out', '方块突击绿色版.exe')
ZIP = os.path.join(BASE, 'dist_selfextract.zip')
OUT = os.path.join(BASE, '方块突击绿色版.exe')

stub = open(STUB, 'rb').read()
zipdata = open(ZIP, 'rb').read()
with open(OUT, 'wb') as f:
    f.write(stub)
    f.write(zipdata)
    f.write(struct.pack('<Q', len(zipdata)))   # 长度必须放文件最末尾，引导器从尾部读取
print('merged:', OUT, os.path.getsize(OUT))
