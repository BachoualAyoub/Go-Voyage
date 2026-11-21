package tn.govoyage.mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.MainActivity
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.ActivityLoginBinding
import tn.govoyage.mobile.model.ConnexionRequest
import tn.govoyage.mobile.viewModel.AuthViewModel

/** Ecran de connexion au vrai backend, place au demarrage de l'application. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var espaceOuvert = false

    private val authViewModel: AuthViewModel by viewModels {
        viewModelFactory {
            initializer { AuthViewModel((application as GoVoyageApplication).auth) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editEmail.setText(intent.getStringExtra(EXTRA_EMAIL).orEmpty())
        observerProfilLocal()
        observerLeCompte()
        observerEtat()
        installerBoutons()
    }

    /** Pre-remplit l'email avec le profil local, sans jamais ouvrir de session. */
    private fun observerProfilLocal() {
        authViewModel.profilLocal.observe(this) { profil ->
            if (profil != null && binding.editEmail.text?.toString().isNullOrEmpty()) {
                binding.editEmail.setText(profil.email)
            }
        }
    }

    /** Ouvre l'espace voyage des qu'une connexion reussit. */
    private fun observerLeCompte() {
        authViewModel.compte.observe(this) { compte ->
            if (compte != null) ouvrirEspace()
        }
    }

    /** Affiche les erreurs une seule fois et suit l'etat des boutons. */
    private fun observerEtat() {
        authViewModel.etat.observe(this) { etat ->
            if (etat.message.isNotEmpty()) {
                Toast.makeText(this, etat.message, Toast.LENGTH_LONG).show()
                authViewModel.viderMessage()
            }
            binding.boutonConnexion.isEnabled = !etat.chargement
            binding.boutonVersInscription.isEnabled = !etat.chargement
        }
    }

    /** Branche les actions de connexion et d'inscription. */
    private fun installerBoutons() {
        binding.boutonVersInscription.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.boutonConnexion.setOnClickListener {
            connecter()
        }
    }

    /** Controle la saisie puis demande la connexion au ViewModel. */
    private fun connecter() {
        val email = binding.editEmail.text.toString().trim()
        val motDePasse = binding.editPassword.text.toString()
        if (email.isEmpty() || motDePasse.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_champs_vides), Toast.LENGTH_SHORT).show()
            return
        }
        authViewModel.connecter(ConnexionRequest(email, motDePasse))
    }

    /** Ouvre MainActivity une seule fois en vidant la pile des ecrans. */
    private fun ouvrirEspace() {
        if (espaceOuvert) return
        espaceOuvert = true
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_EMAIL = "EXTRA_EMAIL"
    }
}
