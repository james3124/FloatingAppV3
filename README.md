# FloatingApp V2 — Full-Featured Android Floating Window

A production-ready Android floating window app with a full suite of features.

---

## ✨ Features Implemented

### UI & Experience
- ✅ Resize by dragging the bottom handle
- ✅ Pinch-to-zoom to resize
- ✅ Smooth scale animations on minimize/restore
- ✅ Snap to screen edges with eased animation on release
- ✅ Opacity/transparency slider in the window
- ✅ Window position and size saved and restored across sessions

### Content Tabs
- ✅ 🌐 **Browser** — full WebView with URL bar, back/forward navigation
- ✅ 📝 **Notes** — auto-saving note pad
- ✅ 🔢 **Calculator** — full operations (+, −, ×, ÷, %, ±, ⌫, AC)
- ✅ ⏱️ **Timer** — stopwatch with start, pause, reset
- ✅ 🎵 **Media** — prev/play-pause/next broadcast controls

### Controls
- ✅ Lock/unlock position button (prevents accidental moves)
- ✅ Pinch-to-zoom resize on content area
- ✅ Double-tap title bar to minimize
- ✅ Minimize to bubble icon with restore/close
- ✅ Settings shortcut button in title bar

### Multi-Window
- ✅ Static instance tracking (`FloatingWindowService.instances`)
- ✅ Architecture ready for multiple simultaneous floating windows

### Settings (SettingsActivity)
- ✅ Custom window title
- ✅ Default size (Small / Medium / Large)
- ✅ Default screen corner (TL / TR / BL / BR)
- ✅ Auto-start on device boot
- ✅ Snap-to-edge toggle
- ✅ High contrast mode toggle
- ✅ Reset to defaults

### Performance & Stability
- ✅ Window state (position, size, minimized, opacity, lock) persisted in SharedPreferences
- ✅ Last open tab saved and restored
- ✅ Last URL saved and restored in WebView
- ✅ Notes content auto-saved
- ✅ WebView properly destroyed on service stop

### Accessibility
- ✅ ContentDescription on all interactive elements (screen reader support)
- ✅ High contrast mode option in settings
- ✅ Notification with Toggle and Stop actions

---

## Project Structure

```
FloatingAppV2/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/floatingapp/
│   │   ├── MainActivity.java           ← Permission UI + start/stop
│   │   ├── SettingsActivity.java       ← All settings
│   │   ├── FloatingWindowService.java  ← Full floating window logic
│   │   ├── BootReceiver.java           ← Auto-start on boot
│   │   └── utils/
│   │       └── AppPreferences.java     ← SharedPreferences helper
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml
│       │   ├── activity_settings.xml
│       │   └── layout_floating_window.xml  ← 5-tab floating UI
│       ├── drawable/                   ← All shapes, selectors, icons
│       ├── menu/main_menu.xml
│       └── values/                    ← Colors, strings, themes
```

---

## Setup

### Requirements
- Android Studio Hedgehog (2023.1) or newer
- Android SDK 34
- Min device: **Android 8.0 (API 26)**

### Steps
1. Open `FloatingAppV2` folder in Android Studio
2. Sync Gradle when prompted
3. Run on device or emulator
4. Grant "Display over other apps" permission
5. Tap **Start Floating Window**

---

## Customizing Content

To add your own content to the floating window, edit `layout_floating_window.xml`.
Add a new tab by:
1. Adding a new `LinearLayout` with a unique id inside `contentArea`
2. Adding a tab button in the Tab Bar row
3. Wiring it up in `setupTabs()` inside `FloatingWindowService.java`

---

## License
MIT
