<h1 align="center">Gboard Utilities Patches</h1>

<p align="center">
  Morphe patches for Gboard maintained by <strong>devilmancrybabyx</strong>, featuring custom media tools, Pinterest ("Pins") integration, recent stickers & GIFs limit overrides, and the unified <em>z-patches</em> in-app settings dashboard.
</p>

<p align="center">
  <a href="https://github.com/devilmancrybabyx/Gboard-utilities-patches/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/devilmancrybabyx/Gboard-utilities-patches?display_name=tag&label=Release&style=for-the-badge"></a>
  <a href="https://github.com/devilmancrybabyx/Gboard-utilities-patches"><img alt="Total downloads" src="https://img.shields.io/github/downloads/devilmancrybabyx/Gboard-utilities-patches/total?label=Downloads&style=for-the-badge"></a>
  <a href="https://morphe.software/add-source?github=devilmancrybabyx/Gboard-utilities-patches"><img alt="Add to Morphe" src="https://img.shields.io/badge/Morphe-Add%20Source-00A8FF?style=for-the-badge"></a>
  <a href="https://github.com/devilmancrybabyx/Gboard-utilities-patches"><img alt="GitHub stars" src="https://img.shields.io/github/stars/devilmancrybabyx/Gboard-utilities-patches?style=social"></a>
</p>

## Overview

**Gboard Utilities Patches** by **devilmancrybabyx** extends Google Gboard via Morphe with essential power-user media utilities, complete control over sticker & GIF limits, device custom gallery integration, direct Pinterest ("Pins") search and sharing, and a dedicated **z-patches** settings dashboard placed right inside Gboard settings.

All older unrelated patches have been pruned to deliver a fast, clean, focused patch bundle containing only the utilities you actually need.

---

## Included Patches

### 1. `Recent Stickers & GIFs Limit`
- **Purpose**: Removes Gboard's hardcoded 10-item limit for recent stickers and GIFs.
- **How it works**: Patches the `RecentImages` constructor bytecode dynamically so all recent stores query `recent_content_suggestion_share_max_count`.
- **Configurable**: Modify the limit on the fly (1 to 5,000 items) via the in-app **z-patches** settings menu or Gboard's Flag Editor with zero re-patching.

---

### 2. `Custom Gallery for GIFs & Stickers`
- **Purpose**: Adds a dedicated custom gallery tab directly inside Gboard's emoji, stickers & GIFs panel.
- **Features**:
  - Add any picture or GIF from your phone gallery using the system photo picker.
  - Organize photos into customizable categories (create, rename, delete).
  - Tap any picture to immediately share and commit it into the active text field via Gboard's FileProvider.
  - Persistent storage in app directory (`files/custom_gallery/`).
  - Toggle and configure directly from the **z-patches** settings section.

---

### 3. `Pins (Pinterest)`
- **Purpose**: Adds a dedicated "Pins" tab next to GIFs and Stickers for direct Pinterest search and sharing.
- **Features**:
  - **Pinterest Search**: Search millions of pins directly from Gboard without switching apps.
  - **Search History Chips**: Dedicated clickable chips for past searches to easily re-run favorite queries.
  - **Explore Related Pins**: Tapping any pin card displays related pins to help discover similar content and aesthetic imagery.
  - **Direct Send**: Each pin card features a tiny quick-send icon in the corner to immediately download and commit/share the high-res image into the active chat or input field.
  - **Control**: Toggle the Pins tab on/off and clear search history in **z-patches** settings.

---

## Settings: z-patches

All features are managed directly inside Gboard via the **z-patches** settings page:
- Configure Recent stickers & GIFs maximum limit (1 - 5,000).
- Toggle Custom Gallery tab, manage categories, and import images.
- Toggle Pins (Pinterest) tab and clear Pins search history.

---

## Install

Add this repository as a Morphe source:

- [Open in Morphe](https://morphe.software/add-source?github=devilmancrybabyx/Gboard-utilities-patches)
- Or manually add `https://github.com/devilmancrybabyx/Gboard-utilities-patches` in Morphe Manager.

---

## Build

Before running Gradle locally or on CI, authenticate to Morphe's GitHub Packages registry with either:

- `GPR_USER` and `GPR_KEY` (with `read:packages` permission) passed as parameters or set as GitHub repository secrets
- `gpr.user` and `gpr.key` in `~/.gradle/gradle.properties`
- `GITHUB_ACTOR` and `GITHUB_TOKEN` as environment variables

Build the Android patch bundle:

```powershell
.\gradlew.bat :patches:buildAndroid -Pgpr.user="<user>" -Pgpr.key="<token>"
```

Regenerate patch metadata:

```powershell
.\gradlew.bat generatePatchesList -Pgpr.user="<user>" -Pgpr.key="<token>"
```

Generated outputs:

- `patches/build/libs/*.mpp`
- `patches-list.json`
- `patches-bundle.json`

---

## Credits & Attribution

- Maintained and developed by **devilmancrybabyx**
- Foundational Morphe patch tooling inspired by [Jason Wu](https://github.com/jasonwu1994/Gboard-patches)

---

## License

Released under the [GNU General Public License v3.0](LICENSE).
