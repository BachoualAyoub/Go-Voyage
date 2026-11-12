package tn.govoyage.mobile.lite.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import tn.govoyage.mobile.lite.R
import tn.govoyage.mobile.lite.databinding.FragmentHomeBinding
import tn.govoyage.mobile.lite.model.Destination
import tn.govoyage.mobile.lite.ui.circuit.CircuitAdapter
import tn.govoyage.mobile.lite.ui.destination.DestinationAdapter
import tn.govoyage.mobile.lite.viewModel.VoyageViewModel
import java.text.Normalizer
import java.util.Locale

/** Ecran Explorer : catalogue, recherche, filtres et circuits proposes. */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val voyageViewModel: VoyageViewModel by activityViewModels()

    private lateinit var destinationAdapter: DestinationAdapter
    private lateinit var modeleAdapter: CircuitAdapter

    private var toutesDestinations: List<Destination> = emptyList()
    private var filtreAmbiance = ""
    private var recherche = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        installerBascule()
        installerRecherche()
        installerFiltres()
        installerListes()

        voyageViewModel.destinations.observe(viewLifecycleOwner) { liste ->
            toutesDestinations = liste
            appliquerFiltre()
        }
        voyageViewModel.modeles.observe(viewLifecycleOwner) { liste ->
            modeleAdapter.updateList(liste)
        }
    }

    /** Bascule entre le catalogue et les circuits proposes. */
    private fun installerBascule() {
        binding.chipDestinations.setOnClickListener {
            binding.groupeDestinations.visibility = View.VISIBLE
            binding.groupeModeles.visibility = View.GONE
        }
        binding.chipModeles.setOnClickListener {
            binding.groupeDestinations.visibility = View.GONE
            binding.groupeModeles.visibility = View.VISIBLE
        }
    }

    /** La recherche se relance a chaque frappe. */
    private fun installerRecherche() {
        binding.editRecherche.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(texte: CharSequence, debut: Int, avant: Int, apres: Int) {
                recherche = texte.toString()
                appliquerFiltre()
            }

            override fun beforeTextChanged(texte: CharSequence, debut: Int, compte: Int, apres: Int) {
                // Aucun traitement avant le changement.
            }

            override fun afterTextChanged(texte: Editable) {
                // Aucun traitement apres le changement.
            }
        })
    }

    /** Filtre le catalogue par ambiance. */
    private fun installerFiltres() {
        binding.chipToutes.setOnClickListener { choisirAmbiance("") }
        binding.chipMer.setOnClickListener { choisirAmbiance("MER") }
        binding.chipCulture.setOnClickListener { choisirAmbiance("CULTURE") }
        binding.chipNature.setOnClickListener { choisirAmbiance("NATURE") }
    }

    /** Memorise l'ambiance choisie puis recalcule la liste. */
    private fun choisirAmbiance(ambiance: String) {
        filtreAmbiance = ambiance
        appliquerFiltre()
    }

    /** Prepare les deux listes et leurs redirections vers les fiches. */
    private fun installerListes() {
        destinationAdapter = DestinationAdapter(
            mutableListOf(),
            onItemClicked = { destination ->
                voyageViewModel.selectDestination(destination)
                findNavController().navigate(R.id.action_nav_home_to_nav_destination_detail)
            }
        )
        binding.listeDestinations.layoutManager = LinearLayoutManager(requireContext())
        binding.listeDestinations.adapter = destinationAdapter

        modeleAdapter = CircuitAdapter(
            mutableListOf(),
            avecActions = false,
            onClique = { circuit -> ouvrirCircuit(circuit) },
            onModifier = { circuit -> ouvrirCircuit(circuit) },
            onSupprimer = { circuit -> ouvrirCircuit(circuit) }
        )
        binding.listeModeles.layoutManager = LinearLayoutManager(requireContext())
        binding.listeModeles.adapter = modeleAdapter
    }

    /** Ouvre la fiche d'un circuit propose. */
    private fun ouvrirCircuit(circuit: tn.govoyage.mobile.lite.model.Circuit) {
        voyageViewModel.selectCircuit(circuit)
        findNavController().navigate(R.id.action_nav_home_to_nav_circuit_detail)
    }

    /** Recalcule la liste affichee a partir de la recherche et du filtre. */
    private fun appliquerFiltre() {
        val liste = toutesDestinations.filter {
            (filtreAmbiance.isEmpty() || it.ambiance == filtreAmbiance) &&
                normaliser("${it.nom} ${it.ville} ${it.region} ${it.ambiance}").contains(normaliser(recherche))
        }
        destinationAdapter.updateList(liste)
        binding.texteAucune.visibility = if (liste.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Rend la recherche insensible aux accents et a la casse. */
    private fun normaliser(texte: String) = Normalizer.normalize(texte.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
