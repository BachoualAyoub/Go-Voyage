package tn.govoyage.mobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import tn.govoyage.mobile.model.Utilisateur

/** Profil du dernier compte connecte, conserve sans jeton ni session. */
@Entity(tableName = "utilisateur")
data class UtilisateurLocal(
    @PrimaryKey val cle: Int = 1,
    @Embedded(prefix = "info_") val compte: Utilisateur
)
