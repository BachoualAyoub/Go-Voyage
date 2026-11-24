package tn.govoyage.mobile.model

import com.google.gson.annotations.SerializedName

/** Hotel du catalogue avec la disponibilite calculee pour une recherche. */
data class Hotel(
    @SerializedName("id_h") val idH: Long,
    @SerializedName("destination_id") val destinationId: Long,
    val nom: String,
    val ville: String,
    val description: String,
    val etoiles: Int,
    @SerializedName("tarif_nuit") val tarifNuit: Int,
    val chambres: Int,
    val capacite: Int,
    val image: String,
    val actif: Boolean,
    val version: Long = 0L,
    val disponibles: Int = 0,
    val reservable: Boolean = false,
    val total: Long = 0L
)
