package tn.govoyage.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import tn.govoyage.mobile.data.AuthRepository
import tn.govoyage.mobile.data.ConnexionRequest
import tn.govoyage.mobile.data.InscriptionRequest
import tn.govoyage.mobile.data.SessionLocale
import tn.govoyage.mobile.data.lireErreur

/** Etat des formulaires d'authentification. */
data class EtatAuth(
    val chargement: Boolean = false,
    val message: String = "",
    val erreur: Boolean = false
)

/** Orchestre les actions du compte et observe la session locale. */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _session = MutableLiveData<SessionLocale?>()
    val session: LiveData<SessionLocale?> = _session

    private val _etat = MutableLiveData(EtatAuth())
    val etat: LiveData<EtatAuth> = _etat

    init {
        viewModelScope.launch {
            auth.session.collect { session -> _session.postValue(session) }
        }
        viewModelScope.launch(Dispatchers.IO) { auth.restaurer() }
    }

    /** Cree le compte sur le serveur. */
    fun inscrire(demande: InscriptionRequest) = effectuer {
        auth.inscrire(demande)
        majEtat { EtatAuth(chargement = false, message = "Votre compte est créé. Vous pouvez vous connecter.") }
    }

    /** Ouvre une session persistante. */
    fun connecter(demande: ConnexionRequest) = effectuer {
        auth.connecter(demande)
    }

    /** Ferme la session et efface le cache personnel. */
    fun deconnecter() = effectuer {
        val session = _session.value ?: return@effectuer
        auth.deconnecter(session.jeton)
    }

    /** Evite un message repete apres son affichage. */
    fun viderMessage() {
        val etat = _etat.value ?: return
        _etat.value = etat.copy(message = "")
    }

    /** Lance une action de compte a la fois. */
    private fun effectuer(action: suspend () -> Unit) {
        if (_etat.value?.chargement == true) return
        _etat.value = EtatAuth(chargement = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
            } catch (cause: HttpException) {
                majEtat { EtatAuth(erreur = true, message = lireErreur(cause)) }
            } catch (_: IOException) {
                majEtat { EtatAuth(erreur = true, message = "API injoignable. Vérifiez que le backend GO VOYAGE est démarré.") }
            } catch (_: JsonParseException) {
                majEtat { EtatAuth(erreur = true, message = "La réponse de l’API est illisible.") }
            } finally {
                majEtat { it.copy(chargement = false) }
            }
        }
    }

    /** Publie l'etat du formulaire depuis le thread principal. */
    private suspend fun majEtat(bloc: (EtatAuth) -> EtatAuth) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatAuth()
            _etat.value = bloc(courant)
        }
    }
}
