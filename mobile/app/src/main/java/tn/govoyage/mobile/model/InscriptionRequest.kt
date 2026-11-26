package tn.govoyage.mobile.model


data class InscriptionRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val motDePasse: String
)
