package tn.govoyage.mobile.ui.destination

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.databinding.FragmentDestinationDetailBinding
import tn.govoyage.mobile.viewModel.VoyageViewModel

/** Fiche d'une destination du catalogue. */
class DestinationDetailFragment : Fragment() {

    private var _binding: FragmentDestinationDetailBinding? = null
    private val binding get() = _binding!!

    private val voyageViewModel: VoyageViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                VoyageViewModel(app.destinations, app.circuits, app.auth)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        voyageViewModel.destinationSelectionne.observe(viewLifecycleOwner) { destination ->
            if (destination != null) {
                binding.texteFicheNom.text = destination.nom
                binding.texteFicheLieu.text = "${destination.region} · ${destination.pays} · ${libelleAmbiance(destination.ambiance)}"
                binding.texteFicheDescription.text = destination.description
                binding.texteFicheTarif.text = "${destination.tarifJourTnd} TND par jour (indicatif)"
            }
        }
        binding.boutonFermerDestination.setOnClickListener {
            voyageViewModel.selectDestination(null)
            // Retour a l'ecran precedent.
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        // Le bouton « Créer un circuit » s'active avec l'editeur (phase suivante).
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
