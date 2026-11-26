package tn.govoyage.mobile.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import tn.govoyage.mobile.R
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.Destination


private val cacheImages = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 8).toInt()
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}


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


fun imageCircuit(circuit: Circuit): Int {
    val premiere = circuit.etapes.firstOrNull() ?: return R.drawable.voyage_sidi
    return imageDestination(codeDestination(premiere.destinationId))
}


fun afficherImageCircuit(image: ImageView, circuit: Circuit) {
    afficherPhoto(image, imageCircuit(circuit))
}


fun afficherImageDestination(image: ImageView, destination: Destination) {
    afficherPhoto(image, imageDestination(destination.code))
}


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
