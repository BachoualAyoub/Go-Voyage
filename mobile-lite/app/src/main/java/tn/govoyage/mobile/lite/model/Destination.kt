package tn.govoyage.mobile.lite.model

/** Destination du catalogue et tarif indicatif par jour, en dinars. */
data class Destination(
    val id: Long, val code: String, val nom: String, val ville: String, val pays: String,
    val region: String, val ambiance: String, val tarifJourTnd: Int, val accroche: String,
    val description: String
)
