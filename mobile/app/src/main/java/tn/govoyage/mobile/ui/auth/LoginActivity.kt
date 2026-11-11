package tn.govoyage.mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.data.ConnexionRequest
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.MainActivity
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.ActivityLoginBinding
import tn.govoyage.mobile.viewModel.AuthViewModel

/** Ecran de connexion, style TP3, branche sur le vrai backend. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels {
        viewModelFactory {
            initializer { AuthViewModel((application as GoVoyageApplication).auth) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Email propose apres une inscription reussie.
        binding.editEmail.setText(intent.getStringExtra("EXTRA_EMAIL").orEmpty())

        // Session deja ouverte : va directement a l'espace de voyage.
        authViewModel.session.observe(this) { session ->
            if (session != null) ouvrirEspace()
        }

        // Erreurs affichees une seule fois.
        authViewModel.etat.observe(this) { etat ->
            if (etat.message.isNotEmpty()) {
                Toast.makeText(this, etat.message, Toast.LENGTH_LONG).show()
                authViewModel.viderMessage()
            }
            binding.boutonConnexion.isEnabled = !etat.chargement
            binding.boutonVersInscription.isEnabled = !etat.chargement
        }

        binding.boutonVersInscription.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.boutonConnexion.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()
            val motDePasse = binding.editPassword.text.toString()
            if (email.isEmpty() || motDePasse.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_champs_vides), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.connecter(ConnexionRequest(email, motDePasse))
        }
    }

    /** Ouvre l'espace connecte et ferme le formulaire. */
    private fun ouvrirEspace() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
