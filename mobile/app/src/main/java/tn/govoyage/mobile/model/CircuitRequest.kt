package tn.govoyage.mobile.model

/** Formulaire de sauvegarde du circuit. */
data class CircuitRequest(
    val nom: String, val budgetTnd: Int?, val etapes: List<EtapeRequest>, val version: Long?
)
