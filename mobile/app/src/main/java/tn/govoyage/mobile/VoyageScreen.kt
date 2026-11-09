package tn.govoyage.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.Normalizer
import java.util.Locale

/** Onglets principaux pour explorer, composer et acceder au compte. */
private enum class OngletVoyage(val libelle: String, val symbole: String) {
    EXPLORER("Explorer", "⌖"), CIRCUITS("Mes circuits", "↝"), COMPTE("Mon compte", "◎")
}

/** Espace connecte du voyageur. */
@Composable
fun VoyageScreen(vm: VoyageViewModel, profil: @Composable () -> Unit) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    var onglet by rememberSaveable { mutableStateOf(OngletVoyage.EXPLORER) }
    var destination by remember { mutableStateOf<Destination?>(null) }
    var detail by remember { mutableStateOf<Circuit?>(null) }
    var suppression by remember { mutableStateOf<Circuit?>(null) }
    var edition by remember { mutableStateOf<EditionCircuit?>(null) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        BarreVoyage()
        if (etat.chargement) LinearProgressIndicator(Modifier.fillMaxWidth())
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            key(onglet) {
                Column(
                    Modifier.widthIn(max = 660.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (onglet != OngletVoyage.COMPTE) RetourVoyage(etat, vm)
                    when (onglet) {
                        OngletVoyage.EXPLORER -> ExplorerVoyage(etat, { destination = it }, { detail = it })
                        OngletVoyage.CIRCUITS -> ListeCircuits(etat, vm::actualiser, { detail = it }, {
                            vm.effacerMessage()
                            edition = EditionCircuit(it)
                        }, { suppression = it })
                        OngletVoyage.COMPTE -> profil()
                    }
                }
            }
        }
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            for (item in OngletVoyage.entries) {
                NavigationBarItem(
                    selected = onglet == item, onClick = { onglet = item },
                    icon = { Text(item.symbole, style = MaterialTheme.typography.titleLarge) },
                    label = { Text(item.libelle) }
                )
            }
        }
    }
    destination?.let { choix ->
        DetailDestination(choix, { destination = null }) {
            destination = null
            vm.effacerMessage()
            edition = EditionCircuit(destination = choix)
        }
    }
    detail?.let { circuit ->
        DetailCircuit(circuit, { detail = null }) {
            detail = null
            vm.effacerMessage()
            edition = EditionCircuit(circuit)
        }
    }
    suppression?.let { circuit ->
        ConfirmerSuppression(circuit, { suppression = null }) {
            suppression = null
            vm.supprimer(circuit.id)
        }
    }
    edition?.let { choix ->
        EditeurCircuitScreen(choix, etat, { edition = null }) { id, demande ->
            vm.sauver(id, demande) {
                edition = null
                onglet = OngletVoyage.CIRCUITS
            }
        }
    }
}

/** Affiche le logo commun au web et au mobile. */
@Composable
private fun BarreVoyage() {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.logo_go_voyage), "GO VOYAGE",
            Modifier.width(158.dp).height(48.dp), contentScale = ContentScale.Fit
        )
    }
}

/** Indique une panne, un chargement ou le succes d'une action. */
@Composable
private fun RetourVoyage(etat: EtatVoyage, vm: VoyageViewModel) {
    if (etat.erreur.isNotEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(etat.erreur, color = MaterialTheme.colorScheme.onErrorContainer)
                TextButton(onClick = vm::actualiser, enabled = !etat.actionEnCours) { Text("Réessayer") }
            }
        }
    }
    if (etat.information.isNotEmpty()) {
        Text(etat.information, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

/** Recherche et filtre le catalogue sans requete a chaque frappe. */
@Composable
private fun ExplorerVoyage(etat: EtatVoyage, ouvrir: (Destination) -> Unit, voirCircuit: (Circuit) -> Unit) {
    var voirModeles by rememberSaveable { mutableStateOf(false) }
    BanniereVoyage()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !voirModeles, onClick = { voirModeles = false }, label = { Text("Destinations") })
        FilterChip(selected = voirModeles, onClick = { voirModeles = true }, label = { Text("Circuits proposés") })
    }
    if (voirModeles) {
        Text("Idées de départ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Choisissez un circuit et adaptez-le à votre rythme.")
        for (circuit in etat.modeles) CarteCircuit(circuit, { voirCircuit(circuit) })
    } else CatalogueVoyage(etat, ouvrir)
}

/** Affiche la recherche, les ambiances et les cartes du catalogue. */
@Composable
private fun CatalogueVoyage(etat: EtatVoyage, ouvrir: (Destination) -> Unit) {
    var recherche by rememberSaveable { mutableStateOf("") }
    var filtre by rememberSaveable { mutableStateOf("") }
    val ambiances = etat.destinations.map { it.ambiance }.distinct()
    val destinations = etat.destinations.filter {
        (filtre.isEmpty() || it.ambiance == filtre) &&
            normaliser("${it.nom} ${it.ville} ${it.region} ${it.ambiance}").contains(normaliser(recherche))
    }
    Text("Où partir ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    OutlinedTextField(
        recherche, { recherche = it }, label = { Text("Chercher une destination") },
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = filtre.isEmpty(), onClick = { filtre = "" }, label = { Text("Toutes") })
        for (ambiance in ambiances) {
            FilterChip(selected = filtre == ambiance, onClick = { filtre = ambiance }, label = { Text(libelleAmbiance(ambiance)) })
        }
    }
    if (destinations.isEmpty() && !etat.chargement && etat.erreur.isEmpty()) {
        Text("Aucune destination trouvée. Essayez une autre recherche.")
    }
    for (destination in destinations) CarteDestination(destination) { ouvrir(destination) }
}

/** Rend la recherche insensible aux accents et a la casse. */
private fun normaliser(texte: String) = Normalizer.normalize(texte.trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

/** Traduit les categories du catalogue en libelles de navigation. */
private fun libelleAmbiance(ambiance: String) = when (ambiance) {
    "MER" -> "Bord de mer"
    "CULTURE" -> "Culture"
    "NATURE" -> "Nature"
    else -> ambiance
}

/** Presente l'ambiance du catalogue tunisien. */
@Composable
private fun BanniereVoyage() {
    Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp))) {
        Image(
            painterResource(R.drawable.voyage_sidi), null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE0092928)))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ESCALE EN TUNISIE", color = Color(0xFF8FF0E3), style = MaterialTheme.typography.labelLarge)
            Text("La Tunisie, à votre rythme.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Du littoral aux dunes, composez votre voyage.", color = Color.White)
        }
    }
}

/** Affiche une destination en une carte ouvrable. */
@Composable
private fun CarteDestination(destination: Destination, ouvrir: () -> Unit) {
    Card(onClick = ouvrir, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(libelleAmbiance(destination.ambiance), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(destination.nom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${destination.ville} · ${destination.region}", style = MaterialTheme.typography.bodySmall)
            Text(destination.accroche)
            Text("${destination.tarifJourTnd} TND / jour  ·  Voir la destination", color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Montre la liste personnelle et les commandes de gestion. */
@Composable
private fun ListeCircuits(
    etat: EtatVoyage, actualiser: () -> Unit, ouvrir: (Circuit) -> Unit, editer: (Circuit?) -> Unit,
    supprimer: (Circuit) -> Unit
) {
    Text("Mes circuits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Retrouvez vos itinéraires sur le web et sur votre téléphone.")
    Button(onClick = { editer(null) }, enabled = !etat.actionEnCours && etat.destinations.isNotEmpty()) {
        Text("+ Nouveau circuit")
    }
    TextButton(onClick = actualiser, enabled = !etat.actionEnCours) { Text("Actualiser mes circuits") }
    if (etat.circuits.isEmpty() && !etat.chargement) {
        Text("Votre carnet est encore vide. Créez un circuit ou partez d’un modèle dans Explorer.")
    }
    for (circuit in etat.circuits) {
        CarteCircuit(circuit, { ouvrir(circuit) }) {
            TextButton(onClick = { editer(circuit) }, enabled = !etat.actionEnCours) { Text("Modifier") }
            TextButton(onClick = { supprimer(circuit) }, enabled = !etat.actionEnCours) { Text("Supprimer") }
        }
    }
}

/** Affiche l'ordre des lieux et les totaux d'un circuit. */
@Composable
private fun CarteCircuit(circuit: Circuit, ouvrir: () -> Unit, actions: (@Composable () -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${circuit.dureeJours} j  ·  ${circuit.estimationTnd} TND estimés", color = MaterialTheme.colorScheme.primary)
            Text(circuit.nom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(circuit.etapes.joinToString(" → ") { it.nom })
            OutlinedButton(onClick = ouvrir) { Text(if (circuit.modele) "Voir le modèle" else "Voir le circuit") }
            if (actions != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}

/** Donne les details d'une destination et permet de commencer un circuit. */
@Composable
private fun DetailDestination(destination: Destination, fermer: () -> Unit, ajouter: () -> Unit) {
    AlertDialog(
        onDismissRequest = fermer,
        title = { Text(destination.nom) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${destination.region} · ${destination.pays} · ${libelleAmbiance(destination.ambiance)}")
                Text(destination.description)
                Text("${destination.tarifJourTnd} TND par jour (indicatif)", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(onClick = ajouter) { Text("Créer un circuit") } },
        dismissButton = { TextButton(onClick = fermer) { Text("Fermer") } }
    )
}

/** Presente l'itineraire complet et le passage a l'editeur. */
@Composable
private fun DetailCircuit(circuit: Circuit, fermer: () -> Unit, editer: () -> Unit) {
    AlertDialog(
        onDismissRequest = fermer, title = { Text(circuit.nom) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                circuit.description?.let { Text(it) }
                Text("${circuit.dureeJours} jours · ${circuit.estimationTnd} TND estimés", fontWeight = FontWeight.Bold)
                circuit.budgetTnd?.let { Text("Budget cible : $it TND") }
                for ((ordre, etape) in circuit.etapes.withIndex()) {
                    Text("${ordre + 1}. ${etape.nom} · ${etape.jours} j · ${etape.coutTnd} TND")
                }
            }
        },
        confirmButton = { Button(onClick = editer) { Text(if (circuit.modele) "Personnaliser" else "Modifier") } },
        dismissButton = { TextButton(onClick = fermer) { Text("Fermer") } }
    )
}

/** Evite la suppression accidentelle d'un circuit. */
@Composable
private fun ConfirmerSuppression(circuit: Circuit, fermer: () -> Unit, supprimer: () -> Unit) {
    AlertDialog(
        onDismissRequest = fermer, title = { Text("Supprimer ce circuit ?") },
        text = { Text("« ${circuit.nom} » sera retiré de votre carnet.") },
        confirmButton = { Button(onClick = supprimer) { Text("Supprimer") } },
        dismissButton = { TextButton(onClick = fermer) { Text("Annuler") } }
    )
}
