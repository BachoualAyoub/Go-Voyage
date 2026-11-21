package tn.govoyage.mobile.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import tn.govoyage.mobile.data.local.dao.UtilisateurDAO
import tn.govoyage.mobile.data.local.entity.UtilisateurLocal
import tn.govoyage.mobile.data.remote.JetonMemoire
import tn.govoyage.mobile.data.remote.api.AuthApi
import tn.govoyage.mobile.model.ConnexionRequest
import tn.govoyage.mobile.model.InscriptionRequest
import tn.govoyage.mobile.model.Utilisateur

/** Authentification simple : jeton en memoire, copie du profil dans Room. */
class AuthRepository(
    private val api: AuthApi,
    private val dao: UtilisateurDAO
) {

    private var compte: Utilisateur? = null

    /** Profil copie sur le telephone, sans aucun jeton. */
    val profilLocal: Flow<Utilisateur?> = dao.observer().map { it?.compte }

    /** Compte connecte courant ou null apres deconnexion. */
    fun utilisateurCourant(): Utilisateur? = compte

    /** Cree le compte sur le backend avant d'en garder une copie locale. */
    suspend fun inscrire(demande: InscriptionRequest) {
        val reponse = api.inscrire(demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val utilisateur = reponse.body() ?: return
        dao.enregistrer(UtilisateurLocal(compte = utilisateur))
    }

    /** Verifie les identifiants sur le backend puis garde le jeton en memoire. */
    suspend fun connecter(demande: ConnexionRequest): Utilisateur {
        val reponse = api.connecter(demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val connexion = reponse.body()
            ?: throw IllegalStateException("Reponse de connexion vide.")
        JetonMemoire.enregistrer(connexion.jeton)
        return try {
            chargerCompte()
        } catch (cause: CancellationException) {
            JetonMemoire.effacer()
            compte = null
            throw cause
        } catch (cause: Exception) {
            JetonMemoire.effacer()
            compte = null
            throw cause
        }
    }

    /** Efface le jeton memoire, le compte et la copie locale du profil. */
    suspend fun deconnecter() {
        JetonMemoire.effacer()
        compte = null
        dao.effacer()
    }

    /** Interroge /api/auth/moi et met a jour la copie locale du profil. */
    private suspend fun chargerCompte(): Utilisateur {
        val reponse = api.moi()
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val utilisateur = reponse.body()
            ?: throw IllegalStateException("Compte introuvable.")
        compte = utilisateur
        dao.enregistrer(UtilisateurLocal(compte = utilisateur))
        return utilisateur
    }
}
