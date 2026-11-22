package tn.govoyage.mobile.ui.voyage

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.viewModel.VolsViewModel
import java.time.LocalDate

/** Comparaison native des offres indicatives, pour un a quatre trajets. */
class VolsFragment : CadreVoyageFragment() {
    private val modele: VolsViewModel by viewModels { viewModelFactory { initializer { VolsViewModel(application.voyage) } } }
    private val villes=listOf("Paris","Lyon","Tunis","Djerba","Tozeur")
    private val champs=mutableListOf<Triple<Spinner,Spinner,EditText>>()
    /** Construit les criteres et les offres comparables par categorie. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preparer(view, "Le départ commence ici.", "Comparez, combinez, choisissez.\nOffres de démonstration par personne · Aucune billetterie réelle.")
        construireFormulaire()
        modele.message.observe(viewLifecycleOwner) { message(it) }
        modele.occupe.observe(viewLifecycleOwner) { attente(it) }
        modele.offres.observe(viewLifecycleOwner) { afficherOffres() }
    }
    /** Ajoute les types de voyage et conserve les criteres en rotation. */
    private fun construireFormulaire() {
        formulaire.removeAllViews(); champs.clear()
        for ((i,t) in modele.trajets.withIndex()) {
            texte(formulaire, "Trajet ${i+1}", 18f, true)
            val depart=choix(formulaire,"Départ",villes); depart.setSelection(villes.indexOf(t.first).coerceAtLeast(0))
            val arrivee=choix(formulaire,"Arrivée",villes); arrivee.setSelection(villes.indexOf(t.second).coerceAtLeast(0))
            val jour=date(formulaire,"Date",t.third)
            champs.add(Triple(depart,arrivee,jour))
        }
        bouton(formulaire,"＋ Ajouter un trajet (maximum 4)") {
            memoriser()
            if (modele.trajets.size<4) {
                modele.trajets.add(Triple(modele.trajets.last().second,"Tunis",LocalDate.parse(modele.trajets.last().third).plusDays(3).toString()))
                invalider(); construireFormulaire()
            }
        }
        bouton(formulaire,"Préparer un aller-retour") {
            memoriser(); val t=modele.trajets.first()
            modele.trajets=mutableListOf(t,Triple(t.second,t.first,LocalDate.parse(t.third).plusDays(7).toString()))
            invalider(); construireFormulaire()
        }
        if (modele.trajets.size>1) bouton(formulaire,"Revenir à l’aller simple") {
            memoriser(); modele.trajets=mutableListOf(modele.trajets.first()); invalider(); construireFormulaire()
        }
        bouton(formulaire,"Comparer les offres") { memoriser(); modele.chercher() }
    }
    /** Lit les criteres avant toute modification du nombre de trajets. */
    private fun memoriser() {
        modele.trajets=champs.map { Triple(it.first.selectedItem.toString(),it.second.selectedItem.toString(),it.third.text.toString()) }.toMutableList()
    }
    /** Efface une comparaison devenue obsolete. */
    private fun invalider() { modele.choix.clear(); modele.offres.value=emptyList() }
    /** Reproduit les cartes en montrant la selection de chaque etape. */
    private fun afficherOffres() {
        resultats.removeAllViews()
        var total=0L
        for (vol in modele.choix.values) total+=vol.prix
        texte(resultats,"Sélection : ${dinars(total)} · ${modele.choix.size}/${modele.trajets.size} trajet(s)",22f,true)
        for ((index, liste) in (modele.offres.value ?: emptyList()).withIndex()) {
            val trajet=modele.trajets[index]
            texte(resultats,"${trajet.first} → ${trajet.second} · ${trajet.third}",18f,true)
            if (liste.isEmpty()) texte(resultats,"Aucune offre. Essayez une autre ville.")
            for (vol in liste) {
                val zone=carte()
                texte(zone,vol.compagnie,20f,true)
                texte(zone,"${vol.heure} · ${vol.duree} min\n${vol.categorie.replace('_',' ')} · ${vol.bagage}")
                texte(zone,dinars(vol.prix),24f,true)
                bouton(zone,if (modele.choix[index]?.id==vol.id) "✓ Sélectionné" else "Sélectionner") {
                    val criteres=modele.trajets.toList()
                    memoriser()
                    if(criteres!=modele.trajets) {
                        invalider(); message("Les critères ont changé. Comparez à nouveau les offres.")
                    } else { modele.choix[index]=vol; afficherOffres() }
                }
            }
        }
    }
}
