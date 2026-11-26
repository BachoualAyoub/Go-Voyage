package tn.govoyage.mobile.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import tn.govoyage.mobile.data.local.entity.DestinationLocale

@Dao
interface DestinationDAO {

    @Query("SELECT * FROM destination ORDER BY info_nom")
    fun observerDestinations(): LiveData<List<DestinationLocale>>

    @Query("SELECT COUNT(*) FROM destination")
    suspend fun nombre(): Int

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
