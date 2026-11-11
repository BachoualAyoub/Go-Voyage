package tn.govoyage.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import tn.govoyage.mobile.data.AuthRepository
import tn.govoyage.mobile.data.CircuitRepository
import tn.govoyage.mobile.data.DestinationRepository
import tn.govoyage.mobile.data.SessionLocale
import tn.govoyage.mobile.data.lireErreur
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.CircuitRequest
import tn.govoyage.mobile.model.Destination

/** Retour des actions distantes vers les ecrans. */
data class EtatVoyage(
    val chargement: Boolean = true,
    val actionEnCours: Boolean = false,
    val erreur: String = "",
    val information: String = ""
)

/** Charge le catalogue et synchronise les modifications du voyageur. */
class VoyageViewModel(
    private val repoDestinations: DestinationRepository,
    private val repoCircuits: CircuitRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val _destinations = MutableLiveData<List<Destination>>()
    val destinations: LiveData<List<Destination>> = _destinations

    private val _modeles = MutableLiveData<List<Circuit>>()
    val modeles: LiveData<List<Circuit>> = _modeles

    private val _circuits = MutableLiveData<List<Circuit>>()
    val circuits: LiveData<List<Circuit>> = _circuits

    private val _etat = MutableLiveData(EtatVoyage())
    val etat: LiveData<EtatVoyage> = _etat

    private val _destinationSelectionne = MutableLiveData<Destination?>()
    val destinationSelectionne: LiveData<Destination?> = _destinationSelectionne

    private val _circuitSelectionne = MutableLiveData<Circuit?>()
    val circuitSelectionne: LiveData<Circuit?> = _circuitSelectionne

    // La session est lue de la base : un ViewModel vit pour un seul passage connecte.
    private var session: SessionLocale? = null

    init {
        viewModelScope.launch {
            // Des que le compte est ouvert : carnet du compte puis chargement distant.
            auth.session.collect { sessionOuverte ->
                if (sessionOuverte != null && session == null) {
                    session = sessionOuverte
                    // L'observation du carnet est un flux continu : elle ne doit pas
                    // bloquer le chargement distant qui suit.
                    viewModelScope.launch { chargerCircuits(sessionOuverte.compte.id) }
                    actualiser()
                }
            }
        }
        viewModelScope.launch {
            repoDestinations.destinations.collect { liste -> _destinations.postValue(liste) }
        }
        viewModelScope.launch {
            repoCircuits.modeles.collect { liste -> _modeles.postValue(liste) }
        }
    }

    /** Recharge les donnees distantes en gardant le cache visible. */
    fun actualiser() = executer(chargement = true) {
        val session = session ?: return@executer
        repoDestinations.actualiser(session)
        repoCircuits.actualiser(session)
    }

    /** Sauvegarde le brouillon et ferme l'editeur apres le succes. */
    fun sauver(id: Long?, demande: CircuitRequest, terminer: () -> Unit) = executer {
        val session = session ?: return@executer
        repoCircuits.sauver(session, id, demande)
        _etat.value = EtatVoyage(information = "Circuit enregistré.")
        terminer()
    }

    /** Supprime un circuit confirme par le voyageur. */
    fun supprimer(id: Long) = executer {
        val session = session ?: return@executer
        repoCircuits.supprimer(session, id)
        _etat.value = EtatVoyage(information = "Circuit supprimé.")
    }

    /** Remet a zero le retour d'une action precedente. */
    fun effacerMessage() {
        val etat = _etat.value ?: return
        if (!etat.actionEnCours) _etat.value = etat.copy(erreur = "", information = "")
    }

    /** Mémorise la destination ou le circuit ouvert dans une fiche. */
    fun selectDestination(destination: Destination?) {
        _destinationSelectionne.value = destination
    }

    fun selectCircuit(circuit: Circuit?) {
        _circuitSelectionne.value = circuit
    }

    /** Observe le carnet local du compte. */
    private suspend fun chargerCircuits(compteId: Long) {
        repoCircuits.observer(compteId).collect { liste -> _circuits.postValue(liste) }
    }

    /** Execute une seule operation distante et preserve les donnees deja lues. */
    private fun executer(chargement: Boolean = false, action: suspend () -> Unit) {
        val etat = _etat.value ?: EtatVoyage()
        if (etat.actionEnCours) return
        _etat.value = etat.copy(actionEnCours = true, chargement = chargement, erreur = "", information = "")
        viewModelScope.launch {
            try {
                action()
            } catch (cause: HttpException) {
                val session = session
                if (cause.code() == 401 || cause.code() == 403) {
                    if (session != null) auth.deconnecter(session.jeton)
                } else {
                    _etat.value = _etat.value?.copy(erreur = lireErreur(cause))
                }
            } catch (_: IOException) {
                _etat.value = _etat.value?.copy(erreur = "Connexion indisponible. Les données déjà chargées restent visibles.")
            } catch (_: JsonParseException) {
                _etat.value = _etat.value?.copy(erreur = "La réponse de l’API est illisible.")
            } finally {
                _etat.value = _etat.value?.copy(actionEnCours = false, chargement = false)
            }
        }
    }
}
