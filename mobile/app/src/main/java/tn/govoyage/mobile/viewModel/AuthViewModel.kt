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
import tn.govoyage.mobile.data.remote.adresseApi
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.repository.AuthRepository
import tn.govoyage.mobile.model.ConnexionRequest
import tn.govoyage.mobile.model.InscriptionRequest
import tn.govoyage.mobile.model.Utilisateur

/** Etat des formulaires d'authentification. */
data class EtatAuth(
    val chargement: Boolean = false,
    val message: String = "",
    val erreur: Boolean = false
)

/** Orchestre la connexion, l'inscription et la deconnexion sans session persistante. */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _compte = MutableLiveData<Utilisateur?>(auth.utilisateurCourant())
    val compte: LiveData<Utilisateur?> = _compte

    private val _profilLocal = MutableLiveData<Utilisateur?>(null)
    val profilLocal: LiveData<Utilisateur?> = _profilLocal

    private val _etat = MutableLiveData(EtatAuth())
    val etat: LiveData<EtatAuth> = _etat

    init {
        viewModelScope.launch {
            auth.profilLocal.collect { profil -> _profilLocal.postValue(profil) }
        }
    }

    /** Cree un compte sur le backend puis revient au formulaire de connexion. */
    fun inscrire(demande: InscriptionRequest) = effectuer(
        "Votre compte est cree. Vous pouvez vous connecter."
    ) {
        auth.inscrire(demande)
    }

    /** Connecte le voyageur et publie son compte aux ecrans. */
    fun connecter(demande: ConnexionRequest) = effectuer {
        val utilisateur = auth.connecter(demande)
        _compte.postValue(utilisateur)
    }

    /** Efface le jeton, le compte et la copie locale, puis previent les ecrans. */
    fun deconnecter() {
        viewModelScope.launch(Dispatchers.IO) {
            auth.deconnecter()
        }
        _compte.postValue(null)
    }

    /** Evite qu'un message de toast soit affiche deux fois. */
    fun viderMessage() {
        val etat = _etat.value ?: return
        _etat.value = etat.copy(message = "")
    }

    /** Lance une seule action de compte a la fois. */
    private fun effectuer(messageSucces: String = "", action: suspend () -> Unit) {
        if (_etat.value?.chargement == true) return
        _etat.value = EtatAuth(chargement = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
                if (messageSucces.isNotEmpty()) {
                    publierEtat { EtatAuth(message = messageSucces) }
                } else {
                    publierEtat { EtatAuth() }
                }
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: HttpException) {
                publierEtat { EtatAuth(erreur = true, message = lireErreur(cause)) }
            } catch (cause: IOException) {
                val detail = cause.message ?: "erreur reseau"
                publierEtat {
                    EtatAuth(erreur = true, message = "API injoignable : $detail (${adresseApi()})")
                }
            } catch (_: JsonParseException) {
                publierEtat { EtatAuth(erreur = true, message = "La reponse de l'API est illisible.") }
            } catch (cause: IllegalStateException) {
                publierEtat { EtatAuth(erreur = true, message = cause.message ?: "Action impossible.") }
            } finally {
                publierEtat { it.copy(chargement = false) }
            }
        }
    }

    /** Publie l'etat sur le thread principal, ou les LiveData sont observees. */
    private suspend fun publierEtat(bloc: (EtatAuth) -> EtatAuth) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatAuth()
            _etat.value = bloc(courant)
        }
    }
}
