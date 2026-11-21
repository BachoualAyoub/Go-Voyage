package tn.govoyage.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tn.govoyage.mobile.data.local.entity.DestinationLocale

/** Acces local au catalogue de destinations. */
@Dao
interface DestinationDAO {

    /** Catalogue observe par les ecrans et l'editeur. */
    @Query("SELECT * FROM destination ORDER BY info_nom")
    fun observerDestinations(): Flow<List<DestinationLocale>>

    /** Nombre de destinations deja presentes sur le telephone. */
    @Query("SELECT COUNT(*) FROM destination")
    suspend fun nombre(): Int

    /** Remplace tout le catalogue en une seule transaction. */
    @Transaction
    suspend fun remplacerCatalogue(destinations: List<DestinationLocale>) {
        effacerDestinations()
        if (destinations.isNotEmpty()) enregistrerDestinations(destinations)
    }

    @Query("DELETE FROM destination")
    suspend fun effacerDestinations()

    @Upsert
    suspend fun enregistrerDestinations(destinations: List<DestinationLocale>)
}
