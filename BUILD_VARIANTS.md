# Build Variants — EyeDroid VPN

## Struktur Nama Task Gradle

Format task Gradle untuk build APK:

```
:ui:assemble{Flavor}{BuildType}
```

| Bagian | Nilai | Keterangan |
|---|---|---|
| `Flavor` | `System`, `Perumda_ti`, ... | Nama tenant (dari `-PtenantFlavors`), huruf pertama kapital, `-` diganti `_` |
| `BuildType` | `Release`, `Debug`, `Googleplay` | Tipe build |

---

## Semua Kombinasi Build

Dengan tenant `system` dan `perumda-ti`, tersedia task berikut:

| Task | Output APK | Keterangan |
|---|---|---|
| `:ui:assembleSystemRelease` | `ui-system-release.apk` | Production — minified, signed, ProGuard aktif |
| `:ui:assembleSystemDebug` | `ui-system-debug.apk` | Debug — logging interceptor aktif, tidak di-obfuscate |
| `:ui:assembleSystemGoogleplay` | `ui-system-googleplay.apk` | Sama seperti release (alias untuk Play Store) |
| `:ui:assemblePerumda_tiRelease` | `ui-perumda_ti-release.apk` | Production tenant Perumda TI |
| `:ui:assemblePerumda_tiDebug` | `ui-perumda_ti-debug.apk` | Debug tenant Perumda TI |
| `:ui:assembleRelease` | semua flavor release | Build semua tenant release sekaligus |
| `:ui:assembleDebug` | semua flavor debug | Build semua tenant debug sekaligus |

---

## Perbedaan Release vs Debug

| Fitur | Release | Debug |
|---|---|---|
| ProGuard/R8 obfuscation | ✅ aktif | ❌ off |
| Minify resources | ✅ aktif | ❌ off |
| `BuildConfig.DEBUG` | `false` | `true` |
| Debug interceptor popup | ❌ tidak muncul | ✅ muncul (setiap request/response) |
| App ID suffix | `com.eyedroid.vpn` | `com.eyedroid.vpn.debug` |
| APK signing | release keystore | debug keystore |
| FLAG_SECURE (screenshot block) | ✅ aktif | ✅ aktif |

---

## BuildConfig Fields Per Flavor

Setiap APK meng-embed nilai ini di dalam binary saat build:

```kotlin
BuildConfig.TENANT_ID    // "system" | "perumda-ti" | ...
BuildConfig.TENANT_NAME  // "EyeDroid" | "Perumda TI" | ...
BuildConfig.DEBUG        // true (debug) | false (release)
```

> **TENANT_ID dan TENANT_NAME sepenuhnya di-override oleh flavor** — nilai `DEFAULT_TENANT` di `AppConfig.kt` tidak berlaku saat build dengan flavor aktif.

---

## Cara Build Manual

```bash
# Satu tenant, release
./gradlew :ui:assembleSystemRelease \
  -PtenantFlavors="system" \
  -PtenantNames="EyeDroid"

# Satu tenant, debug
./gradlew :ui:assembleSystemDebug \
  -PtenantFlavors="system" \
  -PtenantNames="EyeDroid"

# Semua tenant sekaligus (release + debug)
./build_apks.sh
```

---

## Lokasi Output APK

```
ui/build/outputs/apk/
├── system/
│   ├── release/   ui-system-release.apk
│   └── debug/     ui-system-debug.apk
└── perumda_ti/
    ├── release/   ui-perumda_ti-release.apk
    └── debug/     ui-perumda_ti-debug.apk
```

Setelah `build_apks.sh`, APK tersedia di:

```
/root/scrcpy/public/apk/
├── eyedroid-system.apk           ← release
├── eyedroid-system-debug.apk     ← debug (superadmin only)
├── eyedroid-perumda-ti.apk
└── eyedroid-perumda-ti-debug.apk
```

---

## Download APK via Server

| Endpoint | Auth | Akses |
|---|---|---|
| `GET /apk/:tenantId` | Bearer token (any) | Semua user login |
| `GET /apk/:tenantId/debug` | Bearer token | Superadmin only |

Backend API: [github.com/snipkode/scrcpy](https://github.com/snipkode/scrcpy)
