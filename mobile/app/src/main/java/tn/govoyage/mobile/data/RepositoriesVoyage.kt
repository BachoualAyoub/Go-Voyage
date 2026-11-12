package tn.govoyage.mobile.data

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import retrofit2.Response
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.CircuitRequest
import tn.govoyage.mobile.model.Destination

/** Verifie le code HTTP puis renvoie le corps attendu de la reponse. */
private fun <T> Response<T>.corps(): T {
    if (!isSuccessful) throw HttpException(this)
    return body() ?: throw HttpException(this)
}

/** Gere les appels d'authentification et la persistance de la session. */
class AuthRepository(private val api: ApiVoyage, private val base: BaseVoyage) {
    val session = base.sessionDAO().observer()

    /** Cree un compte sur le serveur. */
    suspend fun inscrire(demande: InscriptionRequest) {
        api.inscrire(demande).corps()
    }

    /** Enregistre la session apres verification du compte. */
    suspend fun connecter(demande: ConnexionRequest) {
        val connexion = api.connecter(demande).corps()
        val compte = api.moi("Bearer " + connexion.jeton).corps()
        base.sessionDAO().enregistrer(SessionLocale(jeton = connexion.jeton, compte = compte))
    }

    /** Actualise une session sans perdre l'acces au cache en cas de panne reseau. */
    suspend fun restaurer() {
        val session = base.sessionDAO().lire() ?: return
        try {
            val compte = api.moi(session.autorisation).corps()
            base.withTransaction {
                if (base.sessionDAO().lire()?.jeton == session.jeton) {
                    base.sessionDAO().enregistrer(session.copy(compte = compte))
                }
            }
        } catch (cause: HttpException) {
            if (cause.code() == 401 || cause.code() == 403) deconnecter(session.jeton)
        } catch (_: IOException) {
            // Le cache reste disponible hors connexion.
        } catch (_: JsonParseException) {
            // Une reponse invalide ne supprime pas la session.
        }
    }

    /** Efface uniquement la session et les circuits du compte actif. */
    suspend fun deconnecter(jeton: String) {
        base.withTransaction {
            val session = base.sessionDAO().lire()
            if (session != null && session.jeton == jeton) {
                base.voyageDAO().effacerCircuits(session.compte.id)
                base.sessionDAO().effacer()
            }
        }
    }
}

/** Lit et actualise le catalogue de destinations. */
class DestinationRepository(private val api: ApiVoyage, private val base: BaseVoyage) {
    private val dao = base.voyageDAO()
    val destinations = dao.observerDestinations().map { lignes -> lignes.map { it.donnee } }

    /** Remplace le catalogue en une seule transaction. */
    suspend fun actualiser(session: SessionLocale) {
        val destinations = api.destinations(session.autorisation).corps()
        base.withTransaction {
            dao.effacerDestinations()
            dao.enregistrerDestinations(destinations.map { DestinationLocale(it.id, it) })
        }
    }
}

/** Synchronise les modeles et les circuits personnels avec le cache. */
class CircuitRepository(private val api: ApiVoyage, private val base: BaseVoyage) {
    private val dao = base.voyageDAO()
    val modeles = observer(0)

    /** Observe les circuits du compte ou les modeles publics. */
    fun observer(compteId: Long) = dao.observerCircuits(compteId).map { lignes ->
        lignes.map { it.versCircuit() }
    }

    /** Recharge les modeles et les circuits du compte actif. */
    suspend fun actualiser(session: SessionLocale) {
        val modeles = api.modeles(session.autorisation).corps()
        val circuits = api.circuits(session.autorisation).corps()
        base.withTransaction {
            if (sessionActive(session)) {
                remplacer(0, modeles)
                remplacer(session.compte.id, circuits)
            }
        }
    }

    /** Cree ou modifie sur le serveur puis actualise la ligne locale. */
    suspend fun sauver(session: SessionLocale, id: Long?, demande: CircuitRequest) {
        val circuit = if (id == null) api.creer(session.autorisation, demande).corps()
        else api.modifier(session.autorisation, id, demande).corps()
        val compteId = session.compte.id
        base.withTransaction {
            if (sessionActive(session)) {
                dao.effacerCircuit(compteId, circuit.id)
                enregistrer(compteId, circuit)
            }
        }
    }

    /** Retire un circuit apres confirmation du serveur. */
    suspend fun supprimer(session: SessionLocale, id: Long) {
        val reponse = api.supprimer(session.autorisation, id)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        base.withTransaction {
            if (sessionActive(session)) dao.effacerCircuit(session.compte.id, id)
        }
    }

    /** Controle que la session n'a pas change pendant l'appel reseau. */
    private suspend fun sessionActive(session: SessionLocale) = base.sessionDAO().lire()?.jeton == session.jeton

    /** Remplace le lot d'un compte. Appel dans une transaction Room. */
    private suspend fun remplacer(compteId: Long, circuits: List<Circuit>) {
        dao.effacerCircuits(compteId)
        for (circuit in circuits) enregistrer(compteId, circuit)
    }

    /** Enregistre l'entete et les etapes dans leur ordre d'arrivee. */
    private suspend fun enregistrer(compteId: Long, circuit: Circuit) {
        dao.enregistrerCircuit(CircuitLocal(
            circuit.id, compteId, circuit.nom, circuit.description, circuit.modele, circuit.budgetTnd,
            circuit.dureeJours, circuit.estimationTnd, circuit.version
        ))
        dao.enregistrerEtapes(circuit.etapes.mapIndexed { ordre, etape -> EtapeLocale(circuit.id, ordre, etape) })
    }
}

/** Affiche un message lisible a partir de la reponse HTTP. */
fun lireErreur(cause: HttpException): String {
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
