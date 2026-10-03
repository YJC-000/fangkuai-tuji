using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace FangKuaiBrowser
{
    // 方块突击 V2.0 —— 游戏专用浏览器
    // 双击即玩：内嵌 node + 服务器 + 游戏页面，WebView2 窗口直接打开游戏，不跳转系统浏览器。
    public class MainForm : Form
    {
        private const int Port = 8080;
        private const string GameKey = "方块突击";
        private readonly string _appDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "方块突击");
        private readonly string _versionFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "方块突击", "VERSION");

        private WebView2 _web;
        private Panel _rail;          // 右侧悬浮工具条
        private Button _btnLan;       // 复制局域网地址
        private Button _btnRemote;    // 远程联机
        private Button _btnReload;    // 刷新
        private Label _railTip;

        private Process _nodeProc;
        private Process _cfProc;
        private string _remoteUrl = "";
        private bool _closing;

        // ===== 【V2.1.1 全屏修复】无边框全屏（F11 切换）+ 消除最大化/全屏时顶部白边 =====
        private bool _fullscreen;
        private Rectangle _savedBounds;

        public MainForm()
        {
            Log("== 方块突击 V2.1.2 启动 ==");
            Text = "方块突击 V2.1.2";
            ClientSize = new Size(1280, 800);
            MinimumSize = new Size(900, 600);
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Color.FromArgb(14, 20, 30);
            Icon = LoadAppIcon();
            KeyPreview = true; // 【全屏修复】让 F11 在 WebView2 获得焦点时也能被窗口收到
            FormClosing += OnFormClosing;

            BuildRail();
            BuildWebView();

            try { UnpackBundle(); } catch (Exception ex) { Log("解压失败: " + ex); ShowFatal("解压游戏资源失败：" + ex.Message); return; }
            EnsureServer();
            LoadGame();
        }

        // 运行日志（定位问题用）
        internal static void Log(string msg)
        {
            try
            {
                string p = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "方块突击", "launcher.log");
                File.AppendAllText(p, DateTime.Now.ToString("HH:mm:ss") + " " + msg + "\r\n");
            }
            catch { }
        }

        private Icon LoadAppIcon()
        {
            try
            {
                string exe = Path.Combine(AppContext.BaseDirectory, "方块突击.exe");
                if (File.Exists(exe)) return Icon.ExtractAssociatedIcon(exe);
            }
            catch { }
            return null;
        }

        // ===== 右侧悬浮工具条 =====
        private void BuildRail()
        {
            _rail = new Panel
            {
                Dock = DockStyle.Right,
                Width = 58,
                BackColor = Color.FromArgb(150, 16, 26, 40),
                Padding = new Padding(5, 8, 5, 8)
            };
            Controls.Add(_rail);

            _btnLan = RailButton("📋", "复制局域网地址（发给同一个 WiFi 下的朋友）");
            _btnRemote = RailButton("☁️", "远程联机：生成公网地址，任何网络都能进");
            _btnReload = RailButton("🔄", "刷新游戏");
            _btnLan.Click += (s, e) => CopyLan();
            _btnRemote.Click += async (s, e) => await StartRemoteAsync();
            _btnReload.Click += (s, e) => { try { _web?.Reload(); } catch { } };

            _railTip = new Label
            {
                Text = "方块突击",
                ForeColor = Color.FromArgb(180, 200, 220),
                Font = new Font("Microsoft YaHei UI", 8.5F, FontStyle.Bold),
                Dock = DockStyle.Bottom,
                Height = 46,
                TextAlign = ContentAlignment.MiddleCenter
            };
            _rail.Controls.Add(_railTip);
            _rail.Controls.Add(_btnReload);
            _rail.Controls.Add(_btnRemote);
            _rail.Controls.Add(_btnLan);
        }

        private Button RailButton(string icon, string tip)
        {
            var b = new Button
            {
                Text = icon,
                Font = new Font("Segoe UI Emoji", 15F),
                Dock = DockStyle.Top,
                Height = 46,
                FlatStyle = FlatStyle.Flat,
                BackColor = Color.FromArgb(30, 70, 100),
                ForeColor = Color.White,
                Cursor = Cursors.Hand,
                Margin = new Padding(0, 0, 0, 6)
            };
            b.FlatAppearance.BorderSize = 0;
            b.FlatAppearance.MouseOverBackColor = Color.FromArgb(45, 120, 170);
            var tipCtrl = new ToolTip();
            tipCtrl.SetToolTip(b, tip);
            return b;
        }

        private void BuildWebView()
        {
            _web = new WebView2 { Dock = DockStyle.Fill };
            Controls.Add(_web);
            _web.BringToFront();
        }

        // ===== 【V2.1.2 全屏修复】无边框真全屏：点最大化按钮/双击标题栏/F11 直接全屏 =====
        private void EnterFullscreen()
        {
            _fullscreen = true;
            _savedBounds = Bounds; // 记住退出全屏后的位置大小
            FormBorderStyle = FormBorderStyle.None;   // 去掉标题栏和边框
            WindowState = FormWindowState.Maximized;  // 配合 WM_GETMINMAXINFO 铺满整屏（含任务栏）
            Log("已进入无边框全屏");
        }

        private void ExitFullscreen()
        {
            _fullscreen = false;
            FormBorderStyle = FormBorderStyle.Sizable;
            WindowState = FormWindowState.Normal;
            if (!_savedBounds.IsEmpty) Bounds = _savedBounds;
            Log("已退出全屏");
        }

        private void ToggleFullscreen()
        {
            if (_fullscreen) ExitFullscreen(); else EnterFullscreen();
        }

        // 键盘：WebView2 聚焦时也拦截 F11（ProcessCmdKey 沿控件链上抛，一定收得到）
        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == Keys.F11) { ToggleFullscreen(); return true; }
            return base.ProcessCmdKey(ref msg, keyData);
        }

        // ===== 【V2.1.1 全屏修复】消除最大化/全屏时顶部的大白边 =====
        // 原因：Windows 默认允许最大化窗口带上系统边框的“外扩”，在 DPI/高分屏下顶部会露出
        // 一条系统边框缝隙；这里把最大化的位置与尺寸严格钉死在工作区，边缝消失。
        [StructLayout(LayoutKind.Sequential)]
        private struct POINT { public int x; public int y; }
        [StructLayout(LayoutKind.Sequential)]
        private struct MINMAXINFO
        {
            public POINT ptReserved;
            public POINT ptMaxSize;
            public POINT ptMaxPosition;
            public POINT ptMinTrackSize;
            public POINT ptMaxTrackSize;
        }
        protected override void WndProc(ref Message m)
        {
            const int WM_GETMINMAXINFO = 0x0024;
            const int WM_SYSCOMMAND = 0x0112;
            const int SC_MAXIMIZE = 0xF030;

            // 【V2.1.2】点最大化按钮 / 双击标题栏 → 直接进入无边框真全屏（而不是普通最大化）
            if (m.Msg == WM_SYSCOMMAND)
            {
                int cmd = m.WParam.ToInt32() & 0xFFF0;
                if (cmd == SC_MAXIMIZE)
                {
                    if (!_fullscreen) EnterFullscreen();
                    m.Result = IntPtr.Zero;
                    return;
                }
            }

            if (m.Msg == WM_GETMINMAXINFO)
            {
                var mmi = (MINMAXINFO)Marshal.PtrToStructure(m.LParam, typeof(MINMAXINFO));
                Screen screen = Screen.FromHandle(Handle);
                // 全屏时钉住整屏（含任务栏 = “全部全屏”）；普通窗口钉工作区（任务栏仍可见）
                Rectangle target = _fullscreen ? screen.Bounds : screen.WorkingArea;
                mmi.ptMaxPosition.x = target.Left;
                mmi.ptMaxPosition.y = target.Top;
                mmi.ptMaxSize.x = target.Width;
                mmi.ptMaxSize.y = target.Height;
                mmi.ptMaxTrackSize.x = target.Width;
                mmi.ptMaxTrackSize.y = target.Height;
                Marshal.StructureToPtr(mmi, m.LParam, true);
                m.Result = IntPtr.Zero;
                return;
            }
            base.WndProc(ref m);
        }

        // ===== 解压内置资源（版本变化才重新解压） =====
        // 【V2.1.1】资源包现在是 Content 文件：单文件发布时会随 exe 打进包体，
        // 运行时由 .NET 自解压到临时目录（AppContext.BaseDirectory），直接按文件读取即可。
        private string BundleZipPath()
        {
            // 单文件自解压模式：内容文件被解压到临时目录；普通目录发布：就在 exe 旁边
            return Path.Combine(AppContext.BaseDirectory, "_bundle.zip");
        }

        private void UnpackBundle()
        {
            if (!Directory.Exists(_appDir)) Directory.CreateDirectory(_appDir);
            string currentVer = File.Exists(_versionFile) ? File.ReadAllText(_versionFile, Encoding.UTF8).Trim() : "";
            string newVer = ExtractVersionFromBundle();
            Log("当前版本=" + currentVer + " 内置版本=" + newVer);
            if (currentVer == newVer && File.Exists(Path.Combine(_appDir, "server.js")) && File.Exists(Path.Combine(_appDir, "枪战.HTML")))
                return; // 已是最新，直接用

            Log("开始解压内置资源…");
            using (var stream = File.OpenRead(BundleZipPath()))
            using (var zip = new ZipArchive(stream, ZipArchiveMode.Read))
            {
                foreach (var e in zip.Entries)
                {
                    string target = Path.Combine(_appDir, e.FullName.Replace('/', Path.DirectorySeparatorChar));
                    if (e.FullName.EndsWith("/")) { Directory.CreateDirectory(target); continue; }
                    Directory.CreateDirectory(Path.GetDirectoryName(target));
                    if (e.Name == "VERSION") { File.WriteAllText(target, newVer, Encoding.UTF8); continue; }
                    using (var src = e.Open())
                    using (var dst = File.Create(target))
                        src.CopyTo(dst);
                }
            }
            Log("解压完成");
        }

        private string ExtractVersionFromBundle()
        {
            try
            {
                using (var stream = File.OpenRead(BundleZipPath()))
                using (var zip = new ZipArchive(stream, ZipArchiveMode.Read))
                {
                    var v = zip.GetEntry("VERSION");
                    if (v != null)
                        using (var r = new StreamReader(v.Open(), Encoding.UTF8))
                            return r.ReadToEnd().Trim();
                }
            }
            catch { }
            return "";
        }

        // ===== 服务器：检测端口，没有则用内置 node 启动 =====
        private void EnsureServer()
        {
            try
            {
                if (IsGameUp()) { Log("检测到已有游戏服务器，直接复用"); return; } // 已有本游戏服务器在跑，直接复用

                string nodePath = Path.Combine(_appDir, "node.exe");
                if (!File.Exists(nodePath)) { Log("未找到 node.exe"); ShowFatal("未找到内置运行环境 node.exe"); return; }

                var psi = new ProcessStartInfo
                {
                    FileName = nodePath,
                    Arguments = "server.js",
                    WorkingDirectory = _appDir,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    WindowStyle = ProcessWindowStyle.Hidden
                };
                _nodeProc = Process.Start(psi);
                Log("已启动内置 node 服务器 pid=" + _nodeProc.Id);
            }
            catch (Exception ex)
            {
                Log("启动服务器失败: " + ex);
                ShowFatal("启动游戏服务器失败：" + ex.Message);
            }
        }

        private bool IsGameUp()
        {
            try
            {
                var req = (HttpWebRequest)WebRequest.Create("http://127.0.0.1:" + Port + "/");
                req.Timeout = 800;
                using (var resp = (HttpWebResponse)req.GetResponse())
                using (var sr = new StreamReader(resp.GetResponseStream()))
                {
                    string body = sr.ReadToEnd();
                    return resp.StatusCode == HttpStatusCode.OK && body.Contains(GameKey);
                }
            }
            catch { return false; }
        }

        // ===== 加载游戏（WebView2 必须在 UI 线程初始化，否则 COM 报 RPC_E_CHANGED_MODE） =====
        private async void LoadGame()
        {
            // 等服务器就绪
            for (int i = 0; i < 60; i++)
            {
                if (IsGameUp()) break;
                await Task.Delay(200);
            }
            Log("服务器就绪，初始化 WebView2…");
            try
            {
                var env = await CoreWebView2Environment.CreateAsync(null, Path.Combine(_appDir, "webview2data"));
                Log("WebView2 环境创建完成");
                await _web.EnsureCoreWebView2Async(env);
                Log("WebView2 控件就绪，导航游戏页面…");
                _web.CoreWebView2.Settings.AreDefaultContextMenusEnabled = true;
                _web.CoreWebView2.Settings.AreDevToolsEnabled = false;
                _web.CoreWebView2.Settings.IsStatusBarEnabled = false;
                // 【全屏修复】F11 由 ProcessCmdKey 统一拦截（见下方），这里不用重复挂事件
                _web.CoreWebView2.NavigationCompleted += (s, e) =>
                    Log("导航完成: HttpStatus=" + e.HttpStatusCode + " 成功=" + e.IsSuccess);
                _web.CoreWebView2.Navigate("http://127.0.0.1:" + Port + "/");
                Log("导航已发出");
            }
            catch (Exception ex) { Log("WebView2 初始化失败: " + ex); }
        }

        // ===== 局域网地址 =====
        private void CopyLan()
        {
            try
            {
                string ip = GetLanIP();
                string url = "http://" + ip + ":" + Port;
                Clipboard.SetText(url);
                _btnLan.Text = "✅";
                _railTip.Text = "已复制局域网地址";
                var t = new System.Windows.Forms.Timer { Interval = 1500 };
                t.Tick += (s, e) => { _btnLan.Text = "📋"; _railTip.Text = "方块突击"; t.Stop(); t.Dispose(); };
                t.Start();
            }
            catch { _railTip.Text = "获取地址失败"; }
        }

        private string GetLanIP()
        {
            var virtualRe = new Regex("vmware|vmnet|virtualbox|vethernet|hyper-?v|docker|wsl|loopback|pseudo|tap-?adapter|tunnel|vpn|bluetooth|teredo|isatap|6to4|以太网|本地连接|以太网 \\*", RegexOptions.IgnoreCase);
            string fallback = null;
            foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
            {
                bool isVirtual = virtualRe.IsMatch(ni.Name);
                foreach (var ua in ni.GetIPProperties().UnicastAddresses)
                {
                    if (ua.Address.AddressFamily != AddressFamily.InterNetwork) continue;
                    if (IPAddress.IsLoopback(ua.Address)) continue;
                    byte[] b = ua.Address.GetAddressBytes();
                    if (b[0] == 169 && b[1] == 254) continue;
                    if (isVirtual) { fallback = fallback ?? ua.Address.ToString(); continue; }
                    return ua.Address.ToString();
                }
            }
            return fallback ?? "127.0.0.1";
        }

        // ===== 远程联机（cloudflared 隧道） =====
        private async Task StartRemoteAsync()
        {
            if (_cfProc != null && !_cfProc.HasExited && !string.IsNullOrEmpty(_remoteUrl))
            {
                new RemoteForm(_remoteUrl, StopRemote).Show(this);
                return;
            }
            if (_cfProc != null && !_cfProc.HasExited)
            {
                _railTip.Text = "隧道启动中…";
                return;
            }

            string cfPath = Path.Combine(_appDir, "cloudflared.exe");
            if (!File.Exists(cfPath)) { _railTip.Text = "缺少 cloudflared.exe"; return; }

            _railTip.Text = "正在创建远程隧道…";
            var psi = new ProcessStartInfo
            {
                FileName = cfPath,
                Arguments = "tunnel --url http://127.0.0.1:" + Port + " --no-autoupdate --protocol http2",
                WorkingDirectory = _appDir,
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            _cfProc = Process.Start(psi);
            var urlRe = new Regex(@"https://[a-z0-9-]+\.trycloudflare\.com");
            var sb = new StringBuilder();

            _cfProc.OutputDataReceived += (s, e) =>
            {
                if (e.Data == null) return;
                sb.AppendLine(e.Data);
                var m = urlRe.Match(e.Data);
                if (m.Success && string.IsNullOrEmpty(_remoteUrl))
                {
                    _remoteUrl = m.Value;
                    this.Invoke((Action)(() =>
                    {
                        _railTip.Text = "远程已开启";
                        new RemoteForm(_remoteUrl, StopRemote).Show(this);
                    }));
                }
            };
            _cfProc.ErrorDataReceived += (s, e) => { if (e.Data != null) sb.AppendLine(e.Data); };
            _cfProc.BeginOutputReadLine();
            _cfProc.BeginErrorReadLine();
        }

        private void StopRemote()
        {
            try { if (_cfProc != null && !_cfProc.HasExited) _cfProc.Kill(); } catch { }
            _cfProc = null;
            _remoteUrl = "";
            _railTip.Text = "远程已停止";
        }

        // ===== 关闭时清理子进程 =====
        private void OnFormClosing(object sender, FormClosingEventArgs e)
        {
            _closing = true;
            try { if (_nodeProc != null && !_nodeProc.HasExited) _nodeProc.Kill(); } catch { }
            try { if (_cfProc != null && !_cfProc.HasExited) _cfProc.Kill(); } catch { }
        }

        private void ShowFatal(string msg)
        {
            MessageBox.Show(this, msg, "方块突击", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }
}
