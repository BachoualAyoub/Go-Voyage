package tn.govoyage.mobile.viewModel

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.repository.VoyageRepository
import tn.govoyage.mobile.model.*

/** Gere les interactions partagees et le dernier fil conserve dans Room. */
class CommunauteViewModel(val depot: VoyageRepository) : ViewModel() {
    val recits = MutableLiveData<List<Publication>>(emptyList())
    val message = MutableLiveData("")
    val occupe = MutableLiveData(false)
    var recherche = ""
    /** Ecrit une seule action a la fois et presente toute erreur au voyageur. */
    fun executer(action: suspend () -> Unit) {
        if (occupe.value == true) return
        occupe.value = true; message.value = ""
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: HttpException) { message.value = lireErreur(e) }
            catch (e: Exception) { message.value = e.message ?: "Action impossible." }
            finally { occupe.value = false }
        }
    }
    /** Actualise le fil et annonce la lecture hors connexion. */
    fun charger() = executer { actualiser() }
    /** Relit le fil apres une interaction. */
    suspend fun actualiser() {
        recits.value = depot.publications(recherche)
        if (depot.horsLigne) message.value = "Hors connexion · derniers récits enregistrés. Les interactions nécessitent le serveur."
    }
    /** Publie un recit et recharge ses compteurs. */
    fun publier(demande: RecitRequest) = executer {
        depot.connecte { depot.api.publier(demande) }
        actualiser()
        message.value = "Votre récit est publié."
    }
}
