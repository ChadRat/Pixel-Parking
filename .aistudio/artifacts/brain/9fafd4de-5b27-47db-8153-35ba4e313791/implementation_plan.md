# Fix Release APK Zero-Byte Download & In-Place Update Compatibility

Deliver a verified, non-truncated Release APK signed with the debug key so it updates directly over existing installations without 0-byte download errors or signature conflicts.

### User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed two critical requirements:
> 1. **The release APK was downloading as 0 bytes**: Direct browser downloads of raw `.apk` files from web file trees trigger download streaming dropouts and browser security interception.
> 2. **Sign the Release APK with the debug key**: This allows the optimized release build to update seamlessly over existing device installations without uninstalling or losing data.

- **Confirmed Decision 1**: Sign the Release APK with `debug.keystore` (using full V1+V2+V3 schemes) so Android permits direct in-place upgrading over the user's existing working debug installation.
- **Confirmed Decision 2**: Provide both the standalone APKs and single-file `.zip` packages (`PixelParking-release.apk.zip`) in the root, and provide direct instructions on downloading via the web IDE vs. the built-in "Export project as ZIP" setting to ensure 0-byte downloads never occur.
- **Confirmed Decision 3**: Also keep `PixelParking-upload-release.apk` (signed with the upload keystore) for Google Play Store upload purposes.

---

### 1. Overview & Core Concept

- **What It Does**: Resolves the 0-byte release file download issue and eliminates signature mismatch errors by signing the release build with the device-matching debug key, providing both raw `.apk` and archive-protected `.zip` files in the project root.
- **Target Audience**: Device owners updating their installed Pixel Parking app.
- **Key Value**: The user gets the optimized, production-ready release build that installs in seconds directly over their existing app with all data preserved.

---

### 2. User Experience & Verification Flows

1. **Download**:
   - The user downloads `PixelParking-release.apk.zip` or `PixelParking-release.apk` from the root directory.
   - The ZIP packaging prevents browser 0-byte truncation.
2. **Update**:
   - The user opens the APK on their phone.
   - Android prompts "Do you want to update this app?" and completes the update successfully because the signature matches the existing installation.

---

### 3. Key Technical Decisions & Execution Strategy

```
┌────────────────────────────────────────────────────────┐
│                   app/build.gradle.kts                  │
│  - Builds optimized release APK (minify=false)         │
└───────────────────────────┬────────────────────────────┘
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
   apksigner sign with debug key   apksigner sign with upload key
   (PixelParking-release.apk)      (PixelParking-upload.apk)
               │                         │
               ├─────────────────────────┤
               ▼                         ▼
    Full V1 + V2 + V3 Verification (apksigner verify -v)
               │
               ▼
┌────────────────────────────────────────────────────────┐
│             Root Directory Deliverables:               │
│  • PixelParking-release.apk (seamless update for device)│
│  • PixelParking-release.apk.zip (0-byte safe download) │
│  • PixelParking-debug.apk   (debug build)              │
│  • PixelParking-upload.apk  (Play Store upload build)  │
└────────────────────────────────────────────────────────┘
```

#### Execution Steps:
1. **Build Optimized Release APK**:
   - Run `gradle :app:assembleRelease --no-daemon`.
2. **Sign with Debug Keystore for In-Place Updates**:
   - Use Android SDK `apksigner` to sign the release APK with `debug.keystore` using `--v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true`.
   - Name this **`PixelParking-release.apk`** and **`app-release.apk`**.
3. **Keep Upload-Signed Build**:
   - Retain **`PixelParking-upload.apk`** signed with `my-upload-key.jks`.
4. **Package Safe ZIP Archives**:
   - Create `PixelParking-release.apk.zip` containing the new release APK.
5. **Verify Full Integrity**:
   - Verify signatures with `apksigner verify -v --print-certs`.
   - Verify ZIP with `unzip -t`.
   - Set permissions to `chmod 666`.
