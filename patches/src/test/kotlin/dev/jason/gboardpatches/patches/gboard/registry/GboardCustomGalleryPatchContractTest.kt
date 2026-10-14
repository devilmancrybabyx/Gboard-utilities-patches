package dev.jason.gboardpatches.patches.gboard.registry

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardCustomGalleryPatchContractTest {
    @Test
    fun `custom gallery stays an independent public patch`() {
        val registry = read(
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/registry/" +
                "GboardPatchRegistry.kt",
        )
        val declaration = registry.substringAfter(
            "val gboardCustomGalleryPatch = gboardPublicResourcePatch(",
        ).substringBefore("\n}\n")

        assertTrue(declaration.contains("name = \"Custom Gallery for GIFs & Stickers\""))
        assertTrue(declaration.contains("gboardCustomGalleryFeatureMarkerPatch"))
        assertTrue(declaration.contains("gboardCustomGalleryCorpusBytecodePatch"))
        assertTrue(declaration.contains("gboardZPatchesSettingsXmlPatch"))
        assertTrue(registry.contains("gboardCustomGalleryPatch,"))
    }

    @Test
    fun `custom gallery components adhere to architecture contracts`() {
        val marker = read(
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/features/" +
                "customgallery/GboardCustomGalleryFeatureMarkerPatch.kt",
        )
        val runtime = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "customgallery/GboardCustomGalleryRuntime.java",
        )
        val storage = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "customgallery/GboardCustomGalleryStorage.java",
        )
        val share = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "customgallery/GboardCustomGalleryShare.java",
        )
        val zpatches = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "settings/zpatches/GboardZPatchesSettingsFeature.java",
        )

        assertTrue(marker.contains("dev.jason.gboardpatches.feature.custom_gallery"))
        assertTrue(runtime.contains("custom_gallery"))
        assertTrue(storage.contains("custom_gallery"))
        assertTrue(share.contains(".fileprovider"))
        assertTrue(share.contains("share_content"))
        assertTrue(zpatches.contains("z_patches"))
    }

    private fun read(relative: String): String =
        Files.readString(repositoryRoot().resolve(relative))

    private fun repositoryRoot(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
}
