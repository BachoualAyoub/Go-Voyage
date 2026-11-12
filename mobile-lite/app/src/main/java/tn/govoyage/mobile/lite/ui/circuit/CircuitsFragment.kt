package tn.govoyage.mobile.lite.ui.circuit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import tn.govoyage.mobile.lite.R
import tn.govoyage.mobile.lite.databinding.FragmentCircuitsBinding
import tn.govoyage.mobile.lite.model.Circuit
import tn.govoyage.mobile.lite.viewModel.VoyageViewModel

/** Carnet personnel du voyageur : liste, creation, modification, suppression. */
class CircuitsFragment : Fragment() {

    private var _binding: FragmentCircuitsBinding? = null
    private val binding get() = _binding!!

    private val voyageViewModel: VoyageViewModel by activityViewModels()

    private lateinit var circuitAdapter: CircuitAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCircuitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        remplirRecyclerView()

        voyageViewModel.circuits.observe(viewLifecycleOwner) { liste ->
            circuitAdapter.updateList(liste)
            binding.texteCarnetVide.visibility = if (liste.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.boutonNouveauCircuit.setOnClickListener {
            voyageViewModel.selectCircuit(null)
            findNavController().navigate(R.id.action_nav_circuits_to_nav_editeur_circuit)
        }
    }

    /** Prepare la liste du carnet avec ses actions. */
    private fun remplirRecyclerView() {
        circuitAdapter = CircuitAdapter(
            mutableListOf(),
            avecActions = true,
            onClique = { circuit -> ouvrirCircuit(circuit) },
            onModifier = { circuit -> ouvrirCircuit(circuit) },
            onSupprimer = { circuit -> confirmerSuppression(circuit) }
        )
        binding.listeCircuits.layoutManager = LinearLayoutManager(requireContext())
        binding.listeCircuits.adapter = circuitAdapter
    }

    /** Ouvre la fiche du circuit avant modification. */
    private fun ouvrirCircuit(circuit: Circuit) {
        voyageViewModel.selectCircuit(circuit)
        findNavController().navigate(R.id.action_nav_circuits_to_nav_circuit_detail)
    }

    /** Evite la suppression accidentelle par un dialogue de confirmation. */
    private fun confirmerSuppression(circuit: Circuit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.suppression_titre)
            .setMessage(getString(R.string.suppression_message, circuit.nom))
            .setPositiveButton(R.string.suppression_oui) { dialog, _ ->
                val supprime = voyageViewModel.supprimer(circuit.id)
                if (supprime) {
                    Toast.makeText(requireContext(), R.string.suppression_succes, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.suppression_non) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
