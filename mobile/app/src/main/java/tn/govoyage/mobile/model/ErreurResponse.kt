package tn.govoyage.mobile.model

/** Erreur renvoyee par le backend pour une validation ou une regle metier. */
data class ErreurResponse(val message: String?, val erreurs: Map<String, String>?)
