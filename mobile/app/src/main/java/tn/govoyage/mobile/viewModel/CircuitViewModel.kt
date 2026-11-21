package tn.govoyage.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.adresseApi
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.repository.CircuitRepository
import tn.govoyage.mobile.data.repository.DestinationRepository
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.model.EtapeCircuit

/** Retour des actions vers les ecrans du carnet local. */
data class EtatCircuit(
    val actionEnCours: Boolean = false,
    val erreur: String = "",
    val information: String = "",
    val toast: String = ""
)

/** VueModele du carnet local et du catalogue utilise par l'editeur. */
class CircuitViewModel(
    private val repoCircuits: CircuitRepository,
    private val repoDestinations: DestinationRepository
) : ViewModel() {

    private val _circuits = MutableLiveData<List<Circuit>>(emptyList())
    val circuits: LiveData<List<Circuit>> = _circuits

    private val _destinations = MutableLiveData<List<Destination>>(emptyList())
    val destinations: LiveData<List<Destination>> = _destinations

    private val _etat = MutableLiveData(EtatCircuit())
    val etat: LiveData<EtatCircuit> = _etat

    private var demarre = false

    /** Demarre les observations locales une seule fois. */
    fun demarrer() {
        if (demarre) return
        demarre = true
        viewModelScope.launch {
            repoCircuits.observer().collect { liste -> _circuits.postValue(liste) }
        }
        viewModelScope.launch {
            repoDestinations.destinations.collect { liste -> _destinations.postValue(liste) }
        }
    }

    /** Charge le catalogue distant uniquement si le cache local est vide. */
    fun chargerDestinations() = executer {
        val nombre = repoDestinations.chargerSiVide()
        if (nombre > 0) {
            majEtat { it.copy(information = "Catalogue charge ($nombre destinations).") }
        }
    }

    /** Enregistre le circuit dans SQLite puis previent l'ecran appelant. */
    fun sauver(id: Long?, nom: String, etapes: List<EtapeCircuit>, terminer: () -> Unit) = executer {
        repoCircuits.sauver(id, nom, etapes)
        withContext(Dispatchers.Main) { terminer() }
    }

    /** Supprime un circuit du carnet local. */
    fun supprimer(id: Long) = executer {
        repoCircuits.supprimer(id)
    }

    /** Envoie les changements locaux puis recupere les circuits du compte. */
    fun synchroniser() = executer(afficherToastReseau = true) {
        val message = repoCircuits.synchroniser()
        majEtat { it.copy(information = message) }
    }

    /** Evite qu'un message de toast soit affiche deux fois. */
    fun effacerMessage() {
        val etat = _etat.value ?: return
        _etat.value = etat.copy(information = "", toast = "")
    }

    /** Execute une action et affiche les erreurs simples. */
    private fun executer(afficherToastReseau: Boolean = false, action: suspend () -> Unit) {
        val etat = _etat.value ?: EtatCircuit()
        if (etat.actionEnCours) return
        _etat.value = etat.copy(actionEnCours = true, erreur = "", information = "", toast = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: IllegalStateException) {
                majEtat { it.copy(erreur = cause.message ?: "Action impossible.") }
            } catch (cause: IllegalArgumentException) {
                majEtat { it.copy(erreur = cause.message ?: "Saisie incorrecte.") }
            } catch (cause: HttpException) {
                majEtat { it.copy(erreur = lireErreur(cause)) }
            } catch (_: IOException) {
                val messageToast = if (afficherToastReseau) {
                    "API injoignable : verifiez le serveur (${adresseApi()})."
                } else {
                    ""
                }
                majEtat {
                    it.copy(
                        erreur = "Connexion indisponible. Reessayez plus tard.",
                        toast = messageToast
                    )
                }
            } finally {
                majEtat { it.copy(actionEnCours = false) }
            }
        }
    }

    /** Modifie l'etat depuis le thread principal. */
    private suspend fun majEtat(bloc: (EtatCircuit) -> EtatCircuit) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatCircuit()
            _etat.value = bloc(courant)
        }
    }
}
