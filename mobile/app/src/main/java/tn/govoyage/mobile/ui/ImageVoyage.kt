package tn.govoyage.mobile.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import tn.govoyage.mobile.R
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.Destination

/** Petit cache memoire : les photos sont decodees une seule fois par taille d'affichage. */
private val cacheImages = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 8).toInt()
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

/** Photo du catalogue associee au code d'une destination. */
fun imageDestination(code: String): Int = when (code) {
    "djerba" -> R.drawable.destination_djerba
    "douz" -> R.drawable.destination_douz
    "el-jem" -> R.drawable.destination_el_jem
    "hammamet" -> R.drawable.destination_hammamet
    "kairouan" -> R.drawable.destination_kairouan
    "sidi-bou-said" -> R.drawable.destination_sidi_bou_said
    "tabarka" -> R.drawable.destination_tabarka
    "tozeur" -> R.drawable.destination_tozeur
    else -> R.drawable.voyage_sidi
}

// Les identifiants suivent le catalogue insere par Flyway.
private fun codeDestination(id: Long): String = when (id) {
    1L -> "sidi-bou-said"
    2L -> "hammamet"
    3L -> "djerba"
    4L -> "kairouan"
    5L -> "tozeur"
    6L -> "tabarka"
    7L -> "el-jem"
    8L -> "douz"
    else -> ""
}

/** Photo d'un circuit : celle de sa premiere etape. */
fun imageCircuit(circuit: Circuit): Int {
    val premiere = circuit.etapes.firstOrNull() ?: return R.drawable.voyage_sidi
    return imageDestination(codeDestination(premiere.destinationId))
}

/** Affiche une photo de circuit en sous-echantillonnant selon la vue. */
fun afficherImageCircuit(image: ImageView, circuit: Circuit) {
    afficherPhoto(image, imageCircuit(circuit))
}

/** Affiche la photo d'une destination du catalogue. */
fun afficherImageDestination(image: ImageView, destination: Destination) {
    afficherPhoto(image, imageDestination(destination.code))
}

/** Decode la ressource a la taille utile puis la met en cache. */
private fun afficherPhoto(image: ImageView, ressource: Int) {
    val largeur = if (image.width > 0) image.width else image.context.resources.displayMetrics.widthPixels
    val hauteur = if (image.height > 0) image.height else largeur / 2
    val cle = "$ressource-$largeur-$hauteur"
    val photo = cacheImages.get(cle) ?: decoderPhoto(image.context, ressource, largeur, hauteur)
    if (photo == null) {
        image.setImageResource(ressource)
        return
    }
    cacheImages.put(cle, photo)
    image.setImageBitmap(photo)
}

/** Lit les dimensions puis ne garde que les octets necessaires a l'affichage. */
private fun decoderPhoto(context: Context, ressource: Int, largeur: Int, hauteur: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.resources.openRawResource(ressource).use {
        BitmapFactory.decodeStream(it, null, options)
    }
    options.inSampleSize = calculerEchantillon(options.outWidth, options.outHeight, largeur, hauteur)
    options.inJustDecodeBounds = false
    return context.resources.openRawResource(ressource).use {
        BitmapFactory.decodeStream(it, null, options)
    }
}

/** Divise la taille source par deux jusqu'a rester proche de la vue. */
private fun calculerEchantillon(
    largeurSource: Int,
    hauteurSource: Int,
    largeurCible: Int,
    hauteurCible: Int
): Int {
    var echantillon = 1
    var largeur = largeurSource.coerceAtLeast(1)
    var hauteur = hauteurSource.coerceAtLeast(1)
    while (largeur / 2 >= largeurCible && hauteur / 2 >= hauteurCible) {
        echantillon *= 2
        largeur /= 2
        hauteur /= 2
    }
    return echantillon
}
