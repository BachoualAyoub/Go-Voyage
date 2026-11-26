package tn.govoyage.mobile.data.remote

import android.os.Build
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val API_PORT = 8081
    private const val LOCAL_IP = "192.168.1.4"

    private fun adresseApi(): String {
        val empreinte = Build.FINGERPRINT ?: ""
        val modele = Build.MODEL ?: ""
        val emulateur = empreinte.contains("generic") || empreinte.contains("emulator") ||
            modele.contains("sdk") || modele.contains("Emulator")
        return if (emulateur) "http://10.0.2.2:$API_PORT/" else "http://$LOCAL_IP:$API_PORT/"
    }

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

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(intercepteurJeton)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(adresseApi())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val circuitApi: CircuitApi by lazy {
        retrofit.create(CircuitApi::class.java)
    }

    val destinationApi: DestinationApi by lazy {
        retrofit.create(DestinationApi::class.java)
    }
}
