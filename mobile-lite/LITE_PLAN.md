# Plan LITE — version de démonstration hors-ligne de GO-VOYAGE (beta)

## Principe

Copie allégée de `mobile/` dans un **projet Android indépendant** `mobile-lite/`
(package `tn.govoyage.mobile.lite`, nom « GO-VOYAGE Lite »), installable à côté de la
version complète. Elle ne contient **que les concepts du cours/TP** :

- une seule Activity (`MainActivity`, Drawer + NavHost, TP5), **Fragments partout** y compris
  connexion et inscription, aucune navigation par Intent ;
- **ViewModel + LiveData** avec `activityViewModels()`, données fictives créées dans le `init {}`
  (comme `EtudiantViewModel` du TP5), tout est **synchrone et en mémoire** ;
- **ViewBinding**, RecyclerView + Adapter (`onItemClicked`, `updateList()` + `notifyDataSetChanged()`),
  CardView, Toast, AlertDialog (déconnexion, confirmation de suppression), Navigation Component ;
- **zéro réseau, zéro base de données, zéro stockage persistant, zéro permission** :
  pas de Retrofit, Room, SharedPreferences, coroutines, ni de dossier `data/` ;
  les ajouts/modifications/suppressions modifient les listes en mémoire et repartent de zéro
  au redémarrage (comportement normal de la beta).

## Écrans conservés (identiques à `mobile/`, aucun écran inventé)

| Écran | Fragment | Contenu |
| --- | --- | --- |
| Connexion | `LoginFragment` (départ du graphe) | e-mail + mot de passe, compte de démo, lien vers inscription |
| Inscription | `RegisterFragment` | prénom, nom, e-mail, téléphone, mot de passe ≥ 8 |
| Explorer | `HomeFragment` | bannière Sidi, bascule Destinations / Circuits proposés, recherche (nom, ville, pays, région), filtres d'ambiance, cartes |
| Fiche destination | `DestinationDetailFragment` | fiche + bouton « Créer un circuit » |
| Fiche circuit | `CircuitDetailFragment` | itinéraire + totaux + « Personnaliser / Modifier » |
| Mes circuits | `CircuitsFragment` | carnet du compte connecté, création, modification, suppression (AlertDialog de confirmation) |
| Éditeur de circuit | `EditeurCircuitFragment` | nom, budget, étapes (0–10, 1–30 j, 60 j max), ordre ↑/↓, estimation et marge en direct |
| Mon compte | `CompteFragment` | profil + déconnexion |

Toolbar et tiroir latéral masqués sur Connexion/Inscription ; après connexion, retour à
l'accueil avec `popUpTo` inclusif ; déconnexion par AlertDialog qui remet l'utilisateur à
`null` et revient à la connexion avec `popUpTo`.

## Données fictives (en mémoire)

- 8 destinations tunisiennes (Sidi Bou Saïd, Tunis, Carthage, Hammamet, Monastir, Sousse,
  Tozeur, Djerba) avec ambiances MER / CULTURE / NATURE et tarifs journaliers en TND.
- 3 circuits modèles du catalogue (« Bleu méditerranéen », « Cap Bon » (hmm nom libre),
  « Oasis et dunes »).
- 2 comptes : un administrateur de démonstration prêt à l'emploi et un voyageur ;
  l'inscription ajoute un compte à la liste. Chaque compte a son carnet de circuits
  (une `MutableMap` par identifiant de compte, toujours en mémoire).

Compte de démo : `admin@govoyage.tn` / `admin123` (rôle ADMIN) ;
second compte de démo : `sarra@govoyage.tn` / `sarra123` (rôle USER).

## Vérifications prévues

1. `.\gradlew.bat :app:assembleDebug` dans `mobile-lite/` doit réussir.
2. `grep -rniE "retrofit|okhttp|room|sharedpreferences|datastore|viewmodelscope|coroutines|compose|glide|coil|intent|internet" mobile-lite/app` ne doit rien renvoyer.
3. Chaque écran fonctionne uniquement avec les listes en mémoire des ViewModels.
4. `git status` ne montre que des fichiers sous `mobile-lite/`.

## Rapport final

Voir la section « Rapport final » en bas de ce document (complétée après le build).
