package tn.govoyage.mobile.lite.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import tn.govoyage.mobile.lite.R
import tn.govoyage.mobile.lite.databinding.FragmentRegisterBinding
import tn.govoyage.mobile.lite.viewModel.AuthViewModel

/** Formulaire d'inscription, ajoute un compte dans la liste en memoire. */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.boutonCreerCompte.setOnClickListener { creerCompte() }
    }

    /** Controle la saisie puis enregistre le nouveau compte. */
    private fun creerCompte() {
        val prenom = binding.editPrenom.text.toString().trim()
        val nom = binding.editNom.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val tel = binding.editTel.text.toString().trim()
        val motDePasse = binding.editPassword.text.toString()

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || tel.isEmpty() || motDePasse.isEmpty()) {
            Toast.makeText(requireContext(), R.string.register_champs_vides, Toast.LENGTH_SHORT).show()
            return
        }
        if (motDePasse.length < 8) {
            Toast.makeText(requireContext(), R.string.register_password_court, Toast.LENGTH_SHORT).show()
            return
        }
        val succes = authViewModel.inscrire(prenom, nom, email, tel, motDePasse)
        if (succes) {
            Toast.makeText(requireContext(), R.string.register_succes, Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_nav_register_to_nav_login)
        } else {
            Toast.makeText(requireContext(), R.string.register_email_existant, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
