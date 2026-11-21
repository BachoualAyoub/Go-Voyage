package tn.govoyage.mobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import tn.govoyage.mobile.model.Circuit

/** Circuit local avec ses etapes rangees. */
data class CircuitAvecEtapes(
    @Embedded val circuit: CircuitLocal,
    @Relation(parentColumn = "id", entityColumn = "circuitId") val etapes: List<EtapeLocale>
) {
    /** Reconstruit un circuit en respectant l'ordre des etapes. */
    fun versCircuit(): Circuit = Circuit(
        id = circuit.id,
        nom = circuit.nom,
        dureeJours = circuit.dureeJours,
        estimationTnd = circuit.estimationTnd,
        etapes = etapes.sortedBy { it.ordre }.map { it.donnee },
        version = circuit.versionServeur
    )
}
