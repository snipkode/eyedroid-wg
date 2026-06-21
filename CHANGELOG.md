# Changelog

## [1.0.0] — 2026-06-21

### Added
- Login screen dengan Material Design 3 dark mode
- Autentikasi ke backend EyeDroid (`POST /api/auth/login`)
- Session management dengan JWT + EncryptedSharedPreferences
- Auto-login saat startup via `GET /api/auth/me`
- Auto provisioning konfigurasi WireGuard dari `GET /api/vpn/config`
- Auto-connect setelah konfigurasi diterima
- Dashboard: status VPN, username, role, tombol connect/disconnect/refresh/logout
- SecurityCheck: root detection, emulator detection, debug detection
- `FLAG_SECURE` pada Login dan Dashboard screen
- Network security config (cleartext hanya ke `perumdati.tech`)

### Changed
- App name: `WireGuard` → `EyeDroid VPN`
- Package: `com.wireguard.android` → `com.eyedroid.vpn`
- Entry point: `MainActivity` → `LoginActivity`
- USER_AGENT: `WireGuard/...` → `EyeDroid/...`
- WireGuard Updater dinonaktifkan

### Removed
- Add/Edit/Delete/Import/Export Tunnel UI
- QR Code Scanner
- Manual Config Editor
- Multiple tunnel management
- Settings Activity dari launcher flow
- Permissions: CAMERA, WRITE_EXTERNAL_STORAGE, REQUEST_INSTALL_PACKAGES

### Security
- `allowBackup=false`
- Semua komponen `exported=false` kecuali yang wajib
- ProGuard/R8 aktif di release build

### Engine
- WireGuard Android `1.0.20260315` (tidak dimodifikasi)
