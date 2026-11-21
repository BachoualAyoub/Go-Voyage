package tn.govoyage.mobile.model

/** Circuit personnel avec ses etapes, tel qu'affiche dans le carnet. */
data class Circuit(
    val id: Long,
    val nom: String,
    val dureeJours: Int,
    val estimationTnd: Int,
    val etapes: List<EtapeCircuit>,
    val version: Long = 0L
)
