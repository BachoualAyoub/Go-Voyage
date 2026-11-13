package tn.govoyage.mobile.data

import android.os.Build
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.CircuitRequest
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.model.UtilisateurResponse

/** Port du backend GO VOYAGE lance sur le PC. */
const val API_PORT = 8081

// IP du PC sur le Wi-Fi : commande ipconfig. A mettre a jour si elle change.
private const val LOCAL_IP = "192.168.1.4"

// L'emulateur joint le PC par 10.0.2.2 ; un telephone passe par le reseau local.
private fun adresseApi(): String {
    val empreinte = Build.FINGERPRINT ?: ""
    val modele = Build.MODEL ?: ""
    val emulateur = empreinte.contains("generic") || empreinte.contains("emulator") ||
        modele.contains("sdk") || modele.contains("Emulator")
    return if (emulateur) "http://10.0.2.2:$API_PORT/" else "http://$LOCAL_IP:$API_PORT/"
}

/** Corps envoye au backend pour creer un compte. */
data class InscriptionRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val motDePasse: String
)

/** Corps envoye au backend pour se connecter. */
data class ConnexionRequest(val email: String, val motDePasse: String)

/** Reponse du backend apres une connexion reussie. */
data class ConnexionResponse(val jeton: String, val utilisateur: UtilisateurResponse)

/** Erreur renvoyee par le backend (validation ou metier). */
data class ErreurResponse(val message: String?, val erreurs: Map<String, String>?)

/** Contrat REST de l'API GO VOYAGE : une methode par route. */
interface ApiVoyage {
    @POST("api/auth/inscription")
    suspend fun inscrire(@Body demande: InscriptionRequest): Response<UtilisateurResponse>

    @POST("api/auth/connexion")
    suspend fun connecter(@Body demande: ConnexionRequest): Response<ConnexionResponse>

    @GET("api/auth/moi")
    suspend fun moi(@Header("Authorization") autorisation: String): Response<UtilisateurResponse>

    @GET("api/destinations")
    suspend fun destinations(@Header("Authorization") autorisation: String): Response<List<Destination>>

    @GET("api/circuits/modeles")
    suspend fun modeles(@Header("Authorization") autorisation: String): Response<List<Circuit>>

    @GET("api/circuits")
    suspend fun circuits(@Header("Authorization") autorisation: String): Response<List<Circuit>>

    @POST("api/circuits")
    suspend fun creer(
        @Header("Authorization") autorisation: String,
        @Body demande: CircuitRequest
    ): Response<Circuit>

    @PUT("api/circuits/{id}")
    suspend fun modifier(
        @Header("Authorization") autorisation: String,
        @Path("id") id: Long,
        @Body demande: CircuitRequest
    ): Response<Circuit>

    @DELETE("api/circuits/{id}")
    suspend fun supprimer(@Header("Authorization") autorisation: String, @Path("id") id: Long): Response<Unit>
}

/** Client Retrofit unique de l'application (construit une seule fois, en lazy). */
object RetrofitInstance {

    /** Journal des appels reseau : utile pour le debogage dans le Logcat. */
    private val journal = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }

    /** Client HTTP avec les delais d'attente du projet. */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(journal)
            .build()
    }

    /** Moteur Retrofit : URL de base, client HTTP et convertisseur JSON. */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(adresseApi())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Service pret a appeler les routes de l'API. */
    val api: ApiVoyage by lazy { retrofit.create(ApiVoyage::class.java) }
}
