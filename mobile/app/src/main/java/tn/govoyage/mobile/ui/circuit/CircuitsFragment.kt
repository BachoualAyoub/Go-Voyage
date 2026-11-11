package tn.govoyage.mobile.ui.circuit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentCircuitsBinding
import tn.govoyage.mobile.viewModel.VoyageViewModel

/** Carnet personnel du voyageur : liste, creation, modification, suppression. */
class CircuitsFragment : Fragment() {

    private var _binding: FragmentCircuitsBinding? = null
    private val binding get() = _binding!!

    private val voyageViewModel: VoyageViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                VoyageViewModel(app.destinations, app.circuits, app.auth)
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
        remplirRecyclerView()

        voyageViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.barreChargement.visibility = if (etat.chargement) View.VISIBLE else View.GONE
            binding.carteErreur.visibility = if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.texteErreur.text = etat.erreur
            binding.texteInformation.visibility = if (etat.information.isEmpty()) View.GONE else View.VISIBLE
            binding.texteInformation.text = etat.information
            binding.boutonNouveauCircuit.isEnabled = !etat.actionEnCours
            binding.boutonActualiser.isEnabled = !etat.actionEnCours
        }

        voyageViewModel.circuits.observe(viewLifecycleOwner) { liste ->
            circuitAdapter.updateList(liste)
            val etat = voyageViewModel.etat.value
            val carnetVide = liste.isEmpty() && etat != null && !etat.chargement
            binding.texteCarnetVide.visibility = if (carnetVide) View.VISIBLE else View.GONE
        }

        voyageViewModel.destinations.observe(viewLifecycleOwner) { liste ->
            // Un circuit a besoin d'au moins une destination du catalogue.
            binding.boutonNouveauCircuit.isEnabled =
                binding.boutonNouveauCircuit.isEnabled && liste.isNotEmpty()
        }

        binding.boutonReessayer.setOnClickListener { voyageViewModel.actualiser() }
        binding.boutonActualiser.setOnClickListener {
            voyageViewModel.effacerMessage()
            voyageViewModel.actualiser()
        }
        binding.boutonNouveauCircuit.setOnClickListener {
            voyageViewModel.selectCircuit(null)
            findNavController().navigate(R.id.action_nav_circuits_to_nav_editeur_circuit)
        }
    }

    /** Prepare la liste reactive du carnet avec ses actions. */
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
    private fun ouvrirCircuit(circuit: tn.govoyage.mobile.model.Circuit) {
        voyageViewModel.selectCircuit(circuit)
        findNavController().navigate(R.id.action_nav_circuits_to_nav_circuit_detail)
    }

    /** Evite la suppression accidentelle, comme dans l'app Compose. */
    private fun confirmerSuppression(circuit: tn.govoyage.mobile.model.Circuit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.suppression_titre)
            .setMessage(getString(R.string.suppression_message, circuit.nom))
            .setPositiveButton(R.string.suppression_oui) { dialog, which ->
                voyageViewModel.supprimer(circuit.id)
            }
            .setNegativeButton(R.string.suppression_non) { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
