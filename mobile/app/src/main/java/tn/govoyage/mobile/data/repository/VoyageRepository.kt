package tn.govoyage.mobile.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MultipartBody
import retrofit2.HttpException
import tn.govoyage.mobile.data.local.dao.CacheVoyageDAO
import tn.govoyage.mobile.data.local.entity.CacheVoyage
import tn.govoyage.mobile.data.remote.JetonMemoire
import tn.govoyage.mobile.data.remote.api.VoyageApi
import tn.govoyage.mobile.model.*

/** Relie les parcours partages au serveur sans remplacer l'authentification locale. */
class VoyageRepository(val api: VoyageApi, private val cache: CacheVoyageDAO, private val auth: AuthRepository) {
    private val gson = Gson()
    private val verrou = Mutex()
    var horsLigne = false
        private set
    /** Identifiant local actif, conserve dans les cles du cache. */
    fun compte(): Long = auth.proprietaireCourant() ?: throw IllegalStateException("Connectez-vous pour continuer.")
    /** Obtient un jeton en memoire puis rejoue une seule fois une session expiree. */
    suspend fun <T> connecte(action: suspend () -> T): T {
        val proprietaire = compte()
        verrou.withLock { if (JetonMemoire.lire() == null) auth.reconnecter() }
        if (compte() != proprietaire) throw IllegalStateException("Le compte actif a changé.")
        return try { action() } catch (e: HttpException) {
            if (e.code() != 401) throw e
            verrou.withLock { auth.reconnecter() }
            if (compte() != proprietaire) throw IllegalStateException("Le compte actif a changé.")
            action()
        }
    }
    /** Les disponibilites exigent toujours une reponse recente du serveur. */
    suspend fun hotels(arrivee: String, depart: String, voyageurs: Int, chambres: Int) =
        api.hotels(arrivee, depart, voyageurs, chambres)
    /** Met a jour le carnet ou lit sa derniere copie si le reseau manque. */
    suspend fun reservations(): List<Reservation> {
        val id = compte()
        horsLigne = false
        return try {
            val liste = connecte { api.reservations() }
            cache.enregistrer(CacheVoyage(id, "reservations", gson.toJson(liste), System.currentTimeMillis()))
            liste
        } catch (e: IOException) {
            val copie = cache.lire(id, "reservations") ?: throw e
            horsLigne = true
            gson.fromJson(copie.contenu, Array<Reservation>::class.java).toList()
        }
    }
    /** L'ecriture n'est jamais simulee hors ligne. */
    suspend fun reserver(d: ReservationRequest) = connecte { api.reserver(d) }
    /** Annule une reservation appartenant au compte courant. */
    suspend fun annuler(id: Long) = connecte { api.annuler(id) }
    /** Les compteurs personnels utilisent le jeton si le profil local connait ses identifiants. */
    suspend fun publications(recherche: String): List<Publication> {
        val id = compte()
        horsLigne = false
        return try {
            val liste = connecte { api.publications(recherche) }
            cache.enregistrer(CacheVoyage(id, "recits:" + recherche, gson.toJson(liste), System.currentTimeMillis()))
            liste
        } catch (e: IOException) {
            val copie = cache.lire(id, "recits:" + recherche) ?: throw e
            horsLigne = true
            gson.fromJson(copie.contenu, Array<Publication>::class.java).toList()
        }
    }
    /** Importe le fichier dans le stockage partage PostgreSQL. */
    suspend fun importer(fichier: MultipartBody.Part) = connecte { api.importer(fichier) }
    /** Fabrique les petits corps de requete de type valeur. */
    fun texte(champ: String, valeur: String) = JsonObject().apply { addProperty(champ, valeur) }
    /** Pose explicitement l'etat d'un like ou d'une moderation. */
    fun etat(valeur: Boolean) = JsonObject().apply { addProperty("actif", valeur) }
}
