package tn.govoyage.mobile.model

/** Compte rendu par l'API, utilise directement dans les ecrans. */
data class Utilisateur(
    val id: Long,
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val role: String
)
