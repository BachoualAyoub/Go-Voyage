package tn.govoyage.mobile.ui.circuit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentEditeurCircuitBinding
import tn.govoyage.mobile.databinding.ItemEtapeBinding
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.model.EtapeCircuit
import tn.govoyage.mobile.viewModel.CircuitViewModel
import tn.govoyage.mobile.viewModel.DestinationViewModel

private const val ARG_CIRCUIT_ID = "circuitId"
private const val ARG_DESTINATION_ID = "destinationId"

private data class EtapeBrouillon(val cle: Int, val destinationId: Long, var jours: String)

private class LigneEtapeVue(val cle: Int, val binding: ItemEtapeBinding)

class EditeurCircuitFragment : Fragment() {

    private var _binding: FragmentEditeurCircuitBinding? = null
    private val binding get() = _binding!!

    private val circuitViewModel: CircuitViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                CircuitViewModel(app.circuits)
            }
        }
    }

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                DestinationViewModel(app.destinations)
            }
        }
    }

    private val circuitId by lazy { arguments?.getLong(ARG_CIRCUIT_ID, -1L) ?: -1L }
    private val destinationId by lazy { arguments?.getLong(ARG_DESTINATION_ID, -1L) ?: -1L }

    private var personnel: Circuit? = null
    private var departCharge = false
    private var destinationInitialAjoutee = false
    private val brouillons = mutableListOf<EtapeBrouillon>()
    private val vues = mutableListOf<LigneEtapeVue>()
    private var prochaineCle = 0
    private var catalogue: Map<Long, Destination> = emptyMap()
    private var listeDestinationsCatalogue: List<Destination> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditeurCircuitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val circuitInitial = circuitViewModel.detail.value
        if (circuitInitial?.id != circuitId) circuitViewModel.effacerDetail()
        if (circuitId != -1L && circuitInitial?.id == circuitId) {
            departCharge = true
            preparerDepart(circuitInitial)
        } else {
            preparerDepart(null)
            if (circuitId != -1L) circuitViewModel.chargerDetail(circuitId)
        }
        installerChamps()
        installerBoutons()

        circuitViewModel.detail.observe(viewLifecycleOwner) { circuit ->
            if (circuit == null || departCharge) return@observe
            departCharge = true
            viderLignes()
            preparerDepart(circuit)
        }

        destinationViewModel.liste.observe(viewLifecycleOwner) { liste ->
            listeDestinationsCatalogue = liste
            catalogue = liste.associateBy { it.id }
            ajouterDestinationInitiale(liste)
            renumeroter()
            recalculerBilan()
        }

        circuitViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.boutonEnregistrer.text =
                if (etat.chargement) getString(R.string.editeur_patientez)
                else getString(R.string.editeur_enregistrer)
            binding.texteErreurEditeur.text = etat.erreur
        }

        if (destinationViewModel.liste.value.isNullOrEmpty()) {
            destinationViewModel.chargerSiVide()
        }
    }

    private fun preparerDepart(circuit: Circuit?) {
        personnel = circuit
        binding.texteTitreEditeur.text = if (circuit == null) {
            getString(R.string.editeur_titre_creation)
        } else {
            getString(R.string.editeur_titre_modification)
        }
        binding.editNomCircuit.setText(circuit?.nom.orEmpty())
        val etapes = if (circuit == null) {
            emptyList()
        } else {
            circuit.etapes.mapIndexed { index, etape ->
                EtapeBrouillon(index, etape.destinationId, etape.jours.toString())
            }
        }
        prochaineCle = etapes.size
        for (etape in etapes) ajouterLigne(etape)
    }

    private fun installerChamps() {
        binding.editNomCircuit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(texte: CharSequence, debut: Int, avant: Int, apres: Int) {
                actualiserBoutonEnregistrer()
            }

            override fun beforeTextChanged(texte: CharSequence, debut: Int, compte: Int, apres: Int) {
            }

            override fun afterTextChanged(texte: Editable) {
            }
        })
    }

    private fun installerBoutons() {
        binding.boutonFermerEditeur.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.boutonAjouterEtape.setOnClickListener { ouvrirChoixDestination() }
        binding.boutonEnregistrer.setOnClickListener { enregistrerCircuit() }
    }

    private fun ajouterLigne(etape: EtapeBrouillon) {
        val ligneBinding = ItemEtapeBinding.inflate(layoutInflater, binding.listeEtapesEditeur, false)
        val vue = LigneEtapeVue(etape.cle, ligneBinding)
        ligneBinding.editJours.setText(etape.jours)
        ligneBinding.editJours.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(texte: CharSequence, debut: Int, avant: Int, apres: Int) {
                brouillons.firstOrNull { it.cle == vue.cle }?.jours = texte.toString()
                recalculerBilan()
            }

            override fun beforeTextChanged(texte: CharSequence, debut: Int, compte: Int, apres: Int) {
            }

            override fun afterTextChanged(texte: Editable) {
            }
        })
        ligneBinding.boutonMonter.setOnClickListener {
            deplacerEtape(indiceDe(vue.cle), -1)
        }
        ligneBinding.boutonDescendre.setOnClickListener {
            deplacerEtape(indiceDe(vue.cle), 1)
        }
        ligneBinding.boutonRetirer.setOnClickListener {
            retirerEtape(indiceDe(vue.cle))
        }
        brouillons.add(etape)
        vues.add(vue)
        binding.listeEtapesEditeur.addView(ligneBinding.root)
        binding.texteEtapesVides.visibility = if (brouillons.isEmpty()) View.VISIBLE else View.GONE
        renumeroter()
        recalculerBilan()
    }

    private fun deplacerEtape(index: Int, delta: Int) {
        val nouvellePosition = index + delta
        if (index < 0 || nouvellePosition < 0 || nouvellePosition >= brouillons.size) return
        val etape = brouillons.removeAt(index)
        brouillons.add(nouvellePosition, etape)
        val vue = vues.removeAt(index)
        vues.add(nouvellePosition, vue)
        rattacherLignes()
        renumeroter()
    }

    private fun retirerEtape(index: Int) {
        if (index < 0 || index >= brouillons.size) return
        brouillons.removeAt(index)
        val vue = vues.removeAt(index)
        binding.listeEtapesEditeur.removeView(vue.binding.root)
        binding.texteEtapesVides.visibility = if (brouillons.isEmpty()) View.VISIBLE else View.GONE
        renumeroter()
        recalculerBilan()
    }

    private fun rattacherLignes() {
        binding.listeEtapesEditeur.removeAllViews()
        for (vue in vues) binding.listeEtapesEditeur.addView(vue.binding.root)
    }

    private fun indiceDe(cle: Int) = vues.indexOfFirst { it.cle == cle }

    private fun renumeroter() {
        binding.texteCompteurEtapes.text = getString(R.string.editeur_compteur_etapes, brouillons.size)
        for ((index, vue) in vues.withIndex()) {
            val etape = brouillons[index]
            val destination = catalogue[etape.destinationId]
            val numero = index + 1
            vue.binding.tvNumeroEtape.text = if (numero < 10) "0$numero" else numero.toString()
            vue.binding.tvNomEtape.text =
                destination?.nom ?: getString(R.string.editeur_destination_indisponible)
            vue.binding.tvTarifEtape.text =
                getString(R.string.editeur_etape_tarif, destination?.tarifJourTnd ?: 0)
            vue.binding.boutonMonter.isEnabled = index > 0
            vue.binding.boutonDescendre.isEnabled = index < vues.size - 1
            vue.binding.boutonMonter.contentDescription = getString(R.string.editeur_etape_monter, numero)
            vue.binding.boutonDescendre.contentDescription = getString(R.string.editeur_etape_descendre, numero)
        }
    }

    private fun ajouterDestinationInitiale(liste: List<Destination>) {
        if (destinationId == -1L || circuitId != -1L || destinationInitialAjoutee) return
        val destination = liste.firstOrNull { it.id == destinationId } ?: return
        destinationInitialAjoutee = true
        ajouterEtape(destination)
    }

    private fun ouvrirChoixDestination() {
        val destinations = listeDestinationsCatalogue
        if (destinations.isEmpty()) return
        val popup = PopupMenu(requireContext(), binding.boutonAjouterEtape)
        for ((index, destination) in destinations.withIndex()) {
            popup.menu.add(
                0, index, index,
                getString(R.string.editeur_etape_ajout_tarif, destination.nom, destination.tarifJourTnd)
            )
        }
        popup.setOnMenuItemClickListener { item ->
            ajouterEtape(destinations[item.itemId])
            true
        }
        popup.show()
    }

    private fun ajouterEtape(destination: Destination) {
        if (brouillons.size >= 10) return
        ajouterLigne(EtapeBrouillon(prochaineCle++, destination.id, "1"))
    }

    private fun recalculerBilan() {
        val jours = brouillons.sumOf { it.jours.toIntOrNull() ?: 0 }
        val estimation = brouillons.sumOf {
            (catalogue[it.destinationId]?.tarifJourTnd ?: 0) * (it.jours.toIntOrNull() ?: 0)
        }
        binding.texteBilanTotaux.text = "$jours j  ·  $estimation TND"
        binding.texteJoursExcessifs.visibility = if (jours > 60) View.VISIBLE else View.GONE
        actualiserBoutonEnregistrer()
    }

    private fun actualiserBoutonEnregistrer() {
        val jours = brouillons.sumOf { it.jours.toIntOrNull() ?: 0 }
        val etapesValides = brouillons.isNotEmpty() && jours <= 60 && brouillons.all {
            (it.jours.toIntOrNull() ?: 0) in 1..30 && catalogue.containsKey(it.destinationId)
        }
        binding.boutonEnregistrer.isEnabled = etapesValides && brouillons.size < 10
    }

    private fun enregistrerCircuit() {
        if (binding.editNomCircuit.text.isBlank()) {
            Toast.makeText(requireContext(), R.string.editeur_nom_obligatoire, Toast.LENGTH_SHORT).show()
            binding.editNomCircuit.requestFocus()
            return
        }
        val nom = binding.editNomCircuit.text.toString().trim()
        circuitViewModel.sauver(personnel, nom, construireEtapes()) {
            findNavController().navigate(R.id.action_nav_editeur_circuit_to_nav_circuits)
        }
    }

    private fun construireEtapes(): List<EtapeCircuit> {
        val etapes = mutableListOf<EtapeCircuit>()
        for (brouillon in brouillons) {
            val destination = catalogue[brouillon.destinationId] ?: continue
            val jours = brouillon.jours.toInt()
            etapes.add(
                EtapeCircuit(
                    destinationId = destination.id,
                    nom = destination.nom,
                    ville = destination.ville,
                    ambiance = destination.ambiance,
                    jours = jours,
                    tarifJourTnd = destination.tarifJourTnd,
                    coutTnd = destination.tarifJourTnd * jours
                )
            )
        }
        return etapes
    }

    private fun viderLignes() {
        brouillons.clear()
        vues.clear()
        binding.listeEtapesEditeur.removeAllViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viderLignes()
        _binding = null
    }
}
