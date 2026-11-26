package tn.govoyage.mobile.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import java.io.IOException
import java.security.MessageDigest
import retrofit2.HttpException
import tn.govoyage.mobile.data.local.dao.UtilisateurDAO
import tn.govoyage.mobile.data.local.entity.UtilisateurLocal
import tn.govoyage.mobile.data.remote.JetonMemoire
import tn.govoyage.mobile.data.remote.RetrofitInstance
import tn.govoyage.mobile.model.ConnexionRequest
import tn.govoyage.mobile.model.InscriptionRequest
import tn.govoyage.mobile.model.Utilisateur

class AuthRepository(private val dao: UtilisateurDAO) {

    private val api = RetrofitInstance.authApi
    private var compte: Utilisateur? = null
    private var identifiants: ConnexionRequest? = null

    val profilLocal: LiveData<Utilisateur?> =
        dao.observer().map { lignes -> lignes.firstOrNull()?.compte }

    fun utilisateurCourant(): Utilisateur? = compte

    suspend fun inscrire(demande: InscriptionRequest) {
        val reponse = api.inscrire(demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val utilisateur = reponse.body() ?: return
        enregistrerCompte(utilisateur, empreinte(demande.email, demande.motDePasse))
    }

    suspend fun connecterLocal(demande: ConnexionRequest): Utilisateur? {
        val empreinte = empreinte(demande.email, demande.motDePasse)
        for (local in dao.lireTous()) {
            val memeEmail = local.compte.email.equals(demande.email, ignoreCase = true)
            if (memeEmail && local.empreinte == empreinte) {
                compte = local.compte
                dao.activer(local.cle)
                return local.compte
            }
        }
        return null
    }

    fun memoriserIdentifiants(demande: ConnexionRequest) {
        identifiants = demande
    }

    suspend fun reconnecter(): Utilisateur {
        val demande = identifiants
            ?: throw IllegalStateException("Connectez-vous avant de reessayer.")
        return connecter(demande)
    }

    suspend fun connecter(demande: ConnexionRequest): Utilisateur {
        memoriserIdentifiants(demande)
        val reponse = api.connecter(demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val connexion = reponse.body()
            ?: throw IllegalStateException("Reponse de connexion vide.")
        JetonMemoire.enregistrer(connexion.jeton)
        try {
            return chargerCompte(empreinte(demande.email, demande.motDePasse))
        } catch (cause: Exception) {
            JetonMemoire.effacer()
            compte = null
            throw cause
        }
    }

    suspend fun deconnecter() {
        JetonMemoire.effacer()
        compte = null
    }

    private suspend fun chargerCompte(empreinte: String): Utilisateur {
        val reponse = api.moi()
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val utilisateur = reponse.body()
            ?: throw IllegalStateException("Compte introuvable.")
        compte = utilisateur
        enregistrerCompte(utilisateur, empreinte)
        return utilisateur
    }

    private suspend fun enregistrerCompte(utilisateur: Utilisateur, empreinte: String) {
        val cle = utilisateur.id.toInt()
        dao.enregistrer(
            UtilisateurLocal(cle = cle, compte = utilisateur, actif = true, empreinte = empreinte)
        )
        dao.activer(cle)
    }

    private fun empreinte(email: String, motDePasse: String): String {
        val donnees = (email.lowercase() + ":" + motDePasse).toByteArray(Charsets.UTF_8)
        val octets = MessageDigest.getInstance("SHA-256").digest(donnees)
        val texte = StringBuilder()
        for (octet in octets) texte.append("%02x".format(octet.toInt() and 0xff))
        return texte.toString()
    }
}
