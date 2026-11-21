package tn.govoyage.mobile.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tn.govoyage.mobile.data.remote.api.AuthApi
import tn.govoyage.mobile.data.remote.api.CircuitApi
import tn.govoyage.mobile.data.remote.api.DestinationApi

/** Configuration Retrofit unique : URL, client HTTP, intercepteur et convertisseur. */
object RetrofitInstance {

    /** Ajoute le jeton en memoire aux appels proteges, sans le persister. */
    private val intercepteurJeton = Interceptor { chaine ->
        val requete = chaine.request()
        val jeton = JetonMemoire.lire()
        if (jeton == null || requete.header("Authorization") != null) {
            chaine.proceed(requete)
        } else {
            chaine.proceed(
                requete.newBuilder().header("Authorization", "Bearer $jeton").build()
            )
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(intercepteurJeton)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(adresseApi())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val destinations: DestinationApi by lazy { retrofit.create(DestinationApi::class.java) }
    val circuits: CircuitApi by lazy { retrofit.create(CircuitApi::class.java) }
}
