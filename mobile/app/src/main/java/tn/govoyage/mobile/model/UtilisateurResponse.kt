package tn.govoyage.mobile.model

/** Compte rendu par l'API, utilisable entre les ecrans. */
data class UtilisateurResponse(
    val id: Long,
    val nom: String,
    val prenom: String,
    val email: String,
    val tel: String,
    val role: String
)
