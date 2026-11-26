package tn.govoyage.mobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import tn.govoyage.mobile.model.Destination


@Entity(tableName = "destination")
data class DestinationLocale(
    @PrimaryKey val id: Long,
    @Embedded(prefix = "info_") val donnee: Destination
)
