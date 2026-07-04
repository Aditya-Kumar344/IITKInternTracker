# Intern Tracker (Android)

An Android app that watches your **IITK webmail inbox**, detects internship-opportunity
emails, extracts the company name / deadline / application link, and reminds you —
persistently, on the day of the deadline — until you actually open it.

## What it does

- **Login**: you sign in once with your IITK email + password (IMAP). Credentials are
  encrypted on-device (Android Keystore) and only ever sent directly to IITK's own mail
  server over a secure connection.
- **Background watching**: a lightweight background service keeps checking your inbox
  (near-instant via IMAP IDLE, plus a 15-minute periodic fallback so it survives the
  service being killed).
- **Detection**: emails are matched against internship-related keywords *and* a
  call-to-action phrase ("apply by", "deadline", "last date", etc.) so random mentions
  of "intern" don't get flagged. Company name, deadline date, and application link are
  extracted automatically.
- **New-mail notification**: fires as soon as a qualifying email is detected.
- **Deadline-day notification**: at 00:00 on the deadline day, a notification appears
  that **cannot be swiped away** — it only clears when you tap "Open link & dismiss"
  (or open the app and mark it done).
- **Dashboard**: every tracked opportunity, sorted by deadline, color-coded by urgency.
  You can also **add/edit events manually** — useful for deadlines you heard about
  outside email.

## Important limitations, read before relying on this

- Email parsing is **rule-based** (keywords + regex), not AI. It will miss unusually
  worded emails and occasionally mis-extract a company name or date. Check the
  `EmailParser.kt` file — it's meant to be tuned. Send me examples of missed emails
  and I can improve the patterns.
- "Cannot be swiped away" means what Android actually allows: a persistent ("ongoing")
  notification. It reappears/stays until the app itself removes it — no app can make a
  notification truly indestructible.
- Some Android phone brands (Xiaomi/MIUI, Oppo, Vivo, Samsung with aggressive battery
  saving) will kill background services unless you manually allow "no battery
  restriction" / "autostart" for this app. Do that in Settings → Apps → Intern Tracker
  → Battery, or the background watching may stop working after a while.
- This uses **IMAP** with your IITK Computer Centre server
  (`newmailhost.cc.iitk.ac.in`, port 993 IMAPS) by default. If your account is on a
  department server (e.g. CSE), change the host on the login screen under "Advanced".

## How to build the APK (no Android Studio needed)

This project auto-builds via **GitHub Actions** — GitHub's own free servers compile it
for you.

1. Go to [github.com](https://github.com) and create a free account if you don't have one.
2. Click the **+** icon (top right) → **New repository**. Name it anything (e.g.
   `intern-tracker`), keep it **Private** if you like, and click **Create repository**.
3. On the new repo's page, click **"uploading an existing file"** (or "Add file" →
   "Upload files").
4. Drag the **entire contents** of this project folder (everything inside
   `IITKInternTracker/`, not the folder itself) into the upload box. GitHub will
   preserve the folder structure automatically.
5. Scroll down and click **Commit changes**.
6. Click the **Actions** tab at the top of the repo. You should see a workflow run
   start automatically ("Build Debug APK"). Click it and wait ~3-5 minutes.
7. When it finishes (green checkmark), scroll down to **Artifacts** and download
   `IITKInternTracker-debug-apk`. It's a zip — unzip it to get `app-debug.apk`.
8. Transfer that `.apk` to your Android phone (email it to yourself, Google Drive,
   USB, whatever's easiest) and tap it to install. You'll need to allow
   "install from unknown sources" the first time — Android will prompt you for this.

If the Actions run fails (red X), click into it, open the failing step, and copy the
error text back to me — I'll fix the code.

## Rebuilding after changes

Any time you edit a file and re-upload it (or ask me for changes and re-upload the
files I give you), just push/upload again and GitHub Actions will automatically build
a fresh APK.

## Permissions the app will ask for

- **Notifications** — required, or you won't get any reminders.
- On first login, Android may also ask you to allow the app to **ignore battery
  optimizations** for reliable background checking — accept this for the persistent
  deadline reminders to work correctly.

## Project structure

```
app/src/main/java/com/internmail/tracker/
  data/           Room database, Event model, encrypted credential storage, repository
  mail/           IMAP client, email parser (detection + extraction), sync worker/service
  notification/   Notification channels, deadline alarm scheduling, persistent reminder
  ui/             Jetpack Compose screens (Login, Dashboard, Detail, Add/Edit)
```
