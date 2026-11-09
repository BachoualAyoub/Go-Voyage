package tn.govoyage.mobile

/** Destination et tarif indicatif par jour, en dinars. */
data class Destination(
    val id: Long, val code: String, val nom: String, val ville: String, val pays: String,
    val region: String, val ambiance: String, val tarifJourTnd: Int, val accroche: String,
    val description: String
)

/** Etape ordonnee par sa position dans le circuit. */
data class EtapeCircuit(
    val destinationId: Long, val nom: String, val ville: String, val ambiance: String,
    val jours: Int, val tarifJourTnd: Int, val coutTnd: Int
)

/** Circuit fourni par le catalogue ou cree par un voyageur. */
data class Circuit(
    val id: Long, val nom: String, val description: String?, val modele: Boolean,
    val budgetTnd: Int?, val dureeJours: Int, val estimationTnd: Int, val version: Long,
    val etapes: List<EtapeCircuit>
)

/** Donnees saisies pour une etape. */
data class EtapeRequest(val destinationId: Long, val jours: Int)

/** Formulaire de sauvegarde du circuit. */
data class CircuitRequest(
    val nom: String, val budgetTnd: Int?, val etapes: List<EtapeRequest>, val version: Long?
)
