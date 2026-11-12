package tn.govoyage.mobile.lite.model

/** Etape d'un circuit, avec son cout calcule a partir du tarif journalier. */
data class Etape(
    val destinationId: Long, val nom: String, val jours: Int, val tarifJourTnd: Int, val coutTnd: Int
)

/** Etape saisie dans l'editeur, avant le calcul du cout. */
data class SaisieEtape(val destinationId: Long, val jours: Int)
