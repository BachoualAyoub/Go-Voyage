package tn.govoyage.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import tn.govoyage.mobile.data.local.dao.DestinationDAO
import tn.govoyage.mobile.data.local.entity.DestinationLocale
import tn.govoyage.mobile.data.remote.api.DestinationApi
import tn.govoyage.mobile.model.Destination

/** Intermediaire entre le catalogue distant, son cache Room et les ecrans. */
class DestinationRepository(
    private val api: DestinationApi,
    private val dao: DestinationDAO
) {

    /** Catalogue observe par les ecrans et l'editeur de circuit. */
    val destinations: Flow<List<Destination>> =
        dao.observerDestinations().map { lignes -> lignes.map { it.donnee } }

    /** Telecharge le catalogue, ou seulement quand le cache est vide. */
    suspend fun charger(force: Boolean = false): Int {
        if (!force && dao.nombre() > 0) return 0
        val reponse = api.lister()
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val liste = reponse.body().orEmpty()
        dao.remplacerCatalogue(liste.map { DestinationLocale(it.id, it) })
        return liste.size
    }

    /** Charge le catalogue uniquement si le telephone ne l'a pas encore. */
    suspend fun chargerSiVide(): Int {
        return charger(false)
    }

    /** Lit une fiche directement sur le backend, qui possede une route detail. */
    suspend fun detail(id: Long): Destination {
        val reponse = api.detail(id)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        return reponse.body() ?: throw IllegalStateException("Destination introuvable.")
    }
}
