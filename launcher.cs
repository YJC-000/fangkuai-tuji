using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace GunLauncher {

  // 半透明日志面板：背景完全透明（透出窗体微透背景），只画文字
  class LogPanel : Panel {
    List<string> lines = new List<string>();
    int top = 0;
    public LogPanel() {
      DoubleBuffered = true;
      BackColor = Color.Transparent;
      Font = new Font("Consolas", 10F);
      ForeColor = Color.FromArgb(215, 225, 240);
    }
    int VisibleLines() { return Math.Max(1, ClientSize.Height / (int)(Font.GetHeight() + 4)); }
    public void Append(string t) {
      lines.Add(t);
      if (lines.Count > 300) lines.RemoveRange(0, lines.Count - 300);
      top = Math.Max(0, lines.Count - VisibleLines());
      Invalidate();
    }
    protected override void OnPaint(PaintEventArgs e) {
      e.Graphics.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
      float lh = Font.GetHeight() + 4;
      float y = 4;
      using (var br = new SolidBrush(ForeColor)) {
        for (int i = top; i < lines.Count; i++) {
          e.Graphics.DrawString(lines[i], Font, br, 6, y);
          y += lh;
          if (y > ClientSize.Height - 4) break;
        }
      }
    }
    protected override void OnMouseWheel(MouseEventArgs e) {
      int v = VisibleLines();
      top -= Math.Sign(e.Delta) * 3;
      top = Math.Max(0, Math.Min(top, Math.Max(0, lines.Count - v)));
      Invalidate();
    }
    protected override void OnMouseEnter(EventArgs e) { base.OnMouseEnter(e); Focus(); }
  }

  public class MainForm : Form {
    // ===== 自包含：游戏文件内嵌在本exe里，运行后解压到这里 =====
    const string GAME_VERSION = "20260927G";
    static readonly string DIR = Path.Combine(
      Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "方块突击");
    static readonly string NODE = Path.Combine(DIR, "node.exe");

    LogPanel logPanel;
    Button btnLan, btnRemote;
    PictureBox qrBox;                     // 二维码显示区（透明背景，融入微透窗口）
    Label qrHint;                         // 二维码区提示文字（画好二维码后隐藏，避免遮挡）
    Process srvProc = null;               // 局域网模式启动的 node server.js
    Process remoteProc = null;            // 远程模式启动的 node remote-run.js
    bool busy = false;                    // 防止重复点击

    [DllImport("dwmapi.dll")] static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int attrValue, int attrSize);
    [DllImport("user32.dll")] static extern bool SetProcessDpiAwarenessContext(IntPtr value);
    [DllImport("user32.dll")] static extern int SetWindowCompositionAttribute(IntPtr hwnd, ref WINDOWCOMPOSITIONATTRIBDATA data);

    [StructLayout(LayoutKind.Sequential)]
    struct WINDOWCOMPOSITIONATTRIBDATA {
      public int Attribute;   // 19 = WCA_ACCENT_POLICY
      public IntPtr Data;
      public int SizeOfData;
    }
    [StructLayout(LayoutKind.Sequential)]
    struct ACCENT_POLICY {
      public int AccentState;     // 4 = ACCENT_ENABLE_ACRYLICBLURBEHIND（亚克力模糊）
      public int AccentFlags;     // 2 = 绘制窗口边框
      public int GradientColor;   // 0xAABBGGRR：tint 颜色与透明度
      public int AnimationId;
    }

    public MainForm() {
      Text = "方块突击 · 游戏启动器";
      ClientSize = new Size(780, 700);
      StartPosition = FormStartPosition.CenterScreen;
      FormBorderStyle = FormBorderStyle.FixedSingle;
      MaximizeBox = false;
      Font = new Font("微软雅黑", 10F);
      BackColor = Color.FromArgb(18, 22, 34);
      DoubleBuffered = true;
      try { Icon = new Icon(Path.Combine(DIR, "launcher.ico")); } catch { }

      // 大标题
      var title = new Label();
      title.Text = "方块突击 · 游戏启动器";
      title.Font = new Font("微软雅黑", 21F, FontStyle.Bold);
      title.ForeColor = Color.White;
      title.AutoSize = false;
      title.TextAlign = ContentAlignment.MiddleCenter;
      title.Bounds = new Rectangle(0, 18, 780, 44);
      Controls.Add(title);

      // 副标题
      var sub = new Label();
      sub.Text = "局域网联机 或 远程联机，点按钮即可一键启动 · 链接自动复制，手机扫码直接进";
      sub.Font = new Font("微软雅黑", 9.5F);
      sub.ForeColor = Color.FromArgb(150, 162, 185);
      sub.AutoSize = false;
      sub.TextAlign = ContentAlignment.MiddleCenter;
      sub.Bounds = new Rectangle(0, 62, 780, 22);
      Controls.Add(sub);

      // 局域网按钮（深蓝色，比背景稍微浅一丢丢）
      btnLan = MakeButton("局域网联机", "同 WiFi · 手机扫码进入", new Rectangle(30, 92, 350, 112),
        Color.FromArgb(46, 60, 100), Color.FromArgb(30, 40, 72), Color.FromArgb(64, 82, 132));
      btnLan.Click += async (s, e) => await RunLan();
      Controls.Add(btnLan);

      // 远程按钮（深蓝色，比背景稍微浅一丢丢）
      btnRemote = MakeButton("远程联机", "不同网络 · 公网隧道", new Rectangle(400, 92, 350, 112),
        Color.FromArgb(46, 60, 100), Color.FromArgb(30, 40, 72), Color.FromArgb(64, 82, 132));
      btnRemote.Click += (s, e) => RunRemote();
      Controls.Add(btnRemote);

      // 二维码显示区（透明背景，融入微透窗口；白色二维码图片清晰可扫）
      qrBox = new PictureBox();
      qrBox.Bounds = new Rectangle(265, 222, 250, 250);
      qrBox.BackColor = Color.Transparent;
      qrBox.BorderStyle = BorderStyle.None;
      qrBox.SizeMode = PictureBoxSizeMode.Zoom;
      Controls.Add(qrBox);

      qrHint = new Label();
      qrHint.Text = "二维码将显示在这里";
      qrHint.Font = new Font("微软雅黑", 10F);
      qrHint.ForeColor = Color.FromArgb(110, 122, 145);
      qrHint.TextAlign = ContentAlignment.MiddleCenter;
      qrHint.Bounds = new Rectangle(265, 330, 250, 34);
      qrHint.BackColor = Color.Transparent;
      Controls.Add(qrHint);
      qrHint.BringToFront();

      // 日志面板（透明背景：透出窗口微透效果，只显示文字）
      logPanel = new LogPanel();
      logPanel.Bounds = new Rectangle(30, 486, 720, 150);
      Controls.Add(logPanel);

      // 底部提示
      var tip = new Label();
      tip.Text = "提示：二维码与链接会自动复制到剪贴板 · 关闭本窗口会停止服务器 / 公网隧道";
      tip.Font = new Font("微软雅黑", 9F);
      tip.ForeColor = Color.FromArgb(120, 132, 155);
      tip.AutoSize = false;
      tip.TextAlign = ContentAlignment.MiddleCenter;
      tip.Bounds = new Rectangle(0, 654, 780, 24);
      Controls.Add(tip);

      FormClosing += (s, e) => Cleanup();
    }

    // 圆角按钮（深蓝风格，深色主题，浅蓝描边）
    Button MakeButton(string main, string sub, Rectangle r, Color c1, Color c2, Color hover) {
      var b = new Button();
      b.Bounds = r;
      b.FlatStyle = FlatStyle.Flat;
      b.FlatAppearance.BorderSize = 1;
      b.FlatAppearance.BorderColor = Color.FromArgb(104, 142, 206);
      b.BackColor = c1;
      b.ForeColor = Color.White;
      b.Text = main + "\n\n" + sub;
      b.Font = new Font("微软雅黑", 14F, FontStyle.Bold);
      b.TextAlign = ContentAlignment.MiddleCenter;
      using (var gp = new GraphicsPath()) {
        int rad = 18;
        gp.AddArc(0, 0, rad, rad, 180, 90);
        gp.AddArc(r.Width - rad - 1, 0, rad, rad, 270, 90);
        gp.AddArc(r.Width - rad - 1, r.Height - rad - 1, rad, rad, 0, 90);
        gp.AddArc(0, r.Height - rad - 1, rad, rad, 90, 90);
        gp.CloseFigure();
        b.Region = new Region(gp);
      }
      b.MouseEnter += (s, e) => b.BackColor = hover;
      b.MouseLeave += (s, e) => b.BackColor = c1;
      return b;
    }

    // 窗口句柄创建后：深色标题栏 + 亚克力微透玻璃背景
    protected override void OnHandleCreated(EventArgs e) {
      base.OnHandleCreated(e);
      try {
        int dark = 1;
        DwmSetWindowAttribute(this.Handle, 20, ref dark, 4);  // 深色标题栏文字
        int cap = ColorTranslator.ToWin32(Color.FromArgb(16, 20, 32));
        DwmSetWindowAttribute(this.Handle, 35, ref cap, 4);   // 标题栏底色
        // 亚克力模糊：窗口背景透出一点桌面（微透玻璃感，类似 ChatGPT）
        var accent = new ACCENT_POLICY();
        accent.AccentState = 4;                       // ACCENT_ENABLE_ACRYLICBLURBEHIND
        accent.AccentFlags = 2;
        accent.GradientColor = unchecked((int)0xAA101420); // alpha170 的深蓝黑 tint
        var data = new WINDOWCOMPOSITIONATTRIBDATA();
        data.Attribute = 19;
        data.SizeOfData = Marshal.SizeOf(typeof(ACCENT_POLICY));
        IntPtr p = Marshal.AllocHGlobal(data.SizeOfData);
        Marshal.StructureToPtr(accent, p, false);
        data.Data = p;
        try { SetWindowCompositionAttribute(this.Handle, ref data); } catch { }
        Marshal.FreeHGlobal(p);
      } catch { }
    }

    // 半透明深色渐变背景（静态微透，配合亚克力形成玻璃质感）
    protected override void OnPaintBackground(PaintEventArgs e) {
      using (var br = new LinearGradientBrush(ClientRectangle,
            Color.FromArgb(205, 20, 24, 36), Color.FromArgb(185, 10, 13, 22), 90F)) {
        e.Graphics.FillRectangle(br, ClientRectangle);
      }
    }

    // 装饰线
    protected override void OnPaint(PaintEventArgs e) {
      base.OnPaint(e);
      using (var pen = new Pen(Color.FromArgb(104, 142, 206), 2)) {
        e.Graphics.DrawLine(pen, 30, 88, 750, 88);
      }
    }

    void Log(string t) {
      if (logPanel.IsHandleCreated) logPanel.BeginInvoke((Action)(() => logPanel.Append(t)));
      else logPanel.Append(t);
    }

    // ===== 自包含解压：把内嵌的 game.zip 释放到 %LOCALAPPDATA%\方块突击 =====
    static void EnsureUnpack() {
      string verFile = Path.Combine(DIR, "VERSION");
      if (File.Exists(Path.Combine(DIR, "node.exe")) &&
          File.Exists(verFile) && File.ReadAllText(verFile).Trim() == GAME_VERSION) {
        return; // 已解压过且版本一致
      }
      if (Directory.Exists(DIR)) {
        try { Directory.Delete(DIR, true); } catch { }
      }
      Directory.CreateDirectory(DIR);
      using (Stream s = Assembly.GetExecutingAssembly().GetManifestResourceStream("game.zip"))
      using (var za = new ZipArchive(s, ZipArchiveMode.Read)) {
        foreach (var e in za.Entries) {
          string fp = Path.Combine(DIR, e.FullName);
          if (e.FullName.EndsWith("/") || e.FullName.EndsWith("\\")) {
            Directory.CreateDirectory(fp);
            continue;
          }
          Directory.CreateDirectory(Path.GetDirectoryName(fp));
          using (var os = File.Create(fp))
          using (var ins = e.Open()) ins.CopyTo(os);
        }
      }
      // 顺便把图标也复制到解压目录（窗口图标用）
      try {
        using (Stream s = Assembly.GetExecutingAssembly().GetManifestResourceStream("launcher.ico"))
        using (var os = File.Create(Path.Combine(DIR, "launcher.ico"))) s.CopyTo(os);
      } catch { }
    }

    // 同步执行命令拿输出
    string RunCmd(string exe, string args) {
      try {
        var p = new Process();
        p.StartInfo.FileName = exe;
        p.StartInfo.Arguments = args;
        p.StartInfo.WorkingDirectory = DIR;
        p.StartInfo.UseShellExecute = false;
        p.StartInfo.RedirectStandardOutput = true;
        p.StartInfo.CreateNoWindow = true;
        p.StartInfo.StandardOutputEncoding = Encoding.UTF8;
        p.Start();
        string o = p.StandardOutput.ReadToEnd();
        p.WaitForExit();
        return o;
      } catch (Exception ex) {
        return "❌ 命令执行失败：" + ex.Message;
      }
    }

    // 解析 node 脚本输出的二维码矩阵（__QRURL__ / __QRSIZE__ / __QRROW__）
    bool ParseQR(string output, out string url, out List<string> rows) {
      url = null; rows = new List<string>();
      int size = 0;
      foreach (var line in output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries)) {
        if (line.StartsWith("__QRURL__:")) url = line.Substring(10).Trim();
        else if (line.StartsWith("__QRSIZE__:")) int.TryParse(line.Substring(11).Trim(), out size);
        else if (line.StartsWith("__QRROW__:")) rows.Add(line.Substring(10).Trim());
        else if (line == "__QREND__") break;
      }
      return url != null && size > 0 && rows.Count == size;
    }

    // 把 0/1 矩阵画成标准方形二维码（带白边），扫码不变形
    Bitmap DrawQRBitmap(List<string> rows) {
      int n = rows.Count;
      int scale = 7, quiet = 4;
      int px = (n + quiet * 2) * scale;
      var bmp = new Bitmap(px, px);
      using (var g = Graphics.FromImage(bmp)) {
        g.Clear(Color.White);
        using (var br = new SolidBrush(Color.Black)) {
          for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
              if (rows[r][c] == '1')
                g.FillRectangle(br, (c + quiet) * scale, (r + quiet) * scale, scale, scale);
        }
      }
      return bmp;
    }

    // 显示二维码图片（同时隐藏中间的提示文字，避免遮挡）
    void ShowQR(Bitmap img) {
      qrBox.Image = img;
      qrHint.Visible = false;
    }

    // 局域网模式：取地址二维码 → 启动 node server.js
    async Task RunLan() {
      if (busy) { Log("（上一个任务还在进行，请稍候）"); return; }
      busy = true;
      btnLan.Enabled = false;
      Log("▶ 正在准备局域网地址与二维码...");
      string outp = await Task.Run(() => RunCmd(NODE, "qrlan.js --json"));
      string url; List<string> rows;
      if (ParseQR(outp, out url, out rows)) {
        ShowQR(DrawQRBitmap(rows));
        Log("✅ 二维码已生成，手机扫码直接进！地址已复制到剪贴板：" + url);
        if (IsPortBusy()) {
          Log("✅ 局域网服务器已在运行（无需重复启动）。");
        } else {
          try {
            srvProc = new Process();
            srvProc.StartInfo.FileName = NODE;
            srvProc.StartInfo.Arguments = "server.js";
            srvProc.StartInfo.WorkingDirectory = DIR;
            srvProc.StartInfo.UseShellExecute = false;
            srvProc.StartInfo.CreateNoWindow = true;
            srvProc.Start();
            Log("✅ 局域网服务器已启动！");
          } catch (Exception ex) {
            Log("❌ 启动服务器失败：" + ex.Message);
          }
        }
      } else {
        Log("❌ 没拿到局域网地址，请检查网络后重试。");
        Log(outp.Trim());
      }
      busy = false;
      btnLan.Enabled = true;
    }

    // 检查本地 8080 服务器是否已在运行
    bool IsPortBusy() {
      try {
        using (var tc = new System.Net.Sockets.TcpClient()) {
          tc.Connect("127.0.0.1", 8080);
          return true;
        }
      } catch { return false; }
    }

    // 远程模式：实时显示 remote-run.js 输出，出现网址自动复制、矩阵自动画图
    void RunRemote() {
      if (busy) { Log("（上一个任务还在进行，请稍候）"); return; }
      busy = true;
      btnRemote.Enabled = false;
      Log("▶ 正在启动远程联机（约 5~15 秒出公网网址）...");
      try {
        remoteProc = new Process();
        remoteProc.StartInfo.FileName = NODE;
        remoteProc.StartInfo.Arguments = "remote-run.js --json";
        remoteProc.StartInfo.WorkingDirectory = DIR;
        remoteProc.StartInfo.UseShellExecute = false;
        remoteProc.StartInfo.RedirectStandardOutput = true;
        remoteProc.StartInfo.CreateNoWindow = true;
        remoteProc.StartInfo.StandardOutputEncoding = Encoding.UTF8;
        remoteProc.OutputDataReceived += OnRemoteLine;
        remoteProc.EnableRaisingEvents = true;
        remoteProc.Exited += (s, e) => {
          busy = false;
          BeginInvoke((Action)(() => { btnRemote.Enabled = true; }));
        };
        remoteProc.Start();
        remoteProc.BeginOutputReadLine();
      } catch (Exception ex) {
        Log("❌ 启动远程失败：" + ex.Message);
        busy = false;
        btnRemote.Enabled = true;
      }
    }

    // 远程输出一行一行进来：攒矩阵，收齐后画二维码
    int _size = 0;
    List<string> _rows = new List<string>();

    void OnRemoteLine(object s, DataReceivedEventArgs e) {
      if (string.IsNullOrEmpty(e.Data)) return;
      string line = e.Data;
      Log(line);
      if (line.StartsWith("__URL__:")) {
        string url = line.Substring(8).Trim();
        try { Clipboard.SetText(url); Log("📋 公网网址已自动复制到剪贴板！"); } catch { }
      } else if (line.StartsWith("__QRSIZE__:")) {
        int.TryParse(line.Substring(11).Trim(), out _size);
        _rows.Clear();
      } else if (line.StartsWith("__QRROW__:")) {
        _rows.Add(line.Substring(10).Trim());
      } else if (line == "__QREND__") {
        if (_size > 0 && _rows.Count == _size) {
          var img = DrawQRBitmap(_rows);
          BeginInvoke((Action)(() => ShowQR(img)));
        }
        _size = 0; _rows.Clear();
        busy = false;
        BeginInvoke((Action)(() => { btnRemote.Enabled = true; }));
      }
    }

    // 关闭窗口时清理自己启动的进程
    void Cleanup() {
      try {
        if (remoteProc != null && !remoteProc.HasExited) {
          var psi = new ProcessStartInfo("taskkill", "/PID " + remoteProc.Id + " /T /F");
          psi.WindowStyle = ProcessWindowStyle.Hidden;
          psi.CreateNoWindow = true;
          Process.Start(psi);
        }
      } catch { }
      try {
        if (srvProc != null && !srvProc.HasExited) srvProc.Kill();
      } catch { }
    }

    [STAThread]
    static void Main() {
      try { SetProcessDpiAwarenessContext((IntPtr)(-4)); } catch { }  // 高DPI下文字清晰
      try { EnsureUnpack(); } catch (Exception ex) {
        MessageBox.Show("游戏文件解压失败：" + ex.Message, "方块突击", MessageBoxButtons.OK, MessageBoxIcon.Error);
        return;
      }
      Application.EnableVisualStyles();
      Application.SetCompatibleTextRenderingDefault(false);
      Application.Run(new MainForm());
    }
  }
}
