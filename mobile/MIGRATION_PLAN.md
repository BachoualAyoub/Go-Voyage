# Plan de migration mobile — Jetpack Compose vers Fragments + XML + MVVM (style TP3/TP5)

Ce document est le plan de la Phase 0. Il est le résultat de la lecture de `AGENTS.md`, `README.md`,
`docs/Plan-Mobile-Equipe.md`, `docs/Sprint-2.md` et de tout le dossier `mobile/`. Aucun code n'a été
modifié à ce stade.

## 1. Inventaire de l'app actuelle (11 fichiers Compose, à plat)

| Fichier Compose | Rôle réel |
| --- | --- |
| `GoVoyageApplication.kt` | Client Retrofit + base Room partagés (inchangé, cible §C) |
| `MainActivity.kt` | Palette Vichy Material 3, routage connexion/accueil, écran profil |
| `ConnexionScreen.kt` | Formulaires connexion + inscription (un seul écran, bascule par boutons) |
| `VoyageScreen.kt` | 3 onglets : Explorer (destinations + circuits proposés), Mes circuits, Mon compte ; dialogs détail destination, détail circuit, confirmation de suppression |
| `EditeurCircuitScreen.kt` | Éditeur plein écran du circuit : nom, budget, étapes (ordre, jours, retrait, ajout), bilan instantané |
| `VoyageViewModel.kt` | StateFlow `EtatVoyage` : destinations, modèles, circuits, chargement, erreurs |
| `CompteViewModel.kt` | StateFlow `EtatCompte` : session Room, inscription, connexion, déconnexion |
| `RepositoriesVoyage.kt` | `AuthRepository`, `DestinationRepository`, `CircuitRepository` (déjà séparés), `lireErreur` |
| `ApiVoyage.kt` | Retrofit + DTOs (`InscriptionRequest`, `ConnexionRequest`, `UtilisateurResponse`, …) |
| `BaseVoyage.kt` | Room : `session`, `destination`, `circuit`, `etape` |
| `ModelesVoyage.kt` | `Destination`, `EtapeCircuit`, `Circuit`, `EtapeRequest`, `CircuitRequest` |

Fonctionnalités réellement présentes : créer un compte, se connecter, se déconnecter, consulter le
catalogue (recherche texte insensible aux accents + filtre ambiance), voir la fiche d'une destination,
voir les circuits proposés, consulter/détail d'un circuit, créer/modifier/supprimer un circuit
personnel avec éditeur d'étapes et suivi du budget, profil, restauration de session au redémarrage.

## 2. Correspondance ancien fichier Compose → nouveaux fichiers

| Ancien (Compose) | Cible (Fragments/XML/MVVM) |
| --- | --- |
| `MainActivity.kt` (EspaceVoyage, EspaceConnecte) | `MainActivity.kt` (Drawer + NavHost, TP5) + theme XML (`themes.xml`, `colors.xml`) |
| `MainActivity.kt` (ProfilScreen, InfoProfil) | `ui/compte/CompteFragment.kt` + `fragment_compte.xml` |
| `ConnexionScreen.kt` (Connexion + Inscription) | `ui/auth/LoginActivity.kt` + `activity_login.xml`, `ui/auth/RegisterActivity.kt` + `activity_register.xml` |
| `VoyageScreen.kt` (onglet Explorer, bannière, recherche, filtres) | `ui/home/HomeFragment.kt` + `fragment_home.xml` |
| `VoyageScreen.kt` (CarteDestination) | `ui/destination/DestinationAdapter.kt` + `item_destination.xml` |
| `VoyageScreen.kt` (DetailDestination dialog) | `ui/destination/DestinationDetailFragment.kt` + `fragment_destination_detail.xml` |
| `VoyageScreen.kt` (ListeCircuits, CarteCircuit) | `ui/circuit/CircuitsFragment.kt` + `fragment_circuits.xml`, `ui/circuit/CircuitAdapter.kt` + `item_circuit.xml` |
| `VoyageScreen.kt` (DetailCircuit dialog) | `ui/circuit/CircuitDetailFragment.kt` + `fragment_circuit_detail.xml` |
| `VoyageScreen.kt` (ConfirmerSuppression dialog) | `AlertDialog` style TP3 dans `CircuitsFragment` |
| `EditeurCircuitScreen.kt` (Dialog plein écran) | `ui/circuit/EditeurCircuitFragment.kt` + `fragment_editeur_circuit.xml` |
| `CompteViewModel.kt` (StateFlow) | `viewModel/AuthViewModel.kt` (LiveData) |
| `VoyageViewModel.kt` (StateFlow) | `viewModel/VoyageViewModel.kt` (LiveData) |
| `ModelesVoyage.kt` | éclaté dans `model/` (`Destination.kt`, `EtapeCircuit.kt`, `Circuit.kt`, `CircuitRequest.kt`, `EtapeRequest.kt`) |
| `ApiVoyage.kt` | `data/ApiVoyage.kt` (inchangé) |
| `BaseVoyage.kt` | `data/BaseVoyage.kt` (inchangé) |
| `RepositoriesVoyage.kt` | `data/RepositoriesVoyage.kt` (inchangé, logique et appels API identiques) |
| `GoVoyageApplication.kt` | inchangé (il expose déjà `auth`, `destinations`, `circuits`) |

## 3. Arborescence finale de `mobile/app/src/main/java/tn/govoyage/mobile/`

```
├── GoVoyageApplication.kt            (inchangé)
├── MainActivity.kt                   (Drawer + NavHost, modèle TP5)
├── model/        Destination.kt, EtapeCircuit.kt, Circuit.kt, EtapeRequest.kt, CircuitRequest.kt,
│                 UtilisateurResponse.kt (rendu @Parcelize, champ et JSON inchangés)
├── data/         ApiVoyage.kt, BaseVoyage.kt, RepositoriesVoyage.kt (déplacés sans changer la logique)
├── viewModel/    AuthViewModel.kt, VoyageViewModel.kt (LiveData)
└── ui/
    ├── auth/       LoginActivity, RegisterActivity
    ├── home/       HomeFragment (Explorer : bannière, chips, recherche, filtre ambiance)
    ├── destination/ DestinationFragment? (voir décision 1), DestinationAdapter, DestinationDetailFragment
    ├── circuit/    CircuitsFragment, CircuitAdapter, CircuitDetailFragment, EditeurCircuitFragment
    └── compte/     CompteFragment (profil, déconnexion)
```

## 4. Navigation et Drawer (mobile_navigation.xml)

Graphe, équivalent des 3 onglets actuels (rien d'inventé) :

| id du fragment | Fragment | Libellé | Rôle |
| --- | --- | --- | --- |
| `nav_home` (départ) | `ui/home/HomeFragment` | Explorer | Bannière, chips Destinations/Circuits proposés, recherche, filtres, cartes |
| `nav_circuits` | `ui/circuit/CircuitsFragment` | Mes circuits | Liste + actualiser + nouveau + modifier/supprimer |
| `nav_compte` | `ui/compte/CompteFragment` | Mon compte | Profil + déconnexion |
| `nav_detail_destination` | `ui/destination/DestinationDetailFragment` | Fiche | Détail d'une destination + « Créer un circuit » |
| `nav_detail_circuit` | `ui/circuit/CircuitDetailFragment` | Fiche circuit | Itinéraire + « Personnaliser/Modifier » |
| `nav_editeur_circuit` | `ui/circuit/EditeurCircuitFragment` | Éditeur | Composition du circuit (nom, budget, étapes) |

Détails : le fragment sélectionné est transmis comme en TP5 (`destinationSelectionne` /
`circuitSelectionne` en LiveData du ViewModel) — pas d'arguments de navigation à sérialiser.
Le « Déconnexion » du Drawer est un item sans destination : clic → `AlertDialog` TP3
(titre, message, Oui/Non, `ic_logout`) → vider la session (via `AuthRepository.deconnecter`) →
`Intent` vers `LoginActivity` + flags CLEAR_TOP/NEW_TASK + `finish()`. Item « Gestion des comptes »
visible seulement si rôle ADMIN (voir écart 2 ci-dessous).

## 5. Authentification (modèle TP3, branchée sur le vrai backend)

- `LoginActivity` = LAUNCHER, `ActivityLoginBinding`, champs `editEmail` / `editPassword`,
  bouton `boutonConnexion`, lien vers `RegisterActivity` (« Pas de compte ? Créez-en un »).
  Champs vides → Toast. Appel `AuthViewModel.connecter()` → `AuthRepository` (Retrofit suspend).
  Succès → la session est déjà en Room (fait par le repository, on garde Room au lieu de
  SharedPreferences car déjà utilisé) → `Intent(this, MainActivity::class.java)`, pas d'`EXTRA_USER`
  nécessaire (la session est en base : `MainActivity` la lit, comme aujourd'hui) — voir décision 3.
  Échec → Toast du message API via `lireErreur` + réseau (`try/catch`, message « API injoignable… »).
- `RegisterActivity` : `editPrenom`, `editNom`, `editEmail`, `editTel`, `editPassword`,
  « 8 caractères minimum. » conservé ; succès → retour Login avec Toast « Votre compte est créé… »
  et l'email pré-rempli (comportement actuel conservé).
- Démarrage : `LoginActivity` lit Room ; jeton présent → `MainActivity` directement.
  `MainActivity` → `AuthViewModel.restaurer()` (appel `/api/auth/moi` déjà existant) ; 401/403 →
  déconnexion et retour Login (comportement actuel conservé).
- Rôle ADMIN : l'app actuelle affiche « La gestion des comptes se fait dans l'espace web » —
  il n'existe pas d'écran Android de gestion des comptes (voir écart 2).
- Manifest : `LoginActivity` LAUNCHER, `RegisterActivity`, `MainActivity` (`exported`,
  `windowSoftInputMode="adjustResize"`), `INTERNET` conservé, debug `usesCleartextTraffic` conservé.
  Thème : remplacer `Theme.Material.Light.NoActionBar` par un thème AppCompat (requis par TP3/TP5)
  basé sur la palette Vichy (`themes.xml`).

## 6. ViewModels (LiveData, style TP5)

- `AuthViewModel` : `_session` (LiveData `SessionLocale?`), `_etat` (chargement, message, erreur,
  emailPrefere), `inscrire`, `connecter`, `deconnecter`, `restaurer`. La session Room (Flow) est
  collectée dans `viewModelScope` et publiée en LiveData (équivalent simple du `collect` actuel).
- `VoyageViewModel` : `destinations`, `modeles`, `circuits`, `chargement`, `actionEnCours`,
  `erreur`, `information` en LiveData ; méthodes `actualiser`, `sauver`, `supprimer`,
  `effacerMessage` inchangées ; `destinationSelectionne` / `circuitSelectionne` (TP5).
- Partage : `activityViewModels()` dans les fragments ; ViewModels créés dans `MainActivity`
  avec une `ViewModelProvider.Factory` (déjà le cas aujourd'hui).
- Logique métier, appels API, gestion d'erreurs : strictement inchangés (déplacés tels quels).

## 7. Gradle (`mobile/app/build.gradle.kts`, même style de déclaration)

- `buildFeatures { viewBinding = true }` (compose gardé jusqu'à la Phase 5).
- Ajouts : navigation-fragment-ktx, navigation-ui-ktx, fragment-ktx, lifecycle-viewmodel-ktx,
  lifecycle-livedata-ktx, lifecycle-runtime-ktx (pour `lifecycleScope`), material, appcompat,
  recyclerview, cardview, constraintlayout, coordinatorlayout, drawerlayout.
- Plugin `kotlin-parcelize` (`org.jetbrains.kotlin.plugin.parcelize`, version alignée sur Kotlin
  2.2.10) pour `UtilisateurResponse : Parcelable`.
- Room + Retrofit + Gson : conservés. Retrait Compose (dépendances + plugin + imports) uniquement
  en Phase 5, quand plus aucun écran Compose ne reste.

## 8. Ressources

- `layout/` : `activity_login`, `activity_register`, `activity_main`, `app_bar_main`, `content_main`,
  `nav_header_main`, `fragment_home`, `fragment_circuits`, `fragment_compte`,
  `fragment_destination_detail`, `fragment_circuit_detail`, `fragment_editeur_circuit`,
  `item_destination`, `item_circuit`, `item_etape`.
- `navigation/mobile_navigation.xml`, `menu/activity_main_drawer.xml`, `menu/main.xml`.
- `values/` : `colors.xml` (palette Vichy existante conservée), `strings.xml` (tous les libellés
  français actuels), `themes.xml` (AppCompat + palette Vichy), `dimens.xml` (`fab_margin`).
- `drawable/` : `ic_logout` (nouveau), réutilise `logo_go_voyage.png`, `voyage_sidi.png`,
  `icone_voyage.xml`.
- La bannière Sidi Bou Saïd, le logo et tous les libellés exacts (« Où partir ? », « + Nouveau
  circuit », « TND / jour · Voir la destination », etc.) sont repris à l'identique.

## 9. Écarts avec le cahier des charges (listés, non implémentés sans accord)

| Fonctionnalité du cahier | État dans l'app actuelle | Décision pour la migration |
| --- | --- | --- |
| Gestion des comptes (CRUD ADMIN) | Absente du mobile (l'app dit « dans l'espace web ») | Non créée ; l'item Drawer ADMIN sera absent tant que l'écran n'existe pas (sauf accord) |
| Hôtels | Absente du mobile | Non migrée (rien à migrer) |
| Réservations | Absente du mobile | Non migrée |
| Publications / commentaires / signalement | Absentes du mobile (web seulement, « périmètre futur ») | Non migrées |
| Comparer les prix des vols | Absente du mobile | Non migrée |
| Recherche par pays ou ville | L'app actuelle cherche sur nom, ville, région et ambiance (insensible aux accents) | Conservée telle quelle, sans l'étendre |
| Personnaliser un circuit avec prix | Présente (éditeur + budget + estimation) | Migrée telle quelle |

La migration ne supprime aucune fonctionnalité existante ; elle n'en ajoute aucune.

## 10. Décisions prises là où les TP ne couvrent pas le cas (à valider)

1. **Fiche destination / fiche circuit** : aujourd'hui ce sont des `AlertDialog` Compose. Le
   plan cible du prompt prévoit `DestinationDetailFragment`. Je propose des **fragments** de détail
   (plus proche du TP5), avec le même contenu. Alternative fidèle à 100 % : garder des dialogs XML.
   → choix par défaut : fragments de détail.
2. **Room pour la session** : le prompt permet SharedPreferences ou Room ; Room est déjà utilisé
   pour la session, on le garde (aucune donnée à re-migrer, logout/restauration inchangés).
3. **EXTRA_USER** : TP3 passe l'`User` par Intent. Ici la session est déjà en Room et
   `MainActivity` la lit via `AuthViewModel` (comportement actuel). Je propose de **garder la
   lecture Room** (pas de doublon jeton/SharedPreferences) ; `UtilisateurResponse` sera quand même
   `@Parcelize` conforme au point 3 du prompt si vous le souhaitez, mais il ne transitera pas par
   l'Intent. → choix par défaut : session Room, pas d'EXTRA_USER.
4. **Éditeur plein écran** : le Dialog Compose devient un Fragment plein écran du graphe de
   navigation (retour = bouton « Fermer », comme aujourd'hui).
5. **Recherche** : champ texte + chips de filtre ambiance sont conservés dans `HomeFragment`
   ( chips = simples boutons/selectables Material, sans lib hors cours).
6. **Restauration rapide au démarrage** : `LoginActivity` fait une lecture Room directe du jeton
   (petite méthode supplémentaire `suspend` dans `AuthRepository`, sans toucher au reste).
7. **Complément de la Phase 3** : `@Parcelize` sur `UtilisateurResponse` (utilisée en `@Embedded`
   par Room) a été essayé puis retiré : la combinaison plugin `kotlin-parcelize` + KSP Room
   produit une erreur `MissingType` sur cette toolchain (AGP 9 Kotlin intégré). Comme aucune
   navigation par Intent ne transporte l'utilisateur (session en Room, décision 3), la classe
   reste une data class simple ; le plugin n'est pas appliqué.
8. **Complément de la Phase 3** : les fichiers Compose (`*Screen.kt`, anciens ViewModels
   StateFlow, `ModelesVoyage.kt`) ont été supprimés dès la Phase 3 : devenus du code mort (plus
   référencés par `MainActivity` depuis la Phase 1) et bloqués par les changements de package et
   la conversion StateFlow→LiveData. La Phase 5 se limite donc à Gradle, aux imports et au README.

## 11. Phases suivantes et vérification

- Phase 1 : branche `migration-fragments`, Gradle, `MainActivity` + layouts Drawer (TP5),
  `mobile_navigation.xml`, menus, `colors/themes/strings/dimens`. Doit compiler.
- Phase 2 : auth TP3 (Login, Register, session, déconnexion, Manifest).
- Phase 3 : déplacement `model/ data/ viewModel/`, StateFlow → LiveData, logique inchangée.
- Phase 4 : écrans un par un (Accueil → Destinations → Circuits → Éditeur → Compte), compilation
  et comparaison du comportement après chaque écran.
- Phase 5 : retrait Compose (fichiers `*Screen.kt`, dépendances, plugin, imports), ressources
  inutiles, mise à jour de la partie mobile du `README.md`.

Vérification à chaque phase : `cd mobile && .\gradlew.bat :app:assembleDebug`. Un commit par phase,
messages en français. Comptes de démonstration (backend Flyway) : `admin@gmail.com` / `admin1234`
(ADMIN), `radhouen@gmail.com` / `radhouen123` (USER).

## 12. Ce que je n'ai pas pu vérifier en Phase 0

La compilation (`assembleDebug`) n'a pas encore été lancée : la Phase 1 la fera. L'app actuelle n'a
pas de tests Android, pas de fichier `DonneesDev.java` (les comptes de démo viennent des migrations
Flyway du backend). Les documents `docs/Sprint0/1/2` au format `.docx/.pdf` n'ont pas été ouverts
(le prompt de mission interdit de lire les archives/PDF et fournit tout le nécessaire).
