package tn.govoyage.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import tn.govoyage.mobile.data.local.dao.CircuitDAO
import tn.govoyage.mobile.data.local.entity.CircuitAvecEtapes
import tn.govoyage.mobile.data.local.entity.CircuitLocal
import tn.govoyage.mobile.data.local.entity.EtapeLocale
import tn.govoyage.mobile.data.remote.JetonMemoire
import tn.govoyage.mobile.data.remote.api.CircuitApi
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.CircuitRequest
import tn.govoyage.mobile.model.EtapeCircuit
import tn.govoyage.mobile.model.EtapeRequest

/** Intermediaire entre le DAO circuit, le backend et les ecrans du carnet local. */
class CircuitRepository(
    private val dao: CircuitDAO,
    private val api: CircuitApi,
    private val auth: AuthRepository
) {

    /** Identifiant du compte connecte, utilise pour separer les carnets. */
    private fun proprietaire(): Long {
        return auth.proprietaireCourant()
            ?: throw IllegalStateException("Connectez-vous avant d'ouvrir le carnet.")
    }

    /** Carnet du compte courant, du plus recent au plus ancien. */
    fun observer(): Flow<List<Circuit>> {
        val proprietaire = auth.proprietaireCourant() ?: return flowOf(emptyList())
        return dao.observerCircuits(proprietaire).map { lignes -> lignes.map { it.versCircuit() } }
    }

    /** Lit un circuit et ses etapes pour la fiche ou l'editeur. */
    suspend fun circuit(id: Long): Circuit? {
        return dao.circuit(id, proprietaire())?.versCircuit()
    }

    /** Cree ou modifie un circuit et remplace ses etapes en une transaction locale. */
    suspend fun sauver(id: Long?, nom: String, etapes: List<EtapeCircuit>): Long {
        val proprietaire = proprietaire()
        val existant = if (id == null) null else dao.circuit(id, proprietaire)
        val circuitId = existant?.circuit?.id ?: nouvelIdentifiant(proprietaire)
        val entete = CircuitLocal(
            id = circuitId,
            proprietaire = proprietaire,
            nom = nom.trim(),
            dureeJours = etapes.sumOf { it.jours },
            estimationTnd = etapes.sumOf { it.coutTnd },
            serveurId = existant?.circuit?.serveurId,
            versionServeur = existant?.circuit?.versionServeur ?: 0L,
            synchronise = false,
            supprime = false
        )
        val lignes = etapes.mapIndexed { ordre, etape -> EtapeLocale(circuitId, ordre, etape) }
        dao.remplacerCircuit(entete, lignes)
        return circuitId
    }

    /** Cache la suppression ; elle sera envoyee au backend lors de la synchronisation. */
    suspend fun supprimer(id: Long) {
        dao.marquerSupprime(id, proprietaire())
    }

    /** Envoie les changements locaux puis recupere les circuits du compte. */
    suspend fun synchroniser(): String {
        if (!JetonMemoire.estPresent()) {
            if (auth.identifiantsConnus()) {
                auth.reconnecter()
            } else {
                throw IllegalStateException("Connectez-vous avant de synchroniser.")
            }
        }
        val proprietaire = proprietaire()
        val suppressions = pousserSuppressions(proprietaire)
        val envoyes = pousserModifications(proprietaire)
        val recus = telechargerDepuisServeur(proprietaire)
        return "Sync terminee : $envoyes envoyes, $recus recus, $suppressions supprimés."
    }

    /** Envoie les suppressions en attente puis purge les lignes locales. */
    private suspend fun pousserSuppressions(proprietaire: Long): Int {
        val lignes = dao.suppressionsASynchroniser(proprietaire)
        for (ligne in lignes) {
            val serveurId = ligne.serveurId
            if (serveurId == null) {
                dao.effacerCircuit(ligne.id)
                continue
            }
            val reponse = api.supprimer(serveurId)
            if (!reponse.isSuccessful && reponse.code() != 404) throw HttpException(reponse)
            dao.effacerCircuit(ligne.id)
        }
        return lignes.size
    }

    /** Envoie les circuits crees ou modifies depuis la derniere synchronisation. */
    private suspend fun pousserModifications(proprietaire: Long): Int {
        val lignes = dao.circuitsASynchroniser(proprietaire)
        for (ligne in lignes) {
            val demande = construireDemande(ligne)
            val serveurId = ligne.circuit.serveurId
            val reponse = if (serveurId == null) api.creer(demande) else api.modifier(serveurId, demande)
            when {
                reponse.isSuccessful -> {
                    val distante = reponse.body()
                        ?: throw IllegalStateException("Reponse circuit vide.")
                    dao.marquerSynchronise(ligne.circuit.id, distante.id, distante.version ?: 0L)
                }
                reponse.code() == 409 && serveurId != null -> {
                    resoudreConflit(ligne, serveurId, proprietaire)
                }
                else -> throw HttpException(reponse)
            }
        }
        return lignes.size
    }

    /** Construit le corps attendu par POST ou PUT avec la version connue. */
    private fun construireDemande(ligne: CircuitAvecEtapes): CircuitRequest {
        val etapes = ligne.etapes.sortedBy { it.ordre }.map {
            EtapeRequest(it.donnee.destinationId, it.donnee.jours)
        }
        return CircuitRequest(
            nom = ligne.circuit.nom,
            budgetTnd = null,
            etapes = etapes,
            version = if (ligne.circuit.serveurId == null) null else ligne.circuit.versionServeur
        )
    }

    /** En cas de conflit, la version du serveur devient la reference locale. */
    private suspend fun resoudreConflit(ligne: CircuitAvecEtapes, serveurId: Long, proprietaire: Long) {
        val reponse = api.consulter(serveurId)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val distante = reponse.body() ?: throw IllegalStateException("Circuit serveur introuvable.")
        enregistrerDepuisServeur(ligne.circuit.id, distante, proprietaire)
    }

    /** Recupere les circuits du compte et supprime ceux qui ont disparu sur le serveur. */
    private suspend fun telechargerDepuisServeur(proprietaire: Long): Int {
        val reponse = api.lister()
        if (!reponse.isSuccessful) throw HttpException(reponse)
        val distants = reponse.body().orEmpty()
        for (distante in distants) {
            val local = dao.parServeurId(distante.id, proprietaire)
            if (local == null) {
                enregistrerDepuisServeur(nouvelIdentifiant(proprietaire), distante, proprietaire)
            } else if (local.circuit.synchronise) {
                enregistrerDepuisServeur(local.circuit.id, distante, proprietaire)
            }
        }
        supprimerCircuitsDisparus(distants.map { it.id }.toSet(), proprietaire)
        return distants.size
    }

    /** Retire du carnet les circuits deja synchronises qui ne sont plus sur le serveur. */
    private suspend fun supprimerCircuitsDisparus(idsDistants: Set<Long>, proprietaire: Long) {
        for (ligne in dao.circuitsSynchronises(proprietaire)) {
            val serveurId = ligne.circuit.serveurId ?: continue
            if (serveurId !in idsDistants) dao.effacerCircuit(ligne.circuit.id)
        }
    }

    /** Ecrit un circuit recu du backend dans le carnet du compte courant. */
    private suspend fun enregistrerDepuisServeur(id: Long, distante: Circuit, proprietaire: Long) {
        val entete = CircuitLocal(
            id = id,
            proprietaire = proprietaire,
            nom = distante.nom,
            dureeJours = distante.dureeJours,
            estimationTnd = distante.estimationTnd,
            serveurId = distante.id,
            versionServeur = distante.version,
            synchronise = true,
            supprime = false
        )
        val etapes = distante.etapes.mapIndexed { ordre, etape ->
            EtapeLocale(
                circuitId = id,
                ordre = ordre,
                donnee = EtapeCircuit(
                    destinationId = etape.destinationId,
                    nom = etape.nom,
                    ville = etape.ville,
                    ambiance = etape.ambiance,
                    jours = etape.jours,
                    tarifJourTnd = etape.tarifJourTnd,
                    coutTnd = etape.coutTnd
                )
            )
        }
        dao.remplacerCircuit(entete, etapes)
    }

    /** Prepare un identifiant local qui ne collisionne pas avec une ligne existante. */
    private suspend fun nouvelIdentifiant(proprietaire: Long): Long {
        var identifiant = System.currentTimeMillis()
        while (dao.circuit(identifiant, proprietaire) != null) identifiant++
        return identifiant
    }
}
