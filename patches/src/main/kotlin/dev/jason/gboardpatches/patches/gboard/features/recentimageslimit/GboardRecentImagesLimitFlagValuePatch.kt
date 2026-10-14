package dev.jason.gboardpatches.patches.gboard.features.recentimageslimit

import dev.jason.gboardpatches.patches.gboard.shared.GboardFlagFamilyFeature
import dev.jason.gboardpatches.patches.gboard.shared.gboardFlagFamilyFeaturePatch

internal val gboardRecentImagesLimitFlagValuePatch = gboardFlagFamilyFeaturePatch(
    description = "在 recent_content_suggestion_share_max_count 原始結果後套用自訂貼圖上限設定值",
    feature = GboardFlagFamilyFeature.RECENT_IMAGES_LIMIT,
)
