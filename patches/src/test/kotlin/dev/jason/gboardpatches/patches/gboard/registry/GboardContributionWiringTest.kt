package dev.jason.gboardpatches.patches.gboard.registry

import com.google.gson.JsonParser
import dev.jason.gboardpatches.patches.gboard.shared.generated.GboardTargetAdmission
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardContributionWiringTest {
    @Test
    fun `typed wiring covers every multi-contribution catalog feature`() {
        val catalog = JsonParser.parseString(
            Files.readString(
                Path.of("src/main/resources/gboard/gboard-port-product-catalog.json"),
            ),
        ).asJsonObject
        val expected = catalog.getAsJsonArray("features")
            .map { feature -> feature.asJsonObject }
            .filter { feature -> feature.getAsJsonArray("contributions").size() > 1 }
            .associate { feature ->
                val featureId = feature.get("feature_id").asString
                featureId to GboardTargetAdmission.admittedContributionIdsByFeature
                    .getValue(featureId)
            }

        assertEquals(expected, GboardContributionWiring.registeredContributionIdsByFeature)
    }

    @Test
    fun `single contribution features have empty multi contribution active ids`() {
        assertTrue(GboardContributionWiring.activeContributionIds("custom_gallery", emptySet()).isEmpty())
        assertTrue(GboardContributionWiring.activeContributionIds("pins", emptySet()).isEmpty())
        assertTrue(GboardContributionWiring.activeContributionIds("recent_images_limit", emptySet()).isEmpty())
    }
}
