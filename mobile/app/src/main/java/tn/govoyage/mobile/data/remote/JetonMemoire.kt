package tn.govoyage.mobile.data.remote


object JetonMemoire {

    @Volatile
    private var valeur: String? = null


    fun enregistrer(jeton: String) {
        valeur = jeton
    }


    fun lire(): String? = valeur


    fun estPresent(): Boolean = valeur != null


    fun effacer() {
        valeur = null
    }
}
