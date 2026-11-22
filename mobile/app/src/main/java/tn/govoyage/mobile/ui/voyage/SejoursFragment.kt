package tn.govoyage.mobile.ui.voyage

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tn.govoyage.mobile.ui.imageDestination
import tn.govoyage.mobile.viewModel.SejourViewModel

/** Recherche et confirmation d'une reservation sur le meme serveur que le web. */
class SejoursFragment : CadreVoyageFragment() {
    private val modele: SejourViewModel by viewModels { viewModelFactory { initializer { SejourViewModel(application.voyage) } } }
    /** Prepare les dates et observe les resultats sans reconstruire le formulaire. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preparer(view, "Votre prochaine escale", "Un lieu pour ralentir. Un séjour à vous.\nCatalogue de démonstration · TND · Sans paiement.")
        val debut = date(formulaire, "Arrivée", modele.arrivee)
        val fin = date(formulaire, "Départ", modele.depart)
        val voyageurs = champ(formulaire, "Voyageurs", modele.voyageurs.toString(), true)
        val chambres = champ(formulaire, "Chambres", modele.chambres.toString(), true)
        bouton(formulaire, "Explorer les séjours") {
            modele.arrivee=debut.text.toString(); modele.depart=fin.text.toString()
            modele.voyageurs=voyageurs.text.toString().toIntOrNull() ?: 0
            modele.chambres=chambres.text.toString().toIntOrNull() ?: 0
            modele.chercher()
        }
        modele.message.observe(viewLifecycleOwner) { message(it) }
        modele.occupe.observe(viewLifecycleOwner) { attente(it) }
        modele.hotels.observe(viewLifecycleOwner) { hotels ->
            resultats.removeAllViews()
            if (hotels.isEmpty()) texte(resultats, "Choisissez vos dates pour voir les disponibilités.")
            for (hotel in hotels) {
                val zone=carte()
                val image=ImageView(requireContext())
                image.setImageResource(imageDestination(hotel.image))
                image.scaleType=ImageView.ScaleType.CENTER_CROP
                image.contentDescription=hotel.ville
                zone.addView(image, LinearLayout.LayoutParams(-1, 360))
                texte(zone, "★".repeat(hotel.etoiles) + " · " + hotel.ville, 12f)
                texte(zone, hotel.nom, 22f, true)
                texte(zone, hotel.description)
                texte(zone, "${hotel.disponibles} chambres disponibles · ${hotel.capacite} personnes / chambre")
                texte(zone, dinars(hotel.total), 26f, true)
                texte(zone, dinars(hotel.tarifNuit) + " / chambre / nuit", 12f)
                bouton(zone, if (hotel.reservable) "Choisir ce séjour" else "Indisponible") {
                    AlertDialog.Builder(requireContext()).setTitle("Confirmer votre séjour ?")
                        .setMessage("${hotel.nom}\n${modele.arrivee} → ${modele.depart}\n${modele.voyageurs} voyageurs · ${modele.chambres} chambres\n${dinars(hotel.total)}\nAnnulation avant le début du séjour.")
                        .setPositiveButton("Confirmer") { _, _ -> modele.reserver(hotel) }
                        .setNegativeButton("Revenir", null).show()
                }.isEnabled=hotel.reservable
            }
        }
        if (modele.hotels.value.isNullOrEmpty()) modele.chercher()
    }
}
