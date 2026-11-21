package tn.govoyage.mobile

import android.app.Application
import androidx.room.Room
import tn.govoyage.mobile.data.local.database.MIGRATION_1_3
import tn.govoyage.mobile.data.local.database.MIGRATION_2_3
import tn.govoyage.mobile.data.local.database.MIGRATION_3_4
import tn.govoyage.mobile.data.local.database.MIGRATION_4_5
import tn.govoyage.mobile.data.local.database.MIGRATION_5_6
import tn.govoyage.mobile.data.local.database.MIGRATION_6_7
import tn.govoyage.mobile.data.local.database.VoyageDatabase
import tn.govoyage.mobile.data.remote.RetrofitInstance
import tn.govoyage.mobile.data.repository.AuthRepository
import tn.govoyage.mobile.data.repository.CircuitRepository
import tn.govoyage.mobile.data.repository.DestinationRepository

/** Partage la base locale et les repositories pendant la vie de l'application. */
class GoVoyageApplication : Application() {

    private val base by lazy {
        Room.databaseBuilder(this, VoyageDatabase::class.java, "go-voyage.db")
            .addMigrations(
                MIGRATION_1_3, MIGRATION_2_3, MIGRATION_3_4,
                MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
            )
            .build()
    }

    val auth by lazy { AuthRepository(RetrofitInstance.auth, base.utilisateurDAO()) }

    val circuits by lazy { CircuitRepository(base.circuitDAO(), RetrofitInstance.circuits) }

    val destinations by lazy { DestinationRepository(RetrofitInstance.destinations, base.destinationDAO()) }
}
