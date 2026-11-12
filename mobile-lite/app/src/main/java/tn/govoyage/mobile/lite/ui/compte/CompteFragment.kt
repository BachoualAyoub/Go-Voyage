package tn.govoyage.mobile.lite.ui.compte

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import tn.govoyage.mobile.lite.R
import tn.govoyage.mobile.lite.databinding.FragmentCompteBinding
import tn.govoyage.mobile.lite.viewModel.AuthViewModel

/** Coordonnees du compte connecte et deconnexion. */
class CompteFragment : Fragment() {

    private var _binding: FragmentCompteBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

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
        authViewModel.utilisateurConnecte.observe(viewLifecycleOwner) { compte ->
            if (compte == null) return@observe
            val estAdmin = compte.role == "ADMIN"
            binding.texteEspace.text = if (estAdmin) {
                getString(R.string.compte_espace_admin)
            } else {
                getString(R.string.compte_espace_voyageur)
            }
            binding.texteBonjour.text = getString(R.string.compte_bonjour, compte.prenom)
            binding.tvValeurNom.text = "${compte.prenom} ${compte.nom}"
            binding.tvValeurEmail.text = compte.email
            binding.tvValeurTelephone.text = compte.tel
            binding.tvValeurRole.text = if (estAdmin) {
                getString(R.string.compte_role_admin)
            } else {
                getString(R.string.compte_role_user)
            }
            binding.texteNoteAdmin.visibility = if (estAdmin) View.VISIBLE else View.GONE
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
            .setPositiveButton(R.string.deconnexion_oui) { dialog, _ ->
                authViewModel.deconnecter()
            }
            .setNegativeButton(R.string.deconnexion_non) { dialog, _ ->
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
