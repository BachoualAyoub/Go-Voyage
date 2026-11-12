package tn.govoyage.mobile.lite.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tn.govoyage.mobile.lite.model.Utilisateur

/** Utilisateurs et session courante, tout en memoire, comme le TP5. */
class AuthViewModel : ViewModel() {

    // Liste fictive de comptes, prete a l'emploi pour la demonstration.
    private val utilisateurs = mutableListOf(
        Utilisateur(1, "Ayoub", "Administrateur", "admin@govoyage.tn", "20123456", "admin123", "ADMIN"),
        Utilisateur(2, "Sarra", "Ben Salah", "sarra@govoyage.tn", "20888877", "sarra123", "USER")
    )

    // La session courante : l'utilisateur connecte, ou null.
    private val _utilisateurConnecte = MutableLiveData<Utilisateur?>()
    val utilisateurConnecte: LiveData<Utilisateur?> = _utilisateurConnecte

    /** Verifie les identifiants et ouvre la session. Renvoie faux en cas d'echec. */
    fun connecter(email: String, motDePasse: String): Boolean {
        val compte = utilisateurs.firstOrNull {
            it.email.equals(email, ignoreCase = true) && it.motDePasse == motDePasse
        }
        if (compte == null) return false
        _utilisateurConnecte.value = compte
        return true
    }

    /** Ajoute un compte a la liste. Renvoie faux si l'email est deja utilise. */
    fun inscrire(prenom: String, nom: String, email: String, tel: String, motDePasse: String): Boolean {
        if (utilisateurs.any { it.email.equals(email, ignoreCase = true) }) return false
        val idSuivant = (utilisateurs.maxOfOrNull { it.id } ?: 0) + 1
        utilisateurs.add(Utilisateur(idSuivant, prenom, nom, email, tel, motDePasse, "USER"))
        return true
    }

    /** Ferme la session : l'utilisateur redevient null. */
    fun deconnecter() {
        _utilisateurConnecte.value = null
    }
}
