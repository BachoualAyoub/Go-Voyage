package tn.govoyage.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tn.govoyage.mobile.data.local.entity.CircuitAvecEtapes
import tn.govoyage.mobile.data.local.entity.CircuitLocal
import tn.govoyage.mobile.data.local.entity.EtapeLocale

/** Acces local aux circuits du carnet et a leurs etapes. */
@Dao
interface CircuitDAO {

    /** Carnet observe, du plus recent au plus ancien, sans circuits supprimes. */
    @Transaction
    @Query("SELECT * FROM circuit WHERE supprime = 0 ORDER BY id DESC")
    fun observerCircuits(): Flow<List<CircuitAvecEtapes>>

    /** Lit un circuit avec ses etapes pour la fiche ou l'editeur. */
    @Transaction
    @Query("SELECT * FROM circuit WHERE id = :id AND supprime = 0")
    suspend fun circuit(id: Long): CircuitAvecEtapes?

    /** Circuits locaux modifies et non encore envoyes au serveur. */
    @Transaction
    @Query("SELECT * FROM circuit WHERE supprime = 0 AND synchronise = 0")
    suspend fun circuitsASynchroniser(): List<CircuitAvecEtapes>

    /** Circuits supprimes localement et dont la suppression reste a envoyer. */
    @Query("SELECT * FROM circuit WHERE supprime = 1 AND synchronise = 0")
    suspend fun suppressionsASynchroniser(): List<CircuitLocal>

    /** Circuits deja lies au serveur et a jour, utiles pour gerer la liste distante. */
    @Transaction
    @Query("SELECT * FROM circuit WHERE supprime = 0 AND synchronise = 1 AND serveurId IS NOT NULL")
    suspend fun circuitsSynchronises(): List<CircuitAvecEtapes>

    /** Retrouve un circuit local a partir de son identifiant serveur. */
    @Transaction
    @Query("SELECT * FROM circuit WHERE serveurId = :serveurId AND supprime = 0 LIMIT 1")
    suspend fun parServeurId(serveurId: Long): CircuitAvecEtapes?

    /** Ecrit l'entete et remplace toutes les etapes en une seule transaction. */
    @Transaction
    suspend fun remplacerCircuit(entete: CircuitLocal, etapes: List<EtapeLocale>) {
        enregistrerCircuit(entete)
        effacerEtapes(entete.id)
        if (etapes.isNotEmpty()) enregistrerEtapes(etapes)
    }

    @Upsert
    suspend fun enregistrerCircuit(circuit: CircuitLocal)

    @Upsert
    suspend fun enregistrerEtapes(etapes: List<EtapeLocale>)

    @Query("DELETE FROM etape WHERE circuitId = :circuitId")
    suspend fun effacerEtapes(circuitId: Long)

    @Query("DELETE FROM circuit WHERE id = :id")
    suspend fun effacerCircuit(id: Long)

    /** Marque le circuit comme supprime localement en attendant la synchronisation. */
    @Query("UPDATE circuit SET supprime = 1, synchronise = 0 WHERE id = :id")
    suspend fun marquerSupprime(id: Long)

    /** Enregistre l'identifiant et la version renvoyes par le backend. */
    @Query(
        "UPDATE circuit SET serveurId = :serveurId, versionServeur = :versionServeur, " +
            "synchronise = 1 WHERE id = :id"
    )
    suspend fun marquerSynchronise(id: Long, serveurId: Long, versionServeur: Long)
}
