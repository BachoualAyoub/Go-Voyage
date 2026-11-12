package tn.govoyage.mobile.lite.model

/** Compte utilisateur fictif, garde en memoire durant la session. */
data class Utilisateur(
    val id: Long,
    val prenom: String,
    val nom: String,
    val email: String,
    val tel: String,
    val motDePasse: String,
    val role: String
)
