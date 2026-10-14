package dev.jason.gboardpatches.patches.gboard.features.recentimageslimit

import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.gboard.features.featureflags.applyFeatureMarker
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardRecentImagesLimitFeatureMarkerPatch = resourcePatch(
    description = "標記 Recent Stickers & GIFs Limit feature 已被打入 target APK",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(RECENT_IMAGES_LIMIT_FEATURE_MARKER)
    }
}

internal const val RECENT_IMAGES_LIMIT_FEATURE_MARKER =
    "dev.jason.gboardpatches.feature.recent_images_limit"
