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
import tn.govoyage.mobile.viewModel.AuthViewModel
import tn.govoyage.mobile.viewModel.CircuitViewModel

class CircuitsFragment : Fragment() {

    private var _binding: FragmentCircuitsBinding? = null
    private val binding get() = _binding!!

    private val circuitViewModel: CircuitViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                CircuitViewModel(app.circuits)
            }
        }
    }

    private val authViewModel: AuthViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                AuthViewModel(app.auth)
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

        circuitViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.barreChargement.visibility =
                if (etat.chargement) View.VISIBLE else View.GONE
            binding.carteErreur.visibility = if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.texteErreur.text = etat.erreur
            binding.boutonNouveauCircuit.isEnabled = !etat.chargement
            binding.boutonReessayerCircuits.isEnabled = !etat.chargement
        }

        circuitViewModel.liste.observe(viewLifecycleOwner) { liste ->
            circuitAdapter.updateList(liste)
            binding.texteCarnetVide.visibility = if (liste.isEmpty()) View.VISIBLE else View.GONE
        }

        authViewModel.etat.observe(viewLifecycleOwner) { etat ->
            if (etat.message.isNotEmpty()) {
                Toast.makeText(requireContext(), etat.message, Toast.LENGTH_LONG).show()
                authViewModel.viderMessage()
            }
        }

        binding.boutonReessayerCircuits.setOnClickListener {
            authViewModel.reconnecter {
                circuitViewModel.charger()
            }
        }

        binding.boutonNouveauCircuit.setOnClickListener {
            findNavController().navigate(R.id.action_nav_circuits_to_nav_editeur_circuit)
        }
        circuitViewModel.charger()
    }

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

    private fun ouvrirCircuit(circuit: Circuit) {
        findNavController().navigate(
            R.id.action_nav_circuits_to_nav_circuit_detail,
            bundleOf("circuitId" to circuit.id)
        )
    }

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
