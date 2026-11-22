package tn.govoyage.mobile.ui.voyage

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.gson.Gson
import com.google.gson.JsonObject
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.viewModel.PilotageViewModel

/** Administration native des hotels, destinations et recits avec graphiques reels. */
class PilotageFragment : CadreVoyageFragment() {
    private val modele: PilotageViewModel by viewModels { viewModelFactory { initializer { PilotageViewModel(application.voyage) } } }
    /** Reserve l'ecran au profil administrateur ; le serveur verifie aussi le role. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preparer(view,"Le pouls de GO VOYAGE","Indicateurs, catalogue et modération. Données partagées avec le site.")
        if (application.auth.utilisateurCourant()?.role!="ADMIN") { message("Accès réservé aux administrateurs."); return }
        for ((id,libelle) in listOf("synthese" to "Vue d’ensemble","hotels" to "Hôtels","destinations" to "Destinations","moderation" to "Modération"))
            bouton(formulaire,libelle) { modele.onglet=id; afficher() }
        bouton(formulaire,"Actualiser les données") { modele.charger() }
        modele.message.observe(viewLifecycleOwner) { message(it) }
        modele.occupe.observe(viewLifecycleOwner) { attente(it) }
        modele.statistiques.observe(viewLifecycleOwner) { afficher() }
        modele.charger()
    }
    /** Remplace la seule zone de resultats selon la rubrique choisie. */
    private fun afficher() {
        resultats.removeAllViews()
        when(modele.onglet) {
            "synthese" -> synthese()
            "hotels" -> hotels()
            "destinations" -> destinations()
            "moderation" -> moderation()
        }
    }
    /** Graphique anime dont les valeurs viennent des reservations confirmees. */
    private fun synthese() {
        val stats=modele.statistiques.value ?: return
        for ((cle,titre) in listOf("reservations" to "Réservations confirmées","montant" to "Montant confirmé (TND)","comptes" to "Voyageurs inscrits","publications" to "Récits publiés")) {
            val zone=carte(); texte(zone,titre,13f); texte(zone,stats[cle].asString,32f,true)
        }
        texte(resultats,"Les escales les plus choisies",20f,true)
        val villes=stats.getAsJsonArray("parVille")
        if (villes.isEmpty) texte(resultats,"Les premières réservations feront apparaître le graphique.")
        else {
            val donnees=villes.map { it.asJsonObject }.map { it["ville"].asString to it["reservations"].asInt }
            val graphique=BarresVoyageView(requireContext(),donnees)
            resultats.addView(graphique,LinearLayout.LayoutParams(-1,donnees.size*130+40))
        }
        texte(resultats,"Montants confirmés, sans encaissement en ligne. Les annulations sont exclues des totaux.",12f)
    }
    /** Liste les hotels et propose creation, modification et archivage. */
    private fun hotels() {
        bouton(resultats,"＋ Ajouter un hôtel") { formulaireHotel(null) }
        for (h in modele.hotels.value.orEmpty()) {
            val zone=carte(); texte(zone,h["nom"].asString,21f,true)
            texte(zone,h["ville"].asString+" · "+dinars(h["tarif_nuit"].asLong)+" / nuit")
            texte(zone,if(h["actif"].asBoolean) "En ligne" else "Archivé")
            bouton(zone,"Modifier") { formulaireHotel(h) }
            if(h["actif"].asBoolean) bouton(zone,"Archiver") {
                confirmer("Archiver cet hôtel ?") { modele.depot.api.archiverHotel(h["id_h"].asLong) }
            }
        }
    }
    /** Formulaire natif avec les memes bornes validees par le serveur. */
    private fun formulaireHotel(h: JsonObject?) {
        val zone=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        val destinations=modele.destinations.value.orEmpty()
        if(destinations.isEmpty()) { message("Ajoutez une destination avant l’hôtel."); return }
        val destination=choix(zone,"Destination",destinations.map { it.nom })
        val index=destinations.indexOfFirst { it.id==h?.get("destination_id")?.asLong }
        if(index>=0) destination.setSelection(index)
        val champs=linkedMapOf<String,EditText>()
        for ((cle,titre,base) in listOf(Triple("nom","Nom",""),Triple("ville","Ville",""),Triple("description","Description",""),
            Triple("etoiles","Étoiles (1–5)","3"),Triple("tarifNuit","Prix par nuit (TND)","100"),
            Triple("chambres","Chambres (1–500)","5"),Triple("capacite","Personnes par chambre (1–10)","2"))) {
            val source=if(cle=="tarifNuit") "tarif_nuit" else cle
            champs[cle]=champ(zone,titre,h?.get(source)?.asString ?: base, cle in listOf("etoiles","tarifNuit","chambres","capacite"))
        }
        val codes=listOf("sidi-bou-said","hammamet","djerba","tozeur","tabarka","douz","el-jem","kairouan")
        val image=choix(zone,"Photographie",codes)
        image.setSelection(codes.indexOf(h?.get("image")?.asString).coerceAtLeast(0))
        val actif=CheckBox(requireContext()).apply { text="Visible dans le catalogue"; isChecked=h?.get("actif")?.asBoolean ?: true }
        zone.addView(actif)
        AlertDialog.Builder(requireContext()).setTitle(if(h==null) "Nouvel hôtel" else "Modifier l’hôtel")
            .setView(ScrollView(requireContext()).apply { addView(zone) }).setNegativeButton("Annuler",null)
            .setPositiveButton("Enregistrer") { _,_ -> modele.executer {
                val d=JsonObject()
                for((cle,champ) in champs) {
                    if(cle in listOf("etoiles","tarifNuit","chambres","capacite")) d.addProperty(cle,champ.text.toString().toIntOrNull() ?: 0)
                    else d.addProperty(cle,champ.text.toString())
                }
                d.addProperty("destinationId",destinations[destination.selectedItemPosition].id)
                d.addProperty("image",image.selectedItem.toString()); d.addProperty("actif",actif.isChecked)
                if(h!=null) d.addProperty("version",h["version"].asLong)
                modele.depot.connecte { if(h==null) modele.depot.api.ajouterHotel(d) else modele.depot.api.modifierHotel(h["id_h"].asLong,d) }
                modele.actualiser()
            } }.show()
    }
    /** Les fiches restent compatibles avec les circuits deja presents. */
    private fun destinations() {
        bouton(resultats,"＋ Ajouter une destination") { formulaireDestination(null) }
        for(d in modele.destinations.value.orEmpty()) {
            val zone=carte(); texte(zone,d.nom,21f,true); texte(zone,"${d.ville} · ${d.pays}")
            bouton(zone,"Modifier") { formulaireDestination(d) }
            bouton(zone,"Supprimer si inutilisée") { confirmer("Supprimer cette destination ?") { modele.depot.api.supprimerDestination(d.id) } }
        }
    }
    /** Edition des attributs de destination sans modifier les anciens circuits. */
    private fun formulaireDestination(destination: Destination?) {
        val zone=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        val source=if(destination==null) JsonObject() else Gson().toJsonTree(destination).asJsonObject
        val champs=linkedMapOf<String,EditText>()
        for((cle,libelle) in listOf("nom" to "Nom","code" to "Code en minuscules avec tirets","ville" to "Ville",
            "pays" to "Pays","region" to "Région","accroche" to "Accroche","description" to "Description","tarifJourTnd" to "Tarif journalier (TND)"))
            champs[cle]=champ(zone,libelle,source[cle]?.asString ?: if(cle=="pays") "Tunisie" else "",cle=="tarifJourTnd")
        val ambiances=listOf("MER","CULTURE","NATURE")
        val ambiance=choix(zone,"Ambiance",ambiances)
        ambiance.setSelection(ambiances.indexOf(destination?.ambiance).coerceAtLeast(0))
        AlertDialog.Builder(requireContext()).setTitle("Fiche destination").setView(ScrollView(requireContext()).apply { addView(zone) })
            .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer") { _,_ -> modele.executer {
                val d=JsonObject()
                for((cle,champ) in champs) if(cle=="tarifJourTnd") d.addProperty(cle,champ.text.toString().toIntOrNull() ?: 0) else d.addProperty(cle,champ.text.toString())
                d.addProperty("ambiance",ambiance.selectedItem.toString())
                modele.depot.connecte { if(destination==null) modele.depot.api.ajouterDestination(d) else modele.depot.api.modifierDestination(destination.id,d) }
                modele.actualiser()
            } }.show()
    }
    /** Examine les signalements et masque ou retablit le contenu. */
    private fun moderation() {
        for(p in modele.publications.value.orEmpty()) {
            val zone=carte()
            texte(zone,p["titre"].asString,21f,true); texte(zone,p["texte"].asString)
            texte(zone,p["statut"].asString+" · "+p["signalements"].asInt+" signalement(s)")
            bouton(zone,"Examiner les motifs") { modele.executer {
                val motifs=modele.depot.connecte { modele.depot.api.motifs(p["id_p"].asLong) }
                AlertDialog.Builder(requireContext()).setTitle("Signalements")
                    .setMessage(if(motifs.isEmpty()) "Aucun signalement." else motifs.joinToString("\n\n") { it["motif"].asString+" · "+it["statut"].asString })
                    .setPositiveButton("Fermer",null).show()
            } }
            val visible=p["statut"].asString=="PUBLIEE"
            bouton(zone,if(visible) "Masquer le récit" else "Rétablir le récit") { confirmer("Appliquer cette décision ?") {
                modele.depot.api.moderer(p["id_p"].asLong,modele.depot.etat(!visible))
            } }
        }
    }
    /** Demande une confirmation concrete avant une suppression ou moderation. */
    private fun confirmer(titre: String, action: suspend () -> Unit) {
        AlertDialog.Builder(requireContext()).setTitle(titre).setPositiveButton("Confirmer") { _,_ ->
            modele.executer { modele.depot.connecte { action() }; modele.actualiser() }
        }.setNegativeButton("Annuler",null).show()
    }
}
