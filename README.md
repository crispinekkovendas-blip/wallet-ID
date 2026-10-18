# Wallet ID

A private, offline wallet for your digital IDs: passports, driver's licenses, national IDs, visas, insurance cards and more.
Add a screenshot or photo of each ID, plus its details, and everything is stored **encrypted on your own device**.

> **Your IDs are never stored in this repository or on any server.** This repo only holds the app's code.
> Don't commit screenshots of your IDs here. The `.gitignore` blocks image and backup files to help prevent that.

## Features

- 💳 **Wallet design.** Every document is a card with its own colours and security-pattern artwork, stacked like Apple Wallet.
- 🇧🇷 **Brazilian documents built in**, each with its own fields: RG, CIN, CNH, CPF, CRECI, professional councils (OAB, CRM, CREA…), Título de Eleitor, Passaporte, CTPS, PIS/NIS, Cartão SUS, health plan, CRLV, Reservista, Certidão, plus "Other" for anything else.
- 🔐 **Passphrase-protected.** AES-256-GCM encryption with a key derived from your passphrase (PBKDF2-SHA256, 600,000 iterations). The key only lives in memory while unlocked.
- 🙈 **Masked numbers.** Sensitive numbers (RG, CPF, CNH, passport) are hidden until you tap Reveal. One-tap copy for every number, and CPF is auto-formatted.
- 📸 **Front and back photos** (screenshots or camera), full-screen zoom, and share to WhatsApp, email and other apps.
- ⏰ **Expiry alerts** for documents that are expired or expire within 90 days.
- 🔒 **Auto-lock** (1, 5 or 15 minutes, or after 1 minute in the background).
- 💾 **Encrypted backup** export and import between devices.
- 📱 **Installable PWA** that works fully offline. The Content-Security-Policy blocks all outside connections, and fonts are bundled locally.

## 📱 Android app (native, Kotlin)

The `android/` folder is a native Android app built with Kotlin and Jetpack Compose. It has the same wallet design and document types as the web version.

**Install:** on your Android phone, open the [latest release](../../releases/latest), download the `WalletID-….apk` file, open it and tap **Install**. The first time, Android asks you to allow installing apps from your browser.

- 🔓 Unlock with **fingerprint, face or your phone PIN**.
- 🔐 Documents and photos are encrypted with **AES-256-GCM** using a key held in the phone's **Android Keystore** (secure hardware on most phones).
- 🚫 The app has **no internet permission**, blocks **screenshots** and the recent-apps preview, and is excluded from cloud backup.
- 🔒 **Auto-locks** when you leave it (immediately, after 1 minute or after 5 minutes).
- 💾 **Encrypted backups** (`.idvault`) work in both directions between the Android app and the web app.

Every push to `main` that changes `android/` runs the tests and builds a new APK on GitHub Actions, then publishes it as a release.

### Signing key (for smooth updates)

Android only installs an update over an existing app when both are signed with the same key. Until a key is set up, each build is signed with a temporary key. To install a newer build, first **export a backup**, uninstall the old app, install the new one, then import the backup.

To fix this permanently, add these repository secrets (Settings → Secrets and variables → Actions): `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. Never commit the key itself; this repository is public.

To build it yourself, open `android/` in Android Studio, or run `./gradlew assembleDebug`.

## Use the web app

1. Open the GitHub Pages site for this repo (Settings → Pages shows the URL).
2. On your phone: **Share → Add to Home Screen** (iOS Safari) or **Install app** (Android Chrome).
3. Create a passphrase. **If you forget it, your data can't be recovered.**
4. Tap **+**, pick a screenshot, fill in the details and save.

Data lives in that browser's storage on that device. Export a backup now and then (⋯ → Export encrypted backup) and keep it somewhere safe.

## Run locally

It's plain HTML/CSS/JS with no build step:

```sh
python3 -m http.server 8000
# open http://localhost:8000
```

## Files

| File | Purpose |
| --- | --- |
| `index.html` | App screens |
| `app.js` | UI, lock and unlock, add/edit, backup |
| `types.js` | Document catalogue: card colours and fields per type |
| `crypto.js` | PBKDF2 + AES-GCM helpers (Web Crypto API) |
| `db.js` | IndexedDB storage (ciphertext only) |
| `sw.js` | Offline cache of the app code |
| `fonts/` | Inter and JetBrains Mono (SIL Open Font License) |
