package tn.govoyage.mobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import tn.govoyage.mobile.model.Utilisateur

@Entity(tableName = "utilisateur")
data class UtilisateurLocal(
    @PrimaryKey val cle: Int,
    @Embedded(prefix = "info_") val compte: Utilisateur,
    val actif: Boolean = false,
    val empreinte: String = ""
)
