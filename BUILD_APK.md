# How to Build the APK

Since an APK must be compiled by the Android SDK (which requires a full build machine),
here are two ways to get your APK — both take under 5 minutes.

---

## Option A — Android Studio (Recommended, Windows/Mac/Linux)

1. **Install Android Studio** (free): https://developer.android.com/studio
2. **Open the project**: File → Open → select the `FloatingAppV3` folder
3. **Sync Gradle**: click "Sync Now" when prompted (downloads dependencies automatically)
4. **Build the APK**:
   - Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - Wait ~1 minute
   - A notification pops up: click **"locate"** about:blank#blockedto find the APK
5. **APK location**: `app/build/outputs/apk/debug/app-debug.apk`
6. **Install on your phone**:
   - Enable "Install from unknown sources" in Android Settings
   - Copy the APK to your phone and tap it, OR
   - With USB: `adb install app-debug.apk`

---

## Option B — Command Line (faster if you have Java installed)

```bash
# 1. Install Android Studio to get the SDK, or install just the command-line tools:
#    https://developer.android.com/studio#command-tools

# 2. Set ANDROID_HOME
export ANDROID_HOME=$HOME/Library/Android/sdk   # macOS
# export ANDROID_HOME=$HOME/Android/Sdk         # Linux

# 3. Navigate to project
cd FloatingAppV3

# 4. Make gradlew executable
chmod +x gradlew

# 5. Build debug APK
./gradlew assembleDebug

# 6. APK is at:
#    app/build/outputs/apk/debug/app-debug.apk
```

---

## Option C — GitHub Actions (build in the cloud, free)

1. Push the `FloatingAppV3` folder to a GitHub repository
2. Create `.github/workflows/build.yml`:

```yaml
name: Build APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build APK
        run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

3. Go to **Actions** tab in GitHub → download the APK artifact after the build finishes.

---

## Install on Phone Without a Computer

If you have an Android phone with a file manager:
1. Build the APK using any option above
2. Upload the APK to Google Drive / Telegram / any cloud storage
3. Open it on your phone and tap to install
4. Allow "Install unknown apps" when prompted

---

## Minimum Requirements

| Item | Requirement |
|------|-------------|
| Android Studio | Hedgehog 2023.1+ |
| Java | 17+ (bundled with Android Studio) |
| Android SDK | API 34 (installed via SDK Manager) |
| Device/Emulator | Android 8.0+ (API 26+) |
