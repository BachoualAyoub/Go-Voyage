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

---

# Rapport final (mission LITE, terminée)

## Arborescence livrée

```
mobile-lite/
  settings.gradle.kts · build.gradle.kts · gradle.properties · gradlew(.bat)
  gradle/wrapper/ (copié de mobile/) · .gitignore · local.properties (non committé)
  README.md (comptes de démo) · LITE_PLAN.md (ce document)
  app/build.gradle.kts          → viewBinding, AGP 9.1.0, deps = liste fermée du cours
  app/src/main/AndroidManifest.xml → MainActivity seule (LAUNCHER), 0 permission
  app/src/main/java/tn/govoyage/mobile/lite/
    MainActivity.kt             → Drawer + NavHost, masquage toolbar/tiroir sur auth
    model/                      → Utilisateur, Destination, Circuit, Etape (+ SaisieEtape)
    viewModel/                  → AuthViewModel, VoyageViewModel (init {} = données fictives)
    ui/auth/                    → LoginFragment, RegisterFragment
    ui/home/                    → HomeFragment (Explorer)
    ui/destination/             → DestinationAdapter (+ libelleAmbiance), DestinationDetailFragment
    ui/circuit/                 → CircuitAdapter, CircuitsFragment, CircuitDetailFragment,
                                  EditeurCircuitFragment
    ui/compte/                  → CompteFragment
  app/src/main/res/
    layout/                     → 15 layouts (activity_main + fragments + items,
                                  fragment_login/register adaptés des anciens Activity XML)
    navigation/mobile_navigation.xml → départ nav_login, actions avec popUpTo
    menu/activity_main_drawer.xml    → ids = ids des fragments du graphe
    values/ (strings, colors, themes, dimens) · drawable/ · drawable-nodpi/
```

## Écrans conservés (aucun écran inventé)

Connexion → Inscription → Explorer (recherche + filtres + circuits proposés) →
fiche destination / fiche circuit → Mes circuits (CRUD + AlertDialog de suppression) →
éditeur de circuit (étapes, budget, marge en direct) → Mon compte → déconnexion (AlertDialog).
Toolbar et tiroir masqués sur Connexion/Inscription ; connexion → accueil avec `popUpTo`
inclusif ; déconnexion → retour connexion avec `popUpTo`.

## Compte de démonstration

`admin@govoyage.tn` / `admin123` (ADMIN) · `sarra@govoyage.tn` / `sarra123` (USER) ;
l'inscription ajoute un compte (mot de passe ≥ 8, e-mail unique).

## Vérifications exécutées

1. `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL** (29 s ; après correction
   d'un import `Toast` manquant dans `CircuitsFragment.kt`).
2. Contrôle grep (retrofit, okhttp, room, sharedpreferences, datastore, viewModelScope,
   coroutines, compose, glide, coil, intent, internet) sur `mobile-lite/app` → seules
   correspondances :
   - `intent-filter` + `android.intent.action.MAIN` dans le Manifest : **incontournable**
     (déclaration d'activité LAUNCHER, exigée par Android, ce n'est pas un Intent en code) ;
   - 2 libellés français dans `strings.xml` (« composez votre voyage », « Composer un
     circuit ») : verbes du texte UI, identiques à `mobile/`, pas la techno.
3. `git status` → 57 fichiers, tous sous `mobile-lite/` ; rien sous `build/`, `.gradle/`,
   `local.properties` (ignorés par `.gitignore`). `mobile/`, `backend/`, `web/` inchangés.

## Décisions prises là où les TP ne couvraient pas le cas

- **Login en départ du graphe** : dans `mobile/` la connexion est une Activity séparée ;
  ici tout tient dans `MainActivity`, donc `nav_login` est la destination de départ, et
  MainActivity observe la session pour ouvrir/fermer le carnet et revenir à la connexion
  (garde-fou : si on est déjà sur `nav_login`, on ne re-navigue pas).
- **ViewModels sans factory** : constructeurs sans paramètre → fabrique par défaut de
  `by viewModels()` / `activityViewModels()`, ce qui évite tout ViewModelFactory (interdit).
- **Carnet par compte** : `MutableMap<Long, MutableList<Circuit>>` dans le ViewModel, un
  carnet par identifiant de compte ; ouvert à la connexion, vidé de l'écran à la
  déconnexion, perdu au redémarrage (normal pour la beta).
- **Rechargement des écrans** : les layouts copiés contiennent des vues d'erreur/chargement
  (`barreChargement`, `carteErreur`, `boutonActualiser`) inutiles hors-ligne ; elles sont
  simplement ignorées par les fragments LITE (visibilité `gone` d'origine).
- **Retour depuis une fiche** : bouton « Fermer » → `onBackPressedDispatcher` (même
  comportement que `mobile/`), aucune navigation par Intent.

