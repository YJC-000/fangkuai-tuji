#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
方块突击 V3.3.0 电脑版启动器
暖色 FlClash 风格原生菜单，点击后启动本地服务器并自动进入对应模式
"""
import os
import sys
import threading
import webbrowser
import http.server
import socketserver
from tkinter import Tk, Frame, Label, Button, Canvas, StringVar, Toplevel, Entry
from tkinter import font as tkfont

BG_TOP = "#1A1612"
BG_BOTTOM = "#241E17"
CARD_BG = "#2A241D"
CARD_BORDER = "#3D3428"
TEXT_PRIMARY = "#F5EDE0"
TEXT_SECONDARY = "#B0A498"
TEXT_MUTED = "#6B6158"
ACCENT_ORANGE = "#E8A04A"
ACCENT_GREEN = "#8BB876"
ACCENT_AMBER = "#D4A574"
ACCENT_RED = "#C77D5E"

PORT = 18080
SERVER_RUNNING = False


def get_game_dir():
    if getattr(sys, "frozen", False):
        return os.path.dirname(sys.executable)
    return os.path.dirname(os.path.abspath(__file__))


def start_server():
    global SERVER_RUNNING
    if SERVER_RUNNING:
        return
    game_dir = get_game_dir()
    os.chdir(game_dir)
    handler = http.server.SimpleHTTPRequestHandler
    socketserver.TCPServer.allow_reuse_address = True
    httpd = socketserver.TCPServer(("127.0.0.1", PORT), handler)
    SERVER_RUNNING = True
    t = threading.Thread(target=httpd.serve_forever, daemon=True)
    t.start()


def launch_game(mode):
    start_server()
    url = f"http://127.0.0.1:{PORT}/枪战.HTML?mode={mode}&autostart=1"
    webbrowser.open(url)


def launch_net():
    start_server()
    url = f"http://127.0.0.1:{PORT}/枪战.HTML?mode=4&autostart=1"
    webbrowser.open(url)


class SettingsWindow(Toplevel):
    def __init__(self, master):
        super().__init__(master)
        self.title("设置 - 方块突击")
        self.configure(bg=BG_TOP)
        self.geometry("420x480")
        self.resizable(False, False)
        self.transient(master)
        self.grab_set()

        Label(self, text="设置", font=("Microsoft YaHei", 18, "bold"),
              fg=ACCENT_ORANGE, bg=BG_TOP).pack(pady=(20, 16))

        frame = Frame(self, bg=CARD_BG, highlightbackground=CARD_BORDER,
                      highlightthickness=1)
        frame.pack(fill="x", padx=24, pady=4)

        Label(frame, text="鼠标灵敏度", font=("Microsoft YaHei", 11),
              fg=TEXT_PRIMARY, bg=CARD_BG).pack(anchor="w", padx=16, pady=(12, 2))
        self.mouse_var = StringVar(value="1.0")
        Entry(frame, textvariable=self.mouse_var, bg=BG_TOP, fg=TEXT_PRIMARY,
              insertbackground=TEXT_PRIMARY, relief="flat", width=10).pack(anchor="w", padx=16, pady=(0, 8))

        Label(frame, text="画质", font=("Microsoft YaHei", 11),
              fg=TEXT_PRIMARY, bg=CARD_BG).pack(anchor="w", padx=16, pady=(8, 2))
        self.quality_var = StringVar(value="medium")
        qf = Frame(frame, bg=CARD_BG)
        qf.pack(anchor="w", padx=16, pady=(0, 8))
        for text, val in [("流畅", "low"), ("均衡", "medium"), ("高清", "high")]:
            Button(qf, text=text, command=lambda v=val: self.quality_var.set(v),
                   bg=BG_TOP, fg=TEXT_SECONDARY, relief="flat", font=("Microsoft YaHei", 10),
                   width=8, activebackground=ACCENT_ORANGE, activeforeground=BG_TOP,
                   cursor="hand2").pack(side="left", padx=2)

        Label(frame, text="天气", font=("Microsoft YaHei", 11),
              fg=TEXT_PRIMARY, bg=CARD_BG).pack(anchor="w", padx=16, pady=(8, 2))
        self.weather_var = StringVar(value="default")
        wf = Frame(frame, bg=CARD_BG)
        wf.pack(anchor="w", padx=16, pady=(0, 12))
        for text, val in [("默认", "default"), ("早晨", "morning"), ("夜晚", "night"),
                          ("永久早晨", "perm_morning"), ("永久夜晚", "perm_night")]:
            Button(wf, text=text, command=lambda v=val: self.weather_var.set(v),
                   bg=BG_TOP, fg=TEXT_SECONDARY, relief="flat", font=("Microsoft YaHei", 9),
                   width=8, activebackground=ACCENT_ORANGE, activeforeground=BG_TOP,
                   cursor="hand2").pack(side="left", padx=2, pady=2)

        Button(self, text="保存并关闭", command=self.save_and_close,
               bg=ACCENT_ORANGE, fg=BG_TOP, font=("Microsoft YaHei", 12, "bold"),
               relief="flat", pady=10, cursor="hand2",
               activebackground="#D4903A").pack(fill="x", padx=24, pady=20)

    def save_and_close(self):
        cfg_path = os.path.join(get_game_dir(), "fk_settings.ini")
        with open(cfg_path, "w", encoding="utf-8") as f:
            f.write(f"mouse_sens={self.mouse_var.get()}\n")
            f.write(f"quality={self.quality_var.get()}\n")
            f.write(f"weather={self.weather_var.get()}\n")
        self.destroy()


class Launcher:
    def __init__(self):
        self.root = Tk()
        self.root.title("方块突击 V3.3.0")
        self.root.geometry("520x640")
        self.root.resizable(False, False)
        self.root.configure(bg=BG_TOP)

        self.canvas = Canvas(self.root, width=520, height=640, bg=BG_TOP, highlightthickness=0)
        self.canvas.pack(fill="both", expand=True)
        self._draw_gradient()
        self._draw_waves()

        self.canvas.create_text(260, 70, text="方块突击", font=("Microsoft YaHei", 32, "bold"),
                                fill=TEXT_PRIMARY)
        self.canvas.create_text(260, 105, text="BLOCK ASSAULT · 3D FPS",
                                font=("Microsoft YaHei", 10), fill=TEXT_MUTED)

        buttons = [
            ("⚔  生存模式", "消灭敌人 · 存活到底", ACCENT_ORANGE, lambda: launch_game(1)),
            ("☠  团队死斗", "限时击杀 · 比拼得分", ACCENT_RED, lambda: launch_game(2)),
            ("∞  无尽狂潮", "无限敌人 · 挑战极限", ACCENT_GREEN, lambda: launch_game(5)),
            ("◎  联机模式", "P2P 直连 · 合作对战", ACCENT_AMBER, launch_net),
        ]
        y = 160
        for title, desc, color, cmd in buttons:
            self._draw_card(y, title, desc, color, cmd)
            y += 92

        settings_btn = Button(self.root, text="⚙  设置", command=self.open_settings,
                              bg=CARD_BG, fg=TEXT_SECONDARY, font=("Microsoft YaHei", 11),
                              relief="flat", padx=28, pady=8, cursor="hand2",
                              activebackground=CARD_BORDER, activeforeground=TEXT_PRIMARY)
        self.canvas.create_window(260, 560, window=settings_btn)

        self.canvas.create_text(260, 610, text="V3.3.0", font=("Microsoft YaHei", 9),
                                fill=TEXT_MUTED)

    def _draw_gradient(self):
        for i in range(640):
            r = int(0x1A + (0x24 - 0x1A) * i / 640)
            g = int(0x16 + (0x1E - 0x16) * i / 640)
            b = int(0x12 + (0x17 - 0x12) * i / 640)
            self.canvas.create_line(0, i, 520, i, fill=f"#{r:02x}{g:02x}{b:02x}")

    def _draw_waves(self):
        self.canvas.create_arc(-100, 480, 620, 680, start=0, extent=180,
                               fill=ACCENT_ORANGE, outline="", stipple="gray50")
        self.canvas.create_arc(-50, 510, 570, 690, start=0, extent=180,
                               fill=ACCENT_GREEN, outline="", stipple="gray50")

    def _draw_card(self, y, title, desc, color, cmd):
        card = Frame(self.root, bg=CARD_BG, highlightbackground=CARD_BORDER,
                     highlightthickness=1, cursor="hand2")
        card.bind("<Button-1>", lambda e: cmd())

        inner = Frame(card, bg=CARD_BG)
        inner.pack(fill="both", expand=True, padx=16, pady=14)

        icon = Canvas(inner, width=44, height=44, bg=CARD_BG, highlightthickness=0)
        icon.create_oval(2, 2, 42, 42, fill=color, outline="")
        icon.pack(side="left", padx=(0, 14))

        text_frame = Frame(inner, bg=CARD_BG)
        text_frame.pack(side="left", fill="both", expand=True)
        Label(text_frame, text=title, font=("Microsoft YaHei", 15, "bold"),
              fg=TEXT_PRIMARY, bg=CARD_BG, anchor="w").pack(fill="x")
        Label(text_frame, text=desc, font=("Microsoft YaHei", 10),
              fg=TEXT_SECONDARY, bg=CARD_BG, anchor="w").pack(fill="x")

        Label(inner, text="›", font=("Microsoft YaHei", 20, "bold"),
              fg=TEXT_MUTED, bg=CARD_BG).pack(side="right")

        for w in (card, inner, text_frame, icon):
            w.bind("<Button-1>", lambda e: cmd())
        for w in text_frame.winfo_children():
            w.bind("<Button-1>", lambda e: cmd())

        self.canvas.create_window(260, y + 36, window=card, width=440, height=76)

    def open_settings(self):
        SettingsWindow(self.root)

    def run(self):
        self.root.mainloop()


if __name__ == "__main__":
    Launcher().run()
