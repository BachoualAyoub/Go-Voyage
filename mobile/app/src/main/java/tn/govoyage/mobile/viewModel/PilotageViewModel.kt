package tn.govoyage.mobile.viewModel

import androidx.lifecycle.*
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.remote.RetrofitInstance
import tn.govoyage.mobile.data.repository.VoyageRepository
import tn.govoyage.mobile.model.Destination

/** Etat administratif natif, controle par le role sur chaque appel serveur. */
class PilotageViewModel(val depot: VoyageRepository) : ViewModel() {
    val statistiques=MutableLiveData<JsonObject>()
    val hotels=MutableLiveData<List<JsonObject>>(emptyList())
    val destinations=MutableLiveData<List<Destination>>(emptyList())
    val publications=MutableLiveData<List<JsonObject>>(emptyList())
    val message=MutableLiveData("")
    val occupe=MutableLiveData(false)
    var onglet="synthese"
    /** Execute une action serveur puis recharge les donnees partagees. */
    fun executer(action: suspend () -> Unit) {
        if (occupe.value==true) return
        occupe.value=true; message.value=""
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: HttpException) { message.value=lireErreur(e) }
            catch (e: Exception) { message.value=e.message ?: "Action impossible." }
            finally { occupe.value=false }
        }
    }
    /** Charge les indicateurs et les catalogues dans une session verifiee. */
    fun charger() = executer { actualiser() }
    /** Relit les collections apres une ecriture. */
    suspend fun actualiser() {
        depot.connecte {
            hotels.value=depot.api.catalogue()
            val reponse=RetrofitInstance.destinations.lister()
            if (!reponse.isSuccessful) throw HttpException(reponse)
            destinations.value=reponse.body().orEmpty()
            publications.value=depot.api.moderation()
            statistiques.value=depot.api.statistiques()
        }
    }
}
