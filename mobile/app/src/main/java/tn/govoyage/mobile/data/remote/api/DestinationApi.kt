package tn.govoyage.mobile.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import tn.govoyage.mobile.model.Destination

/** Endpoints du catalogue, verifies dans DestinationController. */
interface DestinationApi {

    @GET("api/destinations")
    suspend fun lister(): Response<List<Destination>>

    @GET("api/destinations/{id}")
    suspend fun detail(@Path("id") id: Long): Response<Destination>
}
