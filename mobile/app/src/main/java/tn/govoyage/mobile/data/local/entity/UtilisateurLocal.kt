package tn.govoyage.mobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import tn.govoyage.mobile.model.Utilisateur

/** Un compte deja connecte, avec son empreinte locale et son etat de connexion. */
@Entity(tableName = "utilisateur")
data class UtilisateurLocal(
    @PrimaryKey val cle: Int,
    @Embedded(prefix = "info_") val compte: Utilisateur,
    val actif: Boolean = false,
    val ferme: Boolean = false,
    val empreinte: String = ""
)
