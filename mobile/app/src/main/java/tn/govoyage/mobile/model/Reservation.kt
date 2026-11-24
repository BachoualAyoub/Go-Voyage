package tn.govoyage.mobile.model

import com.google.gson.annotations.SerializedName

/** Sejour reserve par le voyageur connecte. */
data class Reservation(
    @SerializedName("id_r") val idR: Long,
    @SerializedName("hotel_nom") val hotelNom: String,
    val ville: String,
    val arrivee: String,
    val depart: String,
    val voyageurs: Int,
    val chambres: Int,
    @SerializedName("tarif_nuit") val tarifNuit: Int = 0,
    val montant: Long = 0L,
    val statut: String = "CONFIRMEE",
    val image: String = ""
)
