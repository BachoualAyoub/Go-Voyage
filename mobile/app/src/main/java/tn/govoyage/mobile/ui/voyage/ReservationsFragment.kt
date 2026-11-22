package tn.govoyage.mobile.ui.voyage

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.viewModel.SejourViewModel
import java.time.LocalDate

/** Carnet partage des reservations avec lecture locale et annulation distante. */
class ReservationsFragment : CadreVoyageFragment() {
    private val modele: SejourViewModel by viewModels { viewModelFactory { initializer { SejourViewModel(application.voyage) } } }
    /** Affiche uniquement le carnet du compte courant. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preparer(view, "Mes réservations", "Toutes vos prochaines escales, du web à votre poche.")
        bouton(formulaire, "Actualiser le carnet") { modele.carnet() }
        modele.message.observe(viewLifecycleOwner) { message(it) }
        modele.occupe.observe(viewLifecycleOwner) { attente(it) }
        modele.reservations.observe(viewLifecycleOwner) { liste ->
            resultats.removeAllViews()
            if (liste.isEmpty()) texte(resultats, "Votre première escale vous attend. Retrouvez nos offres dans Séjours.")
            for (r in liste) {
                val zone=carte()
                texte(zone, "GO VOYAGE · GV-${r.id}", 12f, true)
                texte(zone, r.hotel, 22f, true)
                texte(zone, "${r.ville}\n${r.arrivee} → ${r.depart}\n${r.voyageurs} voyageurs · ${r.chambres} chambres")
                texte(zone, dinars(r.montant), 26f, true)
                texte(zone, if (r.statut=="CONFIRMEE") "Séjour confirmé" else "Séjour annulé", 13f, true)
                if (r.statut=="CONFIRMEE" && r.arrivee>LocalDate.now().toString()) bouton(zone, "Annuler ce séjour") {
                    AlertDialog.Builder(requireContext()).setTitle("Annuler votre réservation ?")
                        .setMessage(r.hotel).setPositiveButton("Confirmer l’annulation") { _, _ -> modele.annuler(r.id) }
                        .setNegativeButton("Conserver", null).show()
                }
            }
        }
        modele.carnet()
    }
}
