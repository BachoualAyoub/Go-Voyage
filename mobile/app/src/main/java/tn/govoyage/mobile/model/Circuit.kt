package tn.govoyage.mobile.model


data class Circuit(
    val id: Long,
    val nom: String,
    val dureeJours: Int,
    val estimationTnd: Int,
    val etapes: List<EtapeCircuit>,
    val version: Long = 0L
)
