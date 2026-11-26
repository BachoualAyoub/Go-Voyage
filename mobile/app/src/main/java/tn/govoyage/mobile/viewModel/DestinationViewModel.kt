package tn.govoyage.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import retrofit2.HttpException
import tn.govoyage.mobile.data.repository.DestinationRepository
import tn.govoyage.mobile.model.Destination

data class EtatDestination(
    val chargement: Boolean = false,
    val erreur: String = "",
    val information: String = ""
)

class DestinationViewModel(private val destinations: DestinationRepository) : ViewModel() {

    val liste: LiveData<List<Destination>> = destinations.destinations

    private val _detail = MutableLiveData<Destination?>(null)
    val detail: LiveData<Destination?> = _detail

    private val _etat = MutableLiveData(EtatDestination())
    val etat: LiveData<EtatDestination> = _etat

    fun chargerSiVide() = executer {
        destinations.chargerSiVide()
    }

    fun synchroniser() = executer {
        val nombre = destinations.charger(true)
        majEtat { it.copy(information = "Catalogue synchronise ($nombre destinations).") }
    }

    fun chargerDetail(id: Long) = executer {
        _detail.postValue(null)
        _detail.postValue(destinations.detail(id))
    }

    fun effacerMessage() {
        val etat = _etat.value ?: return
        if (!etat.chargement) _etat.value = etat.copy(erreur = "", information = "")
    }

    private fun executer(action: suspend () -> Unit) {
        val etat = _etat.value ?: EtatDestination()
        if (etat.chargement) return
        _etat.value = etat.copy(chargement = true, erreur = "", information = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
            } catch (cause: Exception) {
                majEtat { it.copy(erreur = messageErreur(cause)) }
            } finally {
                majEtat { it.copy(chargement = false) }
            }
        }
    }

    // Traduit l'erreur technique en message comprehensible pour l'utilisateur
    private fun messageErreur(cause: Exception): String {
        if (cause is HttpException && cause.code() == 401) {
            return "Session expiree. Reconnectez-vous au serveur."
        }
        if (cause is IOException) {
            return "Serveur injoignable. Verifiez le Wi-Fi et le backend."
        }
        return cause.message ?: "Impossible de charger le catalogue."
    }

    private suspend fun majEtat(bloc: (EtatDestination) -> EtatDestination) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatDestination()
            _etat.value = bloc(courant)
        }
    }
}
