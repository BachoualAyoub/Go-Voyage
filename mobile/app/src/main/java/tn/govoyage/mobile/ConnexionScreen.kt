package tn.govoyage.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Propose les formulaires d'acces au compte mobile. */
@Composable
fun ConnexionScreen(vm: CompteViewModel) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("VOTRE ESPACE VOYAGE", style = MaterialTheme.typography.labelLarge)
            Text("Le départ, c’est ici.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { vm.changerMode(false) }, enabled = !etat.chargement, modifier = Modifier.weight(1f)) {
            Text("Connexion", fontWeight = if (!etat.inscription) FontWeight.Bold else FontWeight.Normal)
        }
        TextButton(onClick = { vm.changerMode(true) }, enabled = !etat.chargement, modifier = Modifier.weight(1f)) {
            Text("Inscription", fontWeight = if (etat.inscription) FontWeight.Bold else FontWeight.Normal)
        }
    }
    key(etat.inscription) {
        FormulaireCompte(etat, vm)
    }
}

/** Saisit les coordonnees et transmet la requete au ViewModel. */
@Composable
private fun FormulaireCompte(etat: EtatCompte, vm: CompteViewModel) {
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(etat.emailPrefere) }
    var tel by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    val inscription = etat.inscription
    val formulaireComplet = email.isNotBlank() && motDePasse.isNotBlank() &&
        (!inscription || (prenom.isNotBlank() && nom.isNotBlank() && tel.isNotBlank() && motDePasse.length >= 8))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (inscription) "Créez votre compte" else "Connectez-vous",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
        )
        if (etat.message.isNotEmpty()) {
            Text(
                etat.message,
                color = if (etat.erreur) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        if (inscription) {
            ChampCompte("Prénom", prenom, { prenom = it })
            ChampCompte("Nom", nom, { nom = it })
        }
        ChampCompte("Adresse e-mail", email, { email = it }, KeyboardType.Email)
        if (inscription) ChampCompte("Téléphone", tel, { tel = it }, KeyboardType.Phone)
        ChampCompte("Mot de passe", motDePasse, { motDePasse = it }, KeyboardType.Password)
        if (inscription) Text("8 caractères minimum.", style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = {
                if (inscription) {
                    vm.inscrire(InscriptionRequest(nom.trim(), prenom.trim(), email.trim(), tel.trim(), motDePasse))
                } else {
                    vm.connecter(ConnexionRequest(email.trim(), motDePasse))
                }
            },
            enabled = formulaireComplet && !etat.chargement,
            modifier = Modifier.fillMaxWidth()
        ) {
            val libelle = if (inscription) "Créer mon compte" else "Me connecter"
            Text(if (etat.chargement) "Veuillez patienter…" else libelle)
        }
    }
}

/** Saisit une valeur sur une ligne, avec le clavier adapte au champ. */
@Composable
private fun ChampCompte(
    label: String,
    valeur: String,
    modifierValeur: (String) -> Unit,
    clavier: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = modifierValeur,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = clavier),
        visualTransformation = if (clavier == KeyboardType.Password) PasswordVisualTransformation()
            else VisualTransformation.None
    )
}
