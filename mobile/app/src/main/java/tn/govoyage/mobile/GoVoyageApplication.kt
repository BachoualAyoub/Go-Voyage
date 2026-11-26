package tn.govoyage.mobile

import android.app.Application
import tn.govoyage.mobile.data.local.database.VoyageDatabase
import tn.govoyage.mobile.data.repository.AuthRepository
import tn.govoyage.mobile.data.repository.CircuitRepository
import tn.govoyage.mobile.data.repository.DestinationRepository

class GoVoyageApplication : Application() {

    private val base by lazy { VoyageDatabase.getDatabase(this) }

    val auth by lazy { AuthRepository(base.utilisateurDAO()) }

    val destinations by lazy { DestinationRepository(base.destinationDAO()) }

    val circuits by lazy { CircuitRepository() }
}
