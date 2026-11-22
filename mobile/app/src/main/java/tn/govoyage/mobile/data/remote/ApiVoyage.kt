package tn.govoyage.mobile.data.remote

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonParseException
import retrofit2.HttpException
import tn.govoyage.mobile.model.ErreurResponse
import java.io.IOException

/** Port du backend GO VOYAGE lance sur le PC. */
const val API_PORT = 8081

/** Adresse du PC sur le Wi-Fi : a mettre a jour si elle change. */
private const val LOCAL_IP = "192.168.1.4"

/** Adresse du backend pour un emulateur ou un telephone physique. */
fun adresseApi(): String {
    val configuration = tn.govoyage.mobile.BuildConfig.GO_VOYAGE_API_URL
    if (configuration.isNotBlank()) return configuration.trimEnd('/') + "/"
    val empreinte = Build.FINGERPRINT ?: ""
    val modele = Build.MODEL ?: ""
    val emulateur = empreinte.contains("generic") || empreinte.contains("emulator") ||
        modele.contains("sdk") || modele.contains("Emulator")
    return if (emulateur) "http://10.0.2.2:$API_PORT/" else "http://$LOCAL_IP:$API_PORT/"
}

/** Affiche un message lisible a partir d'une reponse HTTP. */
fun lireErreur(cause: HttpException): String {
    val defaut = "La requete a echoue (${cause.code()})."
    return try {
        val json = cause.response()?.errorBody()?.string()
        val retour = Gson().fromJson(json, ErreurResponse::class.java)
        val details = retour?.erreurs?.values?.joinToString("\n").orEmpty()
        listOf(retour?.message ?: defaut, details).filter { it.isNotEmpty() }.joinToString("\n")
    } catch (_: JsonParseException) {
        defaut
    } catch (_: IOException) {
        defaut
    }
}
