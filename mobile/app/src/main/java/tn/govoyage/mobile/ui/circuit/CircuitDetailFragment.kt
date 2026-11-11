package tn.govoyage.mobile.ui.circuit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentCircuitDetailBinding
import tn.govoyage.mobile.viewModel.VoyageViewModel

/** Fiche complete d'un circuit : itineraire et totaux. */
class CircuitDetailFragment : Fragment() {

    private var _binding: FragmentCircuitDetailBinding? = null
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
        _binding = FragmentCircuitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        voyageViewModel.circuitSelectionne.observe(viewLifecycleOwner) { circuit ->
            if (circuit != null) {
                binding.texteFicheCircuitNom.text = circuit.nom
                if (circuit.description.isNullOrEmpty()) {
                    binding.texteFicheCircuitDescription.visibility = View.GONE
                } else {
                    binding.texteFicheCircuitDescription.visibility = View.VISIBLE
                    binding.texteFicheCircuitDescription.text = circuit.description
                }
                binding.texteFicheCircuitTotaux.text =
                    getString(R.string.fiche_circuit_jours_estimes, circuit.dureeJours, circuit.estimationTnd)
                if (circuit.budgetTnd != null) {
                    binding.texteFicheCircuitBudget.visibility = View.VISIBLE
                    binding.texteFicheCircuitBudget.text =
                        getString(R.string.fiche_circuit_budget, circuit.budgetTnd)
                } else {
                    binding.texteFicheCircuitBudget.visibility = View.GONE
                }
                remplirEtapes(circuit)
                // Le bouton « Personnaliser / Modifier » s'active avec l'editeur (phase suivante).
            }
        }
        binding.boutonFermerCircuit.setOnClickListener {
            voyageViewModel.selectCircuit(null)
            // Retour a l'ecran precedent.
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    /** Ajoute une ligne par etape, dans l'ordre du circuit. */
    private fun remplirEtapes(circuit: tn.govoyage.mobile.model.Circuit) {
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
