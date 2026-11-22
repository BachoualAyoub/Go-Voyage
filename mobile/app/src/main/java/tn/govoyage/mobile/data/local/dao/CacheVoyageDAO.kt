package tn.govoyage.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import tn.govoyage.mobile.data.local.entity.CacheVoyage

/** Lecture et renouvellement explicite des instantanes des nouveaux parcours. */
@Dao
interface CacheVoyageDAO {
    @Query("SELECT * FROM cache_voyage WHERE compte=:compte AND cle=:cle")
    suspend fun lire(compte: Long, cle: String): CacheVoyage?
    @Upsert suspend fun enregistrer(cache: CacheVoyage)
}
