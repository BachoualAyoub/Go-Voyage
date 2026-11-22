package tn.govoyage.mobile.viewModel

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.repository.VoyageRepository
import tn.govoyage.mobile.model.*
import java.time.LocalDate
import java.util.UUID

/** Etat des sejours et du carnet, conserve pendant une rotation. */
class SejourViewModel(private val depot: VoyageRepository) : ViewModel() {
    val hotels = MutableLiveData<List<Hotel>>(emptyList())
    val reservations = MutableLiveData<List<Reservation>>(emptyList())
    val message = MutableLiveData("")
    val occupe = MutableLiveData(false)
    var arrivee = LocalDate.now().plusDays(1).toString()
    var depart = LocalDate.now().plusDays(3).toString()
    var voyageurs = 2
    var chambres = 1
    private var demande: ReservationRequest? = null
    /** Centralise les erreurs visibles et preserve l'annulation des coroutines. */
    fun executer(action: suspend () -> Unit) {
        if (occupe.value == true) return
        occupe.value = true
        message.value = ""
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: HttpException) { message.value = lireErreur(e) }
            catch (e: Exception) { message.value = e.message ?: "Serveur inaccessible." }
            finally { occupe.value = false }
        }
    }
    /** Relit la disponibilite ; les anciens resultats ne restent pas selectionnables. */
    fun chercher() = executer {
        hotels.value = emptyList()
        hotels.value = depot.hotels(arrivee, depart, voyageurs, chambres)
    }
    /** Garde la meme cle pour une nouvelle tentative de la meme confirmation. */
    fun reserver(hotel: Hotel) = executer {
        val ancienne = demande
        if (ancienne == null || ancienne.hotelId != hotel.id || ancienne.arrivee != arrivee ||
            ancienne.depart != depart || ancienne.voyageurs != voyageurs || ancienne.chambres != chambres)
            demande = ReservationRequest(hotel.id, arrivee, depart, voyageurs, chambres, UUID.randomUUID().toString(),hotel.total)
        val resultat = depot.reserver(demande!!)
        demande = null
        message.value = "Réservation GV-${resultat.id} confirmée · ${resultat.montant} TND."
        hotels.value = depot.hotels(arrivee, depart, voyageurs, chambres)
    }
    /** Affiche la copie locale uniquement en lecture et l'annonce au voyageur. */
    fun carnet() = executer {
        reservations.value = depot.reservations()
        if (depot.horsLigne) message.value = "Hors connexion · dernier carnet enregistré. Reconnectez-vous avant une annulation."
    }
    /** Retrouve le nouvel etat partage apres l'annulation. */
    fun annuler(id: Long) = executer {
        depot.annuler(id)
        reservations.value = depot.reservations()
        message.value = "Le séjour a été annulé."
    }
}
