# Rapport final — migration mobile GO VOYAGE (Compose → Fragments + XML + MVVM)

Branche : `migration-fragments`. Vérification à chaque phase : `cd mobile && .\gradlew.bat :app:assembleDebug`
→ **BUILD SUCCESSFUL** aux phases 1, 2, 3, 4a, 4b, 4c et 5.

## 1. Arborescence finale de `mobile/app/src/main/java/tn/govoyage/mobile/`

```
├── GoVoyageApplication.kt            (inchangé : client Retrofit + base Room partagés)
├── MainActivity.kt                   (Drawer + Toolbar + NavHost, modèle TP5 ; déconnexion avec AlertDialog TP3)
├── model/        Destination.kt, EtapeCircuit.kt, Circuit.kt, EtapeRequest.kt, CircuitRequest.kt,
│                 UtilisateurResponse.kt
├── data/         ApiVoyage.kt, BaseVoyage.kt, RepositoriesVoyage.kt (logique inchangée)
├── viewModel/    AuthViewModel.kt (session + inscription/connexion/déconnexion), VoyageViewModel.kt
│                 (catalogue, modèles, carnet, actions, sélection fiche) — LiveData
└── ui/
    ├── auth/         LoginActivity (LAUNCHER), RegisterActivity
    ├── home/         HomeFragment (bannière, chips Destinations/Circuits proposés, recherche, filtres ambiance)
    ├── destination/  DestinationAdapter, DestinationDetailFragment (fiche + « Créer un circuit »)
    ├── circuit/      CircuitsFragment (liste + CRUD), CircuitAdapter, CircuitDetailFragment,
    │                 EditeurCircuitFragment (bilan instantané, étapes réordonnables)
    └── compte/       CompteFragment (profil, déconnexion)
```

## 2. Tableau ancien fichier Compose → nouveaux fichiers

| Ancien (Compose) | Nouveaux fichiers |
| --- | --- |
| `MainActivity.kt` (palette + routage connexion/accueil) | `MainActivity.kt` (Drawer TP5), `themes.xml`, `colors.xml` |
| `MainActivity.kt` (ProfilScreen, InfoProfil) | `ui/compte/CompteFragment.kt` + `fragment_compte.xml` |
| `ConnexionScreen.kt` | `ui/auth/LoginActivity.kt` + `activity_login.xml`, `ui/auth/RegisterActivity.kt` + `activity_register.xml` |
| `VoyageScreen.kt` (onglet Explorer, bannière, recherche, filtres) | `ui/home/HomeFragment.kt` + `fragment_home.xml` |
| `VoyageScreen.kt` (CarteDestination) | `ui/destination/DestinationAdapter.kt` + `item_destination.xml` |
| `VoyageScreen.kt` (DetailDestination) | `ui/destination/DestinationDetailFragment.kt` + `fragment_destination_detail.xml` |
| `VoyageScreen.kt` (ListeCircuits, CarteCircuit) | `ui/circuit/CircuitsFragment.kt` + `fragment_circuits.xml`, `ui/circuit/CircuitAdapter.kt` + `item_circuit.xml` |
| `VoyageScreen.kt` (DetailCircuit) | `ui/circuit/CircuitDetailFragment.kt` + `fragment_circuit_detail.xml` |
| `VoyageScreen.kt` (ConfirmerSuppression) | `AlertDialog` dans `CircuitsFragment` (style TP3) |
| `EditeurCircuitScreen.kt` (Dialog plein écran) | `ui/circuit/EditeurCircuitFragment.kt` + `fragment_editeur_circuit.xml` + `item_etape.xml` |
| `CompteViewModel.kt` (StateFlow) | `viewModel/AuthViewModel.kt` (LiveData) |
| `VoyageViewModel.kt` (StateFlow) | `viewModel/VoyageViewModel.kt` (LiveData) |
| `ModelesVoyage.kt` | `model/Destination.kt`, `EtapeCircuit.kt`, `Circuit.kt`, `EtapeRequest.kt`, `CircuitRequest.kt` |
| `ApiVoyage.kt` / `BaseVoyage.kt` / `RepositoriesVoyage.kt` | déplacés dans `data/`, logique et endpoints inchangés |
| `GoVoyageApplication.kt` | inchangé (imports mis à jour) |

## 3. Fonctionnalités

**Conservées (parité complète)** :
- Créer un compte (prénom, nom, e-mail, téléphone, mot de passe ≥ 8 caractères) ; se connecter ;
  se déconnecter (Drawer + bouton profil, avec AlertDialog TP3) ; restauration de session au
  démarrage (jeton Room) et rejet 401/403 → retour connexion.
- Explorer : bannière Sidi Bou Saïd, bascule Destinations / Circuits proposés, recherche texte
  insensible aux accents et à la casse (nom, ville, région, ambiance), filtres d'ambiance
  (Toutes / Bord de mer / Culture / Nature), fiches destination et circuit.
- CRUD réactif des circuits : créer depuis une destination, un modèle ou à vide ; éditeur avec
  nom, budget cible, étapes (0 à 10, 1 à 30 jours chacune, 60 jours max), réordonnancement ↑/↓,
  retrait, estimation et marge de budget en direct ; enregistrer ; modifier ; supprimer avec
  confirmation ; actualiser le carnet ; erreur 409 de version gérée par le backend (message API).
- Profil : espace administrateur / voyageur, nom complet, e-mail, téléphone, rôle, note web pour ADMIN.

**Partielles / absentes (non développées dans le mobile d'origine, non créées sur accord de l'équipe)** :
- Gestion des comptes ADMIN (l'app dit « dans l'espace web ») — l'item Drawer ADMIN n'existe pas.
- Hôtels, réservations, publications/commentaires/signalement, comparateur de prix des vols
  (cités par le cahier des charges, marqués « périmètre futur » dans le README).

## 4. Décisions prises quand les TP ne couvraient pas le cas

1. **Session en Room** (pas SharedPreferences) : le prompt permet les deux « ou Room si déjà
   utilisé » — Room était déjà utilisé pour la session, donc conservé tel quel.
2. **Pas d'`EXTRA_USER` par Intent** (écart au TP3 assumé) : la session vit en Room ;
   `LoginActivity` n'ouvre `MainActivity` que via l'observation de la session, et `MainActivity`
   la relit via `AuthViewModel`. Aucun Parcelable n'est donc nécessaire (voir 6).
3. **`@Parcelize` abandonné** : l'ajout du plugin `kotlin-parcelize` sur `UtilisateurResponse`
   (utilisée en `@Embedded` par Room) provoque une erreur KSP `MissingType` sur cette toolchain
   (AGP 9, Kotlin intégré). Comme aucun Intent ne transporte l'utilisateur (décision 2), la
   classe reste une data class simple et le plugin n'est pas appliqué.
4. **Fiches en Fragments** (pas en dialogs) : les dialogs Compose de détail deviennent
   `DestinationDetailFragment` et `CircuitDetailFragment`, plus proche du TP5, même contenu.
5. **Chips Material** (`Widget.MaterialComponents.Chip.Choice`) pour remplacer les FilterChips
   Compose ; le thème est passé à `Theme.MaterialComponents.Light.NoActionBar` (exigé par les
   Chips), la palette Vichy est conservée.
6. **Fiches d'étapes de la fiche circuit** : lignes ajoutées par programme dans un LinearLayout
   (pas de RecyclerView : la fiche est en lecture seule, comme le dialog Compose).
7. **Éditeur d'étapes** : les lignes sont des vues conservées en mémoire (une `ItemEtapeBinding`
   par étape) pour ne pas perdre la saisie lors du réordonnancement ; le choix de destination
   d'une étape utilise un `PopupMenu` (équivalent du DropdownMenu Compose).
8. **VoyageViewModel lit la session lui-même** (au lieu de la recevoir en paramètre) : un
   ViewModel vit pour un seul passage connecté, la session ne change pas pendant sa vie.
9. **Fichiers Compose supprimés dès la Phase 3** : devenus du code mort (plus référencés depuis
   la Phase 1) et bloqués par les changements de package ; la Phase 5 n'a eu que Gradle à nettoyer.
10. **Textes** : les libellés fixes sont dans `strings.xml` ; les chaînes composites des cartes
    (ex. « 3 j · 330 TND estimés ») restent construites en code, à l'image de l'adapter TP5.
11. **`local.properties`** (sdk.dir) a été créé sur ce poste pour pouvoir compiler ; il est
    ignoré par `.gitignore`.

## 5. Commandes pour lancer et tester

1. Backend + base + web (Docker) : `docker compose --profile web up -d --build --wait` à la racine.
2. Application : Android Studio, ouvrir `mobile` (JDK 21 pour Gradle, SDK 36), émulateur, **Run ▶**
   (ou `.\gradlew.bat :app:assembleDebug` puis installer l'APK).
3. Comptes de démonstration (créés par les migrations Flyway du backend) :
   - Administrateur : `admin@gmail.com` / `admin1234`
   - Voyageur : `radhouen@gmail.com` / `radhouen123` (ou `ayoub@mail.tn` / `sarra@mail.tn`,
     mot de passe `user12345`)
4. Parcours de test : connexion → Explorer (recherche « sidi », filtre Bord de mer) → fiche
   destination → « Créer un circuit » → nom + budget 400 TND → enregistrer → Mes circuits →
   modifier/supprimer → Mon compte → Déconnexion. Fermer et rouvrir l'app : la session revient.

## 6. Ce qui n'a pas pu être vérifié ici

- La **compilation** est vérifiée à chaque phase ; l'**exécution sur émulateur** et les appels
  réseau réels n'ont pas pu être testés dans cet environnement (pas d'émulateur disponible,
  backend non démarré ici). Les écrans sont rédigés pour reproduire exactement le comportement
  Compose, mais un passage manuel sur émulateur reste à faire par l'équipe.
- Aucun test instrumenté Android n'existe dans le projet (avant comme après la migration).
