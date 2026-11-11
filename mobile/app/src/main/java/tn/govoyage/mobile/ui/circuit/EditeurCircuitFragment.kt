package tn.govoyage.mobile.ui.circuit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
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
import tn.govoyage.mobile.model.CircuitRequest
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.model.EtapeRequest
import tn.govoyage.mobile.viewModel.VoyageViewModel

/** Etape editable avec une cle stable lors des deplacements. */
private data class EtapeBrouillon(val cle: Int, val destinationId: Long, var jours: String)

/** Vue d'une ligne d'etape, gardee en memoire pour conserver la saisie. */
private class LigneEtapeVue(val cle: Int, val binding: ItemEtapeBinding)

/** Editeur plein ecran avec bilan instantane, comme dans l'app Compose. */
class EditeurCircuitFragment : Fragment() {

    private var _binding: FragmentEditeurCircuitBinding? = null
    private val binding get() = _binding!!

    private val voyageViewModel: VoyageViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as GoVoyageApplication
                VoyageViewModel(app.destinations, app.circuits, app.auth)
            }
        }
    }

    private var personnel: Circuit? = null
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
        preparerDepart()
        installerChamps()
        installerBoutons()

        voyageViewModel.destinations.observe(viewLifecycleOwner) { liste ->
            listeDestinationsCatalogue = liste
            catalogue = liste.associateBy { it.id }
            renumeroter()
            recalculerBilan()
        }
        voyageViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.boutonEnregistrer.text =
                if (etat.actionEnCours) getString(R.string.editeur_patientez) else getString(R.string.editeur_enregistrer)
            binding.texteErreurEditeur.text = etat.erreur
        }
    }

    /** Pre-remplit l'editeur depuis la fiche ouverte ou depuis zero. */
    private fun preparerDepart() {
        val circuit = voyageViewModel.circuitSelectionne.value
        personnel = circuit?.takeUnless { it.modele }
        binding.texteTitreEditeur.text = if (personnel != null) {
            getString(R.string.editeur_titre_modification)
        } else {
            getString(R.string.editeur_titre_creation)
        }
        binding.editNomCircuit.setText(circuit?.nom.orEmpty())
        binding.editBudget.setText(circuit?.budgetTnd?.toString().orEmpty())
        for (etape in preparerEtapes()) ajouterLigne(etape)
    }

    /** Construit le brouillon sans modifier le modele d'origine. */
    private fun preparerEtapes(): List<EtapeBrouillon> {
        val circuit = voyageViewModel.circuitSelectionne.value
        if (circuit != null) {
            return circuit.etapes.mapIndexed { index, etape ->
                EtapeBrouillon(index, etape.destinationId, etape.jours.toString())
            }
        }
        val destination = voyageViewModel.destinationSelectionne.value ?: return emptyList()
        return listOf(EtapeBrouillon(0, destination.id, "1"))
    }

    /** Installe les ecouteurs des champs nom et budget. */
    private fun installerChamps() {
        binding.editNomCircuit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(texte: CharSequence, debut: Int, avant: Int, apres: Int) {
                actualiserBoutonEnregistrer()
            }

            override fun beforeTextChanged(texte: CharSequence, debut: Int, compte: Int, apres: Int) {
                // Aucun traitement avant le changement.
            }

            override fun afterTextChanged(texte: Editable) {
                // Aucun traitement apres le changement.
            }
        })
        binding.editBudget.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(texte: CharSequence, debut: Int, avant: Int, apres: Int) {
                recalculerBilan()
            }

            override fun beforeTextChanged(texte: CharSequence, debut: Int, compte: Int, apres: Int) {
                // Aucun traitement avant le changement.
            }

            override fun afterTextChanged(texte: Editable) {
                // Aucun traitement apres le changement.
            }
        })
    }

    /** Installe les boutons de l'editeur. */
    private fun installerBoutons() {
        binding.boutonFermerEditeur.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.boutonAjouterEtape.setOnClickListener { ouvrirChoixDestination() }
        binding.boutonEnregistrer.setOnClickListener { enregistrerCircuit() }
    }

    /** Ajoute une ligne d'etape a la fin, avec sa duree par defaut. */
    private fun ajouterLigne(etape: EtapeBrouillon) {
        val ligneBinding = ItemEtapeBinding.inflate(layoutInflater)
        val vue = LigneEtapeVue(etape.cle, ligneBinding)
        ligneBinding.editJours.setText(etape.jours)
        ligneBinding.editJours.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(texte: CharSequence, debut: Int, avant: Int, apres: Int) {
                brouillons.firstOrNull { it.cle == vue.cle }?.jours = texte.toString()
                recalculerBilan()
            }

            override fun beforeTextChanged(texte: CharSequence, debut: Int, compte: Int, apres: Int) {
                // Aucun traitement avant le changement.
            }

            override fun afterTextChanged(texte: Editable) {
                // Aucun traitement apres le changement.
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

    /** Deplace une etape en conservant la duree saisie. */
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

    /** Retire une etape de la composition. */
    private fun retirerEtape(index: Int) {
        if (index < 0 || index >= brouillons.size) return
        brouillons.removeAt(index)
        val vue = vues.removeAt(index)
        binding.listeEtapesEditeur.removeView(vue.binding.root)
        binding.texteEtapesVides.visibility = if (brouillons.isEmpty()) View.VISIBLE else View.GONE
        renumeroter()
        recalculerBilan()
    }

    /** Replace les vues dans le nouvel ordre des etapes. */
    private fun rattacherLignes() {
        binding.listeEtapesEditeur.removeAllViews()
        for (vue in vues) binding.listeEtapesEditeur.addView(vue.binding.root)
    }

    /** Position d'une ligne dans la liste courante. */
    private fun indiceDe(cle: Int) = vues.indexOfFirst { it.cle == cle }

    /** Met a jour les titres, les descriptions et les fleches actives. */
    private fun renumeroter() {
        for ((index, vue) in vues.withIndex()) {
            val etape = brouillons[index]
            val destination = catalogue[etape.destinationId]
            vue.binding.tvNomEtape.text = "${index + 1}. ${
                destination?.nom ?: getString(R.string.editeur_destination_indisponible)
            }"
            vue.binding.boutonMonter.isEnabled = index > 0
            vue.binding.boutonDescendre.isEnabled = index < vues.size - 1
            vue.binding.boutonMonter.contentDescription = getString(R.string.editeur_etape_monter, index + 1)
            vue.binding.boutonDescendre.contentDescription = getString(R.string.editeur_etape_descendre, index + 1)
        }
    }

    /** Ouvre le catalogue pour ajouter une etape. */
    private fun ouvrirChoixDestination() {
        val destinations = listeDestinationsCatalogue
        if (destinations.isEmpty()) return
        val popup = PopupMenu(requireContext(), binding.boutonAjouterEtape)
        for ((index, destination) in destinations.withIndex()) {
            popup.menu.add(0, index, index, getString(R.string.editeur_etape_ajout_tarif, destination.nom, destination.tarifJourTnd))
        }
        popup.setOnMenuItemClickListener { item ->
            ajouterEtape(destinations[item.itemId])
            true
        }
        popup.show()
    }

    /** Ajoute une etape depuis le catalogue. */
    private fun ajouterEtape(destination: Destination) {
        if (brouillons.size >= 10) return
        ajouterLigne(EtapeBrouillon(prochaineCle++, destination.id, "1"))
    }

    /** Affiche les deux totaux et la marge de budget pendant la saisie. */
    private fun recalculerBilan() {
        val jours = brouillons.sumOf { it.jours.toIntOrNull() ?: 0 }
        val estimation = brouillons.sumOf {
            (catalogue[it.destinationId]?.tarifJourTnd ?: 0) * (it.jours.toIntOrNull() ?: 0)
        }
        binding.texteBilanTotaux.text = "$jours j  ·  $estimation TND"

        val budget = binding.editBudget.text.toString().toIntOrNull()
        val budgetValide = binding.editBudget.text.toString().isEmpty() || (budget ?: 0) in 1..1_000_000
        binding.texteErreurBudget.visibility = if (budgetValide) View.GONE else View.VISIBLE

        if (budget != null && budget > 0) {
            binding.barreBudget.visibility = View.VISIBLE
            binding.barreBudget.max = budget
            binding.barreBudget.progress = estimation
            val ecart = budget - estimation
            val depasse = estimation > budget
            binding.texteBilanMarge.text = if (depasse) {
                getString(R.string.editeur_bilan_depassement, -ecart)
            } else {
                getString(R.string.editeur_bilan_marge, ecart)
            }
            val couleur = if (depasse) R.color.palette_erreur else R.color.palette_turquoise_fonce
            binding.texteBilanMarge.setTextColor(requireContext().getColor(couleur))
            binding.barreBudget.progressTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(couleur))
        } else {
            binding.barreBudget.visibility = View.GONE
            binding.texteBilanMarge.text = getString(R.string.editeur_bilan_tarifs)
            binding.texteBilanMarge.setTextColor(requireContext().getColor(R.color.palette_texte_secondaire))
        }

        binding.texteJoursExcessifs.visibility = if (jours > 60) View.VISIBLE else View.GONE
        actualiserBoutonEnregistrer()
    }

    /** Le bouton enregistre quand le formulaire est complet. */
    private fun actualiserBoutonEnregistrer() {
        val jours = brouillons.sumOf { it.jours.toIntOrNull() ?: 0 }
        val budget = binding.editBudget.text.toString().toIntOrNull()
        val budgetValide = binding.editBudget.text.toString().isEmpty() || (budget ?: 0) in 1..1_000_000
        val etapesValides = brouillons.isNotEmpty() && jours <= 60 && brouillons.all {
            (it.jours.toIntOrNull() ?: 0) in 1..30 && catalogue.containsKey(it.destinationId)
        }
        binding.boutonEnregistrer.isEnabled = binding.editNomCircuit.text.isNotBlank()
            && budgetValide && etapesValides && brouillons.size < 10
    }

    /** Envoie la sauvegarde puis revient au carnet. */
    private fun enregistrerCircuit() {
        val demande = CircuitRequest(
            binding.editNomCircuit.text.toString().trim(),
            binding.editBudget.text.toString().toIntOrNull(),
            brouillons.map { EtapeRequest(it.destinationId, it.jours.toInt()) },
            personnel?.version
        )
        voyageViewModel.sauver(personnel?.id, demande) {
            findNavController().navigate(R.id.action_nav_editeur_circuit_to_nav_circuits)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        brouillons.clear()
        vues.clear()
        _binding = null
    }
}
