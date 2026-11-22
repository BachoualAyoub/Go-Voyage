package tn.govoyage.mobile.viewModel

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.repository.VoyageRepository
import tn.govoyage.mobile.model.Vol
import java.time.LocalDate

/** Etat du comparateur simple, aller-retour et multi-destinations. */
class VolsViewModel(private val depot: VoyageRepository) : ViewModel() {
    val offres = MutableLiveData<List<List<Vol>>>(emptyList())
    val message = MutableLiveData("")
    val occupe = MutableLiveData(false)
    var trajets = mutableListOf(Triple("Paris", "Tunis", LocalDate.now().plusDays(7).toString()))
    val choix = mutableMapOf<Int, Vol>()
    /** Compare chaque trajet dans l'ordre de depart choisi. */
    fun chercher() {
        if (occupe.value == true) return
        occupe.value = true; choix.clear(); offres.value = emptyList(); message.value = ""
        viewModelScope.launch {
            try {
                val resultat = mutableListOf<List<Vol>>()
                for ((i, trajet) in trajets.withIndex()) {
                    if (i > 0 && trajet.third < trajets[i-1].third) error("Respectez l’ordre chronologique des trajets.")
                    resultat.add(depot.api.vols(trajet.first, trajet.second, trajet.third))
                }
                offres.value = resultat
            } catch (e: CancellationException) { throw e }
            catch (e: HttpException) { message.value = lireErreur(e) }
            catch (e: Exception) { message.value = e.message ?: "Comparaison impossible." }
            finally { occupe.value = false }
        }
    }
}
