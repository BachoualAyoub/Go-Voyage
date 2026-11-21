package tn.govoyage.mobile.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import tn.govoyage.mobile.model.ConnexionRequest
import tn.govoyage.mobile.model.ConnexionResponse
import tn.govoyage.mobile.model.InscriptionRequest
import tn.govoyage.mobile.model.Utilisateur

/** Endpoints d'authentification verifies dans AuthentificationController. */
interface AuthApi {

    @POST("api/auth/inscription")
    suspend fun inscrire(@Body demande: InscriptionRequest): Response<Utilisateur>

    @POST("api/auth/connexion")
    suspend fun connecter(@Body demande: ConnexionRequest): Response<ConnexionResponse>

    @GET("api/auth/moi")
    suspend fun moi(): Response<Utilisateur>
}
