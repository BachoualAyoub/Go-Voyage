package tn.govoyage.mobile.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import tn.govoyage.mobile.model.Hotel
import tn.govoyage.mobile.model.Reservation
import tn.govoyage.mobile.model.ReservationRequest

/** Routes sejour deja utilisees par le web, reprises telles quelles sur mobile. */
interface SejourApi {

    /** Offres d'hotels pour une periode et un effectif. */
    @GET("api/hotels")
    suspend fun hotels(
        @Query("arrivee") arrivee: String,
        @Query("depart") depart: String,
        @Query("voyageurs") voyageurs: Int,
        @Query("chambres") chambres: Int
    ): Response<List<Hotel>>

    /** Carnet des reservations du compte connecte. */
    @GET("api/reservations")
    suspend fun reservations(): Response<List<Reservation>>

    /** Confirmation d'une reservation idempotente. */
    @POST("api/reservations")
    suspend fun reserver(@Body demande: ReservationRequest): Response<Reservation>

    /** Annulation d'une reservation future. */
    @DELETE("api/reservations/{id}")
    suspend fun annuler(@Path("id") id: Long): Response<Unit>
}
