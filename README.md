# GO VOYAGE

![Logo GO VOYAGE](web/public/logo-go-voyage.png)

## Le principe

GO VOYAGE est une plateforme de voyage en Tunisie. Elle présente les destinations à visiter et permet à chaque voyageur de composer son propre itinéraire, **100 % personnalisé, à la carte** : il choisit ses destinations, l'ordre des étapes et la durée de chaque étape.

Le site se parcourt **comme une boutique en ligne** : tout le monde consulte librement le catalogue des destinations et des circuits proposés, avec photos, tarifs par jour et estimations en dinars. Dès qu'une opération demande un compte — composer, personnaliser, modifier ou supprimer un circuit — l'utilisateur est invité à se connecter ou à créer son compte, puis il revient à ce qu'il faisait.

Le voyageur s'inscrit avec ses coordonnées (prénom, nom, téléphone, e-mail, mot de passe). Connecté, il retrouve son carnet « Mes circuits », modifie son profil et change son mot de passe. L'administrateur gère les comptes, le catalogue et la modération.

### Périmètre livré

| Partie | Fonctionnalités livrées |
| --- | --- |
| **Site web** | Sprints 1 à 4 : comptes et JWT, catalogue des destinations, éditeur de circuits, comparateur de vols, recherche et réservation de séjours, carnet, gestion des hôtels et destinations, indicateurs, livre d'or, avis, likes, signalements et modération. |
| **Application mobile** | Sprints 1 et 2 uniquement : inscription et connexion, repli local hors ligne, catalogue des destinations conservé dans Room, synchronisation manuelle du catalogue, consultation et édition des circuits avec leurs étapes via Retrofit, profil et déconnexion. |
| **API commune** | Spring Boot qui sert les deux applications : authentification, utilisateurs, destinations, circuits/étapes, séjours/hôtels/vols, communauté et administration. |

Les parcours **séjours, vols, réservations, livre d'or et administration ne sont pas présents dans l'application mobile** : ils restent disponibles sur le site web et dans l'API. Ce choix garde l'application mobile simple et conforme aux TP6/TP7.

## Structure du dépôt

| Dossier | Contenu |
| --- | --- |
| `backend/` | Java 21, Spring Boot 3, Spring Security (JWT), Flyway, PostgreSQL |
| `web/` | React 18, TypeScript, Vite, axios |
| `mobile/` | Android, Kotlin, Fragments + XML (MVVM, ViewBinding, LiveData), Retrofit, Room |
| `postman/` | Collection et environnement Postman de l'API (48 requêtes) |
| `scripts/` | Script d'instance PostgreSQL isolée (optionnel) |

## Démarrer l'application

Deux façons de lancer, au choix de l'équipe : **l'option Docker, identique sur toutes les machines**, ou l'option composants séparés pour travailler sur un composant précis.

### Option 1 — tout lancer en une seule commande (Docker)

Seul prérequis : [Docker Desktop](https://www.docker.com/products/docker-desktop/) installé et démarré. **Aucun JDK, Node.js ou PostgreSQL n'est nécessaire sur le poste** : tout est conteneurisé, y compris la compilation du backend.

À la racine du dépôt, dans VSCode ou un terminal. La première fois, créez le fichier des ports :

```bat
copy .env.example .env
```

Puis la commande unique qui lance la base, l'API et le site :

```
docker compose --profile web up -d --build --wait
```

Elle construit les images (le premier démarrage télécharge les dépendances : prévoir quelques minutes), démarre PostgreSQL, attend qu'il soit prêt, puis lance l'API, puis le site, et ne rend la main que lorsque tout est sain. Ouvrez ensuite [localhost:5173](http://localhost:5173).

| Commande | Effet |
| --- | --- |
| `docker compose --profile web up -d --wait` | relancer l'application après un arrêt |
| `docker compose --profile web down` | arrêter l'application (les données sont conservées) |
| `docker compose down -v` | arrêter et effacer la base de démonstration |

Les ports publiés se règlent dans `.env` : `DB_PORT=55433` (choisi pour ne pas gêner un éventuel service PostgreSQL local sur le port 5432), `API_PORT=8081`, `WEB_PORT=5173`. Si un port est déjà occupé sur le poste, arrêtez le service en cause ou changez le numéro dans `.env`. Les données de la base vivent dans le volume Docker `govoyage_data` et survivent aux arrêts.

### Option 2 — lancer chaque composant séparément (sans Docker)

#### 1. PostgreSQL

Le plus simple : le service PostgreSQL de Windows (port 5432). Créez une seule fois le rôle et la base de développement `govoyage`, par exemple avec pgAdmin ou psql en superutilisateur :

```sql
CREATE ROLE govoyage LOGIN PASSWORD 'govoyage';
CREATE DATABASE govoyage OWNER govoyage;
```

Le backend pointe alors par défaut sur `localhost:5432/govoyage` : aucune variable à définir. Alternatives : la base Docker de l'équipe (`docker compose up -d --wait db`, port 55433 avec `.env`) ou l'instance isolée `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\postgres-local.ps1` (port 55432). Dans ces deux cas, définissez `DB_PORT` avant de lancer le backend.

#### 2. Backend (API) — nécessite un JDK 21

L'emplacement du JDK dépend du poste : ne recopiez pas un chemin trouvé sur une autre machine. Pour trouver le vôtre :

- `where java` dans un terminal : le chemin affiché se termine par `...\bin\java.exe` ; JAVA_HOME est le dossier au-dessus de `bin` ;
- si Android Studio est installé, son JDK intégré fait l'affaire : `C:\Program Files\Android\Android Studio\jbr` ;
- sinon, installez un JDK 21, par exemple `winget install --id EclipseAdoptium.Temurin.21.JDK`, puis fermez et rouvrez le terminal ;
- vérifiez avec `java -version` (doit afficher une version 21).

Depuis la racine du dépôt :

**PowerShell**

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Invite de commandes (CMD)**

```bat
cd backend
.\mvnw.cmd spring-boot:run
```

Tous les réglages locaux (base `govoyage`, port 8081, profil `dev`) sont dans `backend/src/main/resources/application.properties` : aucune variable d'environnement n'est nécessaire. Dans VS Code ou IntelliJ, le bouton **Run** sur `GoVoyageApplication` suffit.

Si votre base n'est pas sur le port 5432, ajoutez `$env:DB_PORT = '55433'` (PowerShell) ou `set "DB_PORT=55433"` (CMD).

L'API écoute sur toutes les interfaces (port 8081), donc aussi sur l'IP du PC : [http://127.0.0.1:8081/swagger-ui.html](http://127.0.0.1:8081/swagger-ui.html). Au premier démarrage, Flyway crée les tables puis insère le catalogue de démonstration : 8 destinations, 3 circuits proposés et 4 comptes.

#### 3. Interface web — nécessite Node.js 22

```powershell
cd web
npm.cmd ci
npm.cmd run dev
```

Ouvrez [localhost:5173](http://localhost:5173). Sur Windows, `npm.ps1` peut être bloqué par la politique d'exécution : utilisez `npm.cmd`.

Pour viser une autre API, copiez `web/.env.example` vers `web/.env`, ajustez `VITE_API_URL`, puis relancez.

#### 4. Application Android

1. Dans Android Studio, ouvrez le dossier `mobile` (JDK 21 dans les réglages Gradle, SDK Android 36).
2. Lancez un émulateur depuis **Device Manager**, avec le backend démarré sur le PC.
3. Cliquez sur **Run ▶** : l'application utilise automatiquement `http://10.0.2.2:8081/`, l'adresse du PC vue depuis l'émulateur.

Pour tester sur un téléphone physique relié au même Wi-Fi que le PC :

1. Démarrez le backend normalement (bouton **Run** ou `.\mvnw.cmd spring-boot:run`) : il écoute déjà sur toutes les interfaces réseau.
2. Relevez l'adresse IPv4 du PC (`ipconfig`) et reportez-la dans la constante `LOCAL_IP` de `mobile/app/src/main/java/tn/govoyage/mobile/data/remote/RetrofitInstance.kt` :

   ```kotlin
   private const val LOCAL_IP = "172.16.20.246"
   ```

3. Si Windows le demande, autorisez le port 8081 pour le profil réseau en cours (réseau « Public » sur un Wi-Fi d'établissement).
4. Recompilez, puis installez l'APK sur le téléphone :

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Le téléphone et le PC doivent être sur le même sous-réseau. En mode debug, le manifeste autorise le HTTP en clair (`mobile/app/src/debug/AndroidManifest.xml`). `adresseApi()` choisit automatiquement l'émulateur ou le téléphone physique. Après un changement d'IP du PC, mettez à jour `LOCAL_IP` et recompilez.

L'APK apparaît dans `mobile/app/build/outputs/apk/debug/`.

L'application Android suit une architecture **MVVM simple** : Activities/Fragments XML avec ViewBinding, ViewModels exposant des LiveData, repositories séparés (`AuthRepository`, `DestinationRepository`, `CircuitRepository`), client Retrofit unique (`RetrofitInstance`) et base Room `go-voyage.db`. Le jeton JWT reste en mémoire (`JetonMemoire`) ; le profil et le catalogue des destinations sont conservés sur l'appareil. Si le serveur est injoignable, la connexion peut utiliser l'empreinte locale enregistrée dans Room ; le bouton **Réessayer** de l'écran circuits tente une reconnexion silencieuse avant de recharger les circuits. Le bouton **Synchroniser les destinations** se trouve dans l'écran Destinations.

## Comptes de démonstration

| Compte | E-mail | Mot de passe | Rôle |
| --- | --- | --- | --- |
| Administrateur | `admin@gmail.com` | `admin1234` | ADMIN |
| Voyageur | `radhouen@gmail.com` | `radhouen123` | USER |
| Voyageur | `ayoub@mail.tn` | `user12345` | USER |
| Voyageur | `sarra@mail.tn` | `user12345` | USER |

Le profil `dev` génère une clé JWT au démarrage si `JWT_SECRET` est absent : les connexions ne survivent pas à un redémarrage du backend. Les variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` et `DB_PASSWORD` permettent de viser une autre base. Les mots de passe sont hachés avec BCrypt et ne sortent jamais de l'API.

## Vérifier l'installation

- [Swagger](http://127.0.0.1:8081/swagger-ui.html) : la documentation de l'API. `GET /api/destinations` et `GET /api/circuits/modeles` sont publiques ; les autres routes exigent le jeton obtenu via `POST /api/auth/connexion` (bouton **Authorize**).
- Catalogue sans compte : `http://127.0.0.1:8081/api/destinations` doit renvoyer la liste des destinations sans erreur.
- Site sans compte : [localhost:5173](http://localhost:5173) affiche le catalogue ; cliquer sur « Commencer un circuit » demande de se connecter.
- Application mobile : se connecter avec un compte de démonstration, ouvrir Destinations, puis Mes circuits. En coupant le Wi-Fi après une première connexion, la connexion locale et le catalogue Room restent utilisables ; les circuits nécessitent le serveur.
- Tests automatisés du backend (base H2 en mémoire) :

```powershell
cd backend
.\mvnw.cmd test
```

- Construction du site :

```powershell
cd web
npm.cmd run build
```

- Collection Postman : importer `postman/GO-Voyage.postman_collection.json` et `postman/GO-Voyage.postman_environment.json`, puis exécuter avec le backend en mode `dev` :

```powershell
npx.cmd --yes newman@6.2.1 run postman/GO-Voyage.postman_collection.json -e postman/GO-Voyage.postman_environment.json
```

## Travailler à plusieurs

Le dépôt suit la branche principale `main`. Chacun travaille sur une branche par sprint (`feat/sprintN-...`) puis publie avec `git push -u origin <branche>`. `.gitignore` exclut les dépendances, les fichiers compilés, les données PostgreSQL, les fichiers `.env` et les clés Android. Sur GitHub, **Settings → Collaborators → Add people** invite vos coéquipiers.

L'équipe procède par commit par phase (`feat(sprintN): <nom de la phase>`). Chaque poste a sa propre base : le service local, le volume Docker ou l'instance `.local` du script, au choix de l'équipe.

## Palette Vichy

| Couleur | Code | Usage |
| --- | --- | --- |
| Turquoise | `#05AD98` | Accent principal, actions et repères de voyage |
| Argent | `#BBBFBF` | Tracés et fonds neutres |
| Gris | `#878787` | Contours et lettrage du logo |
| Blanc | `#FFFFFF` | Surfaces et détails du logo |

Les couleurs et leurs nuances sont regroupées dans `web/src/style.css` et `mobile/app/src/main/res/values/colors.xml`. Les photos des destinations proviennent de Wikimedia Commons (licences libres).
