package tn.govoyage.mobile.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import tn.govoyage.mobile.data.local.entity.UtilisateurLocal

@Dao
interface UtilisateurDAO {

    @Query("SELECT * FROM utilisateur ORDER BY actif DESC, cle DESC")
    fun observer(): LiveData<List<UtilisateurLocal>>

    @Query("SELECT * FROM utilisateur ORDER BY actif DESC, cle DESC")
    suspend fun lireTous(): List<UtilisateurLocal>

    @Upsert
    suspend fun enregistrer(utilisateur: UtilisateurLocal)

    @Query("UPDATE utilisateur SET actif = CASE WHEN cle = :cle THEN 1 ELSE 0 END")
    suspend fun activer(cle: Int)
}
