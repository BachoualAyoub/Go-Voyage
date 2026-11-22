package tn.govoyage.mobile.data.remote.api

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.http.*
import tn.govoyage.mobile.model.*

/** Services des sejours, du livre d'or et de l'administration partages. */
interface VoyageApi {
    @GET("api/hotels")
    suspend fun hotels(@Query("arrivee") arrivee: String, @Query("depart") depart: String,
        @Query("voyageurs") voyageurs: Int, @Query("chambres") chambres: Int): List<Hotel>
    @GET("api/reservations") suspend fun reservations(): List<Reservation>
    @POST("api/reservations") suspend fun reserver(@Body demande: ReservationRequest): Reservation
    @DELETE("api/reservations/{id}") suspend fun annuler(@Path("id") id: Long)
    @GET("api/vols") suspend fun vols(@Query("depart") depart: String,
        @Query("arrivee") arrivee: String, @Query("date") date: String): List<Vol>
    @GET("api/publications") suspend fun publications(@Query("recherche") recherche: String): List<Publication>
    @POST("api/publications") suspend fun publier(@Body demande: RecitRequest)
    @DELETE("api/publications/{id}") suspend fun supprimer(@Path("id") id: Long)
    @GET("api/publications/{id}/avis") suspend fun avis(@Path("id") id: Long): List<AvisVoyage>
    @PUT("api/publications/{id}/avis") suspend fun commenter(@Path("id") id: Long, @Body avis: JsonObject)
    @PUT("api/publications/{id}/like") suspend fun aimer(@Path("id") id: Long, @Body etat: JsonObject)
    @POST("api/publications/{id}/signalements") suspend fun signaler(@Path("id") id: Long, @Body motif: JsonObject)
    @Multipart @POST("api/medias") suspend fun importer(@Part fichier: MultipartBody.Part): Map<String,String>
    @GET("api/admin/statistiques") suspend fun statistiques(): JsonObject
    @GET("api/admin/hotels") suspend fun catalogue(): List<JsonObject>
    @POST("api/admin/hotels") suspend fun ajouterHotel(@Body hotel: JsonObject)
    @PUT("api/admin/hotels/{id}") suspend fun modifierHotel(@Path("id") id: Long, @Body hotel: JsonObject)
    @DELETE("api/admin/hotels/{id}") suspend fun archiverHotel(@Path("id") id: Long)
    @POST("api/admin/destinations") suspend fun ajouterDestination(@Body destination: JsonObject)
    @PUT("api/admin/destinations/{id}") suspend fun modifierDestination(@Path("id") id: Long, @Body destination: JsonObject)
    @DELETE("api/admin/destinations/{id}") suspend fun supprimerDestination(@Path("id") id: Long)
    @GET("api/admin/publications") suspend fun moderation(): List<JsonObject>
    @GET("api/admin/publications/{id}/signalements") suspend fun motifs(@Path("id") id: Long): List<JsonObject>
    @PUT("api/admin/publications/{id}") suspend fun moderer(@Path("id") id: Long, @Body etat: JsonObject)
}
