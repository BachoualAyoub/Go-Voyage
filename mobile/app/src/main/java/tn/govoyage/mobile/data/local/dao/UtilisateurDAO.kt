package tn.govoyage.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tn.govoyage.mobile.data.local.entity.UtilisateurLocal

/** Acces local au profil du compte, jamais au jeton d'acces. */
@Dao
interface UtilisateurDAO {

    /** Profil local observe par les ecrans d'authentification. */
    @Query("SELECT * FROM utilisateur WHERE cle = 1")
    fun observer(): Flow<UtilisateurLocal?>

    /** Enregistre ou remplace le profil apres une connexion reussie. */
    @Upsert
    suspend fun enregistrer(utilisateur: UtilisateurLocal)

    /** Supprime le profil local lors de la deconnexion. */
    @Query("DELETE FROM utilisateur")
    suspend fun effacer()
}
