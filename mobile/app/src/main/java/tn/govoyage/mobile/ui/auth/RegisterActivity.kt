package tn.govoyage.mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.ActivityRegisterBinding
import tn.govoyage.mobile.model.InscriptionRequest
import tn.govoyage.mobile.viewModel.AuthViewModel

/** Ecran de creation de compte, branche sur le vrai backend. */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private val authViewModel: AuthViewModel by viewModels {
        viewModelFactory {
            initializer { AuthViewModel((application as GoVoyageApplication).auth) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observerEtat()
        binding.boutonCreerCompte.setOnClickListener { creerCompte() }
    }

    /** Revient a la connexion apres une inscription reussie. */
    private fun observerEtat() {
        authViewModel.etat.observe(this) { etat ->
            if (etat.message.isNotEmpty()) {
                Toast.makeText(this, etat.message, Toast.LENGTH_LONG).show()
                if (!etat.erreur) revenirConnexion()
            }
            binding.boutonCreerCompte.isEnabled = !etat.chargement
        }
    }

    /** Controle la saisie puis demande la creation du compte. */
    private fun creerCompte() {
        val prenom = binding.editPrenom.text.toString().trim()
        val nom = binding.editNom.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val tel = binding.editTel.text.toString().trim()
        val motDePasse = binding.editPassword.text.toString()

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || tel.isEmpty() || motDePasse.isEmpty()) {
            Toast.makeText(this, getString(R.string.register_champs_vides), Toast.LENGTH_SHORT).show()
            return
        }
        if (motDePasse.length < 8) {
            Toast.makeText(this, getString(R.string.register_password_court), Toast.LENGTH_SHORT).show()
            return
        }
        authViewModel.inscrire(InscriptionRequest(nom, prenom, email, tel, motDePasse))
    }

    /** Renvoie au formulaire de connexion avec l'email deja saisi. */
    private fun revenirConnexion() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_EMAIL, binding.editEmail.text.toString().trim())
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}
