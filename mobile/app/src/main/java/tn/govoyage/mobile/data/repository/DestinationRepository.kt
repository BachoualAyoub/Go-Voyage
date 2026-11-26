package tn.govoyage.mobile.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import retrofit2.HttpException
import tn.govoyage.mobile.data.local.dao.DestinationDAO
import tn.govoyage.mobile.data.local.entity.DestinationLocale
import tn.govoyage.mobile.data.remote.RetrofitInstance
import tn.govoyage.mobile.model.Destination

class DestinationRepository(private val dao: DestinationDAO) {

    private val api = RetrofitInstance.destinationApi

    val destinations: LiveData<List<Destination>> =
        dao.observerDestinations().map { lignes -> lignes.map { it.donnee } }

    suspend fun charger(force: Boolean = false): Int {
        if (!force && dao.nombre() > 0) return 0
        val reponse = api.lister()
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val liste = reponse.body().orEmpty()
        dao.remplacerCatalogue(liste.map { DestinationLocale(it.id, it) })
        return liste.size
    }

    suspend fun chargerSiVide(): Int = charger(false)

    suspend fun detail(id: Long): Destination {
        val reponse = api.detail(id)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        return reponse.body() ?: throw IllegalStateException("Destination introuvable.")
    }
}
