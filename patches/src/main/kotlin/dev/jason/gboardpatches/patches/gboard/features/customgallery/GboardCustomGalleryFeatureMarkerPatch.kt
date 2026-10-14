package dev.jason.gboardpatches.patches.gboard.features.customgallery

import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.gboard.features.featureflags.applyFeatureMarker
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardCustomGalleryFeatureMarkerPatch = resourcePatch(
    description = "標記 Custom Gallery for GIFs & Stickers feature 已被打入 target APK",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(CUSTOM_GALLERY_FEATURE_MARKER)
    }
}

internal const val CUSTOM_GALLERY_FEATURE_MARKER =
    "dev.jason.gboardpatches.feature.custom_gallery"
