package tn.govoyage.mobile.model

/** Reponse de connexion : seul le jeton est utilise, le compte est recharge via /moi. */
data class ConnexionResponse(val jeton: String)
