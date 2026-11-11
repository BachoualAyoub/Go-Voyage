package tn.govoyage.mobile.model

/** Etape ordonnee par sa position dans le circuit. */
data class EtapeCircuit(
    val destinationId: Long, val nom: String, val ville: String, val ambiance: String,
    val jours: Int, val tarifJourTnd: Int, val coutTnd: Int
)
