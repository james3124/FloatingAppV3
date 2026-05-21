# GitHub Upload & APK Build Guide

## Step 1 — Create a GitHub Repository

1. Go to https://github.com/new
2. Name it `FloatingAppV3` (or anything you like)
3. Set it to **Public** or **Private**
4. **Do NOT** initialize with README (the project already has one)
5. Click **Create repository**

---

## Step 2 — Upload the Project

### Option A: GitHub Desktop (easiest for beginners)
1. Download [GitHub Desktop](https://desktop.github.com)
2. Click **File → Add Local Repository** and point to your `FloatingAppV3` folder
3. Commit all files, then push to the repository you just created

### Option B: Git command line
```bash
cd FloatingAppV3          # enter the project folder
git init
git add .
git commit -m "Initial commit — FloatingAppV3"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/FloatingAppV3.git
git push -u origin main
```

### Option C: GitHub web upload
1. Open your new repository on GitHub
2. Click **uploading an existing file**
3. Drag-and-drop all project files (not the outer folder, the files inside)

---

## Step 3 — The Workflow Runs Automatically

Once you push to `main` or `master`, GitHub Actions will:

| Step | What happens |
|------|-------------|
| Checkout | Downloads your code |
| JDK 17 | Sets up Java (required by Gradle 8) |
| Android SDK | Installs the SDK, platform-34, build-tools-34 |
| Gradle cache | Speeds up future builds |
| `assembleDebug` | Builds a debug APK you can install directly |
| `assembleRelease` | Builds an unsigned release APK |
| Upload artifact | APKs appear under **Actions → your run → Artifacts** |

Build time: **~3–5 minutes** on first run (downloads deps), **~1–2 min** after caching.

---

## Step 4 — Download Your APK

1. Open your repository on GitHub
2. Click the **Actions** tab
3. Click the latest **Build APK** workflow run
4. Scroll to the bottom → **Artifacts**
5. Download `FloatingAppV3-debug-<run number>.zip`
6. Unzip → install `app-debug.apk` on your Android device

---

## Step 5 — (Optional) Signed Release APK

To publish on the Play Store or share without "Unknown sources" warnings,
you need a signed APK. Steps:

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore my-release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias my-key-alias
   ```

2. Add these **Repository Secrets** (Settings → Secrets → Actions):
   | Secret name | Value |
   |-------------|-------|
   | `KEYSTORE_BASE64` | `base64 my-release-key.jks` output |
   | `KEY_ALIAS` | `my-key-alias` |
   | `KEY_PASSWORD` | the key password you set |
   | `STORE_PASSWORD` | the store password you set |

3. Uncomment the **Sign & publish** block in `.github/workflows/build.yml`

4. Push a tag to trigger a signed release:
   ```bash
   git tag v3.0.0
   git push origin v3.0.0
   ```

---

## Triggering a Build Manually

You can also trigger the workflow without pushing code:
1. Go to **Actions → Build APK**
2. Click **Run workflow → Run workflow**

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `SDK location not found` | The workflow sets `ANDROID_HOME` automatically via `setup-android` |
| `Permission denied: ./gradlew` | Already handled — the workflow runs `chmod +x gradlew` |
| `Gradle version mismatch` | `gradle-wrapper.properties` pins Gradle 8.2; AGP 8.2.0 is compatible |
| Build fails on `assembleRelease` | Check that `minifyEnabled false` in `app/build.gradle` (already set) |
