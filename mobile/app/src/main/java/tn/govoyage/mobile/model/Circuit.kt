package tn.govoyage.mobile.model

/** Circuit fourni par le catalogue ou cree par un voyageur. */
data class Circuit(
    val id: Long, val nom: String, val description: String?, val modele: Boolean,
    val budgetTnd: Int?, val dureeJours: Int, val estimationTnd: Int, val version: Long,
    val etapes: List<EtapeCircuit>
)
