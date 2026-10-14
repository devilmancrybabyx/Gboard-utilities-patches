package dev.jason.gboardpatches.patches.gboard.registry

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardRecentImagesLimitPatchContractTest {
    @Test
    fun `recent images limit stays an independent public patch`() {
        val registry = read(
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/registry/" +
                "GboardPatchRegistry.kt",
        )
        val declaration = registry.substringAfter(
            "val gboardRecentImagesLimitPatch = gboardPublicResourcePatch(",
        ).substringBefore("\n}\n")

        assertTrue(declaration.contains("name = \"Recent Stickers & GIFs Limit\""))
        assertTrue(declaration.contains("gboardRecentImagesLimitFeatureMarkerPatch"))
        assertTrue(declaration.contains("gboardRecentImagesLimitBytecodePatch"))
        assertTrue(declaration.contains("gboardRecentImagesLimitFlagValuePatch"))
        assertTrue(declaration.contains("gboardZPatchesSettingsXmlPatch"))
        assertTrue(registry.contains("gboardRecentImagesLimitPatch,"))
    }

    @Test
    fun `marker runtime bytecode and settings use the dedicated boundary`() {
        val marker = read(
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/features/" +
                "recentimageslimit/GboardRecentImagesLimitFeatureMarkerPatch.kt",
        )
        val bytecode = read(
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/features/" +
                "recentimageslimit/GboardRecentImagesLimitBytecodePatch.kt",
        )
        val runtime = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "recentimageslimit/GboardRecentImagesLimitRuntime.java",
        )
        val settings = read(
            "extensions/extension/src/main/java/dev/jason/gboardpatches/extension/" +
                "recentimageslimit/GboardRecentImagesLimitSettings.java",
        )

        assertTrue(marker.contains("dev.jason.gboardpatches.feature.recent_images_limit"))
        assertTrue(bytecode.contains("recent_content_suggestion_shared"))
        assertTrue(bytecode.contains("0x5f0d37d6"))
        assertTrue(runtime.contains("recent_content_suggestion_share_max_count"))
        assertTrue(settings.contains("pref_recent_images_limit"))
        assertTrue(settings.contains("DEFAULT_LIMIT = 10"))
        assertTrue(settings.contains("MAX_LIMIT = 5000"))
    }

    private fun read(relative: String): String =
        Files.readString(repositoryRoot().resolve(relative))

    private fun repositoryRoot(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
}
