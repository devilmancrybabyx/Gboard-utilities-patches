package dev.jason.gboardpatches.patches.gboard.shared

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD
import org.w3c.dom.Document
import org.w3c.dom.Element

internal val gboardZPatchesSettingsXmlPatch = resourcePatch(
    description = "在 Gboard 主設定最下方加入 z-patches 專用設定區塊。"
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardPatchesExtensionCarrierPatch
    )

    finalize {
        applyZPatchesSettingsXmlPatch()
    }
}

context(context: ResourcePatchContext)
private fun applyZPatchesSettingsXmlPatch() = with(context) {
    document("AndroidManifest.xml").use { manifestDoc ->
        val packageName = manifestDoc.documentElement.getAttribute("package")
        GBOARD_SETTINGS_XML_PATHS.forEach { xmlPath ->
            document(xmlPath).use { settingsDoc ->
                injectZPatchesSection(settingsDoc, packageName)
            }
        }
    }
}

private const val Z_PATCHES_CATEGORY_KEY = "z_patches_category"
private const val Z_PATCHES_ENTRY_KEY = "z_patches_entry"
private const val Z_PATCHES_TITLE = "z-patches"
private const val Z_PATCHES_SUMMARY = "Recent stickers & GIFs limit, Custom Gallery"
private const val HEADER_PREFERENCE_TAG =
    "com.google.android.libraries.inputmethod.settings.widget.HeaderPreference"

private fun injectZPatchesSection(document: Document, packageName: String) {
    val root = document.documentElement
    val footerPreference = root.childElements().firstOrNull {
        it.tagName == "com.android.settingslib.widget.FooterPreference"
    }

    val existingCategory = root.childElements("androidx.preference.PreferenceCategory").firstOrNull {
        it.getAttributeNS(ANDROID_NS, "key") == Z_PATCHES_CATEGORY_KEY ||
            it.getAttribute("android:key") == Z_PATCHES_CATEGORY_KEY
    }

    val category = existingCategory ?: document.createElement("androidx.preference.PreferenceCategory").also { newCat ->
        if (footerPreference != null) {
            root.insertBefore(newCat, footerPreference)
        } else {
            root.appendChild(newCat)
        }
    }

    category.setAttributeNS(ANDROID_NS, "android:key", Z_PATCHES_CATEGORY_KEY)
    category.setAttributeNS(ANDROID_NS, "android:title", Z_PATCHES_TITLE)

    val existingEntry = category.childElements().firstOrNull {
        it.getAttributeNS(ANDROID_NS, "key") == Z_PATCHES_ENTRY_KEY ||
            it.getAttribute("android:key") == Z_PATCHES_ENTRY_KEY
    }

    val entry = existingEntry ?: document.createElement(HEADER_PREFERENCE_TAG).also { newEntry ->
        category.appendChild(newEntry)
    }

    entry.setAttributeNS(ANDROID_NS, "android:key", Z_PATCHES_ENTRY_KEY)
    entry.setAttributeNS(ANDROID_NS, "android:title", Z_PATCHES_TITLE)
    entry.setAttributeNS(ANDROID_NS, "android:summary", Z_PATCHES_SUMMARY)
    entry.setAttributeNS(ANDROID_NS, "android:persistent", "false")
    entry.setAttributeNS(ANDROID_NS, "android:selectable", "true")

    val intent = entry.childElements("intent").firstOrNull() ?: document.createElement("intent").also { newIntent ->
        entry.appendChild(newIntent)
    }
    intent.setAttributeNS(ANDROID_NS, "android:action", "android.intent.action.MAIN")
    intent.setAttributeNS(ANDROID_NS, "android:targetPackage", packageName)
    intent.setAttributeNS(ANDROID_NS, "android:targetClass", GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS)

    val extra = intent.childElements("extra").firstOrNull {
        it.getAttributeNS(ANDROID_NS, "name") == "target_feature" ||
            it.getAttribute("android:name") == "target_feature"
    } ?: document.createElement("extra").also { newExtra ->
        intent.appendChild(newExtra)
    }
    extra.setAttributeNS(ANDROID_NS, "android:name", "target_feature")
    extra.setAttributeNS(ANDROID_NS, "android:value", "z_patches")

    // Ensure category is strictly last among categories (before footer if present)
    if (footerPreference != null && category.nextSibling !== footerPreference) {
        root.removeChild(category)
        root.insertBefore(category, footerPreference)
    }
}
