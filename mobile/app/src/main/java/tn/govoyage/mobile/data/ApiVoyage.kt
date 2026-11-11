package tn.govoyage.mobile.data

import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.CircuitRequest
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.model.EtapeRequest
import tn.govoyage.mobile.model.UtilisateurResponse
import tn.govoyage.mobile.BuildConfig
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class InscriptionRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val motDePasse: String
)

data class ConnexionRequest(val email: String, val motDePasse: String)

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

    @GET("api/destinations")
    suspend fun destinations(@Header("Authorization") autorisation: String): List<Destination>

    @GET("api/circuits/modeles")
    suspend fun modeles(@Header("Authorization") autorisation: String): List<Circuit>

    @GET("api/circuits")
    suspend fun circuits(@Header("Authorization") autorisation: String): List<Circuit>

    @POST("api/circuits")
    suspend fun creer(@Header("Authorization") autorisation: String, @Body demande: CircuitRequest): Circuit

    @PUT("api/circuits/{id}")
    suspend fun modifier(
        @Header("Authorization") autorisation: String, @Path("id") id: Long, @Body demande: CircuitRequest
    ): Circuit

    @DELETE("api/circuits/{id}")
    suspend fun supprimer(@Header("Authorization") autorisation: String, @Path("id") id: Long): Response<Unit>
}

/** Construit le client Retrofit pour l'adresse definie par Gradle. */
fun creerApiVoyage(): ApiVoyage {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiVoyage::class.java)
}
