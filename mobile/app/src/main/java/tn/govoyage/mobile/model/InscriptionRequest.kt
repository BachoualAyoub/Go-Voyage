package tn.govoyage.mobile.model

/** Corps envoye au backend pour creer un compte. */
data class InscriptionRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val motDePasse: String
)
