package tn.govoyage.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import tn.govoyage.mobile.data.local.entity.CacheSejour

/** Acces au cache local des recherches de sejour et des reservations. */
@Dao
interface CacheSejourDAO {

    /** Relit une recherche ou le carnet enregistres sur le telephone. */
    @Query("SELECT * FROM cache_voyage WHERE compte = :compte AND cle = :cle")
    suspend fun lire(compte: Long, cle: String): CacheSejour?

    /** Ecrit ou remplace un instantane local. */
    @Upsert
    suspend fun enregistrer(ligne: CacheSejour)

    /** Supprime toutes les copies d'un compte apres une reservation. */
    @Query("DELETE FROM cache_voyage WHERE compte = :compte")
    suspend fun effacerCompte(compte: Long)
}
