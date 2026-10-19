package dev.jason.gboardpatches.patches.gboard.registry

import com.google.gson.JsonParser
import dev.jason.gboardpatches.patches.gboard.shared.generated.GboardTargetAdmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardPublishedPatchCatalogContractTest {
    @Test
    fun catalogExposesEachPublishedMorpheRegistrationExactlyOnce() {
        val registrations = GboardPublishedPatchCatalog.morpheRegistrations

        assertTrue(registrations.isNotEmpty())
        assertEquals(registrations.size, registrations.map { patch -> patch.name }.distinct().size)
        assertSame(
            gboardRecentImagesLimitPatch,
            registrations.single { patch -> patch.name == "Recent Stickers & GIFs Limit" },
        )
        assertSame(
            gboardCustomGalleryPatch,
            registrations.single { patch -> patch.name == "Custom Gallery for GIFs & Stickers" },
        )
        assertSame(
            gboardPinsPatch,
            registrations.single { patch -> patch.name == "Pins (Pinterest)" },
        )
    }

    @Test
    fun catalogRendersThePublishedInventoryFromItsMorpheRegistrations() {
        val inventory = JsonParser.parseString(
            GboardPublishedPatchCatalog.publishedInventory("test-version"),
        ).asJsonObject
        val patches = inventory.getAsJsonArray("patches").map { element -> element.asJsonObject }

        assertEquals("test-version", inventory.get("version").asString)
        assertEquals(
            patches.map { patch -> patch.get("name").asString }.sorted(),
            patches.map { patch -> patch.get("name").asString },
        )

        val recentImagesLimit = patches.single { patch ->
            patch.get("name").asString == "Recent Stickers & GIFs Limit"
        }
        assertTrue(recentImagesLimit.get("use").asBoolean)
        assertEquals(
            listOf("18.0.3.954559732-release-arm64-v8a"),
            recentImagesLimit.getAsJsonObject("compatiblePackages")
                .getAsJsonArray("com.google.android.inputmethod.latin")
                .map { version -> version.asString },
        )
        assertTrue(recentImagesLimit.getAsJsonArray("options").isEmpty)
    }

    @Test
    fun catalogUsesOnlyTheGeneratedTargetAdmission() {
        val patches = generatedPublishedPatches()

        assertMatchesPublishedRegistrationCount(patches.size)
        assertEquals(
            setOf(GboardTargetAdmission.packageName),
            patches.flatMap { patch ->
                patch.getAsJsonObject("compatiblePackages").keySet()
            }.toSet(),
        )
        assertEquals(
            GboardTargetAdmission.versionNames.toSet(),
            patches.flatMap { patch ->
                patch.getAsJsonObject("compatiblePackages")
                    .getAsJsonArray(GboardTargetAdmission.packageName)
                    .map { version -> version.asString }
            }.toSet(),
        )
    }
}
