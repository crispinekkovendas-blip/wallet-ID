# ID Vault

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

## Use it

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
