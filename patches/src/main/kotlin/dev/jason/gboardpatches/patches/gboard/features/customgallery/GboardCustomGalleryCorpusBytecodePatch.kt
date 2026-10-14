package dev.jason.gboardpatches.patches.gboard.features.customgallery

import app.morphe.patcher.patch.bytecodePatch
import dev.jason.gboardpatches.patches.gboard.features.symbolfooter.gboardSymbolFooterOrderBytecodePatch
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardCustomGalleryCorpusBytecodePatch = bytecodePatch(
    description = "在 expression panel footer 加入自訂圖片與貼圖相簿 (Custom Gallery) 分頁。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesExtensionCarrierPatch,
        gboardSymbolFooterOrderBytecodePatch,
    )

    execute {
        // Shared expression corpus hook is driven through the extension runtime
    }
}
