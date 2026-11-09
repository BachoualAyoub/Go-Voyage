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

/** Etat des formulaires et de la session mobile. */
data class EtatCompte(
    val session: SessionLocale? = null,
    val initialisation: Boolean = true,
    val inscription: Boolean = false,
    val emailPrefere: String = "",
    val chargement: Boolean = false,
    val message: String = "",
    val erreur: Boolean = false
)

/** Orchestre les actions du compte et observe la session locale. */
class CompteViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _etat = MutableStateFlow(EtatCompte())
    val etat = _etat.asStateFlow()

    init {
        viewModelScope.launch {
            auth.session.collect { session ->
                _etat.update { it.copy(session = session, initialisation = false) }
            }
        }
        viewModelScope.launch { auth.restaurer() }
    }

    /** Bascule entre inscription et connexion. */
    fun changerMode(inscription: Boolean) {
        _etat.update { it.copy(inscription = inscription, message = "", erreur = false) }
    }

    /** Cree le compte puis revient au formulaire de connexion. */
    fun inscrire(demande: InscriptionRequest) = effectuer {
        auth.inscrire(demande)
        _etat.update { it.copy(
            emailPrefere = demande.email, inscription = false,
            message = "Votre compte est créé. Vous pouvez vous connecter."
        ) }
    }

    /** Ouvre une session persistante. */
    fun connecter(demande: ConnexionRequest) = effectuer {
        auth.connecter(demande)
    }

    /** Ferme la session et efface le cache personnel. */
    fun deconnecter() = effectuer {
        val session = _etat.value.session ?: return@effectuer
        auth.deconnecter(session.jeton)
        _etat.update { EtatCompte(initialisation = false) }
    }

    /** Lance une action de compte a la fois. */
    private fun effectuer(action: suspend () -> Unit) {
        if (_etat.value.chargement) return
        _etat.update { it.copy(chargement = true, erreur = false, message = "") }
        viewModelScope.launch {
            try {
                action()
            } catch (cause: HttpException) {
                signaler(lireErreur(cause))
            } catch (_: IOException) {
                signaler("API injoignable. Vérifiez que le backend GO VOYAGE est démarré.")
            } catch (_: JsonParseException) {
                signaler("La réponse de l’API est illisible.")
            } finally {
                _etat.update { it.copy(chargement = false) }
            }
        }
    }

    /** Publie l'erreur du formulaire. */
    private fun signaler(message: String) {
        _etat.update { it.copy(erreur = true, message = message) }
    }
}
