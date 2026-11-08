package tn.govoyage.mobile

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class InscriptionRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val motDePasse: String
)

data class ConnexionRequest(val email: String, val motDePasse: String)

data class UtilisateurResponse(
    val id: Long,
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val role: String
)

data class ConnexionResponse(val jeton: String, val utilisateur: UtilisateurResponse)
data class ErreurResponse(val message: String?, val erreurs: Map<String, String>?)

/** Requetes d'inscription et de connexion vers le backend. */
interface ApiVoyage {
    @POST("api/auth/inscription")
    suspend fun inscrire(@Body demande: InscriptionRequest): UtilisateurResponse

    @POST("api/auth/connexion")
    suspend fun connecter(@Body demande: ConnexionRequest): ConnexionResponse

    @GET("api/auth/moi")
    suspend fun moi(@Header("Authorization") autorisation: String): UtilisateurResponse
}

/** Construit le client Retrofit pour l'adresse definie par Gradle. */
fun creerApiVoyage(): ApiVoyage {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiVoyage::class.java)
}
