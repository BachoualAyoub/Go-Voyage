package tn.govoyage.mobile.ui.destination

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentDestinationDetailBinding
import tn.govoyage.mobile.ui.afficherImageDestination
import tn.govoyage.mobile.viewModel.DestinationViewModel


class DestinationDetailFragment : Fragment() {

    private var _binding: FragmentDestinationDetailBinding? = null
    private val binding get() = _binding!!

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                DestinationViewModel(app.destinations)
            }
        }
    }

    private val destinationId by lazy {
        arguments?.getLong(ARG_DESTINATION_ID, -1L) ?: -1L
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
        observerFiche()
        installerBoutons()
        if (destinationId != -1L) destinationViewModel.chargerDetail(destinationId)
    }


    private fun observerFiche() {
        destinationViewModel.detail.observe(viewLifecycleOwner) { destination ->
            if (destination == null) return@observe
            afficherImageDestination(binding.imageDestinationDetail, destination)
            binding.texteFicheNom.text = destination.nom
            binding.texteFicheLieu.text =
                "${destination.region} · ${destination.pays} · ${libelleAmbiance(destination.ambiance)}"
            binding.texteFicheDescription.text = destination.description
            binding.texteFicheTarif.text =
                "${destination.tarifJourTnd} ${getString(R.string.fiche_destination_tarif_jour)}"
            binding.boutonCreerCircuit.visibility = View.VISIBLE
        }
        destinationViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.barreChargementDetail.visibility =
                if (etat.chargement) View.VISIBLE else View.GONE
            binding.texteErreurDetail.visibility =
                if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.boutonReessayerDetail.visibility =
                if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.texteErreurDetail.text = etat.erreur
        }
    }


    private fun installerBoutons() {
        binding.boutonFermerDestination.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.boutonReessayerDetail.setOnClickListener {
            if (destinationId != -1L) destinationViewModel.chargerDetail(destinationId)
        }
        binding.boutonCreerCircuit.setOnClickListener {
            findNavController().navigate(
                R.id.action_nav_destination_detail_to_nav_editeur_circuit,
                bundleOf("destinationId" to destinationId)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_DESTINATION_ID = "destinationId"
    }
}
