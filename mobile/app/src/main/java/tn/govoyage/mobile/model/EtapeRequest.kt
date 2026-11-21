package tn.govoyage.mobile.model

/** Choix d'une destination et d'une duree envoye au backend. */
data class EtapeRequest(val destinationId: Long, val jours: Int)
