package tn.govoyage.mobile.model

import com.google.gson.annotations.SerializedName

/** Offre hoteliere issue du meme catalogue que le web. */
data class Hotel(
    @SerializedName("id_h") val id: Long, val nom: String, val ville: String,
    val description: String, val etoiles: Int, @SerializedName("tarif_nuit") val tarifNuit: Long,
    val chambres: Int, val capacite: Int, val image: String, val disponibles: Int,
    val reservable: Boolean, val total: Long
)
/** Reservation confirmee par le serveur, disponible dans le carnet local. */
data class Reservation(
    @SerializedName("id_r") val id: Long, @SerializedName("hotel_nom") val hotel: String,
    val ville: String, val arrivee: String, val depart: String, val voyageurs: Int,
    val chambres: Int, val montant: Long, val statut: String
)
/** Offre indicative pour une etape du comparateur. */
data class Vol(
    @SerializedName("id_v") val id: Long, val compagnie: String, val categorie: String,
    val depart: String, val arrivee: String, @SerializedName("heure_depart") val heure: String,
    @SerializedName("duree_minutes") val duree: Int, val prix: Long, val bagage: String
)
/** Recit et compteurs calcules par la plateforme. */
data class Publication(
    @SerializedName("id_p") val id: Long, @SerializedName("utilisateur_id") val auteurId: Long,
    val titre: String, val texte: String, val pays: String, val ville: String, val auteur: String,
    @SerializedName("media_url") val mediaUrl: String?, @SerializedName("media_type") val mediaType: String?,
    val likes: Int, val avis: Int, val note: Double, val aime: Int
)
/** Commentaire note d'un voyageur. */
data class AvisVoyage(val auteur: String, val note: Int, val texte: String)
/** Corps d'une reservation ; la cle survit aux tentatives pendant l'ecran. */
data class ReservationRequest(val hotelId: Long, val arrivee: String, val depart: String,
    val voyageurs: Int, val chambres: Int, val cle: String, val montantAttendu: Long)
/** Corps d'un recit partage. */
data class RecitRequest(val titre: String, val texte: String, val pays: String, val ville: String,
    val mediaUrl: String? = null, val mediaType: String? = null)
