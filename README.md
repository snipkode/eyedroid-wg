# EyeDroid VPN

<p align="center">
  <img src="ui/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="EyeDroid VPN Logo" />
</p>

<p align="center">
  <strong>Aplikasi VPN enterprise berbasis WireGuard untuk Android</strong><br/>
  Login otomatis · Provisioning dari server · Auto-connect · Security hardening
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
  <img src="https://img.shields.io/badge/Min%20SDK-24-blue" />
  <img src="https://img.shields.io/badge/Engine-WireGuard%201.0.20260315-orange" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-lightgrey" />
</p>

---

## Daftar Isi

- [Overview](#overview)
- [Fitur](#fitur)
- [Arsitektur](#arsitektur)
- [Alur Kerja Aplikasi](#alur-kerja-aplikasi)
- [API Backend](#api-backend)
- [Struktur Proyek](#struktur-proyek)
- [Persyaratan Build](#persyaratan-build)
- [Build Debug](#build-debug)
- [Dev Mode (`dev.sh`)](#dev-mode-devsh)
- [Build Release & Signing](#build-release--signing)
- [Build Satu Tenant (build_one.sh)](#build-satu-tenant-build_onesh)
- [Build Multi-Tenant (Build Flavors)](#build-multi-tenant-build-flavors)
- [APK Download via scrcpy-ui](#apk-download-via-scrcpy-ui)
- [📋 Build Variants Reference](BUILD_VARIANTS.md)
- [Konfigurasi](#konfigurasi)
- [Security Hardening](#security-hardening)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)

---

## Overview

EyeDroid VPN adalah aplikasi Android yang menggunakan **WireGuard Android `1.0.20260315`** sebagai VPN engine. Aplikasi dirancang untuk deployment enterprise dengan backend EyeDroid — pengguna cukup login dengan username/password, konfigurasi VPN diambil otomatis dari server, dan koneksi dibuat tanpa interaksi tambahan.

> Pengguna tidak mengetahui bahwa aplikasi berbasis WireGuard. Semua branding WireGuard telah dihapus.

---

## Fitur

| Fitur | Keterangan |
|---|---|
| Login screen | Material Design 3, dark mode, tanpa tenant selector |
| Auto session | Token disimpan terenkripsi, re-login otomatis saat startup |
| Auto provisioning | Konfigurasi WireGuard diambil dari API setelah login |
| Auto connect | VPN langsung tersambung setelah konfigurasi diterima |
| Dashboard | Status VPN, IP, last handshake, RX/TX traffic |
| Refresh config | Update konfigurasi dan reconnect tanpa restart app |
| Logout | Clear session + disconnect VPN |
| Root detection | Warning + blokir jika device ter-root |
| Emulator detection | Warning + blokir di emulator |
| FLAG_SECURE | Layar login & dashboard tidak bisa di-screenshot |

### Fitur WireGuard yang Dinonaktifkan

Fitur-fitur berikut sengaja dihapus dari UI agar pengguna tidak dapat mengubah konfigurasi VPN secara manual:

- ~~Add / Create Tunnel~~
- ~~Edit Tunnel~~
- ~~Delete Tunnel~~
- ~~Import / Export Tunnel~~
- ~~QR Code Scanner~~
- ~~Generate Keys~~
- ~~Manual Config Editor~~
- ~~Multiple Tunnel Management~~

---

## Arsitektur

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                      │
│  LoginActivity  ←→  LoginViewModel              │
│  DashboardActivity  ←→  DashboardViewModel      │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│               Repository Layer                  │
│  AuthRepository        VpnRepository            │
│  SessionManager (EncryptedSharedPreferences)    │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│               Data Layer                        │
│  ApiService (Retrofit + OkHttp)                 │
│  LoginRequest / LoginResponse / VpnConfigResponse│
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│            WireGuard Engine                     │
│  com.wireguard.android.Application              │
│  TunnelManager  →  GoBackend  →  VpnService     │
└─────────────────────────────────────────────────┘
```

**Stack:**
- Kotlin + Coroutines
- MVVM + Repository Pattern
- ViewBinding (tidak ada DataBinding di EyeDroid layer)
- Retrofit 2.11 + OkHttp 4.12
- EncryptedSharedPreferences (security-crypto)
- Material Design 3

---

## Alur Kerja Aplikasi

```
App Start
    │
    ▼
Token ada di storage?
    ├─ YA  ──→  GET /api/auth/me
    │               ├─ 200 OK ──→ Dashboard ──→ GET /api/vpn/config ──→ Create Tunnel ──→ Connect
    │               └─ 401    ──→ Hapus token ──→ Login Screen
    │
    └─ TIDAK ──→ Login Screen
                    │
                    ▼
             POST /api/auth/login
                    ├─ 200 OK ──→ Simpan JWT ──→ GET /api/vpn/config ──→ Create Tunnel ──→ Connect
                    └─ Error  ──→ Tampilkan pesan error
                                            │
                                            ▼
                                     Dashboard
                                  (Status: Connected)
```

---

## API Backend

**Base URL:** `http://perumdati.tech/apio`

### POST `/api/auth/login`

Login dan mendapatkan JWT token.

**Request:**
```json
{
  "tenantId": "system",
  "username": "admin",
  "password": "password"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "123",
    "username": "admin",
    "role": "user"
  }
}
```

---

### GET `/api/auth/me`

Validasi session token yang tersimpan.

**Header:** `Authorization: Bearer <token>`

**Response:** `200 OK` → session valid | `401` → session expired

---

### GET `/api/vpn/config`

Ambil konfigurasi WireGuard untuk user yang sedang login.

**Header:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "config": "[Interface]\nPrivateKey=...\nAddress=10.0.0.2/32\n\n[Peer]\nPublicKey=...\nEndpoint=vpn.example.com:51820\nAllowedIPs=0.0.0.0/0\n"
}
```

> Aplikasi juga mendukung respons plain text (tanpa JSON wrapper).

---

## Struktur Proyek

```
eyedroid-wg/
├── tunnel/                          # WireGuard engine (JANGAN DIMODIFIKASI)
│   └── src/main/java/com/wireguard/
│       ├── android/backend/         # GoBackend, WgQuickBackend
│       └── config/                  # Config parser
│
├── ui/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/
│       │   ├── com/eyedroid/vpn/              ← EyeDroid layer
│       │   │   ├── AppConfig.kt
│       │   │   ├── data/
│       │   │   │   ├── api/ApiService.kt
│       │   │   │   ├── api/RetrofitClient.kt
│       │   │   │   ├── model/Models.kt
│       │   │   │   ├── repository/AuthRepository.kt
│       │   │   │   ├── repository/VpnRepository.kt
│       │   │   │   └── session/SessionManager.kt
│       │   │   ├── ui/
│       │   │   │   ├── login/LoginActivity.kt
│       │   │   │   ├── login/LoginViewModel.kt
│       │   │   │   ├── login/LoginViewModelFactory.kt
│       │   │   │   ├── dashboard/DashboardActivity.kt
│       │   │   │   ├── dashboard/DashboardViewModel.kt
│       │   │   │   └── dashboard/DashboardViewModelFactory.kt
│       │   │   └── util/SecurityCheck.kt
│       │   │
│       │   └── com/wireguard/android/         ← WireGuard layer (dipertahankan)
│       │       ├── Application.kt
│       │       ├── model/TunnelManager.kt
│       │       └── backend/GoBackend.java
│       │
│       └── res/
│           ├── layout/
│           │   ├── activity_login.xml
│           │   └── activity_dashboard.xml
│           ├── values/strings.xml
│           └── xml/network_security_config.xml
│
├── gradle/libs.versions.toml
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

---

## Persyaratan Build

| Requirement | Version |
|---|---|
| JDK | 17+ |
| Android Studio | Hedgehog 2023.1+ (atau Ladybug 2024.2+) |
| Android SDK | compileSdk 36 |
| Android NDK | r26+ (untuk WireGuard Go backend) |
| Gradle | 8.x (wrapper sudah tersedia) |
| Min Android | API 24 (Android 7.0) |

---

## Dev Mode (`dev.sh`)

Build debug + langsung install ke device — mirip `npm run dev`.

```bash
./dev.sh                          # tenant: system
./dev.sh perumda-ti "Perumda TI"  # tenant lain
```

Script akan:
1. Build debug APK (incremental, ~30 detik)
2. `adb install -r` ke device yang terhubung
3. Launch `LoginActivity` otomatis

> Pastikan device terhubung via USB dengan USB Debugging aktif (`adb devices`).

---

## Build Debug

```bash
# Clone repo
git clone https://github.com/YOUR_ORG/eyedroid-wg.git
cd eyedroid-wg

# Build debug APK
./gradlew :ui:assembleDebug

# Output:
# ui/build/outputs/apk/debug/ui-debug.apk
```

Install ke device:
```bash
adb install ui/build/outputs/apk/debug/ui-debug.apk
```

---

## Build Release & Signing

### 1. Generate Keystore (sekali saja)

```bash
keytool -genkeypair -v \
  -keystore eyedroid-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias eyedroid \
  -dname "CN=EyeDroid, OU=Mobile, O=Perumdati, L=Indonesia, ST=Indonesia, C=ID"
```

> ⚠️ Simpan `eyedroid-release.jks` dengan aman. **Jangan commit ke Git.**

### 2. Konfigurasi Signing di `ui/build.gradle.kts`

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../eyedroid-release.jks")
            storePassword = System.getenv("KEYSTORE_PASS")
            keyAlias = "eyedroid"
            keyPassword = System.getenv("KEY_PASS")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3. Build Release APK

```bash
export KEYSTORE_PASS=your_store_password
export KEY_PASS=your_key_password

./gradlew :ui:assembleRelease

# Output:
# ui/build/outputs/apk/release/ui-release.apk
```

### 4. Verifikasi APK

```bash
apksigner verify --verbose ui/build/outputs/apk/release/ui-release.apk
```

---

## Build Satu Tenant (`build_one.sh`)

Untuk build APK satu tenant saja dengan `tenantId` dan `Tenant Name` yang ditentukan secara eksplisit — tanpa fetch API.

### Mode Interaktif (Recommended)

```bash
./build_one.sh
```

Script akan menanyakan step-by-step:

```
╔══════════════════════════════════════╗
║       EyeDroid APK Builder           ║
╚══════════════════════════════════════╝

▶ Tenant ID       : perumdati
▶ Tenant Name     : Perumdati

Pilih build variant:
  1) Debug
  2) Release
  3) Debug + Release
  4) Upload ke server (Release)
▶ Pilihan [1-4]   : 4

▶ Username        : admin
▶ Password        : ****
🔐 Login ke http://perumdati.tech ...
✅ Login berhasil
🏗️  Building: perumdati (Perumdati) — variant: upload
📦 ✅ Uploaded: eyedroid-perumdati.apk
```

> Pilihan **4** otomatis login ke `POST /api/auth/login` untuk mendapatkan JWT token — tidak perlu copy-paste token manual.

### Mode Non-Interaktif (CI/CD)

```bash
# Format
./build_one.sh <tenantId> "<Tenant Name>" [OUTPUT_DIR]

# Contoh
./build_one.sh perumdati "Perumdati"
./build_one.sh system "EyeDroid"
./build_one.sh bpd-jabar "BPD Jabar" /tmp/apk-out

# Upload langsung ke server (token sudah ada)
APK_UPLOAD_URL=http://perumdati.tech APK_UPLOAD_TOKEN=<jwt> \
  ./build_one.sh perumdati "Perumdati"
```

Output APK:
```
/root/scrcpy/public/apk/
├── eyedroid-perumdati.apk         ← release
└── eyedroid-perumdati-debug.apk   ← debug
```

| Aspek | `build_one.sh` | `build_apks.sh` |
|---|---|---|
| Input tenant | Argumen CLI / interaktif | Fetch dari API |
| Jumlah tenant | Satu | Semua |
| Kecepatan | Lebih cepat | Build semua |
| Upload | Login username/password | Env `APK_UPLOAD_TOKEN` |
| Use case | Build ulang tenant tertentu | Full deployment |

---

## Build Multi-Tenant (Build Flavors)

Setiap tenant mendapat APK tersendiri dengan `tenantId` yang sudah tertanam di dalam binary (via `BuildConfig.TENANT_ID`). Build semua tenant dilakukan dalam **satu perintah** menggunakan script `build_apks.sh`.

> **Catatan:** Build flavor **akan menggantikan (override)** `DEFAULT_TENANT` yang hardcoded di `AppConfig.kt`. Nilai `TENANT_ID` dan `TENANT_NAME` sepenuhnya ditentukan oleh flavor yang di-build — bukan dari file konfigurasi statis.

### Cara Kerja

```
build_apks.sh
    │
    ▼
GET http://perumdati.tech/api/auth/tenants
    │  → [{ "id": "system", "name": "EyeDroid" }, { "id": "perumda-ti", "name": "Perumda TI" }]
    ▼
./gradlew :ui:assembleRelease
    -PtenantFlavors="system,perumda-ti"
    -PtenantNames="EyeDroid|Perumda TI"
    │
    ├─ ui-system-release.apk
    │    BuildConfig.TENANT_ID   = "system"
    │    BuildConfig.TENANT_NAME = "EyeDroid"
    │
    └─ ui-perumda_ti-release.apk
         BuildConfig.TENANT_ID   = "perumda-ti"
         BuildConfig.TENANT_NAME = "Perumda TI"
    │
    ▼
/root/scrcpy/public/apk/
    ├─ eyedroid-system.apk
    └─ eyedroid-perumda-ti.apk
```

### Jalankan Build

```bash
cd /root/eyedroid-wg-apk
./build_apks.sh

# Output APK default ke /root/scrcpy/public/apk/
# Atau tentukan folder output:
./build_apks.sh /path/to/output
```

> Script otomatis fetch daftar tenant terbaru dari API — tidak perlu update manual saat tenant baru ditambahkan.

### Tenant Baru Otomatis

Saat tenant baru dibuat via scrcpy-ui, cukup jalankan ulang `build_apks.sh`. APK baru akan ter-build dan langsung tersedia untuk didownload.

### Backend API (scrcpy)

APK di-serve dan di-upload via backend scrcpy. Source code backend tersedia di:

> 🔗 **[github.com/snipkode/scrcpy](https://github.com/snipkode/scrcpy)**

Endpoint yang digunakan:
| Method | Endpoint | Keterangan |
|---|---|---|
| `GET` | `/api/auth/tenants` | Fetch daftar tenant untuk build flavor |
| `GET` | `/apk/:tenantId` | Download APK per tenant |
| `POST` | `/api/admin/apk/:tenantId` | Upload APK hasil build (auth superadmin) |

---

## APK Download via scrcpy-ui

APK per-tenant tersedia untuk didownload langsung dari halaman **Tenant Management** di scrcpy-ui.

### Endpoint Download

```
GET /apk/:tenantId
```

Contoh:
```
http://perumdati.tech/apk/perumda-ti   → eyedroid-perumda-ti.apk
http://perumdati.tech/apk/system       → eyedroid-system.apk
```

### UI

Di halaman **Tenant Management**, setiap baris tenant memiliki tombol 📥 (Download) yang langsung mendownload APK untuk tenant tersebut. APK hanya tersedia setelah `build_apks.sh` dijalankan.

### File APK disimpan di

```
/root/scrcpy/public/apk/eyedroid-{tenantId}.apk
```

---

## Konfigurasi

### Ganti Backend URL

Edit **satu file**: `ui/src/main/java/com/eyedroid/vpn/AppConfig.kt`

```kotlin
object AppConfig {
    const val BASE_URL = "https://url-backend-kamu.com/api/"  // ← ganti di sini
    const val DEFAULT_TENANT = "system"
}
```

> Semua request (login, validasi session, ambil config VPN) otomatis menggunakan `BASE_URL` ini.  
> Tidak perlu mengubah file lain.

### `gradle.properties`

```properties
wireguardPackageName=com.eyedroid.vpn
wireguardVersionName=1.0.0
wireguardVersionCode=1
```

---

## Security Hardening

| Mekanisme | Status | Keterangan |
|---|---|---|
| `FLAG_SECURE` | ✅ | Screenshot diblokir di Login & Dashboard |
| EncryptedSharedPreferences | ✅ | JWT token dienkripsi AES-256-GCM |
| Root detection | ✅ | Cek `/su`, `/system/xbin/su`, dll. |
| Emulator detection | ✅ | Cek `Build.FINGERPRINT`, `Build.HARDWARE` |
| Debug detection | ✅ | Cek `FLAG_DEBUGGABLE` |
| `allowBackup=false` | ✅ | Backup ADB dinonaktifkan |
| `exported=false` | ✅ | Semua komponen non-publik tidak terekspos |
| Network Security Config | ✅ | Cleartext hanya ke `perumdati.tech` |
| ProGuard/R8 | ✅ | Aktif di release build |
| Auto logout 401 | ✅ | Token expired → hapus session → ke Login |
| Private key hidden | ✅ | Konfigurasi VPN tidak ditampilkan ke user |

> **TODO production:** Migrasi backend ke HTTPS dan aktifkan certificate pinning.

---

## Permissions

| Permission | Alasan |
|---|---|
| `INTERNET` | Komunikasi ke backend API EyeDroid |
| `FOREGROUND_SERVICE` | VPN service berjalan di foreground |
| `RECEIVE_BOOT_COMPLETED` | Restore koneksi VPN setelah reboot |
| `BIND_VPN_SERVICE` | Wajib untuk Android VPN framework (system use) |

Permission `CAMERA`, `WRITE_EXTERNAL_STORAGE`, dan `REQUEST_INSTALL_PACKAGES` dari WireGuard asli **telah dihapus** karena tidak diperlukan.

---

## Troubleshooting

**Login gagal / "Login failed (401)"**
- Pastikan username dan password benar
- Cek koneksi internet
- Pastikan server `perumdati.tech` dapat diakses

**VPN tidak tersambung / "Tunnel error"**
- Tekan tombol **Refresh Configuration**
- Pastikan backend mengembalikan konfigurasi WireGuard yang valid
- Cek format response: harus ada field `config` atau plain text WireGuard

**App crash saat startup**
- Pastikan build menggunakan JDK 17
- Jalankan `./gradlew clean` lalu build ulang

**Build error: "NDK not found"**
- Install NDK via Android Studio: SDK Manager → SDK Tools → NDK (Side by side)
- Atau set `ANDROID_NDK_HOME` di environment

**`security-crypto` error di Android < 6.0**
- `minSdk` sudah dikonfigurasi 24, tidak ada dukungan di bawah Android 7.0

---

## Lisensi

WireGuard engine (`tunnel/`) dilisensikan di bawah **Apache 2.0**.  
Kode EyeDroid VPN (`ui/src/main/java/com/eyedroid/`) adalah properti Perumdati.

---

<p align="center">Built with ❤️ on top of <a href="https://www.wireguard.com/">WireGuard</a></p>
