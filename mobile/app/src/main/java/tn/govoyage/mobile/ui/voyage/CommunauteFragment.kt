package tn.govoyage.mobile.ui.voyage

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import tn.govoyage.mobile.data.remote.adresseApi
import tn.govoyage.mobile.model.*
import tn.govoyage.mobile.viewModel.CommunauteViewModel
import java.net.URL

/** Livre d'or natif : medias, avis, likes, recherche et signalement. */
class CommunauteFragment : CadreVoyageFragment() {
    private val modele: CommunauteViewModel by viewModels { viewModelFactory { initializer { CommunauteViewModel(application.voyage) } } }
    private var media: Map<String,String>? = null
    private val choisirMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importer(uri)
    }
    /** Installe la recherche et les actions de publication. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preparer(view, "Les histoires restent.", "Le livre d’or des voyageurs. Partagez une photo, une vidéo et vos impressions.")
        val recherche=champ(formulaire,"Pays ou ville",modele.recherche)
        bouton(formulaire,"Explorer les récits") { modele.recherche=recherche.text.toString(); modele.charger() }
        bouton(formulaire,"Importer une photo ou vidéo (10 Mo)") { choisirMedia.launch("*/*") }
        bouton(formulaire,"＋ Raconter mon voyage") { ecrire() }
        modele.message.observe(viewLifecycleOwner) { message(it) }
        modele.occupe.observe(viewLifecycleOwner) { attente(it) }
        modele.recits.observe(viewLifecycleOwner) { liste ->
            resultats.removeAllViews()
            if (liste.isEmpty()) texte(resultats,"Une page à écrire. Aucun récit pour cette recherche.")
            for (recit in liste) afficher(recit)
        }
        modele.charger()
    }
    /** Transfere le fichier limite sans bloquer l'interface. */
    private fun importer(uri: Uri) = modele.executer {
        val contexte=requireContext().applicationContext
        val type=contexte.contentResolver.getType(uri).orEmpty()
        require(type in listOf("image/jpeg","image/png","video/mp4")) { "Choisissez une photo JPG/PNG ou une vidéo MP4." }
        val contenu=withContext(Dispatchers.IO) {
            contexte.contentResolver.openInputStream(uri)?.use { it.readNBytes(10_000_001) }
                ?: error("Le fichier ne peut pas être lu.")
        }
        require(contenu.size<=10_000_000) { "Le média est limité à 10 Mo." }
        val fichier=MultipartBody.Part.createFormData("fichier","souvenir",contenu.toRequestBody(type.toMediaType()))
        media=modele.depot.importer(fichier)
        modele.message.value="Média importé. Ajoutez-le à votre prochain récit."
    }
    /** Recueille les libelles francais du recit avant envoi. */
    private fun ecrire() {
        val zone=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        val titre=champ(zone,"Titre")
        val pays=champ(zone,"Pays","Tunisie")
        val ville=champ(zone,"Ville")
        val texte=champ(zone,"Votre récit").apply { setSingleLine(false); minLines=3; maxLines=6 }
        texte(zone, if (media==null) "Sans média. Vous pouvez d’abord importer une photo ou une vidéo." else "Le média importé sera joint à ce récit.",12f)
        val defiler=ScrollView(requireContext()).apply { addView(zone) }
        val dialogue=AlertDialog.Builder(requireContext()).setTitle("Raconter mon voyage").setView(defiler)
            .setPositiveButton("Publier",null).setNegativeButton("Annuler",null).create()
        dialogue.setOnShowListener {
            dialogue.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (listOf(titre,pays,ville,texte).any { it.text.isBlank() }) { titre.error="Complétez tous les champs."; return@setOnClickListener }
                modele.publier(RecitRequest(titre.text.toString(),texte.text.toString(),pays.text.toString(),ville.text.toString(),media?.get("url"),media?.get("type")))
                media=null
                dialogue.dismiss()
            }
        }
        dialogue.show()
    }
    /** Affiche une carte et les interactions rattachees au compte courant. */
    private fun afficher(recit: Publication) {
        val zone=carte()
        texte(zone,"${recit.ville} · ${recit.pays}",12f,true)
        texte(zone,recit.titre,23f,true)
        if (recit.mediaUrl != null) afficherMedia(zone,recit)
        texte(zone,recit.texte)
        texte(zone,"Par ${recit.auteur} · ★ ${"%.1f".format(recit.note)} · ${recit.avis} avis",12f)
        bouton(zone,(if (recit.aime>0) "♥" else "♡") + " ${recit.likes} · J’aime") { modele.executer {
            modele.depot.connecte { modele.depot.api.aimer(recit.id,modele.depot.etat(recit.aime==0)) }
            modele.actualiser()
        } }
        bouton(zone,"Lire les avis et donner mon avis") { avis(recit) }
        bouton(zone,"Signaler ce récit") { signaler(recit.id) }
        if (recit.auteurId==modele.depot.compte()) bouton(zone,"Retirer mon récit") {
            AlertDialog.Builder(requireContext()).setTitle("Retirer ce récit ?").setMessage("Cette suppression est définitive.")
                .setPositiveButton("Retirer") { _,_ -> modele.executer {
                    modele.depot.connecte { modele.depot.api.supprimer(recit.id) }; modele.actualiser()
                } }.setNegativeButton("Conserver",null).show()
        }
    }
    /** Charge la photo publiee ou confie une video au lecteur du telephone. */
    private fun afficherMedia(zone: LinearLayout, recit: Publication) {
        val url=adresseApi().trimEnd('/') + recit.mediaUrl
        if (recit.mediaType=="VIDEO") {
            bouton(zone,"▶ Lire la vidéo") {
                val lecteur=VideoView(requireContext())
                lecteur.layoutParams=LinearLayout.LayoutParams(-1,650)
                val commandes=MediaController(requireContext())
                commandes.setAnchorView(lecteur)
                lecteur.setMediaController(commandes)
                lecteur.setVideoURI(Uri.parse(url))
                val dialogue=AlertDialog.Builder(requireContext()).setTitle(recit.titre).setView(lecteur)
                    .setPositiveButton("Fermer",null).create()
                dialogue.setOnDismissListener { lecteur.stopPlayback() }
                lecteur.setOnPreparedListener { lecteur.start() }
                lecteur.setOnErrorListener { _,_,_ -> message("La vidéo ne peut pas être lue. Vérifiez votre connexion."); dialogue.dismiss(); true }
                dialogue.show()
            }
            return
        }
        val image=ImageView(requireContext())
        image.contentDescription=recit.titre; image.scaleType=ImageView.ScaleType.CENTER_CROP
        zone.addView(image,LinearLayout.LayoutParams(-1,420))
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap=withContext(Dispatchers.IO) {
                try { URL(url).openStream().use { BitmapFactory.decodeStream(it) } } catch (_: java.io.IOException) { null }
            }
            if (bitmap!=null) image.setImageBitmap(bitmap) else image.visibility=View.GONE
        }
    }
    /** Lit les avis puis propose une note unique par compte. */
    private fun avis(recit: Publication) = modele.executer {
        val liste=modele.depot.api.avis(recit.id)
        val zone=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        for (avis in liste) texte(zone,"${avis.auteur} · ${"★".repeat(avis.note)}\n${avis.texte}")
        if (liste.isEmpty()) texte(zone,"Soyez le premier à laisser un avis.")
        val note=choix(zone,"Votre note",listOf("5","4","3","2","1"))
        val commentaire=champ(zone,"Votre avis").apply { setSingleLine(false) }
        AlertDialog.Builder(requireContext()).setTitle(recit.titre).setView(ScrollView(requireContext()).apply { addView(zone) })
            .setPositiveButton("Enregistrer") { _,_ ->
                modele.executer {
                    val d=modele.depot.texte("texte",commentaire.text.toString())
                    d.addProperty("note",note.selectedItem.toString().toInt())
                    modele.depot.connecte { modele.depot.api.commenter(recit.id,d) }; modele.actualiser()
                }
            }.setNegativeButton("Fermer",null).show()
    }
    /** Un signalement transmet un motif au moderateur. */
    private fun signaler(id: Long) {
        val saisie=EditText(requireContext()).apply { hint="Motif du signalement"; maxLines=4 }
        AlertDialog.Builder(requireContext()).setTitle("Signaler ce contenu").setView(saisie)
            .setPositiveButton("Envoyer") { _,_ -> modele.executer {
                modele.depot.connecte { modele.depot.api.signaler(id,modele.depot.texte("motif",saisie.text.toString())) }
                modele.message.value="Signalement transmis à l’administrateur."
            } }.setNegativeButton("Annuler",null).show()
    }
}
