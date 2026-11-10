package tn.govoyage.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Donnees de lecture et retour des actions distantes. */
data class EtatVoyage(
    val destinations: List<Destination> = emptyList(),
    val modeles: List<Circuit> = emptyList(),
    val circuits: List<Circuit> = emptyList(),
    val chargement: Boolean = true,
    val actionEnCours: Boolean = false,
    val erreur: String = "",
    val information: String = ""
)

/** Charge le catalogue et synchronise les modifications du voyageur. */
class VoyageViewModel(
    private val destinations: DestinationRepository,
    private val circuits: CircuitRepository,
    private val auth: AuthRepository,
    private val session: SessionLocale
) : ViewModel() {
    private val _etat = MutableStateFlow(EtatVoyage())
    val etat = _etat.asStateFlow()

    init {
        viewModelScope.launch {
            destinations.destinations.collect { liste -> _etat.update { it.copy(destinations = liste) } }
        }
        viewModelScope.launch {
            circuits.modeles.collect { liste -> _etat.update { it.copy(modeles = liste) } }
        }
        viewModelScope.launch {
            circuits.observer(session.compte.id).collect { liste -> _etat.update { it.copy(circuits = liste) } }
        }
        actualiser()
    }

    /** Recharge les donnees distantes en gardant le cache visible. */
    fun actualiser() = executer(chargement = true) {
        destinations.actualiser(session)
        circuits.actualiser(session)
    }

    /** Sauvegarde le brouillon et ferme l'editeur apres le succes. */
    fun sauver(id: Long?, demande: CircuitRequest, terminer: () -> Unit) = executer {
        circuits.sauver(session, id, demande)
        _etat.update { it.copy(information = "Circuit enregistré.") }
        terminer()
    }

    /** Supprime un circuit confirme par le voyageur. */
    fun supprimer(id: Long) = executer {
        circuits.supprimer(session, id)
        _etat.update { it.copy(information = "Circuit supprimé.") }
    }

    /** Remet a zero le retour d'une action precedente. */
    fun effacerMessage() {
        if (!_etat.value.actionEnCours) _etat.update { it.copy(erreur = "", information = "") }
    }

    /** Execute une seule operation distante et preserve les donnees deja lues. */
    private fun executer(chargement: Boolean = false, action: suspend () -> Unit) {
        if (_etat.value.actionEnCours) return
        _etat.update { it.copy(actionEnCours = true, chargement = chargement, erreur = "", information = "") }
        viewModelScope.launch {
            try {
                action()
            } catch (cause: HttpException) {
                if (cause.code() == 401 || cause.code() == 403) auth.deconnecter(session.jeton)
                else signaler(lireErreur(cause))
            } catch (_: IOException) {
                signaler("Connexion indisponible. Les données déjà chargées restent visibles.")
            } catch (_: JsonParseException) {
                signaler("La réponse de l’API est illisible.")
            } finally {
                _etat.update { it.copy(actionEnCours = false, chargement = false) }
            }
        }
    }

    /** Publie une erreur pour l'ecran. */
    private fun signaler(message: String) {
        _etat.update { it.copy(erreur = message) }
    }
}
