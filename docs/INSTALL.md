# Install Keeb (team / internal testing)

Debug APKs are built in GitHub Actions so you can share a **link** instead of emailing `.apk` files (often blocked as unsafe).

## Option A — GitHub Release (recommended)

1. Open **[Releases](https://github.com/elfrtz/keeb/releases)** on the repo.
2. Download the latest `keeb-v*-debug.apk`.
3. On your phone, allow install from the browser or Files app.
4. Open the APK → **Install**.
5. **Settings → System → Languages & input → On-screen keyboard** → enable **Keeb**.

### Publish a new release (maintainers)

From repo root, after pushing to `master`:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Or: **Actions → Release APK → Run workflow** and enter a tag (e.g. `v0.1.0`).

## Option B — Latest build from `master`

1. Open **[Actions](https://github.com/elfrtz/keeb/actions)** → workflow **Build APK**.
2. Open the latest green run → **Artifacts** → download `keeb-debug-apk` (contains `app-debug.apk`).
3. Install as in Option A.

## Play Protect / “unknown app”

Sideloaded debug builds are not from the Play Store. Android may show a warning; that is normal for internal testing if you trust the `elfrtz/keeb` repo.

## Setup after install

See the main [README](../README.md#setup) for wallet configuration and enabling the keyboard.
