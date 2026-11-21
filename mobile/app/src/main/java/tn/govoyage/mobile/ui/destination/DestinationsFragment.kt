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
import androidx.recyclerview.widget.LinearLayoutManager
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentDestinationsBinding
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.viewModel.DestinationViewModel

/** Catalogue des destinations, avec erreur reseau, liste vide et bouton reessayer. */
class DestinationsFragment : Fragment() {

    private var _binding: FragmentDestinationsBinding? = null
    private val binding get() = _binding!!

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                DestinationViewModel(app.destinations)
            }
        }
    }

    private lateinit var destinationAdapter: DestinationAdapter
    private var listeCourante: List<Destination> = emptyList()
    private var erreurCourante = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preparerListe()
        observerDonnees()

        binding.boutonReessayerDestinations.setOnClickListener {
            destinationViewModel.charger(true)
        }
        if (destinationViewModel.liste.value.isNullOrEmpty()) {
            destinationViewModel.chargerSiVide()
        }
    }

    /** Prepare la RecyclerView et la navigation vers la fiche. */
    private fun preparerListe() {
        destinationAdapter = DestinationAdapter(
            mutableListOf(),
            onItemClicked = { destination -> ouvrirDetail(destination) }
        )
        binding.listeDestinations.layoutManager = LinearLayoutManager(requireContext())
        binding.listeDestinations.adapter = destinationAdapter
    }

    /** Observe la liste, l'etat de chargement et les erreurs. */
    private fun observerDonnees() {
        destinationViewModel.liste.observe(viewLifecycleOwner) { liste ->
            listeCourante = liste
            destinationAdapter.updateList(liste)
            actualiserEtatVide()
        }
        destinationViewModel.etat.observe(viewLifecycleOwner) { etat ->
            erreurCourante = etat.erreur
            binding.barreChargementDestinations.visibility =
                if (etat.chargement) View.VISIBLE else View.GONE
            binding.carteErreurDestinations.visibility =
                if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.texteErreurDestinations.text = etat.erreur
            binding.boutonReessayerDestinations.isEnabled = !etat.chargement
            actualiserEtatVide()
        }
    }

    /** Ne montre le message vide que lorsqu'il n'y a ni donnees ni erreur. */
    private fun actualiserEtatVide() {
        val afficher = listeCourante.isEmpty() && erreurCourante.isEmpty()
        binding.texteDestinationsVides.visibility = if (afficher) View.VISIBLE else View.GONE
    }

    /** Transmet seulement l'identifiant pour recharger la fiche sur le backend. */
    private fun ouvrirDetail(destination: Destination) {
        findNavController().navigate(
            R.id.action_nav_destinations_to_nav_destination_detail,
            bundleOf("destinationId" to destination.id)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
