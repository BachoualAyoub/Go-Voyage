package tn.govoyage.mobile.ui.voyage

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import tn.govoyage.mobile.R
import tn.govoyage.mobile.GoVoyageApplication
import java.time.LocalDate

/** Composants natifs communs aux nouveaux ecrans, dans la palette du projet. */
abstract class CadreVoyageFragment : Fragment(R.layout.fragment_voyage_plus) {
    protected val application get() = requireActivity().application as GoVoyageApplication
    protected lateinit var formulaire: LinearLayout
    protected lateinit var resultats: LinearLayout
    protected var charge = false
    /** Prepare les deux zones sans conserver de reference apres destruction de la vue. */
    protected fun preparer(vue: View, titre: String, description: String) {
        vue.findViewById<TextView>(R.id.titre_voyage).text = titre
        vue.findViewById<TextView>(R.id.description_voyage).text = description
        formulaire = vue.findViewById(R.id.formulaire_voyage)
        resultats = vue.findViewById(R.id.resultats_voyage)
    }
    /** Affiche le retour de l'action courante. */
    protected fun message(texte: String) { view?.findViewById<TextView>(R.id.message_voyage)?.text = texte }
    /** Desactive les boutons de resultat durant une ecriture. */
    protected fun attente(actif: Boolean) {
        charge = actif
        view?.findViewById<View>(R.id.attente_voyage)?.visibility = if (actif) View.VISIBLE else View.GONE
    }
    /** Affiche un texte adapte aux petites largeurs. */
    protected fun texte(parent: LinearLayout, valeur: String, taille: Float = 14f, fort: Boolean = false): TextView {
        val champ = TextView(requireContext())
        champ.text = valeur; champ.textSize = taille; champ.setTextColor(Color.rgb(31,76,61))
        champ.setPadding(0, 8, 0, 12)
        if (fort) champ.setTypeface(null, Typeface.BOLD)
        parent.addView(champ)
        return champ
    }
    /** Cree un champ avec son libelle d'accessibilite. */
    protected fun champ(parent: LinearLayout, libelle: String, valeur: String = "", nombre: Boolean = false): EditText {
        val etiquette = texte(parent, libelle, 12f, true)
        val champ = EditText(requireContext())
        champ.id = View.generateViewId()
        etiquette.labelFor = champ.id
        champ.setSingleLine(true)
        champ.setText(valeur)
        champ.inputType = if (nombre) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        champ.layoutParams = LinearLayout.LayoutParams(-1, -2)
        parent.addView(champ)
        return champ
    }
    /** Emploie un calendrier natif pour eviter une saisie de date ambigue. */
    protected fun date(parent: LinearLayout, libelle: String, valeur: String): EditText {
        val champ = champ(parent, libelle, valeur)
        champ.isFocusable = false
        champ.setOnClickListener {
            val jour = LocalDate.parse(champ.text.toString())
            DatePickerDialog(requireContext(), { _, annee, mois, j ->
                champ.setText(LocalDate.of(annee, mois + 1, j).toString())
            }, jour.year, jour.monthValue - 1, jour.dayOfMonth).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }
        return champ
    }
    /** Cree un choix natif dans un champ de formulaire. */
    protected fun choix(parent: LinearLayout, libelle: String, valeurs: List<String>): Spinner {
        texte(parent, libelle, 12f, true)
        val champ = Spinner(requireContext())
        champ.contentDescription = libelle
        champ.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, valeurs)
        champ.minimumHeight = 110
        parent.addView(champ, LinearLayout.LayoutParams(-1, -2))
        return champ
    }
    /** Ajoute une action tactile avec garde contre les doubles clics. */
    protected fun bouton(parent: LinearLayout, libelle: String, action: () -> Unit): MaterialButton {
        val bouton = MaterialButton(requireContext())
        bouton.text = libelle; bouton.isAllCaps = false; bouton.cornerRadius = 30
        bouton.setOnClickListener { if (!charge) action() }
        parent.addView(bouton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
        return bouton
    }
    /** Encadre une offre ou un recit dans une carte claire. */
    protected fun carte(): LinearLayout {
        val zone = LinearLayout(requireContext())
        zone.orientation = LinearLayout.VERTICAL; zone.setPadding(28,28,28,28)
        zone.background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius=38f; setStroke(2, Color.rgb(222,234,225)) }
        resultats.addView(zone, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin=28 })
        return zone
    }
    /** Retourne un montant simple en dinars. */
    protected fun dinars(montant: Long) = java.text.NumberFormat.getIntegerInstance(java.util.Locale.FRANCE).format(montant) + " TND"
}
