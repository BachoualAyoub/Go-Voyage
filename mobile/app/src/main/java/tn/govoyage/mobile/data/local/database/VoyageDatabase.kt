package tn.govoyage.mobile.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import tn.govoyage.mobile.data.local.dao.DestinationDAO
import tn.govoyage.mobile.data.local.dao.UtilisateurDAO
import tn.govoyage.mobile.data.local.entity.DestinationLocale
import tn.govoyage.mobile.data.local.entity.UtilisateurLocal

@Database(
    entities = [UtilisateurLocal::class, DestinationLocale::class],
    version = 15,
    exportSchema = false
)
abstract class VoyageDatabase : RoomDatabase() {

    abstract fun utilisateurDAO(): UtilisateurDAO
    abstract fun destinationDAO(): DestinationDAO

    companion object {
        @Volatile
        private var INSTANCE: VoyageDatabase? = null

        fun getDatabase(context: Context): VoyageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoyageDatabase::class.java,
                    "go-voyage.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
