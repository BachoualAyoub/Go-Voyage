package tn.govoyage.mobile.data.repository

import java.security.MessageDigest
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

/** Authentification simple : jeton en memoire, comptes deja connectes dans Room. */
class AuthRepository(
    private val api: AuthApi,
    private val dao: UtilisateurDAO
) {

    private var compte: Utilisateur? = null
    private var identifiants: ConnexionRequest? = null

    /** Indique apres redemarrage si l'utilisateur avait demande la deconnexion. */
    suspend fun sessionEstFermee(): Boolean = dao.sessionEstFermee() ?: false

    /** Indique si des identifiants ont deja ete saisis pendant la vie du processus. */
    fun identifiantsConnus(): Boolean = identifiants != null

    /** Cherche le compte local qui correspond, sans reseau ni jeton. */
    suspend fun connecterLocal(demande: ConnexionRequest): Utilisateur? {
        val empreinte = empreinte(demande.email, demande.motDePasse)
        for (local in dao.lireTous()) {
            val memeEmail = local.compte.email.equals(demande.email, ignoreCase = true)
            if (memeEmail && local.empreinte == empreinte) {
                dao.activer(local.cle)
                return local.compte
            }
        }
        return null
    }

    /** Garde les identifiants en memoire pour une reconnexion sans ressaisie. */
    fun memoriserIdentifiants(demande: ConnexionRequest) {
        identifiants = demande
    }

    /** Dernier compte utilise, pour pre-remplir l'e-mail au demarrage. */
    val profilLocal: Flow<Utilisateur?> = dao.observer().map { lignes ->
        lignes.firstOrNull()?.compte
    }

    /** Compte connecte courant ou null apres deconnexion. */
    fun utilisateurCourant(): Utilisateur? = compte

    /** Identifiant du compte connecte, pour isoler les circuits locaux. */
    fun proprietaireCourant(): Long? = compte?.id

    /** Ouvre l'espace avec le compte local deja verifie, sans reseau ni jeton. */
    suspend fun utiliserProfilLocal(profil: Utilisateur) {
        compte = profil
        dao.activer(profil.id.toInt())
        dao.marquerOuverte()
    }

    /** Cree le compte sur le backend avant d'en garder une copie locale. */
    suspend fun inscrire(demande: InscriptionRequest) {
        val reponse = api.inscrire(demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val utilisateur = reponse.body() ?: return
        enregistrerCompte(utilisateur, empreinte(demande.email, demande.motDePasse))
    }

    /** Verifie les identifiants sur le backend puis garde le jeton en memoire. */
    suspend fun connecter(demande: ConnexionRequest): Utilisateur {
        memoriserIdentifiants(demande)
        val reponse = api.connecter(demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val connexion = reponse.body()
            ?: throw IllegalStateException("Reponse de connexion vide.")
        JetonMemoire.enregistrer(connexion.jeton)
        return try {
            chargerCompte(empreinte(demande.email, demande.motDePasse))
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

    /** Reprend un jeton avec les identifiants deja saisis, sans ecran de connexion. */
    suspend fun reconnecter() {
        val demande = identifiants
            ?: throw IllegalStateException("Connectez-vous avant de synchroniser.")
        connecter(demande)
    }

    /** Ferme la session sans effacer les comptes Room, utile hors ligne. */
    suspend fun deconnecter() {
        JetonMemoire.effacer()
        compte = null
        identifiants = null
        dao.marquerFermee()
    }

    /** Interroge /api/auth/moi puis garde le compte comme actif sur le telephone. */
    private suspend fun chargerCompte(empreinte: String): Utilisateur {
        val reponse = api.moi()
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val utilisateur = reponse.body()
            ?: throw IllegalStateException("Compte introuvable.")
        compte = utilisateur
        enregistrerCompte(utilisateur, empreinte)
        return utilisateur
    }

    /** Ecrit un compte et en fait le compte actif, sans toucher aux autres. */
    private suspend fun enregistrerCompte(utilisateur: Utilisateur, empreinte: String) {
        val cle = utilisateur.id.toInt()
        dao.enregistrer(
            UtilisateurLocal(cle = cle, compte = utilisateur, actif = true, empreinte = empreinte)
        )
        dao.activer(cle)
    }

    /** Calcule une empreinte du mot de passe, jamais stocke en clair. */
    private fun empreinte(email: String, motDePasse: String): String {
        val donnees = (email.lowercase() + ":" + motDePasse).toByteArray(Charsets.UTF_8)
        val octets = MessageDigest.getInstance("SHA-256").digest(donnees)
        val texte = StringBuilder()
        for (octet in octets) texte.append("%02x".format(octet.toInt() and 0xff))
        return texte.toString()
    }
}
