package tn.govoyage.mobile.ui

import tn.govoyage.mobile.R
import tn.govoyage.mobile.model.Circuit

/** Photo du web associee au code d'une destination. */
fun imageDestination(code: String): Int = when (code) {
    "djerba" -> R.drawable.destination_djerba
    "douz" -> R.drawable.destination_douz
    "el-jem" -> R.drawable.destination_el_jem
    "hammamet" -> R.drawable.destination_hammamet
    "kairouan" -> R.drawable.destination_kairouan
    "sidi-bou-said" -> R.drawable.destination_sidi_bou_said
    "tabarka" -> R.drawable.destination_tabarka
    "tozeur" -> R.drawable.destination_tozeur
    else -> R.drawable.voyage_sidi
}

// Les identifiants suivent le catalogue insere par Flyway.
private fun codeDestination(id: Long): String = when (id) {
    1L -> "sidi-bou-said"
    2L -> "hammamet"
    3L -> "djerba"
    4L -> "kairouan"
    5L -> "tozeur"
    6L -> "tabarka"
    7L -> "el-jem"
    8L -> "douz"
    else -> ""
}

/** Photo d'un circuit : celle de sa premiere etape. */
fun imageCircuit(circuit: Circuit): Int {
    val premiere = circuit.etapes.firstOrNull() ?: return R.drawable.voyage_sidi
    return imageDestination(codeDestination(premiere.destinationId))
}
