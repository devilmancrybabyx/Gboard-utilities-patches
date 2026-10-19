package dev.jason.gboardpatches.patches.gboard.features.pins

import app.morphe.patcher.patch.bytecodePatch
import dev.jason.gboardpatches.patches.gboard.features.symbolfooter.gboardSymbolFooterOrderBytecodePatch
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardPinsCorpusBytecodePatch = bytecodePatch(
    description = "在 expression panel footer 加入 Pins (Pinterest) 搜尋與分享分頁。",
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
