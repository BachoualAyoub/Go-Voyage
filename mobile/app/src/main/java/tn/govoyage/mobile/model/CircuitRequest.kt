package tn.govoyage.mobile.model


data class CircuitRequest(
    val nom: String,
    val budgetTnd: Int?,
    val etapes: List<EtapeRequest>,
    val version: Long?
)
