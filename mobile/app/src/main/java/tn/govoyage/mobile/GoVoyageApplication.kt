package tn.govoyage.mobile

import android.app.Application
import androidx.room.Room
import tn.govoyage.mobile.data.AuthRepository
import tn.govoyage.mobile.data.BaseVoyage
import tn.govoyage.mobile.data.CircuitRepository
import tn.govoyage.mobile.data.DestinationRepository
import tn.govoyage.mobile.data.RetrofitInstance

/** Partage le client et la base privee durant la vie de l'application. */
class GoVoyageApplication : Application() {
    private val api = RetrofitInstance.api
    private val base by lazy { Room.databaseBuilder(this, BaseVoyage::class.java, "go-voyage.db").build() }
    val auth by lazy { AuthRepository(api, base) }
    val destinations by lazy { DestinationRepository(api, base) }
    val circuits by lazy { CircuitRepository(api, base) }
}
