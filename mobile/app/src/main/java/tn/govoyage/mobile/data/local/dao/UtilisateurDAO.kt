package tn.govoyage.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tn.govoyage.mobile.data.local.entity.UtilisateurLocal

/** Acces local aux comptes deja connectes, jamais au jeton d'acces. */
@Dao
interface UtilisateurDAO {

    /** Comptes locaux, le plus recent en premier, pour pre-remplir l'e-mail. */
    @Query("SELECT * FROM utilisateur ORDER BY actif DESC, cle DESC")
    fun observer(): Flow<List<UtilisateurLocal>>

    /** Lit tous les comptes pour verifier les identifiants saisis. */
    @Query("SELECT * FROM utilisateur ORDER BY actif DESC, cle DESC")
    suspend fun lireTous(): List<UtilisateurLocal>

    /** Enregistre ou remplace un compte apres une connexion reussie. */
    @Upsert
    suspend fun enregistrer(utilisateur: UtilisateurLocal)

    /** Rend un seul compte actif et retire le statut aux autres. */
    @Query("UPDATE utilisateur SET actif = CASE WHEN cle = :cle THEN 1 ELSE 0 END")
    suspend fun activer(cle: Int)

    /** Lit l'etat de deconnexion du compte actif. */
    @Query("SELECT ferme FROM utilisateur WHERE actif = 1 LIMIT 1")
    suspend fun sessionEstFermee(): Boolean?

    /** Marque le compte actif comme ferme sans supprimer son profil. */
    @Query("UPDATE utilisateur SET ferme = 1 WHERE actif = 1")
    suspend fun marquerFermee()

    /** Marque le compte actif comme ouvert apres une connexion reussie. */
    @Query("UPDATE utilisateur SET ferme = 0 WHERE actif = 1")
    suspend fun marquerOuverte()
}
