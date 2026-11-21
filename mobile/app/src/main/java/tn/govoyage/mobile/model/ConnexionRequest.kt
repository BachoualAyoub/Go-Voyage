package tn.govoyage.mobile.model

/** Corps envoye au backend pour se connecter. */
data class ConnexionRequest(val email: String, val motDePasse: String)
