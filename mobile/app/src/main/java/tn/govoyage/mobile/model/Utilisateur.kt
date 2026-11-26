package tn.govoyage.mobile.model


data class Utilisateur(
    val id: Long,
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val role: String
)
