# GO VOYAGE

![Logo GO VOYAGE](web/public/logo-go-voyage.png)

## Le principe

GO VOYAGE est une plateforme de voyage en Tunisie. Elle présente les meilleures destinations à visiter, avec un programme de séjour détaillé pour chacune, et propose une sélection d'itinéraires parmi les plus beaux du pays. À partir de cette sélection, chaque voyageur compose son propre séjour **100 % personnalisé, à la carte** : il choisit ses destinations, l'ordre des étapes et la durée de chaque étape.

Le site se parcourt **comme une boutique en ligne** : tout le monde consulte librement le catalogue des destinations et des circuits proposés, avec photos, tarifs par jour et estimations en dinars. Dès qu'une opération demande un compte — composer, personnaliser, modifier ou supprimer un circuit — l'utilisateur est invité à se connecter ou à créer son compte, avec un message explicite, puis il revient à ce qu'il faisait.

Le voyageur s'inscrit avec ses coordonnées et informations personnelles (prénom, nom, téléphone, e-mail, mot de passe). Connecté, il retrouve son carnet « Mes circuits », modifie son profil et change son mot de passe. L'administrateur, responsable de la plateforme, gère les comptes des utilisateurs : consultation, création, modification, suppression.

L'application Android reprend l'essentiel : inscription, connexion, profil, catalogue et composition de circuits, avec un cache local Room. L'administration reste dans l'interface web.

**Périmètre futur, prévu par le cahier des charges mais pas encore développé** : le comparateur de vols et d'hôtels (billetterie au meilleur tarif) et le livre d'or où les voyageurs partagent leurs expériences, photos et avis.

## Structure du dépôt

| Dossier | Contenu |
| --- | --- |
| `backend/` | Java 21, Spring Boot 3, Spring Security (JWT), Flyway, PostgreSQL |
| `web/` | React 18, TypeScript, Vite, axios |
| `mobile/` | Android, Kotlin, Fragments + XML (MVVM, ViewBinding, LiveData), Retrofit, Room |
| `docs/` | Documents d'origine, maquette, collection Postman, guides de sprint |
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
$env:JAVA_HOME = '<dossier de votre JDK 21>'   # ex. C:\Program Files\Android\Android Studio\jbr
$env:SPRING_PROFILES_ACTIVE = 'dev'
cd backend
.\mvnw.cmd spring-boot:run
```

**Invite de commandes (CMD)**

```bat
set "JAVA_HOME=<dossier de votre JDK 21>"
set "SPRING_PROFILES_ACTIVE=dev"
cd backend
.\mvnw.cmd spring-boot:run
```

Si votre base n'est pas sur le port 5432, ajoutez `$env:DB_PORT = '55433'` (PowerShell) ou `set "DB_PORT=55433"` (CMD).

L'API écoute sur [127.0.0.1:8081](http://127.0.0.1:8081/swagger-ui.html). Au premier démarrage, Flyway crée les tables puis insère le catalogue de démonstration : 8 destinations tunisiennes, 3 circuits proposés et 4 comptes.

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
3. Cliquez sur **Run ▶** : l'application détecte l'émulateur et appelle `http://10.0.2.2:8081/`, l'adresse du PC vue depuis l'émulateur.

Pour tester sur un téléphone physique relié au même Wi-Fi que le PC :

1. Démarrez le backend sur toutes les interfaces réseau :

   ```powershell
   cd backend
   $env:SERVER_ADDRESS = "0.0.0.0"
   $env:SPRING_PROFILES_ACTIVE = "dev"
   .\mvnw.cmd spring-boot:run
   ```

2. Relevez l'adresse IPv4 du PC (`ipconfig`) et reportez-la dans la constante `LOCAL_IP` de `mobile/app/src/main/java/tn/govoyage/mobile/data/ApiVoyage.kt` :

   ```kotlin
   private const val LOCAL_IP = "192.168.1.4"
   ```

3. Si Windows le demande, autorisez le port 8081 pour le profil réseau en cours (réseau « Public » sur un Wi-Fi d'établissement).
4. Recompilez, puis installez l'APK sur le téléphone :

   ```powershell
   cd mobile
   .\gradlew.bat :app:assembleDebug
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

Le téléphone et le PC doivent être sur le même sous-réseau ; l'application de développement autorise le HTTP en clair (`src/debug/AndroidManifest.xml`). Sur un téléphone, `adresseApi()` choisit automatiquement `http://<LOCAL_IP>:8081/`, sur l'émulateur `http://10.0.2.2:8081/`. Après un changement d'IP du PC, mettez à jour `LOCAL_IP` et recompilez.

Pour générer un APK de développement :

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

L'APK apparaît dans `mobile/app/build/outputs/apk/debug/`.

L'application suit l'architecture MVVM vue en cours : écrans en **Fragments/Activities** (XML + ViewBinding) avec un **Drawer** latéral, **ViewModels** exposant des **LiveData**, **repositories** séparés (`AuthRepository`, `DestinationRepository`, `CircuitRepository`) et session en **Room**. Les appels REST passent par un client **Retrofit** unique (`RetrofitInstance` dans `ApiVoyage.kt` : adresse choisie selon l'appareil, `OkHttpClient`, convertisseur Gson, journal des requêtes) et les repositories vérifient le code HTTP (`isSuccessful`, `code()`) avant d'écrire dans Room. La connexion et l'inscription sont deux Activities (`LoginActivity`, `RegisterActivity`) ; l'espace de voyage est hébergé par `MainActivity` avec la navigation Fragments (`mobile_navigation.xml`). Le détail de la migration Compose → Fragments se trouve dans `mobile/MIGRATION_PLAN.md` et `mobile/RAPPORT_MIGRATION.md`.

## Comptes de démonstration

| Compte | E-mail | Mot de passe | Rôle |
| --- | --- | --- | --- |
| Administrateur | `admin@gmail.com` | `admin1234` | ADMIN |
| Voyageur | `radhouen@gmail.com` | `radhouen123` | USER |
| Voyageur | `ayoub@mail.tn` | `user12345` | USER |
| Voyageur | `sarra@mail.tn` | `user12345` | USER |

Le profil `dev` génère une clé JWT au démarrage si `JWT_SECRET` est absent : les connexions ne survivent pas à un redémarrage du backend. Les variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` et `DB_PASSWORD` permettent de viser une autre base. Les mots de passe sont hachés avec BCrypt et ne sortent jamais de l'API.

## Vérifier l'installation

- [Swagger](http://127.0.0.1:8081/swagger-ui.html) : la documentation de l'API. Les routes du catalogue (`GET /api/destinations`, `GET /api/circuits/modeles`) sont publiques ; les autres exigent le jeton obtenu via `POST /api/auth/connexion` (bouton **Authorize**).
- Catalogue sans compte : `http://127.0.0.1:8081/api/destinations` doit renvoyer la liste des 8 destinations sans erreur.
- Site sans compte : [localhost:5173](http://localhost:5173) affiche le catalogue ; cliquer sur « Commencer un circuit » demande de se connecter.
- Tests automatisés du backend (base H2 en mémoire, 16 tests) :

```powershell
cd backend
.\mvnw.cmd test
```

- Collection Postman : importez `docs/GO-VOYAGE-Sprint-1.postman_collection.json` ou exécutez-la avec le backend en mode `dev` :

```powershell
npx.cmd --yes newman@6.2.1 run docs/GO-VOYAGE-Sprint-1.postman_collection.json
```

## Travailler à plusieurs

Le dépôt suit la branche principale `main`. Chacun travaille sur une branche par sprint (`feat/sprintN-...`) puis publie avec `git push -u origin <branche>`. `.gitignore` exclut les dépendances, les fichiers compilés, les données PostgreSQL, les fichiers `.env` et les clés Android. Sur GitHub, **Settings → Collaborators → Add people** invite vos coéquipiers.

Chaque poste a sa propre base : le service local, le volume Docker ou l'instance `.local` du script, au choix de l'équipe.

## Palette Vichy

| Couleur | Code | Usage |
| --- | --- | --- |
| Turquoise | `#05AD98` | Accent principal, actions et repères de voyage |
| Argent | `#BBBFBF` | Tracés et fonds neutres |
| Gris | `#878787` | Contours et lettrage du logo |
| Blanc | `#FFFFFF` | Surfaces et détails du logo |

Les couleurs et leurs nuances sont regroupées dans `web/src/style.css` et `mobile/app/src/main/res/values/colors.xml`. Les photos des destinations proviennent de Wikimedia Commons (licences libres).
