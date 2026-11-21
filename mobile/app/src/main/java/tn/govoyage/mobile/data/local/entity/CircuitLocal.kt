package tn.govoyage.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entete d'un circuit personnel enregistre sur le telephone. */
@Entity(tableName = "circuit")
data class CircuitLocal(
    @PrimaryKey val id: Long,
    val proprietaire: Long = 0L,
    val nom: String,
    val dureeJours: Int,
    val estimationTnd: Int,
    val serveurId: Long? = null,
    val versionServeur: Long = 0L,
    val synchronise: Boolean = true,
    val supprime: Boolean = false
)
