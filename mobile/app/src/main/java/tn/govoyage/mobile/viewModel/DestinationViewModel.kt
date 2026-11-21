package tn.govoyage.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.repository.DestinationRepository
import tn.govoyage.mobile.model.Destination

/** Etat du catalogue : chargement et erreur. */
data class EtatDestination(
    val chargement: Boolean = false,
    val erreur: String = ""
)

/** VueModele du catalogue de destinations et de sa fiche detail. */
class DestinationViewModel(private val destinations: DestinationRepository) : ViewModel() {

    private val _liste = MutableLiveData<List<Destination>>(emptyList())
    val liste: LiveData<List<Destination>> = _liste

    private val _detail = MutableLiveData<Destination?>(null)
    val detail: LiveData<Destination?> = _detail

    private val _etat = MutableLiveData(EtatDestination())
    val etat: LiveData<EtatDestination> = _etat

    init {
        viewModelScope.launch {
            destinations.destinations.collect { lignes -> _liste.postValue(lignes) }
        }
    }

    /** Charge le catalogue, en forcant un nouvel appel reseau si demande. */
    fun charger(force: Boolean = false) = executer {
        destinations.charger(force)
    }

    /** Charge le catalogue seulement si le telephone ne l'a pas encore. */
    fun chargerSiVide() = executer {
        destinations.chargerSiVide()
    }

    /** Lit la fiche d'une destination sur le backend. */
    fun chargerDetail(id: Long) = executer {
        _detail.postValue(null)
        val destination = destinations.detail(id)
        _detail.postValue(destination)
    }

    /** Remet a zero les messages avant une nouvelle action. */
    fun effacerMessage() {
        val etat = _etat.value ?: return
        if (!etat.chargement) _etat.value = etat.copy(erreur = "")
    }

    /** Execute une operation de catalogue et affiche les erreurs. */
    private fun executer(action: suspend () -> Unit) {
        val etat = _etat.value ?: EtatDestination()
        if (etat.chargement) return
        _etat.value = etat.copy(chargement = true, erreur = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: HttpException) {
                majEtat { it.copy(erreur = lireErreur(cause)) }
            } catch (_: IOException) {
                majEtat { it.copy(erreur = "Connexion indisponible. Verifiez le serveur et reessayez.") }
            } catch (_: JsonParseException) {
                majEtat { it.copy(erreur = "La reponse de l'API est illisible.") }
            } catch (cause: IllegalStateException) {
                majEtat { it.copy(erreur = cause.message ?: "Donnee introuvable.") }
            } finally {
                majEtat { it.copy(chargement = false) }
            }
        }
    }

    /** Publie l'etat depuis le thread principal. */
    private suspend fun majEtat(bloc: (EtatDestination) -> EtatDestination) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatDestination()
            _etat.value = bloc(courant)
        }
    }
}
