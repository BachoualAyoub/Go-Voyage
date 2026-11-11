package tn.govoyage.mobile.ui.compte

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tn.govoyage.mobile.databinding.FragmentCompteBinding

/** Ecran Mon compte, squelette de la phase 1. */
class CompteFragment : Fragment() {

    private var _binding: FragmentCompteBinding? = null
    private val binding get() = _binding!!

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
        // Le profil et la deconnexion arrivent aux phases 2 et 4.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
