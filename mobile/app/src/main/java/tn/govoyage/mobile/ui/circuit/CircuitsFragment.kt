package tn.govoyage.mobile.ui.circuit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentCircuitsBinding
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.viewModel.CircuitViewModel

/** Carnet local : liste, creation, modification et suppression. */
class CircuitsFragment : Fragment() {

    private var _binding: FragmentCircuitsBinding? = null
    private val binding get() = _binding!!

    private val circuitViewModel: CircuitViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                CircuitViewModel(app.circuits, app.destinations)
            }
        }
    }

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
        circuitViewModel.demarrer()
        remplirRecyclerView()

        circuitViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.barreChargement.visibility =
                if (etat.actionEnCours) View.VISIBLE else View.GONE
            binding.carteErreur.visibility = if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.texteErreur.text = etat.erreur
            binding.boutonNouveauCircuit.isEnabled = !etat.actionEnCours
            binding.boutonSyncDistance.isEnabled = !etat.actionEnCours
            if (etat.toast.isNotEmpty()) {
                Toast.makeText(requireContext(), etat.toast, Toast.LENGTH_LONG).show()
                circuitViewModel.effacerMessage()
            } else if (etat.information.isNotEmpty()) {
                Toast.makeText(requireContext(), etat.information, Toast.LENGTH_LONG).show()
                circuitViewModel.effacerMessage()
            }
        }

        circuitViewModel.circuits.observe(viewLifecycleOwner) { liste ->
            circuitAdapter.updateList(liste)
            binding.texteCarnetVide.visibility = if (liste.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.boutonNouveauCircuit.setOnClickListener {
            findNavController().navigate(R.id.action_nav_circuits_to_nav_editeur_circuit)
        }
        binding.boutonSyncDistance.setOnClickListener {
            circuitViewModel.synchroniser()
        }
    }

    /** Prepare la liste locale avec les actions du carnet. */
    private fun remplirRecyclerView() {
        circuitAdapter = CircuitAdapter(
            mutableListOf(),
            onClique = { circuit -> ouvrirCircuit(circuit) },
            onModifier = { circuit -> ouvrirCircuit(circuit) },
            onSupprimer = { circuit -> confirmerSuppression(circuit) }
        )
        binding.listeCircuits.layoutManager = LinearLayoutManager(requireContext())
        binding.listeCircuits.adapter = circuitAdapter
    }

    /** Ouvre la fiche en transmettant seulement l'identifiant du circuit. */
    private fun ouvrirCircuit(circuit: Circuit) {
        findNavController().navigate(
            R.id.action_nav_circuits_to_nav_circuit_detail,
            bundleOf("circuitId" to circuit.id)
        )
    }

    /** Evite la suppression accidentelle. */
    private fun confirmerSuppression(circuit: Circuit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.suppression_titre)
            .setMessage(getString(R.string.suppression_message, circuit.nom))
            .setPositiveButton(R.string.suppression_oui) { dialog, _ ->
                circuitViewModel.supprimer(circuit.id)
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
