package tn.govoyage.mobile.data.remote

/** Jeton conserve uniquement en memoire le temps de la vie du processus. */
object JetonMemoire {

    @Volatile
    private var valeur: String? = null

    /** Enregistre le jeton apres une connexion reussie. */
    fun enregistrer(jeton: String) {
        valeur = jeton
    }

    /** Jeton courant ou null quand personne n'est connecte. */
    fun lire(): String? = valeur

    /** Verifie qu'un jeton est disponible pour les appels proteges. */
    fun estPresent(): Boolean = valeur != null

    /** Supprime le jeton lors de la deconnexion. */
    fun effacer() {
        valeur = null
    }
}
