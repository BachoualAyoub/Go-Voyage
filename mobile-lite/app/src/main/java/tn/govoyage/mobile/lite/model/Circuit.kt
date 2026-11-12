package tn.govoyage.mobile.lite.model

/** Circuit du catalogue (modele) ou cree par un voyageur (personnel). */
data class Circuit(
    val id: Long, val nom: String, val description: String?, val modele: Boolean,
    val budgetTnd: Int?, val dureeJours: Int, val estimationTnd: Int, val etapes: List<Etape>
)
