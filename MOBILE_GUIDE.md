# 📱 How to Get Your APK Using Only Your Phone

This guide uses GitHub (free) to build your APK in the cloud.
You only need a browser — no computer required.

---

## Step 1 — Create a Free GitHub Account

1. Open your phone browser and go to: **https://github.com**
2. Tap **Sign up**
3. Enter your email, create a password, choose a username
4. Verify your email
5. You now have a free GitHub account ✅

---

## Step 2 — Create a New Repository

1. After logging in, tap the **+** icon (top right) → **New repository**
2. Repository name: `FloatingApp`
3. Set it to **Private** (keeps your code private)
4. Tap **Create repository**

---

## Step 3 — Upload the Project Files

GitHub lets you upload files directly from your phone browser.

1. On your new repository page, tap **uploading an existing file** (or **Add file → Upload files**)
2. **Unzip** the `FloatingAppV3.zip` file on your phone first:
   - Use a file manager app like **Files by Google** or **ZArchiver**
   - Long press the zip → Extract here
3. Upload the files — you need to upload the **contents** of the FloatingAppV3 folder.
   - GitHub only lets you upload one folder level at a time, so use this trick:

### Easier upload method using GitHub.dev (web editor):

1. On your empty repository page, press the **period key ( . )** on your keyboard
   - This opens **github.dev**, a full web code editor in your browser
2. In the left sidebar, right-click → **Upload files**
3. Navigate to your unzipped FloatingAppV3 folder and select ALL files/folders
4. Wait for upload to complete
5. Click the **Source Control** icon (branch icon on left) → type a commit message like "Initial upload" → click **Commit & Push**

---

## Step 4 — Watch the Build Run

1. Go back to your repository on **https://github.com**
2. Tap the **Actions** tab at the top
3. You should see a workflow called **"Build APK"** running (yellow circle = in progress)
4. Wait about **3–5 minutes** for it to finish
5. When done, it turns **green ✅**

> If it's not running automatically, tap **Actions → Build APK → Run workflow → Run workflow**

---

## Step 5 — Download Your APK

1. Click on the completed green workflow run
2. Scroll down to the **Artifacts** section
3. Tap **FloatingApp-v3-debug** to download the ZIP
4. Unzip it on your phone — inside is **FloatingApp.apk**

---

## Step 6 — Install the APK on Your Phone

1. Open your phone **Settings**
2. Go to **Apps** (or Security) → **Install unknown apps**
3. Find your browser or file manager → enable **Allow from this source**
4. Go to your Downloads folder → tap **FloatingApp.apk**
5. Tap **Install**
6. Done! 🎉 Open **FloatingApp** from your app drawer

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Build failed (red ✗) | Tap the failed run → read the error log → ask me what it says |
| "Install blocked" | Enable "Install unknown apps" for your file manager in Settings |
| Can't find APK after download | Check Downloads folder or use Files by Google app |
| GitHub Actions tab not showing | Make sure the `.github/workflows/build.yml` file was uploaded |

---

## Quick Summary

```
github.com → New repo → Upload files → Actions tab → Wait 5min → Download APK → Install
```

That's it! The whole thing takes about 10 minutes the first time.
