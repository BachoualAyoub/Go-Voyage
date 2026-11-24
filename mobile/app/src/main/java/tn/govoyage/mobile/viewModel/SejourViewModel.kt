package tn.govoyage.mobile.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.adresseApi
import tn.govoyage.mobile.data.remote.lireErreur
import tn.govoyage.mobile.data.repository.SejourRepository
import tn.govoyage.mobile.model.Hotel
import tn.govoyage.mobile.model.Reservation
import tn.govoyage.mobile.model.ReservationRequest

/** Etat des ecrans de sejour : chargement, messages et lecture locale. */
data class EtatSejour(
    val chargement: Boolean = false,
    val erreur: String = "",
    val information: String = "",
    val horsLigne: Boolean = false
)

/** VueModele de la recherche d'hotels et du carnet de reservations. */
class SejourViewModel(private val sejours: SejourRepository) : ViewModel() {

    private val _hotels = MutableLiveData<List<Hotel>>(emptyList())
    val hotels: LiveData<List<Hotel>> = _hotels

    private val _reservations = MutableLiveData<List<Reservation>>(emptyList())
    val reservations: LiveData<List<Reservation>> = _reservations

    private val _etat = MutableLiveData(EtatSejour())
    val etat: LiveData<EtatSejour> = _etat

    private var cleReservation = ""

    /** Cherche des hotels, avec retour au cache local si le serveur est injoignable. */
    fun rechercher(arrivee: String, depart: String, voyageurs: Int, chambres: Int) = executer {
        verifierSaisie(arrivee, depart, voyageurs, chambres)
        val resultat = sejours.hotels(arrivee, depart, voyageurs, chambres)
        _hotels.postValue(resultat.liste)
        majEtat {
            it.copy(
                horsLigne = resultat.horsLigne,
                information = if (resultat.horsLigne) {
                    "Offres enregistrees sur le telephone. Actualisez-les des que possible."
                } else {
                    ""
                }
            )
        }
    }

    /** Charge les reservations du compte, en lecture seule hors ligne. */
    fun chargerReservations() = executer {
        val resultat = sejours.reservations()
        _reservations.postValue(resultat.liste)
        majEtat { it.copy(horsLigne = resultat.horsLigne) }
    }

    /** Reserve un hotel apres verification du montant affiche. */
    fun reserver(
        hotel: Hotel,
        arrivee: String,
        depart: String,
        voyageurs: Int,
        chambres: Int,
        terminer: () -> Unit
    ) = executer {
        verifierSaisie(arrivee, depart, voyageurs, chambres)
        if (cleReservation.isBlank()) cleReservation = UUID.randomUUID().toString()
        val demande = ReservationRequest(
            hotelId = hotel.idH,
            arrivee = arrivee,
            depart = depart,
            voyageurs = voyageurs,
            chambres = chambres,
            cle = cleReservation,
            montantAttendu = hotel.total
        )
        val reservation = sejours.reserver(demande)
        cleReservation = ""
        majEtat { it.copy(information = "Reservation ${reservation.idR} confirmee.") }
        withContext(Dispatchers.Main) { terminer() }
    }

    /** Annule une reservation, uniquement avec le serveur. */
    fun annuler(id: Long) = executer {
        sejours.annuler(id)
        majEtat { it.copy(information = "Reservation annulee.") }
        try {
            val resultat = sejours.reservations()
            _reservations.postValue(resultat.liste)
            majEtat { it.copy(horsLigne = resultat.horsLigne) }
        } catch (_: IOException) {
            // L'annulation est confirmee ; l'utilisateur pourra actualiser plus tard.
        } catch (_: HttpException) {
            // Le carnet reste affiche tel quel jusqu'a un nouvel actualiser.
        }
    }

    /** Remet a zero les messages apres affichage, sans toucher au chargement. */
    fun effacerMessage() {
        val etat = _etat.value ?: return
        _etat.value = etat.copy(erreur = "", information = "")
    }

    /** Controle les dates et les effectifs avant tout appel. */
    private fun verifierSaisie(arrivee: String, depart: String, voyageurs: Int, chambres: Int) {
        val debut = lireDate(arrivee, "arrivee")
        val fin = lireDate(depart, "depart")
        val nuits = ChronoUnit.DAYS.between(debut, fin)
        if (nuits < 1 || nuits > 60 || debut.isBefore(LocalDate.now()) ||
            debut.isAfter(LocalDate.now().plusYears(1))
        ) {
            throw IllegalArgumentException("Choisissez un sejour de 1 a 60 nuits dans les douze prochains mois.")
        }
        if (voyageurs !in 1..30 || chambres !in 1..10) {
            throw IllegalArgumentException("Effectifs invalides.")
        }
    }

    /** Transforme une date AAAA-MM-JJ, ou explique la saisie incorrecte. */
    private fun lireDate(texte: String, nom: String): LocalDate {
        return try {
            LocalDate.parse(texte.trim())
        } catch (_: Exception) {
            throw IllegalArgumentException("Date de $nom invalide : utilisez AAAA-MM-JJ.")
        }
    }

    /** Execute une action sejour et affiche les erreurs. */
    private fun executer(action: suspend () -> Unit) {
        val etat = _etat.value ?: EtatSejour()
        if (etat.chargement) return
        _etat.value = etat.copy(chargement = true, erreur = "", information = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: HttpException) {
                majEtat { it.copy(erreur = lireErreur(cause)) }
            } catch (_: IOException) {
                majEtat {
                    it.copy(erreur = "Connexion indisponible. Verifiez le serveur (${adresseApi()}).")
                }
            } catch (_: JsonParseException) {
                majEtat { it.copy(erreur = "La reponse de l'API est illisible.") }
            } catch (cause: IllegalStateException) {
                majEtat { it.copy(erreur = cause.message ?: "Donnee introuvable.") }
            } catch (cause: IllegalArgumentException) {
                majEtat { it.copy(erreur = cause.message ?: "Saisie incorrecte.") }
            } finally {
                majEtat { it.copy(chargement = false) }
            }
        }
    }

    /** Publie l'etat depuis le thread principal. */
    private suspend fun majEtat(bloc: (EtatSejour) -> EtatSejour) {
        withContext(Dispatchers.Main) {
            val courant = _etat.value ?: EtatSejour()
            _etat.value = bloc(courant)
        }
    }
}
