package tn.govoyage.mobile.lite.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tn.govoyage.mobile.lite.model.Circuit
import tn.govoyage.mobile.lite.model.Destination
import tn.govoyage.mobile.lite.model.Etape
import tn.govoyage.mobile.lite.model.SaisieEtape

/** Catalogue, circuits proposes et carnet du compte, tout en memoire, comme le TP5. */
class VoyageViewModel : ViewModel() {

    // Catalogue fictif de huit destinations tunisiennes.
    private val catalogue = mutableListOf(
        Destination(1, "SID", "Sidi Bou Saïd", "Sidi Bou Saïd", "Tunisie", "Grand Tunis", "CULTURE",
            85, "Le village blanc et bleu face à la mer.",
            "Perché sur une falaise, Sidi Bou Saïd séduit par ses murs blancs, ses portes bleues et ses cafés donnant sur le golfe de Tunis. Les ruelles descendant vers le port sont un régal pour les promenades du matin."),
        Destination(2, "TUN", "Médina de Tunis", "Tunis", "Tunisie", "Grand Tunis", "CULTURE",
            60, "Souks, zelliges et patios depuis le 8e siècle.",
            "La médina de Tunis traverse treize siècles d'histoire : souks couverts, mosquées, mausolées et fondouks. Chaque ruelle mène à un artisanat vivant, du cuivre au chéchias."),
        Destination(3, "CAR", "Carthage", "Tunis", "Tunisie", "Grand Tunis", "CULTURE",
            70, "La cité punique et ses thermes romains.",
            "Ancienne rivale de Rome, Carthage garde ses thermes d'Antonin, son théâtre et son tophet. Le site se visite lentement, entre vestiges et vue sur la mer."),
        Destination(4, "HAM", "Hammamet", "Hammamet", "Tunisie", "Nord-Est", "MER",
            110, "Plages douces et jasmins au bord de la mer.",
            "Sable fin, citadelle espagnole et jardins de jasmin : Hammamet réunit la baignade facile et les sorties du soir le long de la marina de Yasmine."),
        Destination(5, "MON", "Monastir", "Monastir", "Tunisie", "Sahel", "MER",
            95, "Ribat ancestral et marina familiale.",
            "Entre son ribat fortifié, son mausolée et sa longue corniche, Monastir alterne culture et baignade. La marina propose des balades en mer vers les plages voisines."),
        Destination(6, "SOU", "Sousse", "Sousse", "Tunisie", "Sahel", "CULTURE",
            75, "La perle du Sahel, médina et remparts.",
            "La médina de Sousse, classée au patrimoine mondial, ceinture une ville vivante. La grande mosquée, les catacombes et le port commercial racontent le passé marchand du Sahel."),
        Destination(7, "TOZ", "Tozeur", "Tozeur", "Tunisie", "Sud-Ouest", "NATURE",
            120, "Palmeraie, oases et décors de Star Wars.",
            "Porte du désert, Tozeur vit au rythme de sa grande palmeraie et de son quartier Ouled el Hadef en briques ochres. Les excursions mènent aux oasis de montagne de Chebika et Tamerza."),
        Destination(8, "DJE", "Djerba", "Houmt Souk", "Tunisie", "Sud-Est", "MER",
            105, "L'île aux rêves : plages et villages de potiers.",
            "L'île de Djerba offre des plages de sable clair, la lagune, la synagogue de la Ghriba et les villages de potiers de Guellala. Les dattes et le poisson grillé y sont un festin.")
    )

    private val _destinations = MutableLiveData<List<Destination>>()
    val destinations: LiveData<List<Destination>> = _destinations

    // Carnets en memoire : un circuit par compte, perte a la fermeture de l'app (beta).
    private val carnets = mutableMapOf<Long, MutableList<Circuit>>()
    private var compteId = -1L
    private var prochainIdCircuit = 100L

    private val _modeles = MutableLiveData<List<Circuit>>()
    val modeles: LiveData<List<Circuit>> = _modeles

    private val _circuits = MutableLiveData<List<Circuit>>()
    val circuits: LiveData<List<Circuit>> = _circuits

    private val _destinationSelectionne = MutableLiveData<Destination?>()
    val destinationSelectionne: LiveData<Destination?> = _destinationSelectionne

    private val _circuitSelectionne = MutableLiveData<Circuit?>()
    val circuitSelectionne: LiveData<Circuit?> = _circuitSelectionne

    init {
        _destinations.value = catalogue.toList()

        // Trois circuits proposes, construits a partir du catalogue.
        _modeles.value = listOf(
            Circuit(10, "Bleu méditerranéen", "Sidi Bou Saïd et Hammamet, le duo des couleurs du Nord.", true,
                400, 4, 440, listOf(
                    Etape(1, "Sidi Bou Saïd", 2, 85, 170),
                    Etape(4, "Hammamet", 2, 110, 220)
                )),
            Circuit(11, "Perles du Sahel", "Monastir et Sousse entre ribat et médina.", true,
                null, 3, 285, listOf(
                    Etape(5, "Monastir", 1, 95, 95),
                    Etape(6, "Sousse", 2, 75, 150)
                )),
            Circuit(12, "Oasis et dunes", "Tozeur, la porte du désert.", true,
                null, 3, 390, listOf(
                    Etape(7, "Tozeur", 3, 120, 390)
                ))
        )
    }

    /** Ouvre le carnet du compte connecte (liste vide ou reprise en memoire). */
    fun ouvrirCarnet(idCompte: Long) {
        compteId = idCompte
        _circuits.value = carnets.getOrPut(idCompte) { mutableListOf() }.toList()
    }

    /** Ferme le carnet a la deconnexion. */
    fun fermerCarnet() {
        compteId = -1L
        _circuits.value = emptyList()
    }

    /** Cree ou modifie un circuit personnel du compte connecte. */
    fun sauver(idCircuit: Long?, nom: String, budgetTnd: Int?, saisies: List<SaisieEtape>): Boolean {
        if (compteId < 0) return false
        val carnet = carnets.getOrPut(compteId) { mutableListOf() }
        val etapes = construireEtapes(saisies)
        val duree = etapes.sumOf { it.jours }
        val estimation = etapes.sumOf { it.coutTnd }
        if (idCircuit != null) {
            val position = carnet.indexOfFirst { it.id == idCircuit }
            if (position < 0) return false
            carnet[position] = Circuit(idCircuit, nom, null, false, budgetTnd, duree, estimation, etapes)
        } else {
            carnet.add(Circuit(prochainIdCircuit++, nom, null, false, budgetTnd, duree, estimation, etapes))
        }
        _circuits.value = carnet.toList()
        return true
    }

    /** Retire un circuit du carnet du compte connecte. */
    fun supprimer(idCircuit: Long): Boolean {
        if (compteId < 0) return false
        val carnet = carnets[compteId] ?: return false
        val retire = carnet.removeAll { it.id == idCircuit }
        _circuits.value = carnet.toList()
        return retire
    }

    /** Transforme la saisie de l'editeur en etapes finales avec le cout calcule. */
    private fun construireEtapes(saisies: List<SaisieEtape>): List<Etape> {
        val etapes = mutableListOf<Etape>()
        for (saisie in saisies) {
            val destination = catalogue.firstOrNull { it.id == saisie.destinationId } ?: continue
            etapes.add(Etape(destination.id, destination.nom, saisie.jours, destination.tarifJourTnd,
                destination.tarifJourTnd * saisie.jours))
        }
        return etapes
    }

    /** Mémorise la destination ou le circuit ouvert dans une fiche. */
    fun selectDestination(destination: Destination?) {
        _destinationSelectionne.value = destination
    }

    fun selectCircuit(circuit: Circuit?) {
        _circuitSelectionne.value = circuit
    }
}
