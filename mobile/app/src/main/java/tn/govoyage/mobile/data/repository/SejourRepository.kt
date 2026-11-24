package tn.govoyage.mobile.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import retrofit2.HttpException
import tn.govoyage.mobile.data.local.dao.CacheSejourDAO
import tn.govoyage.mobile.data.local.entity.CacheSejour
import tn.govoyage.mobile.data.remote.JetonMemoire
import tn.govoyage.mobile.data.remote.api.SejourApi
import tn.govoyage.mobile.model.Hotel
import tn.govoyage.mobile.model.Reservation
import tn.govoyage.mobile.model.ReservationRequest

/** Resultat d'une recherche, avec l'indication d'une lecture hors ligne. */
data class ResultatHotels(val liste: List<Hotel>, val horsLigne: Boolean)

/** Resultat du carnet, avec l'indication d'une lecture hors ligne. */
data class ResultatReservations(val liste: List<Reservation>, val horsLigne: Boolean)

/** Melange l'API sejour, le cache SQLite et le jeton en memoire. */
class SejourRepository(
    private val api: SejourApi,
    private val dao: CacheSejourDAO,
    private val auth: AuthRepository
) {

    private val gson = Gson()
    private val typeHotels = object : TypeToken<List<Hotel>>() {}.type
    private val typeReservations = object : TypeToken<List<Reservation>>() {}.type

    /** Recherche des hotels, avec replay local quand le reseau manque. */
    suspend fun hotels(arrivee: String, depart: String, voyageurs: Int, chambres: Int): ResultatHotels {
        val compte = compteCourant()
        val cle = "hotels:$arrivee:$depart:$voyageurs:$chambres"
        try {
            val reponse = api.hotels(arrivee, depart, voyageurs, chambres)
            if (!reponse.isSuccessful) throw HttpException(reponse)
            val liste = reponse.body().orEmpty()
            dao.enregistrer(CacheSejour(compte, cle, gson.toJson(liste), System.currentTimeMillis()))
            return ResultatHotels(liste, false)
        } catch (_: IOException) {
            val ligne = dao.lire(compte, cle)
                ?: throw IllegalStateException("Aucun sejour enregistre sur ce telephone. Relancez la recherche avec le serveur.")
            val liste = gson.fromJson(ligne.contenu, typeHotels) ?: emptyList<Hotel>()
            return ResultatHotels(liste, true)
        }
    }

    /** Lit le carnet du compte, en servant la copie locale si le reseau tombe. */
    suspend fun reservations(): ResultatReservations {
        assurerJeton()
        val compte = compteCourant()
        try {
            var reponse = api.reservations()
            if (reponse.code() == 401 && reconnecter()) reponse = api.reservations()
            if (!reponse.isSuccessful) throw HttpException(reponse)
            val liste = reponse.body().orEmpty()
            dao.enregistrer(
                CacheSejour(compte, "reservations", gson.toJson(liste), System.currentTimeMillis())
            )
            return ResultatReservations(liste, false)
        } catch (_: IOException) {
            val ligne = dao.lire(compte, "reservations")
                ?: throw IllegalStateException("Aucune reservation enregistree sur ce telephone.")
            val liste = gson.fromJson(ligne.contenu, typeReservations) ?: emptyList<Reservation>()
            return ResultatReservations(liste, true)
        }
    }

    /** Confirme une reservation. Jamais simulee hors ligne. */
    suspend fun reserver(demande: ReservationRequest): Reservation {
        assurerJeton()
        var reponse = api.reserver(demande)
        if (reponse.code() == 401 && reconnecter()) reponse = api.reserver(demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val reservation = reponse.body()
            ?: throw IllegalStateException("Reservation confirmee mais reponse vide.")
        dao.effacerCompte(compteCourant())
        return reservation
    }

    /** Annule une reservation future. Jamais simulee hors ligne. */
    suspend fun annuler(id: Long) {
        assurerJeton()
        var reponse = api.annuler(id)
        if (reponse.code() == 401 && reconnecter()) reponse = api.annuler(id)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        dao.effacerCompte(compteCourant())
    }

    /** Identifiant local du compte, ou zero avant la premiere connexion. */
    private fun compteCourant(): Long = auth.proprietaireCourant() ?: 0L

    /** Recupere un jeton avec les identifiants deja saisis si besoin. */
    private suspend fun assurerJeton() {
        if (!JetonMemoire.estPresent()) {
            if (auth.identifiantsConnus()) auth.reconnecter()
            else throw IllegalStateException("Connectez-vous pour gerer vos reservations.")
        }
    }

    /** Tente une reconnexion silencieuse une seule fois apres un 401. */
    private suspend fun reconnecter(): Boolean {
        if (!auth.identifiantsConnus()) return false
        auth.reconnecter()
        return true
    }
}
