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
import tn.govoyage.mobile.data.repository.CircuitRepository
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.EtapeCircuit

data class EtatCircuit(
    val chargement: Boolean = false,
    val erreur: String = "",
    val information: String = ""
)

class CircuitViewModel(private val circuits: CircuitRepository) : ViewModel() {

    private val _liste = MutableLiveData<List<Circuit>>(emptyList())
    val liste: LiveData<List<Circuit>> = _liste

    private val _detail = MutableLiveData<Circuit?>(null)
    val detail: LiveData<Circuit?> = _detail

    private val _etat = MutableLiveData(EtatCircuit())
    val etat: LiveData<EtatCircuit> = _etat

    fun charger() = executer {
        try {
            _liste.postValue(circuits.lister())
        } catch (cause: Exception) {
            _liste.postValue(emptyList())
            throw cause
        }
    }

    fun chargerDetail(id: Long) = executer {
        _detail.postValue(null)
        _detail.postValue(circuits.detail(id))
    }

    fun effacerDetail() {
        _detail.postValue(null)
    }

    fun sauver(circuit: Circuit?, nom: String, etapes: List<EtapeCircuit>, terminer: () -> Unit) = executer {
        val sauvegarde = circuits.sauver(circuit, nom, etapes)
        val restants = _liste.value.orEmpty().filter { it.id != sauvegarde.id }.toMutableList()
        restants.add(0, sauvegarde)
        _liste.postValue(restants)
        withContext(Dispatchers.Main) { terminer() }
    }

    fun supprimer(id: Long) = executer {
        circuits.supprimer(id)
        val restants = _liste.value.orEmpty().filter { it.id != id }
        _liste.postValue(restants)
    }

    fun effacerMessage() {
        val etat = _etat.value ?: return
        if (!etat.chargement) _etat.value = etat.copy(erreur = "", information = "")
    }

    private fun executer(action: suspend () -> Unit) {
        val etat = _etat.value ?: EtatCircuit()
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

    private fun messageErreur(cause: Exception): String {
        if (cause is HttpException && cause.code() == 401) {
            return "Session expiree. Reconnectez-vous au serveur."
        }
        if (cause is IOException) {
            return "Serveur injoignable. Verifiez le Wi-Fi et le backend."
        }
        return cause.message ?: "Action impossible."
    }

    private suspend fun majEtat(bloc: (EtatCircuit) -> EtatCircuit) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatCircuit()
            _etat.value = bloc(courant)
        }
    }
}
