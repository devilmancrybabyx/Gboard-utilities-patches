package dev.jason.gboardpatches.patches.gboard.registry

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.ResourcePatchBuilder

internal class GboardContributionWiringUnit(
    val contributionIds: Set<String>,
    private val dependencySupplier: () -> Array<Patch<*>>,
) {
    fun dependencies(): Array<Patch<*>> = dependencySupplier()
}

internal object GboardContributionWiring {
    private val unitsByFeature = emptyMap<String, List<GboardContributionWiringUnit>>()

    val registeredContributionIdsByFeature: Map<String, Set<String>> =
        unitsByFeature.mapValues { (_, units) ->
            units.flatMapTo(linkedSetOf()) { unit -> unit.contributionIds }
        }

    fun activeContributionIds(featureId: String, admitted: Set<String>): Set<String> =
        activeUnits(featureId, admitted).flatMapTo(linkedSetOf()) { unit -> unit.contributionIds }

    fun wireAdmitted(
        featureId: String,
        admitted: Set<String>,
        builder: ResourcePatchBuilder,
    ) {
        activeUnits(featureId, admitted).forEach { unit ->
            builder.dependsOn(*unit.dependencies())
        }
    }

    private fun activeUnits(
        featureId: String,
        admitted: Set<String>,
    ): List<GboardContributionWiringUnit> {
        val units = unitsByFeature[featureId] ?: return emptyList()
        val registeredIds = units.flatMapTo(linkedSetOf()) { unit -> unit.contributionIds }
        check(admitted.all { contributionId -> contributionId in registeredIds }) {
            "$featureId admits unregistered contribution IDs ${(admitted - registeredIds).sorted()}"
        }
        units.forEach { unit ->
            val admittedUnitIds = unit.contributionIds.intersect(admitted)
            check(admittedUnitIds.isEmpty() || admittedUnitIds == unit.contributionIds) {
                "$featureId partially admits indivisible execution unit " +
                    "${unit.contributionIds.sorted()}: ${admittedUnitIds.sorted()}"
            }
        }
        return units.filter { unit -> unit.contributionIds.all(admitted::contains) }
    }

    private fun unit(
        vararg contributionIds: String,
        dependencySupplier: () -> Array<Patch<*>>,
    ) = GboardContributionWiringUnit(contributionIds.toSet(), dependencySupplier)
}
