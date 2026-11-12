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
import tn.govoyage.mobile.lite.databinding.FragmentLoginBinding
import tn.govoyage.mobile.lite.viewModel.AuthViewModel

/** Formulaire de connexion, compte de demonstration prete a l'emploi. */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.boutonConnexion.setOnClickListener { seConnecter() }
        binding.boutonVersInscription.setOnClickListener {
            findNavController().navigate(R.id.action_nav_login_to_nav_register)
        }
    }

    /** Verifie les identifiants dans la liste en memoire puis va a l'accueil. */
    private fun seConnecter() {
        val email = binding.editEmail.text.toString().trim()
        // trim pour tolerer un espace accidentel du clavier.
        val motDePasse = binding.editPassword.text.toString().trim()
        if (email.isEmpty() || motDePasse.isEmpty()) {
            Toast.makeText(requireContext(), R.string.login_champs_vides, Toast.LENGTH_SHORT).show()
            return
        }
        if (authViewModel.connecter(email, motDePasse)) {
            findNavController().navigate(R.id.action_nav_login_to_nav_home)
        } else {
            Toast.makeText(requireContext(), R.string.login_identifiants_incorrects, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
