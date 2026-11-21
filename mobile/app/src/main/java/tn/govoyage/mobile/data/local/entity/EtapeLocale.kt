package tn.govoyage.mobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import tn.govoyage.mobile.model.EtapeCircuit

/** Etape mise en cache avec sa position dans le circuit. */
@Entity(
    tableName = "etape",
    primaryKeys = ["circuitId", "ordre"],
    foreignKeys = [ForeignKey(
        entity = CircuitLocal::class,
        parentColumns = ["id"],
        childColumns = ["circuitId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EtapeLocale(
    val circuitId: Long,
    val ordre: Int,
    @Embedded val donnee: EtapeCircuit
)
