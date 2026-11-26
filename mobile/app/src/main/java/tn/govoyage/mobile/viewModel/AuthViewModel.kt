package tn.govoyage.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.govoyage.mobile.data.repository.AuthRepository
import tn.govoyage.mobile.model.ConnexionRequest
import tn.govoyage.mobile.model.InscriptionRequest
import tn.govoyage.mobile.model.Utilisateur

data class EtatAuth(
    val chargement: Boolean = false,
    val message: String = "",
    val erreur: Boolean = false,
    val toast: String = ""
)

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _compte = MutableLiveData<Utilisateur?>(auth.utilisateurCourant())
    val compte: LiveData<Utilisateur?> = _compte

    val profilLocal: LiveData<Utilisateur?> = auth.profilLocal

    private val _etat = MutableLiveData(EtatAuth())
    val etat: LiveData<EtatAuth> = _etat

    fun inscrire(demande: InscriptionRequest) = effectuer(
        messageSucces = "Votre compte est cree. Vous pouvez vous connecter."
    ) {
        auth.inscrire(demande)
    }

    fun connecter(demande: ConnexionRequest) = effectuer {
        auth.memoriserIdentifiants(demande)
        val utilisateur = try {
            auth.connecter(demande)
        } catch (cause: IOException) {
            auth.connecterLocal(demande) ?: throw cause
        }
        _compte.postValue(utilisateur)
    }

    fun reconnecter(surSucces: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val utilisateur = auth.reconnecter()
                _compte.postValue(utilisateur)
                withContext(Dispatchers.Main) { surSucces() }
            } catch (cause: Exception) {
                publierEtat {
                    EtatAuth(erreur = true, message = cause.message ?: "Reconnexion impossible.")
                }
            }
        }
    }

    fun deconnecter() {
        viewModelScope.launch(Dispatchers.IO) {
            auth.deconnecter()
            _compte.postValue(null)
        }
    }

    fun viderMessage() {
        val etat = _etat.value ?: return
        _etat.value = etat.copy(message = "", toast = "")
    }

    private fun effectuer(messageSucces: String = "", action: suspend () -> Unit) {
        if (_etat.value?.chargement == true) return
        _etat.value = EtatAuth(chargement = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
                publierEtat {
                    if (messageSucces.isEmpty()) EtatAuth() else EtatAuth(message = messageSucces)
                }
            } catch (cause: Exception) {
                publierEtat {
                    EtatAuth(erreur = true, message = cause.message ?: "Action impossible.")
                }
            } finally {
                publierEtat { it.copy(chargement = false) }
            }
        }
    }

    private suspend fun publierEtat(bloc: (EtatAuth) -> EtatAuth) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatAuth()
            _etat.value = bloc(courant)
        }
    }
}
