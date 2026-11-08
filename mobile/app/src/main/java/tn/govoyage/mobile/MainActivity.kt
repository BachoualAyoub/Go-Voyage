package tn.govoyage.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** Applique les couleurs Vichy aux roles de Material 3. */
@Composable
private fun paletteVichy() = lightColorScheme(
    primary = colorResource(R.color.palette_turquoise_fonce),
    onPrimary = colorResource(R.color.palette_blanc),
    primaryContainer = colorResource(R.color.palette_turquoise),
    onPrimaryContainer = colorResource(R.color.palette_encre),
    inversePrimary = colorResource(R.color.palette_turquoise),
    secondary = colorResource(R.color.palette_texte_secondaire),
    onSecondary = colorResource(R.color.palette_blanc),
    secondaryContainer = colorResource(R.color.palette_argent),
    onSecondaryContainer = colorResource(R.color.palette_encre),
    background = colorResource(R.color.palette_fond),
    onBackground = colorResource(R.color.palette_encre),
    surface = colorResource(R.color.palette_blanc),
    onSurface = colorResource(R.color.palette_encre),
    surfaceVariant = colorResource(R.color.palette_gris_clair),
    onSurfaceVariant = colorResource(R.color.palette_texte_secondaire),
    outline = colorResource(R.color.palette_gris),
    outlineVariant = colorResource(R.color.palette_argent),
    surfaceTint = colorResource(R.color.palette_turquoise_fonce)
)

/** Point d'entree de l'application Android GO VOYAGE. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme = paletteVichy()) { EspaceVoyage() } }
    }
}

/** Choisit l'ecran selon l'etat de connexion. */
@Composable
private fun EspaceVoyage(etat: CompteViewModel = viewModel()) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { marge ->
        Box(
            modifier = Modifier.fillMaxSize().padding(marge),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_go_voyage),
                    contentDescription = "GO VOYAGE",
                    modifier = Modifier.width(200.dp).height(65.dp)
                )
                val compte = etat.compte
                if (compte == null) ConnexionScreen(etat)
                else ProfilScreen(compte, etat::deconnecter)
            }
        }
    }
}

/** Affiche les coordonnees du compte et son role. */
@Composable
private fun ProfilScreen(compte: UtilisateurResponse, deconnecter: () -> Unit) {
    val estAdmin = compte.role == "ADMIN"
    Text(
        if (estAdmin) "ESPACE ADMINISTRATEUR" else "ESPACE VOYAGEUR",
        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary
    )
    Text("Bonjour, ${compte.prenom}.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mon compte", style = MaterialTheme.typography.titleLarge)
            InfoProfil("Nom complet", "${compte.prenom} ${compte.nom}")
            InfoProfil("Adresse e-mail", compte.email)
            InfoProfil("Téléphone", compte.tel)
            InfoProfil("Rôle", if (estAdmin) "Administrateur" else "Utilisateur")
            if (estAdmin) Text("La gestion des comptes se fait dans l’espace web.")
        }
    }
    OutlinedButton(onClick = deconnecter, modifier = Modifier.fillMaxWidth()) { Text("Déconnexion") }
}

/** Affiche une information de profil. */
@Composable
private fun InfoProfil(titre: String, valeur: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titre, style = MaterialTheme.typography.labelMedium)
        Text(valeur, style = MaterialTheme.typography.bodyLarge)
    }
}
