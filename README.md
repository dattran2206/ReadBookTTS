# Wordwave (VietReader TTS) 📖🎙️

<p align="center">
  <img src="app/src/main/res/drawable/app_logo_transparent.png" alt="Wordwave Logo" width="128" height="128" />
</p>

<p align="center">
  <b>Ứng dụng đọc sách nói thông minh, kết hợp công nghệ AI TTS Neural giọng đọc tự nhiên và trải nghiệm cá nhân hóa cao cấp trên Android.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple.svg" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%201.7-blue.svg" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-orange.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/Engine-Sherpa--ONNX%20AI%20TTS-red.svg" alt="Engine" />
  <img src="https://img.shields.io/badge/Compliance-Android%2014%2F15-brightgreen.svg" alt="Android 14/15" />
</p>

---

## 📌 Giới thiệu Tổng quan

**Wordwave** là ứng dụng đọc sách điện tử (EPUB, TXT) tích hợp trình phát âm thanh sách nói (Audiobook) hiện đại. Ứng dụng cung cấp giải pháp đọc sách kết hợp nghe nhìn toàn diện: vừa theo dõi văn bản được bôi sáng trực tiếp theo thời gian thực (Real-time Highlighting), vừa lắng nghe giọng đọc truyền cảm từ các mô hình AI Neural tiên tiến chạy hoàn toàn **Offline**, hòa cùng các bản nhạc nền tĩnh tâm nhẹ nhàng.

---

## ✨ Tính năng Nổi bật

### 1. 📚 Quản lý Thư viện Sách Thông minh
- **Hỗ trợ định dạng phong phú**: Nhập trực tiếp tệp sách `.epub` và `.txt` từ bộ nhớ thiết bị.
- **Trích xuất thông minh**: Tự động bóc tách mục lục chương, lọc nội dung văn bản sạch sẽ, tính toán số từ, ước tính thời lượng nghe và tiến độ hoàn thành.
- **Phân loại & Tìm kiếm**: Bộ lọc đa năng (Đang đọc, Đã hoàn thành, Thể loại) và tìm kiếm tức thì.

### 2. 🎧 Trình Đọc & Nghe Chuyên sâu (Reader Screen)
- **3 Chế độ Đọc linh hoạt**:
  1. **Nghe & Đọc đồng thời (Read-Along Mode)**: Văn bản cuộn mượt mà theo từng câu và từng từ đang được phát.
  2. **Tập trung Đọc (Focus Read Mode)**: Giao diện tối giản thanh lịch, loại bỏ các chi tiết gây xao nhãng.
  3. **Chế độ Lái xe An toàn (Driving / Listen Mode)**: Bộ nút điều khiển siêu lớn (Play 84dp, Tua 56dp), thẻ câu thoại cỡ đại (22sp), tự động giữ sáng màn hình (`KEEP_SCREEN_ON`).
- **Real-time Text Highlighting**: Highlight câu và từ đồng bộ chính xác từng mili-giây với sóng âm PCM từ AudioTrack.
- **Cá nhân hóa giao diện**:
  - 3 Chủ đề đọc bảo vệ mắt: **Trắng ngà (Ivory)**, **Giấy cổ Sepia**, **Tối dịu mắt (Night)**.
  - Tùy chỉnh kích thước chữ (15px – 21px).
  - Tự động cuộn trang (Auto-scroll) và tùy chọn Dừng ở cuối mỗi chương.
- **Đánh dấu trang (Bookmarks)**: Lưu lại những câu trích dẫn tâm đắc kèm mốc thời gian và vị trí chương cụ thể.

### 3. 🤖 Động cơ Giọng đọc Kép (Hybrid Dual TTS Engine)
- **Hệ thống giọng Android (System TTS)**: Tương thích hoàn hảo với Google TTS, Samsung TTS và các gói giọng hệ thống có sẵn trên thiết bị.
- **AI Neural Voice Engine (Sherpa-ONNX & Piper VITS)**:
  - Chạy mô hình mạng nơ-ron cục bộ 100% **Offline**, bảo mật tuyệt đối, không tiêu tốn dữ liệu mạng khi nghe.
  - **Look-ahead Pre-fetching**: Luồng Coroutine ngầm tự động tổng hợp trước câu thoại kế tiếp (`N + 1`) vào RAM khi câu hiện tại (`N`) đang phát, xóa bỏ hoàn toàn độ trễ ngắt quãng giữa các câu (0-latency transition).
  - Kiểm tra tính toàn vẹn Protobuf Header của file `.onnx`, ngăn ngừa tuyệt đối crash native.
- **Bộ chỉnh âm nâng cao (Voice Tuning)**:
  - Tùy chỉnh tốc độ đọc siêu mượt (`0.5x` đến `2.5x`).
  - Điều chỉnh cao độ giọng đọc (`pitch`).
  - Tùy biến khoảng nghỉ giữa các câu văn (`sentence pause` từ `0.1s` đến `2.0s`).
- **Từ điển Phát âm Cá nhân (Pronunciation Dictionary)**: Cho phép tự định nghĩa các quy tắc phát âm cho từ viết tắt, từ Hán Việt hoặc thuật ngữ chuyên ngành.

### 4. 🎵 Hòa âm Nhạc nền Thư giãn (Ambient Background Music)
- **Kho nhạc tích hợp**: 4 bản nhạc nền ambient tĩnh tâm (Giai điệu Tĩnh tâm, Mưa đêm êm dịu, Sóng biển bình yên, Rừng chiều thanh vắng).
- **Nhập nhạc cá nhân**: Thêm bất kỳ tệp nhạc nào từ máy (`.mp3`, `.wav`, `.m4a`, `.ogg`, `.flac`).
- **Bộ trộn âm thời gian thực (Audio Mixer)**: Điều chỉnh thanh trượt âm lượng nhạc nền độc lập để hòa quyện hài hòa với giọng đọc truyện.

### 5. ⏰ Hẹn giờ Ngủ & Nhắc nhở Đọc sách
- **Hẹn giờ tắt thông minh (Sleep Timer)**:
  - Các mốc chọn nhanh: 5, 10, 15, 30, 45, 60, 90, 120 phút.
  - Tùy chọn dừng sau khi phát hết chương hiện tại.
  - Thanh đếm ngược tùy chỉnh chính xác từ 1 đến 480 phút.
- **Thông báo nhắc đọc sách hàng ngày (Reading Reminder)**:
  - Sử dụng Android `AlarmManager` (`setExactAndAllowWhileIdle`) lập lịch đánh thức chính xác.
  - Tùy chọn giờ phút và các ngày lặp lại trong tuần (T2 - CN).

### 6. ☁️ Sao lưu & Khôi phục Đám mây (Cloud Backup & Restore)
- **Tự động đồng bộ với Google Drive (One-Tap Sync)**:
  - Đăng nhập tài khoản Google chỉ bằng 1 chạm.
  - Tự động đẩy bản sao lưu lên thư mục bảo mật `appDataFolder` trên Google Drive.
  - Khôi phục thư viện, tiến độ đọc, dấu trang và từ điển tức thì khi đổi thiết bị mà không cần người dùng thao tác tệp tin.
- **Xuất / Nhập tệp cục bộ (.json)**: Hỗ trợ Storage Access Framework (SAF) để xuất file backup lưu về thẻ nhớ hoặc chia sẻ.

### 7. 🛡️ Chạy nền Chuẩn mực Android 14/15
- **`PlaybackForegroundService`**: Tuân thủ chuẩn loại dịch vụ `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
- **`MediaSession` & `MediaStyle Notification`**: Điều khiển phát/tạm dừng, chuyển câu, tua thời gian mượt mà từ màn hình khóa, thanh thông báo và tai nghe Bluetooth.
- Tự động tạm dừng khi rút tai nghe (`ACTION_AUDIO_BECOMING_NOISY`) và xử lý xung đột âm thanh (`AudioFocusRequest`).

---

## 🏗️ Kiến trúc Phần mềm (Architecture & Tech Stack)

Dự án được xây dựng theo mô hình **Clean Architecture** kết hợp mẫu thiết kế **MVVM (Model-View-ViewModel)** chuẩn mực của Android:

```
app/src/main/java/com/vietreader/tts/
├── data/
│   ├── cloud/                  # Google Drive API & OAuth Sync Manager
│   ├── local/                  # LocalStorage (SharedPreferences & Library State)
│   ├── parser/                 # Bộ phân tích cú pháp EPUB & TXT
│   ├── repository/             # Triển khai BookRepositoryImpl (Single Source of Truth)
│   └── tts/                    # Core Audio: SherpaOnnxTtsController, SystemTtsController,
│                               # BackgroundMusicController, AiVoiceModelManager
├── domain/
│   ├── model/                  # Domain Models (Book, Chapter, Sentence, Settings, VoiceItem)
│   └── repository/             # BookRepository Interface
├── service/                    # PlaybackForegroundService, ReadingReminderReceiver, Scheduler
└── ui/
    ├── ambient/                # Màn hình Nhạc nền (Ambient Music Tab)
    ├── bookdetail/             # Màn hình Chi tiết Sách & Mục lục
    ├── components/             # Reusable UI Components & BottomSheets
    ├── library/                # Màn hình Thư viện Sách (Library Tab)
    ├── navigation/             # Jetpack Compose Navigation Graph & Tab Routes
    ├── reader/                 # Màn hình Đọc & Nghe chính (3 chế độ đọc)
    ├── settings/               # Màn hình Cài đặt, Hồ sơ độc giả, Sao lưu
    ├── theme/                  # Hệ thống Design Tokens, Color Palette, Typography
    └── voices/                 # Màn hình Quản lý & Tải giọng đọc (Voices Tab)
```

### Công nghệ sử dụng:
- **UI Framework**: Jetpack Compose 1.7 (Compose BOM 2024.04.01), Material 3.
- **Ngôn ngữ**: Kotlin (Coroutines, StateFlow, SharedFlow, Channels).
- **AI Neural TTS**: `k2-fsa/sherpa-onnx` (1.12.23) + Piper VITS Models (`vi_VN-vivos-x_low.onnx`, `vi_VN-25hours-single-low.onnx`).
- **Phát âm thanh Native**: Android `AudioTrack` (32-bit Float PCM Streaming) & `MediaPlayer`.
- **Dịch vụ Nền**: Android `ForegroundService`, `MediaSession`, `AlarmManager`, `WakeLock`.
- **Cloud & Auth**: Google Play Services Auth (`play-services-auth:21.3.0`), Google Drive REST API v3.

---

## 🚀 Hướng dẫn Cài đặt & Build Ứng dụng

### Yêu cầu Môi trường:
- **Android Studio**: Android Studio Koala / Ladybug trở lên.
- **JDK**: Java 17 hoặc Java 21 (ví dụ JetBrains Runtime / OpenJDK).
- **Android SDK**: Compile SDK **35**, Target SDK **35**, Min SDK **26** (Android 8.0 trở lên).

### Các bước Build dự án:

1. **Clone repository về máy**:
   ```bash
   git clone https://github.com/your-username/ReadBookTTS.git
   cd ReadBookTTS
   ```

2. **Kiểm tra và tải mô hình giọng AI (Nếu muốn nạp trước vào assets)**:
   - Các tệp model `.onnx` có thể tải trực tiếp từ giao diện màn hình **Giọng đọc** trong ứng dụng qua CDN của Hugging Face.
   - Thư mục dữ liệu phiên âm chia sẻ `espeak-ng-data.zip` và `tokens.txt` đã được tích hợp sẵn trong `app/src/main/assets/`.

3. **Biên dịch bản Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Cài đặt trực tiếp lên thiết bị Android**:
   ```bash
   ./gradlew installDebug
   ```

5. **Biên dịch bản Release APK**:
   ```bash
   ./gradlew assembleRelease
   ```
   *(Tệp APK xuất ra tại `app/build/outputs/apk/release/app-release.apk`)*

---

## 🔑 Cấu hình Google Drive Sync (Dành cho Developer)

Để kích hoạt tính năng **Sao lưu tự động 1 chạm lên Google Drive**:

1. Truy cập [Google Cloud Console](https://console.cloud.google.com).
2. Tạo dự án mới và bật dịch vụ **Google Drive API**.
3. Tại mục **Credentials**, tạo **OAuth 2.0 Client ID** với:
   - **Application type**: `Android`
   - **Package name**: `com.vietreader.tts`
   - **SHA-1 certificate fingerprint**: Lấy mã SHA-1 của keystore máy bạn (với debug keystore: `./gradlew signingReport`).
4. Thêm tài khoản Gmail của bạn vào danh sách **Test users** trong mục **OAuth consent screen** (hoặc bấm *Publish app*).

---

## 📄 Bản quyền & Đóng góp (License & Credits)

- Phát triển với niềm đam mê dành cho cộng đồng yêu thích sách và công nghệ hỗ trợ đọc tại Việt Nam.
- Động cơ AI TTS sử dụng thư viện mã nguồn mở [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) và các mô hình giọng tiếng Việt chất lượng cao.
- Mọi đóng góp (Pull Request, Issue) đều được chào đón nồng nhiệt!
