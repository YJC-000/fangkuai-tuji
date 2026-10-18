using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Net.Http;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Web.Script.Serialization;

namespace GunLauncher {

  // ========== 暖色配色 ==========
  static class Theme {
    public static readonly Color BG_TOP    = Color.FromArgb(0x1A, 0x16, 0x12);
    public static readonly Color BG_BOTTOM = Color.FromArgb(0x24, 0x1E, 0x17);
    public static readonly Color CARD      = Color.FromArgb(0x2A, 0x24, 0x1D);
    public static readonly Color CARD_HOVER= Color.FromArgb(0x35, 0x2D, 0x24);
    public static readonly Color BORDER    = Color.FromArgb(0x3D, 0x34, 0x28);
    public static readonly Color TEXT      = Color.FromArgb(0xF5, 0xED, 0xE0);
    public static readonly Color TEXT2     = Color.FromArgb(0xB0, 0xA4, 0x98);
    public static readonly Color TEXT3     = Color.FromArgb(0x6B, 0x61, 0x58);
    public static readonly Color ORANGE    = Color.FromArgb(0xE8, 0xA0, 0x4A);
    public static readonly Color GREEN     = Color.FromArgb(0x8B, 0xB8, 0x76);
    public static readonly Color AMBER     = Color.FromArgb(0xD4, 0xA5, 0x74);
    public static readonly Color RED       = Color.FromArgb(0xC7, 0x7D, 0x5E);
  }

  // ========== 圆角面板 ==========
  class RoundPanel : Panel {
    public int Radius = 16;
    public Color BorderColor = Theme.BORDER;
    public int BorderWidth = 1;
    public Color FillColor = Theme.CARD;
    public bool Hover = false;
    public RoundPanel() { DoubleBuffered = true; BackColor = Color.Transparent; }
    protected override void OnPaint(PaintEventArgs e) {
      e.Graphics.SmoothingMode = SmoothingMode.AntiAlias;
      var c = Hover ? Theme.CARD_HOVER : FillColor;
      using (var path = RoundedRect(ClientRectangle, Radius))
      using (var br = new SolidBrush(c))
      using (var pen = new Pen(BorderColor, BorderWidth)) {
        e.Graphics.FillPath(br, path);
        e.Graphics.DrawPath(pen, path);
      }
    }
    public static GraphicsPath RoundedRect(Rectangle r, int radius) {
      var p = new GraphicsPath();
      int d = radius * 2;
      p.AddArc(r.X, r.Y, d, d, 180, 90);
      p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
      p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
      p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
      p.CloseFigure();
      return p;
    }
  }

  // ========== 圆角按钮 ==========
  class RoundButton : Button {
    public int Radius = 14;
    public Color BgColor = Theme.ORANGE;
    public Color TextColor = Color.FromArgb(0x1A, 0x17, 0x14);
    public Color HoverColor = Color.FromArgb(0xF0, 0xB0, 0x5A);
    bool hovering = false;
    public RoundButton() {
      SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
               ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
      FlatStyle = FlatStyle.Flat;
      FlatAppearance.BorderSize = 0;
      BackColor = Color.Transparent;
      Cursor = Cursors.Hand;
      MouseEnter += (s, e) => { hovering = true; Invalidate(); };
      MouseLeave += (s, e) => { hovering = false; Invalidate(); };
    }
    protected override void OnPaint(PaintEventArgs e) {
      e.Graphics.SmoothingMode = SmoothingMode.AntiAlias;
      var c = hovering ? HoverColor : BgColor;
      if (c.A > 0) {
        using (var path = RoundPanel.RoundedRect(ClientRectangle, Radius))
        using (var br = new SolidBrush(c)) {
          e.Graphics.FillPath(br, path);
        }
      }
      TextRenderer.DrawText(e.Graphics, Text, Font, ClientRectangle, TextColor,
        TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter | TextFormatFlags.NoPadding);
    }
  }

  // ========== 自绘滑块（避免原生 TrackBar 渲染冲突） ==========
  class CustomSlider : Control {
    int _min = 1, _max = 100, _value = 50;
    bool dragging = false;
    public int Value { get { return _value; } set { _value = Math.Max(_min, Math.Min(_max, value)); Invalidate(); } }
    public int Minimum { get { return _min; } set { _min = value; Invalidate(); } }
    public int Maximum { get { return _max; } set { _max = value; Invalidate(); } }
    public event Action<int> ValueChanged;
    public Color TrackColor = Color.FromArgb(0x3D, 0x34, 0x28);
    public Color FillColor = Theme.ORANGE;
    public CustomSlider() {
      SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
               ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
               ControlStyles.SupportsTransparentBackColor, true);
      BackColor = Color.FromArgb(0x22, 0x1D, 0x18);
      Height = 32;
    }
    float ThumbX() {
      float pad = 10;
      return pad + (Width - pad * 2) * (_value - _min) / (float)(_max - _min);
    }
    protected override void OnPaint(PaintEventArgs e) {
      var g = e.Graphics;
      g.SmoothingMode = SmoothingMode.AntiAlias;
      int cy = Height / 2;
      float pad = 10;
      // 轨道
      using (var br = new SolidBrush(TrackColor))
      using (var path = new GraphicsPath()) {
        int th = 6;
        path.AddArc(pad, cy - th/2, th, th, 90, 180);
        path.AddArc(Width - pad - th, cy - th/2, th, th, 270, 180);
        path.AddRectangle(new RectangleF(pad + th/2, cy - th/2, Width - pad*2 - th, th));
        g.FillPath(br, path);
      }
      // 已填充部分
      float tx = ThumbX();
      using (var br = new SolidBrush(FillColor))
      using (var path = new GraphicsPath()) {
        int th = 6;
        path.AddArc(pad, cy - th/2, th, th, 90, 180);
        path.AddLine(pad + th/2, cy, tx, cy);
        path.AddLine(tx, cy, pad + th/2, cy);
        path.AddRectangle(new RectangleF(pad + th/2, cy - th/2, tx - pad - th/2, th));
        g.FillPath(br, path);
      }
      // 滑块圆点
      using (var br = new SolidBrush(Color.FromArgb(0xF5, 0xED, 0xE0))) {
        g.FillEllipse(br, tx - 9, cy - 9, 18, 18);
      }
      // 数值
      using (var br = new SolidBrush(Theme.TEXT))
      using (var f = new Font("Microsoft YaHei UI", 9F)) {
        var sf = new StringFormat { Alignment = StringAlignment.Far, LineAlignment = StringAlignment.Center };
        g.DrawString(_value.ToString(), f, br, new RectangleF(Width - 36, 0, 36, Height), sf);
      }
    }
    void SetFromMouse(int x) {
      float pad = 10;
      float ratio = (x - pad) / (Width - pad * 2);
      ratio = Math.Max(0, Math.Min(1, ratio));
      _value = _min + (int)Math.Round(ratio * (_max - _min));
      Invalidate();
      ValueChanged?.Invoke(_value);
    }
    protected override void OnMouseDown(MouseEventArgs e) {
      base.OnMouseDown(e);
      dragging = true;
      SetFromMouse(e.X);
    }
    protected override void OnMouseMove(MouseEventArgs e) {
      base.OnMouseMove(e);
      if (dragging) SetFromMouse(e.X);
    }
    protected override void OnMouseUp(MouseEventArgs e) {
      base.OnMouseUp(e);
      dragging = false;
    }
  }

  // ========== 房间数据 ==========
  class RoomInfo {
    public string _id;
    public string roomId;
    public string name;
    public bool hasPwd;
    public int players;
  }

  // ========== 主窗体 ==========
  public class MainForm : Form {
    const string ROOM_API = "https://crudcrud.com/api/77ffb1c1c7424d8a9eeb5ed8e590fdab";
    static readonly string DIR = Path.Combine(
      Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "方块突击");
    static readonly string GAME_HTML = Path.Combine(DIR, "枪战.HTML");
    static readonly int PORT = 18080;

    Panel contentPanel;
    Panel navPanel;
    Panel topBar;
    HttpClient http = new HttpClient();

    // 导航按钮
    RoundButton btnSurvival, btnTeam, btnEndless, btnOnline, btnSettings;
    Label lblTitle;

    public MainForm() {
      try {
        Text = "方块突击 V3.4.1";
        Size = new Size(960, 640);
        MinimumSize = new Size(800, 560);
        StartPosition = FormStartPosition.CenterScreen;
        BackColor = Theme.BG_TOP;
        Font = new Font("Microsoft YaHei UI", 10F);
        ShowInTaskbar = true;
        WindowState = FormWindowState.Normal;
        LoadIcon();

        // 解压游戏文件
        ExtractGame();

        BuildUI();
        ShowMode("survival");
      } catch (Exception ex) {
        File.WriteAllText(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Desktop), "fk_launcher_error.txt"),
          "MainForm构造失败:\n" + ex.ToString());
        MessageBox.Show("界面初始化失败：" + ex.Message, "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
      }
    }

    void ExtractGame() {
      try {
        Directory.CreateDirectory(DIR);
        // 从内嵌资源解压
        var asm = Assembly.GetExecutingAssembly();
        using (var stream = asm.GetManifestResourceStream("GunLauncher.game.zip")) {
          if (stream != null) {
            using (var zip = new ZipArchive(stream, ZipArchiveMode.Read)) {
              foreach (var entry in zip.Entries) {
                var target = Path.Combine(DIR, entry.FullName);
                Directory.CreateDirectory(Path.GetDirectoryName(target));
                entry.ExtractToFile(target, true);
              }
            }
          }
        }
        // 如果没有内嵌资源，尝试从同目录复制
        if (!File.Exists(GAME_HTML)) {
          var local = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "枪战.HTML");
          if (File.Exists(local)) File.Copy(local, GAME_HTML, true);
        }
      } catch { }
    }

    void LoadIcon() {
      try {
        var asm = Assembly.GetExecutingAssembly();
        using (var stream = asm.GetManifestResourceStream("GunLauncher.launcher.ico")) {
          if (stream != null) this.Icon = new Icon(stream);
        }
      } catch { }
    }

    void BuildUI() {
      DoubleBuffered = true;

      // 顶部标题栏
      topBar = new Panel {
        Height = 56,
        BackColor = Color.FromArgb(0x1F, 0x1A, 0x15)
      };
      Controls.Add(topBar);

      lblTitle = new Label {
        Text = "  方块突击",
        Font = new Font("Microsoft YaHei UI", 18F, FontStyle.Bold),
        ForeColor = Theme.TEXT,
        Location = new Point(0, 0),
        Size = new Size(200, 56),
        TextAlign = ContentAlignment.MiddleLeft
      };
      topBar.Controls.Add(lblTitle);

      var lblVer = new Label {
        Text = "V3.4.1  ",
        Font = new Font("Microsoft YaHei UI", 9F),
        ForeColor = Theme.TEXT3,
        TextAlign = ContentAlignment.MiddleRight,
        Location = new Point(0, 0),
        Size = new Size(80, 56),
        Anchor = AnchorStyles.Top | AnchorStyles.Right
      };
      topBar.Controls.Add(lblVer);

      // 左侧导航
      navPanel = new FlowLayoutPanel {
        Width = 190,
        BackColor = Color.Transparent,
        Padding = new Padding(16, 12, 12, 16),
        FlowDirection = FlowDirection.TopDown,
        WrapContents = false,
        AutoScroll = false
      };
      Controls.Add(navPanel);

      btnSurvival = MakeNavButton("生存模式", Theme.ORANGE);
      btnTeam     = MakeNavButton("团队死斗", Theme.RED);
      btnEndless  = MakeNavButton("无尽狂潮", Theme.GREEN);
      btnOnline   = MakeNavButton("远程联机", Theme.AMBER);
      btnSettings = MakeNavButton("游戏设置", Theme.TEXT2);

      btnSurvival.Click += (s, e) => ShowMode("survival");
      btnTeam.Click     += (s, e) => ShowMode("team");
      btnEndless.Click  += (s, e) => ShowMode("endless");
      btnOnline.Click   += (s, e) => ShowMode("online");
      btnSettings.Click += (s, e) => ShowMode("settings");

      navPanel.Controls.Add(btnSurvival);
      navPanel.Controls.Add(btnTeam);
      navPanel.Controls.Add(btnEndless);
      navPanel.Controls.Add(btnOnline);
      navPanel.Controls.Add(btnSettings);

      // 内容区
      contentPanel = new Panel {
        BackColor = Color.Transparent
      };
      Controls.Add(contentPanel);

      ResizeLayout();
    }

    void ResizeLayout() {
      if (topBar == null || navPanel == null || contentPanel == null) return;
      int w = ClientSize.Width, h = ClientSize.Height;
      topBar.Location = new Point(0, 0);
      topBar.Size = new Size(w, 56);
      // 版本号靠右
      foreach (Control c in topBar.Controls) {
        if (c.Text == "V3.4.1  ") c.Location = new Point(w - 90, 0);
      }
      navPanel.Location = new Point(0, 56);
      navPanel.Size = new Size(190, h - 56);
      contentPanel.Location = new Point(190, 56);
      contentPanel.Size = new Size(w - 190, h - 56);
      contentPanel.Padding = new Padding(0, 0, 0, 0);
      foreach (Control c in contentPanel.Controls) {
        c.Location = new Point(12, 12);
        c.Size = new Size(contentPanel.Width - 24, contentPanel.Height - 24);
      }
    }

    protected override void OnResize(EventArgs e) {
      base.OnResize(e);
      ResizeLayout();
    }

    protected override void OnPaint(PaintEventArgs e) {
      base.OnPaint(e);
      var g = e.Graphics;
      g.SmoothingMode = SmoothingMode.AntiAlias;
      // 渐变背景
      using (var brush = new LinearGradientBrush(ClientRectangle, Theme.BG_TOP, Theme.BG_BOTTOM, 90f))
        g.FillRectangle(brush, ClientRectangle);
      // 三层波浪
      DrawWave(g, ClientSize.Width, ClientSize.Height, 0.12f, Color.FromArgb(25, 0xE8, 0xA0, 0x4A), 0);
      DrawWave(g, ClientSize.Width, ClientSize.Height, 0.10f, Color.FromArgb(18, 0x8B, 0xB8, 0x76), 1);
      DrawWave(g, ClientSize.Width, ClientSize.Height, 0.07f, Color.FromArgb(12, 0xD4, 0xA5, 0x74), 2);
    }
    void DrawWave(Graphics g, int w, int h, float amp, Color c, int layer) {
      using (var path = new GraphicsPath())
      using (var br = new SolidBrush(c)) {
        float baseY = h * (0.6f + layer * 0.13f);
        path.AddLine(0, h, 0, baseY);
        for (float x = 0; x <= w; x += 4) {
          float y = baseY + (float)Math.Sin(x * 0.008 + layer * 2) * h * amp
                           + (float)Math.Sin(x * 0.015 + layer) * h * amp * 0.5f;
          path.AddLine(x, y, x + 4, y);
        }
        path.AddLine(w, h, 0, h);
        path.CloseFigure();
        g.FillPath(br, path);
      }
    }

    RoundButton MakeNavButton(string text, Color color) {
      var btn = new RoundButton {
        Text = text,
        BgColor = Color.FromArgb(0x33, 0x2C, 0x24),
        TextColor = Theme.TEXT2,
        HoverColor = Color.FromArgb(0x3D, 0x34, 0x28),
        Radius = 12,
        Size = new Size(158, 44),
        Margin = new Padding(0, 4, 0, 4),
        Font = new Font("Microsoft YaHei UI", 11F)
      };
      return btn;
    }

    void SetNavActive(RoundButton active) {
      foreach (Control c in active.Parent.Controls) {
        if (c is RoundButton b) {
          b.BgColor = (b == active) ? Theme.CARD : Color.FromArgb(0x33, 0x2C, 0x24);
          b.TextColor = (b == active) ? Theme.TEXT : Theme.TEXT2;
          b.Invalidate();
        }
      }
    }

    void ShowMode(string mode) {
      contentPanel.Controls.Clear();
      try {
        switch (mode) {
          case "survival": SetNavActive(btnSurvival); BuildSurvivalPage(); break;
          case "team":     SetNavActive(btnTeam);     BuildTeamPage(); break;
          case "endless":  SetNavActive(btnEndless);  BuildEndlessPage(); break;
          case "online":   SetNavActive(btnOnline);   BuildOnlinePage(); break;
          case "settings": SetNavActive(btnSettings); BuildSettingsPage(); break;
        }
      } catch (Exception ex) {
        File.WriteAllText(Path.Combine(Path.GetTempPath(), "fk_page_error.txt"),
          mode + "\n" + ex.ToString());
        throw;
      }
    }

    // ========== 生存模式页 ==========
    void BuildSurvivalPage() {
      var card = new RoundPanel {
        Radius = 20,
        FillColor = Color.FromArgb(220, 0x2A, 0x24, 0x1D),
        Dock = DockStyle.Fill
      };
      contentPanel.Controls.Add(card);

      var title = new Label {
        Text = "生存模式",
        Font = new Font("Microsoft YaHei UI", 28F, FontStyle.Bold),
        ForeColor = Theme.ORANGE,
        Location = new Point(40, 36),
        AutoSize = true
      };
      card.Controls.Add(title);

      var desc = new Label {
        Text = "在随机生成的战场中生存，消灭红色机械敌人。\n拾取弹药和医疗包，利用建筑物掩护自己。",
        Font = new Font("Microsoft YaHei UI", 12F),
        ForeColor = Theme.TEXT2,
        Location = new Point(40, 90),
        Size = new Size(500, 60)
      };
      card.Controls.Add(desc);

      // 操作说明
      var opsTitle = new Label {
        Text = "操作说明",
        Font = new Font("Microsoft YaHei UI", 14F, FontStyle.Bold),
        ForeColor = Theme.AMBER,
        Location = new Point(40, 170),
        AutoSize = true
      };
      card.Controls.Add(opsTitle);

      string[] ops = {
        "WASD - 移动",
        "鼠标 - 视角",
        "左键 - 射击",
        "右键 - 瞄准",
        "空格 - 跳跃",
        "1/2/3 - 切换武器",
        "5 - 手雷（长按右键投掷）",
        "ESC - 暂停"
      };
      for (int i = 0; i < ops.Length; i++) {
        var lbl = new Label {
          Text = ops[i],
          Font = new Font("Consolas", 11F),
          ForeColor = Theme.TEXT2,
          Location = new Point(40 + (i % 2) * 220, 205 + (i / 2) * 32),
          AutoSize = true
        };
        card.Controls.Add(lbl);
      }

      var startBtn = new RoundButton {
        Text = "开始游戏",
        BgColor = Theme.ORANGE,
        HoverColor = Color.FromArgb(0xF0, 0xB0, 0x5A),
        TextColor = Color.FromArgb(0x1A, 0x17, 0x14),
        Radius = 16,
        Size = new Size(200, 56),
        Location = new Point(40, 380),
        Font = new Font("Microsoft YaHei UI", 16F, FontStyle.Bold)
      };
      startBtn.Click += (s, e) => LaunchGame(1);
      card.Controls.Add(startBtn);
    }

    // ========== 团队死斗页 ==========
    void BuildTeamPage() {
      var card = new RoundPanel { Radius = 20, FillColor = Color.FromArgb(220, 0x2A, 0x24, 0x1D), Dock = DockStyle.Fill };
      contentPanel.Controls.Add(card);

      card.Controls.Add(new Label {
        Text = "团队死斗",
        Font = new Font("Microsoft YaHei UI", 28F, FontStyle.Bold),
        ForeColor = Theme.RED, Location = new Point(40, 36), AutoSize = true
      });
      card.Controls.Add(new Label {
        Text = "与朋友对战，先达到击杀数的一方获胜。\n较小的地图，快节奏的对抗。",
        Font = new Font("Microsoft YaHei UI", 12F), ForeColor = Theme.TEXT2,
        Location = new Point(40, 90), Size = new Size(500, 60)
      });

      var startBtn = new RoundButton {
        Text = "开始游戏", BgColor = Theme.RED,
        HoverColor = Color.FromArgb(0xD7, 0x8D, 0x6E),
        TextColor = Color.FromArgb(0x1A, 0x17, 0x14), Radius = 16,
        Size = new Size(200, 56), Location = new Point(40, 380),
        Font = new Font("Microsoft YaHei UI", 16F, FontStyle.Bold)
      };
      startBtn.Click += (s, e) => LaunchGame(2);
      card.Controls.Add(startBtn);
    }

    // ========== 无尽狂潮页 ==========
    void BuildEndlessPage() {
      var card = new RoundPanel { Radius = 20, FillColor = Color.FromArgb(220, 0x2A, 0x24, 0x1D), Dock = DockStyle.Fill };
      contentPanel.Controls.Add(card);

      card.Controls.Add(new Label {
        Text = "无尽狂潮",
        Font = new Font("Microsoft YaHei UI", 28F, FontStyle.Bold),
        ForeColor = Theme.GREEN, Location = new Point(40, 36), AutoSize = true
      });
      card.Controls.Add(new Label {
        Text = "敌人会越来越多，越来越快。\n开局6只，随时间加速刷新，看你能撑多久。",
        Font = new Font("Microsoft YaHei UI", 12F), ForeColor = Theme.TEXT2,
        Location = new Point(40, 90), Size = new Size(500, 60)
      });

      var startBtn = new RoundButton {
        Text = "开始游戏", BgColor = Theme.GREEN,
        HoverColor = Color.FromArgb(0x9B, 0xC8, 0x86),
        TextColor = Color.FromArgb(0x1A, 0x17, 0x14), Radius = 16,
        Size = new Size(200, 56), Location = new Point(40, 380),
        Font = new Font("Microsoft YaHei UI", 16F, FontStyle.Bold)
      };
      startBtn.Click += (s, e) => LaunchGame(5);
      card.Controls.Add(startBtn);
    }

    // ========== 远程联机页 ==========
    Panel roomListPanel;
    Label lblRoomStatus;
    TextBox txtName, txtRoom, txtPwd;
    RadioButton rbCreate, rbJoin, rbCoop, rbVs;
    string hostedRoomApiId = null;  // 本机创建并托管在 crudcrud 的房间记录 _id

    void BuildOnlinePage() {
      var card = new RoundPanel { Radius = 20, FillColor = Color.FromArgb(220, 0x2A, 0x24, 0x1D), Dock = DockStyle.Fill };
      contentPanel.Controls.Add(card);

      card.Controls.Add(new Label {
        Text = "远程联机",
        Font = new Font("Microsoft YaHei UI", 24F, FontStyle.Bold),
        ForeColor = Theme.AMBER, Location = new Point(24, 16), AutoSize = true
      });

      // 左侧：房间列表
      var listCard = new RoundPanel {
        Radius = 14, FillColor = Color.FromArgb(0x1A, 0x17, 0x14),
        Location = new Point(24, 60), Size = new Size(340, 420)
      };
      card.Controls.Add(listCard);

      roomListPanel = new Panel {
        Dock = DockStyle.Fill, BackColor = Color.Transparent,
        AutoScroll = true, Padding = new Padding(8)
      };
      listCard.Controls.Add(roomListPanel);

      var listHeader = new Panel { Dock = DockStyle.Top, Height = 40, BackColor = Color.Transparent };
      listCard.Controls.Add(listHeader);

      lblRoomStatus = new Label {
        Text = "正在刷新…", Font = new Font("Microsoft YaHei UI", 10F),
        ForeColor = Theme.TEXT2, Dock = DockStyle.Left, Width = 160,
        TextAlign = ContentAlignment.MiddleLeft, Padding = new Padding(12, 0, 0, 0)
      };
      listHeader.Controls.Add(lblRoomStatus);

      var btnRefresh = new RoundButton {
        Text = "刷新", BgColor = Theme.CARD, HoverColor = Theme.CARD_HOVER,
        TextColor = Theme.AMBER, Radius = 10, Size = new Size(70, 30),
        Location = new Point(180, 5), Font = new Font("Microsoft YaHei UI", 9F)
      };
      btnRefresh.Click += (s, e) => RefreshRooms();
      listHeader.Controls.Add(btnRefresh);

      var btnRandom = new RoundButton {
        Text = "随机", BgColor = Theme.GREEN, HoverColor = Color.FromArgb(0x9B, 0xC8, 0x86),
        TextColor = Color.FromArgb(0x1A, 0x17, 0x14), Radius = 10, Size = new Size(70, 30),
        Location = new Point(255, 5), Font = new Font("Microsoft YaHei UI", 9F)
      };
      btnRandom.Click += (s, e) => RandomJoin();
      listHeader.Controls.Add(btnRandom);

      // 右侧：创建/加入表单
      var formCard = new RoundPanel {
        Radius = 14, FillColor = Color.FromArgb(0x1A, 0x17, 0x14),
        Location = new Point(380, 60), Size = new Size(340, 420)
      };
      card.Controls.Add(formCard);

      int y = 16;
      // 模式选择
      rbCreate = new RadioButton { Text = "创建房间", ForeColor = Theme.TEXT, BackColor = Color.Transparent,
        Location = new Point(16, y), Width = 100, Font = new Font("Microsoft YaHei UI", 11F), Checked = true };
      rbJoin = new RadioButton { Text = "加入房间", ForeColor = Theme.TEXT, BackColor = Color.Transparent,
        Location = new Point(130, y), Width = 100, Font = new Font("Microsoft YaHei UI", 11F) };
      formCard.Controls.Add(rbCreate);
      formCard.Controls.Add(rbJoin);
      y += 36;

      rbCoop = new RadioButton { Text = "合作打怪", ForeColor = Theme.TEXT, BackColor = Color.Transparent,
        Location = new Point(16, y), Width = 100, Font = new Font("Microsoft YaHei UI", 11F), Checked = true };
      rbVs = new RadioButton { Text = "玩家对战", ForeColor = Theme.TEXT, BackColor = Color.Transparent,
        Location = new Point(130, y), Width = 100, Font = new Font("Microsoft YaHei UI", 11F) };
      formCard.Controls.Add(rbCoop);
      formCard.Controls.Add(rbVs);
      y += 40;

      formCard.Controls.Add(new Label { Text = "玩家昵称", ForeColor = Theme.TEXT2, BackColor = Color.Transparent,
        Location = new Point(16, y), AutoSize = true, Font = new Font("Microsoft YaHei UI", 10F) });
      y += 22;
      txtName = new TextBox {
        Text = "玩家" + new Random().Next(100, 999),
        BackColor = Color.FromArgb(0x2A, 0x24, 0x1D), ForeColor = Theme.TEXT,
        BorderStyle = BorderStyle.FixedSingle, Location = new Point(16, y),
        Size = new Size(300, 32), Font = new Font("Microsoft YaHei UI", 11F)
      };
      formCard.Controls.Add(txtName);
      y += 42;

      formCard.Controls.Add(new Label { Text = "房间号", ForeColor = Theme.TEXT2, BackColor = Color.Transparent,
        Location = new Point(16, y), AutoSize = true, Font = new Font("Microsoft YaHei UI", 10F) });
      y += 22;
      txtRoom = new TextBox {
        Text = new Random().Next(1000, 9999).ToString(),
        BackColor = Color.FromArgb(0x2A, 0x24, 0x1D), ForeColor = Theme.TEXT,
        BorderStyle = BorderStyle.FixedSingle, Location = new Point(16, y),
        Size = new Size(300, 32), Font = new Font("Microsoft YaHei UI", 11F)
      };
      formCard.Controls.Add(txtRoom);
      y += 42;

      formCard.Controls.Add(new Label { Text = "房间密码（可选）", ForeColor = Theme.TEXT2, BackColor = Color.Transparent,
        Location = new Point(16, y), AutoSize = true, Font = new Font("Microsoft YaHei UI", 10F) });
      y += 22;
      txtPwd = new TextBox {
        BackColor = Color.FromArgb(0x2A, 0x24, 0x1D), ForeColor = Theme.TEXT,
        BorderStyle = BorderStyle.FixedSingle, Location = new Point(16, y),
        Size = new Size(300, 32), Font = new Font("Microsoft YaHei UI", 11F)
      };
      formCard.Controls.Add(txtPwd);
      y += 50;

      var btnStart = new RoundButton {
        Text = "开始联机", BgColor = Theme.ORANGE,
        HoverColor = Color.FromArgb(0xF0, 0xB0, 0x5A),
        TextColor = Color.FromArgb(0x1A, 0x17, 0x14), Radius = 14,
        Location = new Point(16, y), Size = new Size(300, 48),
        Font = new Font("Microsoft YaHei UI", 14F, FontStyle.Bold)
      };
      btnStart.Click += (s, e) => StartOnline();
      formCard.Controls.Add(btnStart);

      // 刷新房间列表
      RefreshRooms();
    }

    async void RefreshRooms() {
      lblRoomStatus.Text = "正在刷新…";
      roomListPanel.Controls.Clear();
      try {
        var json = await HttpGetRetry(ROOM_API + "/rooms");
        var serializer = new JavaScriptSerializer();
        var rooms = serializer.Deserialize<List<Dictionary<string, object>>>(json);
        lblRoomStatus.Text = $"共 {rooms.Count} 个房间";
        if (rooms.Count == 0) {
          roomListPanel.Controls.Add(new Label {
            Text = "暂无在线房间\n创建一个吧！",
            ForeColor = Theme.TEXT3, BackColor = Color.Transparent,
            Font = new Font("Microsoft YaHei UI", 11F),
            Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleCenter
          });
          return;
        }
        foreach (var r in rooms) {
          var roomId = r.ContainsKey("roomId") ? r["roomId"]?.ToString() : "?";
          var name = r.ContainsKey("name") ? r["name"]?.ToString() : "玩家";
          var hasPwd = r.ContainsKey("hasPwd") && r["hasPwd"]?.ToString() == "True";
          var players = r.ContainsKey("players") ? Convert.ToInt32(r["players"]) : 1;
          var isVs = r.ContainsKey("mode") && r["mode"]?.ToString() == "6";
          roomListPanel.Controls.Add(MakeRoomItem(roomId, name, hasPwd, players, isVs));
        }
      } catch (Exception ex) {
        lblRoomStatus.Text = "刷新失败";
        roomListPanel.Controls.Add(new Label {
          Text = "网络错误：" + ex.Message,
          ForeColor = Theme.RED, BackColor = Color.Transparent,
          Font = new Font("Microsoft YaHei UI", 9F), Dock = DockStyle.Fill
        });
      }
    }

    RoundPanel MakeRoomItem(string roomId, string name, bool hasPwd, int players, bool isVs) {
      var item = new RoundPanel {
        Radius = 10, FillColor = Color.FromArgb(0x2A, 0x24, 0x1D),
        Height = 54, Width = 308, Cursor = Cursors.Hand
      };
      item.Controls.Add(new Label {
        Text = $"房间 {roomId}", Font = new Font("Microsoft YaHei UI", 12F, FontStyle.Bold),
        ForeColor = Theme.TEXT, BackColor = Color.Transparent,
        Location = new Point(12, 5), AutoSize = true
      });
      // 模式标签
      item.Controls.Add(new Label {
        Text = isVs ? "对战" : "合作",
        Font = new Font("Microsoft YaHei UI", 8F),
        ForeColor = isVs ? Theme.RED : Theme.GREEN, BackColor = Color.Transparent,
        Location = new Point(110, 9), AutoSize = true
      });
      item.Controls.Add(new Label {
        Text = $"主机：{name} · {players}人",
        Font = new Font("Microsoft YaHei UI", 9F), ForeColor = Theme.TEXT2,
        BackColor = Color.Transparent, Location = new Point(12, 30), AutoSize = true
      });
      // 右侧：锁 / 加入
      var tag = new Label {
        Text = hasPwd ? "有锁" : "加入",
        Font = new Font("Microsoft YaHei UI", 10F, FontStyle.Bold),
        ForeColor = hasPwd ? Theme.AMBER : Theme.GREEN,
        BackColor = Color.Transparent, Location = new Point(248, 16),
        Size = new Size(52, 24), TextAlign = ContentAlignment.MiddleCenter
      };
      item.Controls.Add(tag);
      item.Click += (s, e) => {
        txtRoom.Text = roomId;
        rbJoin.Checked = true;
        if (!hasPwd) txtPwd.Text = "";
      };
      tag.Click += (s, e) => {
        txtRoom.Text = roomId;
        rbJoin.Checked = true;
        if (!hasPwd) txtPwd.Text = "";
      };
      item.MouseEnter += (s, e) => { item.Hover = true; item.Invalidate(); };
      item.MouseLeave += (s, e) => { item.Hover = false; item.Invalidate(); };
      return item;
    }

    async void RandomJoin() {
      lblRoomStatus.Text = "随机匹配中…";
      try {
        var json = await http.GetStringAsync(ROOM_API + "/rooms");
        var serializer = new JavaScriptSerializer();
        var rooms = serializer.Deserialize<List<Dictionary<string, object>>>(json);
        var open = new List<Dictionary<string, object>>();
        foreach (var r in rooms)
          if (!(r.ContainsKey("hasPwd") && r["hasPwd"]?.ToString() == "True")) open.Add(r);
        if (open.Count == 0) {
          lblRoomStatus.Text = "没有无密码房间";
          MessageBox.Show("没有可随机进入的无密码房间", "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
          return;
        }
        var rnd = open[new Random().Next(open.Count)];
        txtRoom.Text = rnd.ContainsKey("roomId") ? rnd["roomId"]?.ToString() : "";
        txtPwd.Text = "";
        rbJoin.Checked = true;
        lblRoomStatus.Text = "已选房间 " + txtRoom.Text;
      } catch { lblRoomStatus.Text = "随机匹配失败"; }
    }

    // crudcrud 免费服务偶发 500 / 网络抖动，自动重试最多 3 次
    async Task<string> HttpGetRetry(string url) {
      Exception last = null;
      for (int i = 0; i < 3; i++) {
        try { return await http.GetStringAsync(url); }
        catch (Exception e) { last = e; await Task.Delay(700); }
      }
      throw last;
    }
    async Task<HttpResponseMessage> HttpPostRetry(string url, string json) {
      Exception last = null;
      for (int i = 0; i < 3; i++) {
        try {
          var resp = await http.PostAsync(url, new StringContent(json, Encoding.UTF8, "application/json"));
          if (resp.IsSuccessStatusCode) return resp;
          last = new Exception("HTTP " + (int)resp.StatusCode);
        } catch (Exception e) { last = e; }
        await Task.Delay(700);
      }
      throw last;
    }

    async void StartOnline() {
      var name = string.IsNullOrWhiteSpace(txtName.Text) ? "玩家" + new Random().Next(100, 999) : txtName.Text.Trim();
      var room = txtRoom.Text.Trim();
      var pwd = txtPwd.Text.Trim();
      if (string.IsNullOrEmpty(room)) {
        MessageBox.Show("请输入房间号", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
        return;
      }
      int mode = rbCoop.Checked ? 4 : 6; // 4=合作, 6=对战（HTML里的mode值）
      var serializer = new JavaScriptSerializer();

      try {
        var json = await HttpGetRetry(ROOM_API + "/rooms");
        var rooms = serializer.Deserialize<List<Dictionary<string, object>>>(json)
                    ?? new List<Dictionary<string, object>>();

        if (rbCreate.Checked) {
          // 检查房间号是否被占用
          foreach (var r in rooms) {
            if (r.ContainsKey("roomId") && r["roomId"]?.ToString() == room) {
              MessageBox.Show("房间号已被占用，请换一个（或点随机）", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
              return;
            }
          }
          // 在托管网站创建房间
          var payload = new Dictionary<string, object> {
            { "roomId", room },
            { "name", name },
            { "hasPwd", !string.IsNullOrEmpty(pwd) },
            { "players", 1 },
            { "mode", mode },
            { "ts", (long)(DateTime.UtcNow - new DateTime(1970,1,1)).TotalSeconds }
          };
          var resp = await HttpPostRetry(ROOM_API + "/rooms", serializer.Serialize(payload));
          var respBody = await resp.Content.ReadAsStringAsync();
          if (resp.IsSuccessStatusCode) {
            var created = serializer.Deserialize<Dictionary<string, object>>(respBody);
            hostedRoomApiId = created != null && created.ContainsKey("_id") ? created["_id"]?.ToString() : null;
            lblRoomStatus.Text = "已创建房间 " + room;
          } else {
            MessageBox.Show("房间创建失败（网络），已改为离线启动", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
          }
        } else {
          // 加入房间：验证房间存在、密码
          Dictionary<string, object> target = null;
          foreach (var r in rooms)
            if (r.ContainsKey("roomId") && r["roomId"]?.ToString() == room) { target = r; break; }
          if (target == null) {
            MessageBox.Show("房间不存在，请刷新列表确认", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return;
          }
          bool hasPwd = target.ContainsKey("hasPwd") && target["hasPwd"]?.ToString() == "True";
          if (hasPwd) {
            if (string.IsNullOrEmpty(pwd)) {
              MessageBox.Show("该房间需要密码", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
              return;
            }
          }
        }
      } catch (Exception ex) {
        var dr = MessageBox.Show("连接房间服务器失败：" + ex.Message + "\n\n是否仍要离线启动游戏？",
          "网络错误", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
        if (dr != DialogResult.Yes) return;
      }

      LaunchGame(mode, name, room, pwd, rbCreate.Checked);
    }

    // 删除本机托管的房间（关闭启动器时）
    async void DeleteHostedRoom() {
      if (string.IsNullOrEmpty(hostedRoomApiId)) return;
      try {
        await http.DeleteAsync(ROOM_API + "/rooms/" + hostedRoomApiId);
      } catch { }
      hostedRoomApiId = null;
    }

    // ========== 设置页 ==========
    // 全局设置（启动游戏时通过 URL 传给游戏）
    public static int MouseSens = 50, TouchSens = 50, PadSens = 50, Volume = 70;
    public static int Weather = 0, Quality = 1;
    string[] weatherNames = { "默认", "早晨", "夜晚", "永久早晨", "永久夜晚" };
    string[] qualityNames = { "流畅", "均衡", "高清" };

    Panel SettingRow(string label, Control right, int y) {
      var row = new Panel {
        BackColor = Color.FromArgb(0x22, 0x1D, 0x18),
        Width = 680, Height = 46, Location = new Point(28, y)
      };
      var lbl = new Label {
        Text = label, Font = new Font("Microsoft YaHei UI", 11F),
        ForeColor = Theme.TEXT, BackColor = Color.FromArgb(0x22, 0x1D, 0x18),
        Location = new Point(16, 0), Height = 46,
        TextAlign = ContentAlignment.MiddleLeft
      };
      row.Controls.Add(lbl);
      return row;
    }

    void BuildSettingsPage() {
      var card = new RoundPanel { Radius = 20, FillColor = Color.FromArgb(220, 0x2A, 0x24, 0x1D), Dock = DockStyle.Fill };
      contentPanel.Controls.Add(card);

      // 可滚动内容区（先添加 Fill，再添加 Top 标题栏）
      var scroll = new Panel {
        Dock = DockStyle.Fill, BackColor = Color.Transparent,
        AutoScroll = true, Padding = new Padding(28, 12, 28, 16)
      };
      card.Controls.Add(scroll);
      var titlePanel = new Panel { Dock = DockStyle.Top, Height = 64, BackColor = Color.Transparent };
      card.Controls.Add(titlePanel);
      titlePanel.Controls.Add(new Label {
        Text = "游戏设置",
        Font = new Font("Microsoft YaHei UI", 24F, FontStyle.Bold),
        ForeColor = Theme.AMBER, Location = new Point(28, 16), AutoSize = true
      });

      int y = 8;
      int rowW = 680, rowH = 46, gap = 8;

      // 灵敏度滑块
      var tbMouse = MakeSlider(MouseSens, v => MouseSens = v);
      var r1 = SettingRow("鼠标灵敏度", tbMouse, y);
      AddSlider(tbMouse, rowW, r1);
      scroll.Controls.Add(r1);
      y += rowH + gap;

      var tbTouch = MakeSlider(TouchSens, v => TouchSens = v);
      var r2 = SettingRow("触摸灵敏度", tbTouch, y);
      AddSlider(tbTouch, rowW, r2);
      scroll.Controls.Add(r2);
      y += rowH + gap;

      var tbPad = MakeSlider(PadSens, v => PadSens = v);
      var r3 = SettingRow("手柄灵敏度", tbPad, y);
      AddSlider(tbPad, rowW, r3);
      scroll.Controls.Add(r3);
      y += rowH + gap;

      // 天气循环
      RoundButton btnWeather = MakeCycler(weatherNames[Weather], null);
      btnWeather.Click += (s, e) => {
        Weather = (Weather + 1) % weatherNames.Length;
        btnWeather.Text = weatherNames[Weather];
      };
      var r4 = SettingRow("天气", btnWeather, y);
      AddRight(btnWeather, rowW, r4);
      scroll.Controls.Add(r4);
      y += rowH + gap;

      // 画质循环
      RoundButton btnQuality = MakeCycler(qualityNames[Quality], null);
      btnQuality.Click += (s, e) => {
        Quality = (Quality + 1) % qualityNames.Length;
        btnQuality.Text = qualityNames[Quality];
      };
      var r5 = SettingRow("画质", btnQuality, y);
      AddRight(btnQuality, rowW, r5);
      scroll.Controls.Add(r5);
      y += rowH + gap;

      // 音量
      var tbVol = MakeSlider(Volume, v => Volume = v);
      var r6 = SettingRow("主音量", tbVol, y);
      AddSlider(tbVol, rowW, r6);
      scroll.Controls.Add(r6);
      y += rowH + gap;

      // 手柄校准
      var btnCalib = MakeActionBtn("开始校准", Theme.GREEN);
      btnCalib.Click += (s, e) => MessageBox.Show("请连接手柄后，将两个摇杆缓慢旋转两圈，再按下 A 键完成校准。", "手柄校准",
        MessageBoxButtons.OK, MessageBoxIcon.Information);
      var r7 = SettingRow("手柄校准", btnCalib, y);
      AddRight(btnCalib, rowW, r7);
      scroll.Controls.Add(r7);
      y += rowH + gap;

      // 控制方式
      var btnCtrl = MakeCycler("自动检测（键盘/手柄）", null);
      btnCtrl.Click += (s, e) => MessageBox.Show("已连接手柄时自动切换为手柄，否则使用键盘鼠标。", "控制方式",
        MessageBoxButtons.OK, MessageBoxIcon.Information);
      var r8 = SettingRow("控制方式", btnCtrl, y);
      AddRight(btnCtrl, rowW, r8);
      scroll.Controls.Add(r8);
      y += rowH + gap + 12;

      scroll.Controls.Add(new Label {
        Text = "方块突击 V3.4.1 · 3D 第一人称射击 · WebRTC 远程联机",
        Font = new Font("Microsoft YaHei UI", 9F), ForeColor = Theme.TEXT3,
        BackColor = Color.Transparent, Location = new Point(0, y), AutoSize = true
      });

      // 让设置行宽度跟随内容区（窄窗口也不横向溢出）
      void FitRows() {
        int w = scroll.ClientSize.Width - scroll.Padding.Left - scroll.Padding.Right;
        if (w < 320) w = 320;
        foreach (Control c in scroll.Controls) {
          if (c is Panel rp && rp.Height == rowH) {
            rp.Width = w;
            // 右侧控件锚定在 SettingRow 时已用绝对定位，这里同步重排
            foreach (Control rc in rp.Controls) {
              if (rc is CustomSlider) rc.Left = w - rc.Width - 16;
              else if (rc is RoundButton) rc.Left = w - rc.Width - 16;
            }
          }
        }
      }
      scroll.Resize += (s, e) => FitRows();
      FitRows();
    }

    CustomSlider MakeSlider(int val, Action<int> onChanged) {
      var tb = new CustomSlider {
        Minimum = 1, Maximum = 100, Value = val,
        Size = new Size(240, 32)
      };
      tb.ValueChanged += v => onChanged(v);
      return tb;
    }
    void AddSlider(CustomSlider tb, int rowW, Panel row) {
      tb.Anchor = AnchorStyles.Right | AnchorStyles.Top;
      tb.Location = new Point(rowW - tb.Width - 16, 7);
      row.Controls.Add(tb);
    }
    RoundButton MakeCycler(string text, Action onClick) {
      var b = new RoundButton {
        Text = text, BgColor = Color.FromArgb(0x3D, 0x34, 0x28), TextColor = Theme.TEXT,
        HoverColor = Color.FromArgb(0x4A, 0x40, 0x32), Radius = 10,
        Size = new Size(240, 32), Font = new Font("Microsoft YaHei UI", 10F)
      };
      if (onClick != null) b.Click += (s, e) => onClick();
      return b;
    }
    RoundButton MakeActionBtn(string text, Color c) {
      return new RoundButton {
        Text = text, BgColor = c, TextColor = Color.FromArgb(0x1A, 0x17, 0x14),
        HoverColor = Color.FromArgb(0x9C, 0xC4, 0x86), Radius = 10,
        Size = new Size(240, 32), Font = new Font("Microsoft YaHei UI", 10F)
      };
    }
    void AddRight(Control c, int rowW, Panel row) {
      c.Anchor = AnchorStyles.Right | AnchorStyles.Top;
      c.Location = new Point(rowW - c.Width - 16, 7);
      row.Controls.Add(c);
    }

    // ========== 启动游戏 ==========
    void LaunchGame(int mode, string name = "", string room = "", string pwd = "", bool create = false) {
      try {
        StartServer();
        System.Threading.Thread.Sleep(300);

        string url = $"http://127.0.0.1:{PORT}/枪战.HTML?mode={mode}&autostart=1&p2p=1"
          + $"&sens={MouseSens}&tsens={TouchSens}&psens={PadSens}&vol={Volume}"
          + $"&weather={Weather}&quality={Quality}";
        if (!string.IsNullOrEmpty(name)) url += $"&name={Uri.EscapeDataString(name)}";
        if (!string.IsNullOrEmpty(room)) url += $"&room={Uri.EscapeDataString(room)}";
        if (!string.IsNullOrEmpty(pwd)) url += $"&pwd={Uri.EscapeDataString(pwd)}";
        url += $"&create={(create ? 1 : 0)}";

        Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
      } catch (Exception ex) {
        MessageBox.Show("启动失败：" + ex.Message, "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
      }
    }

    System.Net.HttpListener listener;
    System.Threading.Thread srvThread;

    void StartServer() {
      if (listener != null && listener.IsListening) return;
      try {
        listener = new System.Net.HttpListener();
        listener.Prefixes.Add($"http://127.0.0.1:{PORT}/");
        listener.Start();
        srvThread = new System.Threading.Thread(ServerLoop) { IsBackground = true };
        srvThread.Start();
      } catch (Exception ex) {
        System.Diagnostics.Debug.WriteLine("服务器启动失败: " + ex.Message);
      }
    }

    void ServerLoop() {
      while (listener != null && listener.IsListening) {
        System.Net.HttpListenerContext ctx;
        try { ctx = listener.GetContext(); }
        catch { break; }
        try {
          ThreadPool.QueueUserWorkItem(_ => ServeFile(ctx));
        } catch { }
      }
    }

    void ServeFile(System.Net.HttpListenerContext ctx) {
      try {
        string raw = ctx.Request.Url.LocalPath.TrimStart('/');
        if (string.IsNullOrEmpty(raw) || raw.Contains("..")) raw = "枪战.HTML";
        raw = Uri.UnescapeDataString(raw);
        string file = Path.Combine(DIR, raw);
        if (!File.Exists(file)) {
          ctx.Response.StatusCode = 404;
          ctx.Response.Close();
          return;
        }
        byte[] bytes = File.ReadAllBytes(file);
        string ext = Path.GetExtension(file).ToLower();
        string mime = ext == ".html" || ext == ".htm" ? "text/html; charset=utf-8"
                   : ext == ".js" ? "application/javascript; charset=utf-8"
                   : ext == ".css" ? "text/css; charset=utf-8"
                   : ext == ".png" ? "image/png"
                   : ext == ".jpg" || ext == ".jpeg" ? "image/jpeg"
                   : ext == ".gif" ? "image/gif"
                   : ext == ".ico" ? "image/x-icon"
                   : ext == ".json" ? "application/json"
                   : "application/octet-stream";
        ctx.Response.ContentType = mime;
        ctx.Response.ContentLength64 = bytes.Length;
        ctx.Response.AddHeader("Access-Control-Allow-Origin", "*");
        ctx.Response.OutputStream.Write(bytes, 0, bytes.Length);
        ctx.Response.Close();
      } catch {
        try { ctx.Response.Close(); } catch { }
      }
    }

    protected override void OnFormClosing(FormClosingEventArgs e) {
      try {
        if (listener != null) { listener.Stop(); listener.Close(); listener = null; }
      } catch { }
      // 关闭时从托管网站移除本机创建的房间（同步等待，最多 2 秒）
      if (!string.IsNullOrEmpty(hostedRoomApiId)) {
        try {
          var id = hostedRoomApiId;
          http.DeleteAsync(ROOM_API + "/rooms/" + id).Wait(2000);
        } catch { }
      }
      base.OnFormClosing(e);
    }
  }

  static class Program {
    [STAThread]
    static void Main() {
      try {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new MainForm());
      } catch (Exception ex) {
        File.WriteAllText(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Desktop), "fk_launcher_error.txt"),
          ex.ToString());
        MessageBox.Show("启动失败：" + ex.Message + "\n\n详细错误已保存到桌面 fk_launcher_error.txt",
          "方块突击", MessageBoxButtons.OK, MessageBoxIcon.Error);
      }
    }
  }
}
