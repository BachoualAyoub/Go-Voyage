package tn.govoyage.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/** Le point de depart de l'editeur, vide ou pre-rempli. */
data class EditionCircuit(val circuit: Circuit? = null, val destination: Destination? = null)

/** Etape editable avec une cle stable lors des deplacements. */
private data class EtapeBrouillon(val cle: Int, val destinationId: Long, val jours: String)

/** Construit le brouillon sans modifier le modele d'origine. */
private fun preparerEtapes(choix: EditionCircuit): List<EtapeBrouillon> {
    val circuit = choix.circuit
    if (circuit != null) {
        return circuit.etapes.mapIndexed { index, etape ->
            EtapeBrouillon(index, etape.destinationId, etape.jours.toString())
        }
    }
    val destination = choix.destination ?: return emptyList()
    return listOf(EtapeBrouillon(0, destination.id, "1"))
}

/** Editeur plein ecran avec commandes accessibles et bilan instantane. */
@Composable
fun EditeurCircuitScreen(
    choix: EditionCircuit, etat: EtatVoyage, fermer: () -> Unit, sauver: (Long?, CircuitRequest) -> Unit
) {
    val circuit = choix.circuit
    val personnel = circuit?.takeUnless { it.modele }
    var nom by remember { mutableStateOf(circuit?.nom.orEmpty()) }
    var budget by remember { mutableStateOf(circuit?.budgetTnd?.toString().orEmpty()) }
    var etapes by remember { mutableStateOf(preparerEtapes(choix)) }
    var prochaineCle by remember { mutableStateOf(etapes.size) }
    val catalogue = etat.destinations.associateBy { it.id }
    val jours = etapes.sumOf { it.jours.toIntOrNull() ?: 0 }
    val estimation = etapes.sumOf { (catalogue[it.destinationId]?.tarifJourTnd ?: 0) * (it.jours.toIntOrNull() ?: 0) }
    val budgetTnd = budget.toIntOrNull()
    val budgetValide = budget.isEmpty() || (budgetTnd ?: 0) in 1..1_000_000
    val etapesValides = etapes.isNotEmpty() && jours <= 60 && etapes.all {
        (it.jours.toIntOrNull() ?: 0) in 1..30 && catalogue.containsKey(it.destinationId)
    }
    val modifiable = !etat.actionEnCours
    Dialog(
        onDismissRequest = { if (modifiable) fermer() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val fenetre = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            if (fenetre != null) WindowCompat.getInsetsController(fenetre, fenetre.decorView).isAppearanceLightStatusBars = true
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().imePadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (personnel != null) "Modifier le circuit" else "Composer un circuit",
                        Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = fermer, enabled = modifiable) { Text("Fermer") }
                }
                BilanCircuit(jours, estimation, budgetTnd)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(nom, { nom = it.take(90) }, label = { Text("Nom du circuit") },
                        singleLine = true, enabled = modifiable, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        budget, { budget = it.filter { chiffre -> chiffre in '0'..'9' }.take(7) },
                        label = { Text("Budget cible en TND (facultatif)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !budgetValide,
                        singleLine = true, enabled = modifiable, modifier = Modifier.fillMaxWidth()
                    )
                    if (!budgetValide) Text("Entrez un budget entre 1 et 1 000 000 TND.", color = MaterialTheme.colorScheme.error)
                    Text("Vos étapes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (etapes.isEmpty()) Text("Ajoutez votre première destination.")
                    for ((index, etape) in etapes.withIndex()) {
                        key(etape.cle) {
                            LigneEtape(index, etape, catalogue[etape.destinationId], etapes.size, modifiable, { valeur ->
                                etapes = etapes.map { if (it.cle == etape.cle) it.copy(jours = valeur) else it }
                            }, { delta -> etapes = deplacerEtape(etapes, index, delta) }, {
                                etapes = etapes.filter { it.cle != etape.cle }
                            })
                        }
                    }
                    ChoixDestination(etat.destinations, modifiable && etapes.size < 10) { destination ->
                        etapes = etapes + EtapeBrouillon(prochaineCle++, destination.id, "1")
                    }
                    Text("1 à 30 jours par étape · 10 étapes et 60 jours maximum.", style = MaterialTheme.typography.bodySmall)
                    if (jours > 60) Text("Ramenez la durée à 60 jours maximum.", color = MaterialTheme.colorScheme.error)
                    if (etat.erreur.isNotEmpty()) Text(etat.erreur, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        val demande = CircuitRequest(
                            nom.trim(), budgetTnd, etapes.map { EtapeRequest(it.destinationId, it.jours.toInt()) },
                            personnel?.version
                        )
                        sauver(personnel?.id, demande)
                    },
                    enabled = modifiable && nom.isNotBlank() && budgetValide && etapesValides,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) { Text(if (modifiable) "Enregistrer le circuit" else "Veuillez patienter…") }
            }
        }
    }
}

/** Affiche les deux totaux et la marge de budget pendant la saisie. */
@Composable
private fun BilanCircuit(jours: Int, estimation: Int, budget: Int?) {
    val depasse = budget != null && estimation > budget
    val palette = MaterialTheme.colorScheme
    val fond = if (depasse) palette.errorContainer else palette.primaryContainer
    val texte = if (depasse) palette.onErrorContainer else palette.onPrimaryContainer
    Card(colors = CardDefaults.cardColors(containerColor = fond, contentColor = texte)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("VOTRE ESTIMATION", style = MaterialTheme.typography.labelMedium)
            Text("$jours j  ·  $estimation TND", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (budget != null && budget > 0) {
                val ecart = budget - estimation
                val couleur = if (depasse) palette.error else texte
                LinearProgressIndicator(
                    progress = { (estimation.toFloat() / budget).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(), color = couleur
                )
                val marge = if (ecart < 0) "${-ecart} TND au-dessus du budget" else "$ecart TND de marge"
                Text(marge, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Tarifs indicatifs par destination.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Saisit la duree et l'ordre d'une etape. */
@Composable
private fun LigneEtape(
    index: Int, etape: EtapeBrouillon, destination: Destination?, total: Int, modifiable: Boolean,
    changerJours: (String) -> Unit, deplacer: (Int) -> Unit, retirer: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${index + 1}. ${destination?.nom ?: "Destination indisponible"}", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                etape.jours, { changerJours(it.filter { chiffre -> chiffre in '0'..'9' }.take(2)) },
                label = { Text("Jours à cette étape") }, singleLine = true, enabled = modifiable,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = (etape.jours.toIntOrNull() ?: 0) !in 1..30, modifier = Modifier.fillMaxWidth()
            )
            Row {
                TextButton(onClick = { deplacer(-1) }, enabled = modifiable && index > 0,
                    modifier = Modifier.semantics { contentDescription = "Monter l’étape ${index + 1}" }) { Text("↑") }
                TextButton(onClick = { deplacer(1) }, enabled = modifiable && index < total - 1,
                    modifier = Modifier.semantics { contentDescription = "Descendre l’étape ${index + 1}" }) { Text("↓") }
                TextButton(onClick = retirer, enabled = modifiable) { Text("Retirer") }
            }
        }
    }
}

/** Ouvre le catalogue pour ajouter une etape. */
@Composable
private fun ChoixDestination(destinations: List<Destination>, actif: Boolean, ajouter: (Destination) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { ouvert = true }, enabled = actif && destinations.isNotEmpty()) { Text("+ Ajouter une étape") }
    DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
        for (destination in destinations) {
            DropdownMenuItem(text = { Text("${destination.nom} · ${destination.tarifJourTnd} TND/j") }, onClick = {
                ouvert = false
                ajouter(destination)
            })
        }
    }
}

/** Deplace une etape en conservant la duree saisie. */
private fun deplacerEtape(etapes: List<EtapeBrouillon>, index: Int, delta: Int): List<EtapeBrouillon> {
    val nouvelleListe = etapes.toMutableList()
    val etape = nouvelleListe.removeAt(index)
    nouvelleListe.add(index + delta, etape)
    return nouvelleListe
}
