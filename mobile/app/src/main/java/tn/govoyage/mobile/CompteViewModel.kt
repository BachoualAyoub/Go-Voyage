package tn.govoyage.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Etat de l'inscription et de la session mobile. */
class CompteViewModel : ViewModel() {
    private val api = creerApiVoyage()
    var compte by mutableStateOf<UtilisateurResponse?>(null)
        private set
    var inscription by mutableStateOf(false)
        private set
    var emailPrefere by mutableStateOf("")
        private set
    var chargement by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var erreur by mutableStateOf(false)
        private set

    /** Bascule entre les deux formulaires. */
    fun changerMode(nouveauMode: Boolean) {
        inscription = nouveauMode
        message = ""
        erreur = false
    }

    /** Cree le compte, puis ramene au formulaire de connexion. */
    fun inscrire(demande: InscriptionRequest) {
        effectuer {
            api.inscrire(demande)
            emailPrefere = demande.email
            inscription = false
            message = "Votre compte est créé. Vous pouvez vous connecter."
        }
    }

    /** Ouvre la session et verifie le compte via le jeton. */
    fun connecter(demande: ConnexionRequest) {
        effectuer {
            val session = api.connecter(demande)
            compte = api.moi("Bearer " + session.jeton)
        }
    }

    /** Ferme la session mobile. */
    fun deconnecter() {
        compte = null
        emailPrefere = ""
        changerMode(false)
    }

    /** Lance une requete a la fois et affiche le resultat. */
    private fun effectuer(requete: suspend () -> Unit) {
        if (chargement) return
        chargement = true
        erreur = false
        message = ""
        viewModelScope.launch {
            try {
                requete()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: HttpException) {
                erreur = true
                message = lireErreur(cause)
            } catch (cause: IOException) {
                erreur = true
                message = "API injoignable. Vérifiez que le backend GO VOYAGE est démarré."
            } catch (cause: JsonParseException) {
                erreur = true
                message = "La réponse de l’API est illisible."
            } finally {
                chargement = false
            }
        }
    }

    /** Lit les messages d'erreur du backend. */
    private fun lireErreur(cause: HttpException): String {
        val defaut = "La requête a échoué (${cause.code()})."
        return try {
            val json = cause.response()?.errorBody()?.string()
            val retour = Gson().fromJson(json, ErreurResponse::class.java)
            val details = retour?.erreurs?.values?.joinToString("\n").orEmpty()
            listOf(retour?.message ?: defaut, details).filter { it.isNotEmpty() }.joinToString("\n")
        } catch (_: JsonParseException) {
            defaut
        } catch (_: IOException) {
            defaut
        }
    }
}
