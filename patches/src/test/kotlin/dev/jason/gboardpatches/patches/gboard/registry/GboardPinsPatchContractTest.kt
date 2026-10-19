package dev.jason.gboardpatches.patches.gboard.registry

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardPinsPatchContractTest {
    @Test
    fun `pins stays an independent public patch`() {
        val registry = read(
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/registry/" +
                "GboardPatchRegistry.kt",
        )
        val declaration = registry.substringAfter(
            "val gboardPinsPatch = gboardPublicResourcePatch(",
        ).substringBefore("\n}\n")

        assertTrue(declaration.contains("name = \"Pins (Pinterest)\""))
        assertTrue(declaration.contains("gboardPinsFeatureMarkerPatch"))
        assertTrue(declaration.contains("gboardPinsCorpusBytecodePatch"))
        assertTrue(declaration.contains("gboardZPatchesSettingsXmlPatch"))
        assertTrue(registry.contains("gboardPinsPatch,"))
    }

    @Test
    fun `pins components adhere to architecture contracts`() {
        val marker = read(
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/features/" +
                "pins/GboardPinsFeatureMarkerPatch.kt",
        )
        val view = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "pins/GboardPinsView.java",
        )
        val service = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "pins/GboardPinsService.java",
        )
        val storage = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "pins/GboardPinsStorage.java",
        )
        val settings = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "pins/GboardPinsSettings.java",
        )

        assertTrue(marker.contains("dev.jason.gboardpatches.feature.pins"))
        assertTrue(view.contains("GboardPinsView"))
        assertTrue(service.contains("pinterest.com"))
        assertTrue(storage.contains("pins_cache"))
        assertTrue(settings.contains("gboard_patches_pins"))
    }

    private fun read(relative: String): String =
        Files.readString(repositoryRoot().resolve(relative))

    private fun repositoryRoot(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
}
