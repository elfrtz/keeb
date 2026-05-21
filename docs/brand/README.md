# Keeb brand assets

Shareable logo files exported from the Android launcher artwork (`ic_launcher_background` + `ic_launcher_foreground`).

| File | Size | Use |
|------|------|-----|
| `keeb-logo.svg` | vector | Figma, slides, print, any scale |
| `keeb-logo-512.png` | 512×512 | Slack, docs, avatars |
| `keeb-logo-1024.png` | 1024×1024 | Decks, store listings, high-res |

**Background:** `#0D1117`  
**Source in app:** `app/src/main/res/drawable/ic_launcher*.xml`

## Regenerate PNGs

From repo root (requires [ImageMagick](https://imagemagick.org/)):

```bash
magick -density 384 docs/brand/keeb-logo.svg -resize 512x512 docs/brand/keeb-logo-512.png
magick -density 384 docs/brand/keeb-logo.svg -resize 1024x1024 docs/brand/keeb-logo-1024.png
```

After changing the in-app icon vectors, update `keeb-logo.svg` paths to match `ic_launcher_foreground.xml`, then run the commands above.
