# Privacy Policy for Wordwave

**Last updated:** October 29, 2026

Welcome to **Wordwave** ("we", "our", or "the App"), an Android application developed by Tran Tien Dat. We respect your privacy and are committed to protecting any information you share with us.

---

## 1. Information Collection and Use

### a. Local Device Storage
- **Text & Audio Files:** Wordwave is designed to work primarily offline. Your imported books (TXT, EPUB, PDF), reading progress, bookmarks, and custom pronunciation dictionary rules are stored **strictly on your local device**.
- **Speech Synthesis:** On-device neural Text-to-Speech (Sherpa-ONNX / Piper) processes your text entirely offline. No text content is ever transmitted across the internet for synthesis.

### b. Google Drive & Account Data (Cloud Backup Feature)
- When you explicitly choose to use the **Cloud Backup & Restore** feature, Wordwave requests access to your Google Account via Google Sign-In with the following scopes:
  - `https://www.googleapis.com/auth/drive.appdata`
  - `https://www.googleapis.com/auth/drive.file`
- **Purpose:** These permissions are used **solely** to upload and download your own encrypted/compressed backup archive (containing your reading progress, bookmarks, and settings) to your private Google Drive `appDataFolder` or application-created backup file.
- **Scope Limitation:** Wordwave **cannot** see, read, modify, or delete any of your photos, documents, or other personal files stored on Google Drive. It can only interact with files that Wordwave itself created.
- **No Third-Party Transmission:** Your Google authentication tokens and backup files are handled directly between your Android device and Google's official Drive API endpoints (`drive.googleapis.com`). We do not run any intermediate server, and your data is never sent to us or any third party.

---

## 2. Permissions Required by the App

- **`INTERNET` & `ACCESS_NETWORK_STATE`:** Required solely for communicating with Google Drive APIs during manual cloud backup/restore and downloading optional offline voice packs when requested by the user.
- **`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`:** Required by Android 14+ to keep audio reading active when the screen is turned off or while multitasking.
- **`POST_NOTIFICATIONS`:** Required to display playback controls (Play, Pause, Skip, Sleep Timer) on your lock screen and notification panel.
- **`READ_MEDIA_AUDIO` / Storage Access:** Required only if you select custom background music or import local audio files.

---

## 3. Data Retention and Deletion

- All local data can be permanently erased at any time by clearing the application storage in Android Settings or uninstalling the App.
- Cloud backup archives stored in your Google Drive can be deleted directly by:
  1. Opening Google Drive on the web -> **Settings** -> **Manage Apps** -> Locate **Wordwave** -> Click **Options** -> **Delete hidden app data**.
  2. Or using the in-app "Xóa sao lưu" button within Wordwave settings.

---

## 4. Children's Privacy

Wordwave does not knowingly collect any personally identifiable information from children under the age of 13.

---

## 5. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be posted on this page with an updated revision date.

---

## 6. Contact Us

If you have any questions, feedback, or concerns regarding this Privacy Policy or your data, please contact us at:

- **Developer:** Tran Tien Dat
- **Email:** [trantiendat2206@gmail.com](mailto:trantiendat2206@gmail.com)
- **GitHub Repository:** [https://github.com/dattran2206/ReadBookTTS](https://github.com/dattran2206/ReadBookTTS)
