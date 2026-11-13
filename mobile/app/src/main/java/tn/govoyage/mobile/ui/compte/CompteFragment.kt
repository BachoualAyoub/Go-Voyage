package tn.govoyage.mobile.ui.compte

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentCompteBinding
import tn.govoyage.mobile.viewModel.AuthViewModel

/** Coordonnees du compte connecte et deconnexion. */
class CompteFragment : Fragment() {

    private var _binding: FragmentCompteBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        viewModelFactory {
            initializer { AuthViewModel((requireActivity().application as GoVoyageApplication).auth) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authViewModel.session.observe(viewLifecycleOwner) { session ->
            val compte = session?.compte ?: return@observe
            binding.texteEspace.text = getString(R.string.compte_espace_voyageur)
            binding.texteBonjour.text = getString(R.string.compte_bonjour, compte.prenom)
            binding.tvValeurNom.text = "${compte.prenom} ${compte.nom}"
            binding.tvValeurEmail.text = compte.email
            binding.tvValeurTelephone.text = compte.tel
        }
        binding.boutonDeconnexion.setOnClickListener {
            confirmerDeconnexion()
        }
    }

    /** Dialogue de confirmation, identique au TP3. */
    private fun confirmerDeconnexion() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.deconnexion_titre)
            .setMessage(R.string.deconnexion_message)
            .setPositiveButton(R.string.deconnexion_oui) { dialog, which ->
                authViewModel.deconnecter()
            }
            .setNegativeButton(R.string.deconnexion_non) { dialog, which ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_logout)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
