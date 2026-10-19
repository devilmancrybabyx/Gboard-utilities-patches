package dev.jason.gboardpatches.patches.gboard.features.pins

import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.gboard.features.featureflags.applyFeatureMarker
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardPinsFeatureMarkerPatch = resourcePatch(
    description = "標記 Pins (Pinterest) feature 已被打入 target APK",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(PINS_FEATURE_MARKER)
    }
}

internal const val PINS_FEATURE_MARKER =
    "dev.jason.gboardpatches.feature.pins"
