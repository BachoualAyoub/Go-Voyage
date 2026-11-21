package tn.govoyage.mobile.ui.circuit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentCircuitDetailBinding
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.ui.afficherImageCircuit
import tn.govoyage.mobile.viewModel.CircuitViewModel

/** Fiche complete d'un circuit : itineraire et totaux. */
class CircuitDetailFragment : Fragment() {

    private var _binding: FragmentCircuitDetailBinding? = null
    private val binding get() = _binding!!

    private val circuitViewModel: CircuitViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                CircuitViewModel(app.circuits, app.destinations)
            }
        }
    }

    private val circuitId by lazy { arguments?.getLong("circuitId", -1L) ?: -1L }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCircuitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        circuitViewModel.demarrer()

        circuitViewModel.circuits.observe(viewLifecycleOwner) { liste ->
            val circuit = liste.firstOrNull { it.id == circuitId } ?: return@observe
            afficherCircuit(circuit)
        }

        binding.boutonFermerCircuit.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.boutonPersonnaliser.setOnClickListener {
            findNavController().navigate(
                R.id.action_nav_circuit_detail_to_nav_editeur_circuit,
                bundleOf("circuitId" to circuitId)
            )
        }
    }

    /** Remplit la fiche avec le circuit trouve dans le carnet local. */
    private fun afficherCircuit(circuit: Circuit) {
        afficherImageCircuit(binding.imageCircuitDetail, circuit)
        binding.texteFicheCircuitNom.text = circuit.nom
        binding.texteFicheCircuitTotaux.text =
            getString(R.string.fiche_circuit_jours_estimes, circuit.dureeJours, circuit.estimationTnd)
        remplirEtapes(circuit)
    }

    /** Ajoute une ligne par etape, dans l'ordre du circuit. */
    private fun remplirEtapes(circuit: Circuit) {
        binding.listeEtapesFiche.removeAllViews()
        for ((ordre, etape) in circuit.etapes.withIndex()) {
            val ligne = TextView(requireContext())
            ligne.text = "${ordre + 1}. ${etape.nom} · ${etape.jours} j · ${etape.coutTnd} TND"
            ligne.textSize = 15f
            ligne.setTextColor(requireContext().getColor(R.color.palette_encre))
            binding.listeEtapesFiche.addView(ligne)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
