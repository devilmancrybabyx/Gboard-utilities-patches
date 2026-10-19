package dev.jason.gboardpatches.patches.gboard.registry

import app.morphe.patcher.patch.Patch
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.jason.gboardpatches.patches.gboard.features.recentimageslimit.gboardRecentImagesLimitFlagValuePatch
import dev.jason.gboardpatches.patches.gboard.shared.GboardFlagFamilyFeature
import dev.jason.gboardpatches.patches.gboard.shared.generated.GboardTargetAdmission
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardPortProductCatalogContractTest {
    private val catalogPath = repositoryRoot().resolve(CATALOG_PATH)
    private val catalog: JsonObject = JsonParser.parseString(
        Files.readString(catalogPath, StandardCharsets.UTF_8),
    ).asJsonObject

    @Test
    fun catalogIsDeterministicAndDeclaresSelectedOnlyZeroSelectionComposition() {
        assertEquals("gboard-port-product-catalog.v1", catalog["format"].asString)
        assertEquals("1.15.0", catalog["catalog_version"].asString)
        val composition = catalog.getAsJsonObject("composition")
        assertEquals(
            setOf("selected_only_call_chain", "runtime_feature_mask"),
            composition.keySet(),
        )
        assertTrue(composition["selected_only_call_chain"].asBoolean)
        assertFalse(composition["runtime_feature_mask"].asBoolean)

        val features = features()
        assertEquals(
            features.map { feature -> feature["feature_id"].asString }.sorted(),
            features.map { feature -> feature["feature_id"].asString },
        )
        assertMatchesPublishedRegistrationCount(features.size)
        assertEquals(features.size, features.map { it["feature_id"].asString }.distinct().size)
        assertEquals(features.size, features.map { it["public_patch_name"].asString }.distinct().size)

        val expectedDigest = Files.readString(
            repositoryRoot().resolve(DIGEST_PATH),
            StandardCharsets.UTF_8,
        ).trim()
        assertEquals(expectedDigest, sha256(normalizedCatalogBytes()))
    }

    @Test
    fun migrationScopeAndConsumerFilesFormAClosedDeterministicControlPlane() {
        val repositoryRoot = repositoryRoot()
        assertEquals(
            EXPECTED_MIGRATION_SCOPES,
            features().associate { feature ->
                feature["feature_id"].asString to feature["migration_scope"].asString
            },
        )

        features().forEach { feature ->
            val featureId = feature["feature_id"].asString
            val consumers = feature.getAsJsonArray("consumer_files").map { it.asString }
            assertTrue("$featureId must declare a consumer file", consumers.isNotEmpty())
            assertTrue(
                "$featureId must include the public patch registry",
                REGISTRY_PATH in consumers,
            )
            assertEquals("$featureId consumer order drift", consumers.sorted(), consumers)
            assertEquals(
                "$featureId duplicate consumer file",
                consumers.size,
                consumers.distinct().size,
            )
            consumers.forEach { consumer ->
                val path = Path.of(consumer)
                assertFalse("$featureId consumer must be relative: $consumer", path.isAbsolute)
                assertFalse("$featureId consumer must use forward slashes: $consumer", '\\' in consumer)
                assertFalse(
                    "$featureId consumer must not contain a glob: $consumer",
                    consumer.any { character -> character in "*?[]{}" },
                )
                assertFalse(
                    "$featureId consumer must not contain traversal: $consumer",
                    consumer.split('/').any { segment -> segment in setOf("", ".", "..") },
                )
                assertTrue(
                    "$featureId consumer must be production-only: $consumer",
                    consumer.startsWith("patches/src/main/kotlin/") ||
                        consumer.startsWith("patches/src/main/resources/"),
                )
                assertTrue(
                    "$featureId consumer does not exist: $consumer",
                    Files.isRegularFile(repositoryRoot.resolve(path)),
                )
            }
        }
    }

    @Test
    fun flagFamilyDeclaresSelectedOnlyComposerCallsInCanonicalOrder() {
        val flagContributions = features().flatMap { feature ->
            feature.getAsJsonArray("contributions")
                .map { contribution -> contribution.asJsonObject }
                .filter { contribution ->
                    contribution["anchor_family_id"].asString == "flag_bool_getter"
                }
                .map { contribution -> feature["feature_id"].asString to contribution }
        }.sortedBy { (_, contribution) -> contribution["order"].asInt }

        assertEquals(1, flagContributions.size)
        assertEquals(
            listOf(550),
            flagContributions.map { (_, contribution) -> contribution["order"].asInt },
        )
        val runtimeCalls = flagContributions.map { (_, contribution) ->
            contribution.getAsJsonArray("runtime_calls").first().asString
        }
        assertEquals(1, runtimeCalls.distinct().size)
        flagContributions.forEach { (_, contribution) ->
            assertEquals(
                setOf("flag_bool_getter", "flag_name_field"),
                contribution.getAsJsonArray("required_bindings").map { it.asString }.toSet(),
            )
        }
        runtimeCalls.forEach { runtimeCall ->
            assertTrue(runtimeCall in enumValues<RuntimeCallId>().map { it.name })
        }
    }

    private fun features(): List<JsonObject> = catalog.getAsJsonArray("features")
        .map { element -> element.asJsonObject }

    private fun normalizedCatalogBytes(): ByteArray = Files.readString(
        catalogPath,
        StandardCharsets.UTF_8,
    ).removePrefix("\uFEFF").replace("\r\n", "\n").toByteArray(StandardCharsets.UTF_8)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun repositoryRoot(): Path {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        return generateSequence(workingDirectory) { directory -> directory.parent }
            .firstOrNull { candidate -> Files.isRegularFile(candidate.resolve("settings.gradle.kts")) }
            ?: error("Could not locate repository root from $workingDirectory")
    }

    private companion object {
        const val CATALOG_PATH =
            "patches/src/main/resources/gboard/gboard-port-product-catalog.json"
        const val DIGEST_PATH =
            "patches/src/main/resources/gboard/gboard-port-product-catalog.sha256"
        const val REGISTRY_PATH =
            "patches/src/main/kotlin/dev/jason/gboardpatches/patches/gboard/registry/GboardPatchRegistry.kt"

        val EXPECTED_MIGRATION_SCOPES = mapOf(
            "custom_gallery" to "version-sensitive",
            "pins" to "version-sensitive",
            "recent_images_limit" to "version-sensitive",
        )
    }
}
