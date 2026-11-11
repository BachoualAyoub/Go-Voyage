package tn.govoyage.mobile.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.model.EtapeCircuit
import tn.govoyage.mobile.model.UtilisateurResponse

/** Session active, conservee dans le stockage prive de l'application. */
@Entity(tableName = "session")
data class SessionLocale(
    @PrimaryKey val cle: Int = 1,
    val jeton: String,
    @Embedded val compte: UtilisateurResponse
) {
    val autorisation: String get() = "Bearer $jeton"
}

/** Destination mise en cache apres une lecture distante. */
@Entity(tableName = "destination")
data class DestinationLocale(@PrimaryKey val id: Long, @Embedded(prefix = "info_") val donnee: Destination)

/** Entete d'un circuit. Le compte 0 designe les modeles du catalogue. */
@Entity(tableName = "circuit", indices = [Index("compteId")])
data class CircuitLocal(
    @PrimaryKey val id: Long, val compteId: Long, val nom: String, val description: String?,
    val modele: Boolean, val budgetTnd: Int?, val dureeJours: Int, val estimationTnd: Int, val version: Long
)

/** Etape mise en cache avec sa position. */
@Entity(
    tableName = "etape",
    primaryKeys = ["circuitId", "ordre"],
    foreignKeys = [ForeignKey(
        entity = CircuitLocal::class, parentColumns = ["id"], childColumns = ["circuitId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EtapeLocale(val circuitId: Long, val ordre: Int, @Embedded val donnee: EtapeCircuit)

/** Circuit local avec ses etapes. */
data class CircuitAvecEtapes(
    @Embedded val circuit: CircuitLocal,
    @Relation(parentColumn = "id", entityColumn = "circuitId") val etapes: List<EtapeLocale>
) {
    /** Reconstruit un circuit en respectant l'ordre des etapes. */
    fun versCircuit() = Circuit(
        circuit.id, circuit.nom, circuit.description, circuit.modele, circuit.budgetTnd,
        circuit.dureeJours, circuit.estimationTnd, circuit.version, etapes.sortedBy { it.ordre }.map { it.donnee }
    )
}

/** Acces au compte actif. */
@Dao
interface SessionDAO {
    @Query("SELECT * FROM session WHERE cle = 1")
    fun observer(): Flow<SessionLocale?>

    @Query("SELECT * FROM session WHERE cle = 1")
    suspend fun lire(): SessionLocale?

    @Upsert
    suspend fun enregistrer(session: SessionLocale)

    @Query("DELETE FROM session")
    suspend fun effacer()
}

/** Acces au cache de lecture des deux parcours voyage. */
@Dao
interface VoyageCacheDAO {
    @Query("SELECT * FROM destination ORDER BY info_nom")
    fun observerDestinations(): Flow<List<DestinationLocale>>

    @Transaction
    @Query("SELECT * FROM circuit WHERE compteId = :compteId ORDER BY id DESC")
    fun observerCircuits(compteId: Long): Flow<List<CircuitAvecEtapes>>

    @Query("DELETE FROM destination")
    suspend fun effacerDestinations()

    @Upsert
    suspend fun enregistrerDestinations(destinations: List<DestinationLocale>)

    @Query("DELETE FROM circuit WHERE compteId = :compteId")
    suspend fun effacerCircuits(compteId: Long)

    @Query("DELETE FROM circuit WHERE compteId = :compteId AND id = :id")
    suspend fun effacerCircuit(compteId: Long, id: Long)

    @Upsert
    suspend fun enregistrerCircuit(circuit: CircuitLocal)

    @Upsert
    suspend fun enregistrerEtapes(etapes: List<EtapeLocale>)
}

/** Stockage prive pour la session et les donnees recues. */
@Database(
    entities = [SessionLocale::class, DestinationLocale::class, CircuitLocal::class, EtapeLocale::class],
    version = 1, exportSchema = false
)
abstract class BaseVoyage : RoomDatabase() {
    abstract fun sessionDAO(): SessionDAO
    abstract fun voyageDAO(): VoyageCacheDAO
}
