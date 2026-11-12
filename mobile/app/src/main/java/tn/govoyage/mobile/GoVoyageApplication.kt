package tn.govoyage.mobile

import android.app.Application
import android.content.pm.PackageManager
import androidx.room.Room
import kotlinx.coroutines.runBlocking
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

    override fun onCreate() {
        super.onCreate()
        // Chaque nouvelle installation de l'APK repart sans session.
        reinitialiserApresMiseAJour()
    }

    /** Efface la session quand l'application a ete reinstallee. */
    @Suppress("DEPRECATION")
    private fun reinitialiserApresMiseAJour() {
        val preferences = getSharedPreferences("installation", MODE_PRIVATE)
        val miseAJour = try {
            packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        } catch (_: PackageManager.NameNotFoundException) {
            return
        }
        if (preferences.getLong("derniereMiseAJour", 0L) == miseAJour) return
        // On attend la suppression avant d'afficher le premier ecran.
        runBlocking { base.sessionDAO().effacer() }
        preferences.edit().putLong("derniereMiseAJour", miseAJour).apply()
    }
}
