package dev.jason.gboardpatches.patches.gboard.registry

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.ResourcePatchBuilder
import app.morphe.patcher.patch.resourcePatch
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.jason.gboardpatches.patches.gboard.features.customgallery.gboardCustomGalleryCorpusBytecodePatch
import dev.jason.gboardpatches.patches.gboard.features.customgallery.gboardCustomGalleryFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.pins.gboardPinsCorpusBytecodePatch
import dev.jason.gboardpatches.patches.gboard.features.pins.gboardPinsFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.recentimageslimit.gboardRecentImagesLimitBytecodePatch
import dev.jason.gboardpatches.patches.gboard.features.recentimageslimit.gboardRecentImagesLimitFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.recentimageslimit.gboardRecentImagesLimitFlagValuePatch
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesSettingsPatch
import dev.jason.gboardpatches.patches.gboard.shared.gboardZPatchesSettingsXmlPatch
import dev.jason.gboardpatches.patches.gboard.shared.generated.GboardTargetAdmission
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

private fun gboardPublicResourcePatch(
    featureId: String,
    name: String,
    description: String,
    default: Boolean,
    block: ResourcePatchBuilder.() -> Unit,
) = GboardTargetAdmission.admittedContributions(featureId).let { admitted ->
    resourcePatch(
        name = name.takeIf { admitted.isNotEmpty() },
        description = description,
        default = default,
    ) {
        if (admitted.isNotEmpty()) {
            block()
            GboardContributionWiring.wireAdmitted(featureId, admitted, this)
        }
    }
}

@Suppress("unused")
val gboardRecentImagesLimitPatch = gboardPublicResourcePatch(
    featureId = "recent_images_limit",
    name = "Recent Stickers & GIFs Limit",
    description = "解除 Recent stickers & GIFs 10 張上限，使所有 recent store 一律讀取 recent_content_suggestion_share_max_count flag。\n" +
        "Remove the 10-item limit for recent stickers and GIFs so all stores read the recent_content_suggestion_share_max_count flag.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardZPatchesSettingsXmlPatch,
        gboardRecentImagesLimitFeatureMarkerPatch,
        gboardRecentImagesLimitBytecodePatch,
        gboardRecentImagesLimitFlagValuePatch
    )
}

@Suppress("unused")
val gboardCustomGalleryPatch = gboardPublicResourcePatch(
    featureId = "custom_gallery",
    name = "Custom Gallery for GIFs & Stickers",
    description = "在表情符號/貼圖/GIF 面板加入自訂相簿分頁，可分類管理並直接傳送手機圖片。\n" +
        "Add a custom gallery section in the emoji/stickers/GIFs panel to organize and share phone images.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardZPatchesSettingsXmlPatch,
        gboardCustomGalleryFeatureMarkerPatch,
        gboardCustomGalleryCorpusBytecodePatch
    )
}

@Suppress("unused")
val gboardPinsPatch = gboardPublicResourcePatch(
    featureId = "pins",
    name = "Pins (Pinterest)",
    description = "在貼圖與 GIF 旁加入 Pins 專屬分頁，支援 Pinterest 搜尋、搜尋紀錄、點擊卡片瀏覽關聯內容，並附有直接傳送按鈕。\n" +
        "Add a dedicated Pins tab next to GIFs and Stickers with Pinterest search, history chips, related pins on card click, and a quick send button.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardZPatchesSettingsXmlPatch,
        gboardPinsFeatureMarkerPatch,
        gboardPinsCorpusBytecodePatch
    )
}

object GboardPublishedPatchCatalog {
    val morpheRegistrations: Set<Patch<*>> = linkedSetOf(
        gboardRecentImagesLimitPatch,
        gboardCustomGalleryPatch,
        gboardPinsPatch,
    ).filterTo(linkedSetOf()) { patch -> patch.name != null }

    @Suppress("DEPRECATION")
    fun publishedInventory(version: String): String {
        val publishedPatches = morpheRegistrations.sortedBy { patch -> patch.name }.map { patch ->
            PublishedPatch(
                name = patch.name!!,
                description = patch.description,
                use = patch.use,
                dependencies = patch.dependencies.mapNotNull { dependency -> dependency.name }.sorted(),
                compatiblePackages = patch.compatiblePackages?.associate { (packageName, versions) ->
                    packageName to versions
                },
                options = patch.options.values.map { option ->
                    PublishedPatch.Option(
                        key = option.key,
                        title = option.title,
                        description = option.description,
                        required = option.required,
                        type = option.type.toString(),
                        default = option.default,
                        values = option.values,
                    )
                },
            )
        }
        val gson = GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()
        val inventory = JsonObject().apply {
            addProperty("version", version)
            add("patches", gson.toJsonTree(publishedPatches))
        }

        return gson.toJson(inventory) + "\n"
    }

    private class PublishedPatch(
        val name: String,
        val description: String?,
        val use: Boolean,
        val dependencies: List<String>,
        val compatiblePackages: Map<String, Set<String>?>?,
        val options: List<Option>,
    ) {
        class Option(
            val key: String,
            val title: String?,
            val description: String?,
            val required: Boolean,
            val type: String,
            val default: Any?,
            val values: Map<String, Any?>?,
        )
    }
}
