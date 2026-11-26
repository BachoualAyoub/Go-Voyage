package tn.govoyage.mobile.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.CircuitRequest

interface CircuitApi {

    @GET("api/circuits")
    suspend fun lister(): Response<List<Circuit>>

    @GET("api/circuits/{id}")
    suspend fun detail(@Path("id") id: Long): Response<Circuit>

    @POST("api/circuits")
    suspend fun creer(@Body demande: CircuitRequest): Response<Circuit>

    @PUT("api/circuits/{id}")
    suspend fun modifier(@Path("id") id: Long, @Body demande: CircuitRequest): Response<Circuit>

    @DELETE("api/circuits/{id}")
    suspend fun supprimer(@Path("id") id: Long): Response<Unit>
}
