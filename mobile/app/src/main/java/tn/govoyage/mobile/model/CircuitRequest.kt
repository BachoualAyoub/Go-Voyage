package tn.govoyage.mobile.model

/** Formulaire de sauvegarde d'un circuit cote backend. */
data class CircuitRequest(
    val nom: String,
    val budgetTnd: Int?,
    val etapes: List<EtapeRequest>,
    val version: Long?
)
