using System;
using System.IO;
using System.IO.Compression;
using System.Diagnostics;
using System.Reflection;
using System.Runtime.InteropServices;

// 方块突击 单文件绿色版 引导器（尾随数据自解压）
// 结构：本 exe = 引导器自身字节 + dist_selfextract.zip（完整游戏目录） + [8字节 zip长度]
// 运行：读取自身尾部的 zip → 解压到临时目录 → 启动 方块突击.exe → 引导器退出。
namespace FangKuaiSFX
{
    static class Program
    {
        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern int MessageBoxW(IntPtr hWnd, string text, string caption, uint type);

        [STAThread]
        static void Main()
        {
            string logDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "方块突击");
            Directory.CreateDirectory(logDir);
            string log = Path.Combine(logDir, "sfx.log");
            void Log(string m) { File.AppendAllText(log, DateTime.Now.ToString("HH:mm:ss") + " " + m + "\r\n"); }
            try
            {
                Log("绿色版启动");
                // 单文件模式下 Assembly.Location 为空，必须用 Environment.ProcessPath 拿自身 exe 路径
                string self = Environment.ProcessPath;
                if (string.IsNullOrEmpty(self)) self = Path.Combine(AppContext.BaseDirectory, "方块突击绿色版.exe");
                long fileLen = new FileInfo(self).Length;
                Log("self=" + self + " len=" + fileLen);

                // 读尾部 8 字节 = zip 长度
                byte[] lenBuf = new byte[8];
                using (var fs = File.OpenRead(self))
                {
                    fs.Seek(-8, SeekOrigin.End);
                    fs.Read(lenBuf, 0, 8);
                }
                long zipLen = BitConverter.ToInt64(lenBuf, 0);
                long zipStart = fileLen - zipLen - 8;
                if (zipStart < 0 || zipLen <= 0) throw new Exception("压缩包数据损坏");
                Log("zipLen=" + zipLen + " zipStart=" + zipStart);

                string target = Path.Combine(Path.GetTempPath(), "方块突击", "run_" + Guid.NewGuid().ToString("N").Substring(0, 8));
                Directory.CreateDirectory(target);
                Log("target=" + target);

                // 读取 zip 段并解压（保留 runtimes/ 等目录结构）
                using (var fs = File.OpenRead(self))
                {
                    fs.Seek(zipStart, SeekOrigin.Begin);
                    using (var ms = new MemoryStream())
                    {
                        byte[] buf = new byte[1 << 20];
                        long remaining = zipLen;
                        while (remaining > 0)
                        {
                            int n = fs.Read(buf, 0, (int)Math.Min(buf.Length, remaining));
                            if (n <= 0) throw new Exception("读取压缩包中断");
                            ms.Write(buf, 0, n);
                            remaining -= n;
                        }
                        ms.Position = 0;
                        using (var zip = new ZipArchive(ms, ZipArchiveMode.Read))
                        {
                            foreach (var e in zip.Entries)
                            {
                                string p = Path.Combine(target, e.FullName.Replace('/', Path.DirectorySeparatorChar));
                                if (e.FullName.EndsWith("/")) { Directory.CreateDirectory(p); continue; }
                                Directory.CreateDirectory(Path.GetDirectoryName(p));
                                using (var src = e.Open())
                                using (var dst = File.Create(p))
                                    src.CopyTo(dst);
                            }
                        }
                    }
                }

                // 启动游戏（游戏进程独立，引导器退出不影响它）
                string gameExe = Path.Combine(target, "方块突击.exe");
                if (!File.Exists(gameExe)) throw new Exception("解压后未找到 方块突击.exe");
                var psi = new ProcessStartInfo
                {
                    FileName = gameExe,
                    WorkingDirectory = target,
                    UseShellExecute = true
                };
                Process.Start(psi);
                Log("已启动游戏进程");
            }
            catch (Exception ex)
            {
                Log("错误: " + ex);
                MessageBoxW(IntPtr.Zero, "启动失败：" + ex.Message, "方块突击", 0x10);
            }
        }
    }
}
