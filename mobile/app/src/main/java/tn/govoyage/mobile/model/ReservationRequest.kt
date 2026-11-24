package tn.govoyage.mobile.model

/** Demande de reservation : le serveur recalcule le montant et verifie la cle. */
data class ReservationRequest(
    val hotelId: Long,
    val arrivee: String,
    val depart: String,
    val voyageurs: Int,
    val chambres: Int,
    val cle: String,
    val montantAttendu: Long
)
