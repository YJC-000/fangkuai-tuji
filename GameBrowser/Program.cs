using System;
using System.Windows.Forms;

namespace FangKuaiBrowser
{
    static class Program
    {
        [STAThread]
        static void Main()
        {
            // 按显示器实际缩放比渲染（高分屏/125%以上缩放时不模糊的关键）
            Application.SetHighDpiMode(HighDpiMode.PerMonitorV2);
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }
}
