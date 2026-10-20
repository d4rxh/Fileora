# Fileora 📂⚡
### Premium Native Android File Utility Suite

[![Android](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Build APK](https://img.shields.io/badge/CI%2FCD-GitHub_Actions_Auto_APK-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/d4rxh/Fileora/actions)
[![Privacy](https://img.shields.io/badge/Privacy-100%25_Offline_&_Private-10B981?style=for-the-badge)](https://github.com/d4rxh/Fileora)

**Fileora** is a modern, privacy-first native Android utility application designed to compress, convert, organize, and inspect your media, documents, and archives entirely on-device. Built from the ground up with **Kotlin**, **Jetpack Compose**, and **Material 3 Expressive**.

---

## 🌟 Key Features

### 🖼️ 1. Image Compressor
- **Dynamic Quality Slider:** Fine-tune target compression ratio from 5% to 100%.
- **Resolution Downscaling:** Quick presets for 100%, 75%, 50%, or 25% downsampling.
- **Real-Time Size Comparison:** Shows original bytes, estimated compressed size, and exact percentage saved (e.g., `-64% (2.4 MB saved)`).
- **Format Flexibility:** Export to optimized JPEG or modern WebP formats.

### 🔄 2. Image Converter
- Seamless bidirectional conversion between **JPEG**, **PNG**, and **WebP**.
- Lossless PNG encoding with transparency preservation.
- Fine-grained lossy quality control for JPEG and WebP.

### 📐 3. Image Resizer
- Custom pixel dimensions (Width × Height).
- **Aspect Ratio Lock:** Automatically updates height when width changes (and vice versa).
- **Social Media Presets:** Instagram Square (1080×1080), Stories/Reels (1080×1920), YouTube Thumbnail (1280×720), and Full HD (1920×1080).
- Scale presets: 25%, 50%, 75%, 125%, 150%, 200%.

### ⚡ 4. Batch Image Processing
- Pick multiple photos simultaneously via Android Photo Picker.
- Multi-image batch actions: **Batch Compress**, **Batch Convert**, and **Batch Resize**.
- Progress tracking with cancellation support.
- Automatically bundles all processed files into a single convenient **ZIP package** or saves them directly to your device storage.

### 📄 5. Images to PDF Converter
- Select and combine multiple photos into a crisp, vector-standard PDF document.
- **Page Reordering:** Easily rearrange images up and down or remove unwanted photos.
- **Layout Controls:** Orientation (Auto, Portrait, Landscape) and customizable margins (None, Normal, Wide).
- Native vector PDF rendering via Android's `PdfDocument` API.

### 📑 6. PDF Merger & Page Organizer
- **PDF Merger:** Combine two or more PDF files into a single unified document with custom sequencing.
- **Page Organizer:** Load any PDF, inspect interactive visual page thumbnails, select/deselect specific pages to delete, and extract a clean new PDF.

### 🔍 7. PDF Inspector
- Deep inspection of PDF documents: page count, file size, and page dimensions (PostScript points).
- **Integrated Page Preview Browser:** Page-by-page visual inspection with crisp rendering.
- Cryptographic checksums (MD5 and SHA-256) for document verification.

### 📦 8. ZIP Archive Suite
- **Create ZIP Archives:** Package multiple files of any type (images, videos, PDFs, APKs) into a compressed archive.
- **Inspect ZIP Contents:** Browse archive contents, file lists, and compression ratios without extracting to disk.

### 🔬 9. File & EXIF Inspector
- Pick ANY file on your device to view:
  - Exact file size down to the byte.
  - MIME type and file format extension.
  - **Cryptographic Hashes:** Instant computation of MD5 and SHA-256 checksums with one-tap copy.
  - **Camera EXIF Metadata:** Camera make/model, focal length, aperture (f-number), ISO speed, exposure time, date taken, and orientation.

### 📜 10. Processing History
- Keeps an on-device log of all operations: tool used, original size, output size, space saved, and timestamp.
- Direct one-tap re-sharing or deletion of history items.

### 🛡️ 11. 100% Offline & Privacy-First
- Zero accounts, zero login, zero advertisements.
- **Zero Cloud Uploads:** All encoding, decoding, compression, and analysis runs 100% locally on your phone's processor.
- Does not even require the `android.permission.INTERNET` permission!

---

## 🏗️ Technical Architecture & Stack

- **Language:** 100% Native Kotlin 2.0.21
- **UI Toolkit:** Jetpack Compose with Material 3 (Material You)
- **Architecture:** Clean MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow
- **Navigation:** Jetpack Navigation Compose with fluid animated transitions
- **Image Loading:** Coil Compose
- **Document & PDF Processing:** Native Android `android.graphics.pdf.PdfRenderer` and `android.graphics.pdf.PdfDocument`
- **File System:** Android Storage Access Framework (SAF) & FileProvider for safe sharing

---

## 🚀 Automatic APK Builds (GitHub Actions)

Fileora comes with a preconfigured CI/CD workflow (`.github/workflows/build-apk.yml`) that automatically builds both **Debug** and **Release** APKs on every commit:

1. Go to the [Actions tab](https://github.com/d4rxh/Fileora/actions) on GitHub.
2. Click on the latest workflow run.
3. Scroll down to **Artifacts** to download `Fileora-Debug-APK` or `Fileora-Release-APK`.
4. Install the `.apk` file directly on your Android phone!

---

## 🛠️ Building Locally

### Prerequisites
- JDK 17 or higher
- Android SDK (API 35)

### Build with Gradle:
```bash
# Clone the repository
git clone https://github.com/d4rxh/Fileora.git
cd Fileora

# Make gradlew executable
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Building on Android using Termux

You can build Fileora directly on an Android device using Termux:

```bash
# Update packages and install OpenJDK 17 + Git
pkg update && pkg install openjdk-17 git -y

# Clone the repository
git clone https://github.com/d4rxh/Fileora.git
cd Fileora

# Build using Gradle Wrapper
chmod +x gradlew
./gradlew assembleDebug
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE). Built with ❤️ by [Darshan (@d4rxh)](https://github.com/d4rxh).
