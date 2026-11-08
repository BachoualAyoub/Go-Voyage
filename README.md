# GO VOYAGE

![Logo GO VOYAGE](web/public/logo-go-voyage.png)

Le Sprint 1 couvre l’inscription, la connexion JWT et le profil sur le web et sur Android, ainsi que la gestion des comptes dans l’espace web administrateur.

| Dossier | Contenu |
| --- | --- |
| `backend/` | Java 21, Spring Boot, Spring Security, Flyway, PostgreSQL |
| `web/` | React 18, TypeScript, Vite |
| `mobile/` | Android, Kotlin, Jetpack Compose, Retrofit |
| `docs/` | Documents d’origine, maquette et collection Postman |

## Démarrage sur Windows

Ouvrez un terminal dans le dossier `GoVoyage`. Il vous faut un **JDK 21**, **Node.js 22**, Git et Docker Desktop ou une installation locale de PostgreSQL. Pour Android, ouvrez Android Studio et installez le SDK 36. Le premier démarrage télécharge les dépendances.

Si le terminal affiche `C:\...>`, utilisez les commandes **Invite de commandes (CMD)**. Si le prompt commence par `PS`, utilisez les commandes **PowerShell**.

### 1. PostgreSQL

**Avec Docker Desktop**, ouvrez Docker, attendez que son moteur soit prêt et lancez la commande suivante. PostgreSQL 16 écoute alors sur le port **5432**. Le volume Docker conserve les comptes.

```powershell
docker compose up -d --wait db
```

**Avec PostgreSQL déjà installé sur Windows**, le script crée une instance isolée dans `.local/postgres` sur le port **55432**. Il utilise les identifiants de développement `govoyage` / `govoyage` et laisse intactes vos autres bases.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\postgres-local.ps1
```

Si PostgreSQL est installé en dehors de `C:\Program Files\PostgreSQL`, ajoutez `-PgBin "C:\chemin\vers\PostgreSQL\bin"`. Pour arrêter l’instance locale, relancez le script avec `-Arreter`. Pour arrêter Docker, utilisez `docker compose stop db`.

### 2. Backend

Dans un deuxième terminal, depuis la racine `GoVoyage`, sélectionnez votre JDK 21 et le profil `dev`. Remplacez le chemin d’exemple par le chemin du JDK. Gardez ce terminal ouvert. Avec Docker, remplacez le port `55432` par `5432`.

**PowerShell**

```powershell
$env:JAVA_HOME = 'C:\chemin\vers\jdk-21'
$env:DB_PORT = '55432'
$env:SPRING_PROFILES_ACTIVE = 'dev'
cd backend
.\mvnw.cmd spring-boot:run
```

**Invite de commandes (CMD)**

```bat
set "JAVA_HOME=C:\chemin\vers\jdk-21"
set "DB_PORT=55432"
set "SPRING_PROFILES_ACTIVE=dev"
cd backend
.\mvnw.cmd spring-boot:run
```

L’API écoute sur [127.0.0.1:8081](http://127.0.0.1:8081/swagger-ui.html). Flyway crée la table `utilisateur` à partir de la migration SQL livrée. Hibernate vérifie qu’elle correspond à l’entité Java. Le profil `dev` initialise les comptes de démonstration sur une base vide.

| Compte de démonstration | E-mail | Mot de passe |
| --- | --- | --- |
| Administrateur | `admin@gmail.com` | `admin1234` |
| Voyageur | `ayoub@mail.tn` | `user12345` |

Ces comptes servent à la démo locale. Le profil `dev` génère une clé JWT au démarrage si `JWT_SECRET` est absent. Reconnectez-vous après un redémarrage ou un changement d’adresse e-mail. Les variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` et `DB_PASSWORD` permettent de choisir une autre instance.

### 3. Interface web

Dans un troisième terminal, revenez d’abord à la racine `GoVoyage`. Le dossier `web` se trouve à côté de `backend`. Ces commandes fonctionnent dans CMD et PowerShell. Attendez la fin de `npm.cmd ci` avant de lancer Vite.

```powershell
cd web
npm.cmd ci
npm.cmd run dev
```

Ouvrez [localhost:5173](http://localhost:5173). Essayez l’inscription, puis connectez-vous pour ouvrir le profil. Connectez-vous ensuite avec le compte administrateur pour lister, ajouter, modifier et supprimer des comptes. Le navigateur appelle l’API, qui vérifie les droits et accède à PostgreSQL.

Pour modifier l’adresse de l’API, copiez `web/.env.example` vers `web/.env`, modifiez `VITE_API_URL` et relancez Vite.

### 4. Application Android

1. Dans Android Studio, ouvrez le dossier `mobile`. Sélectionnez le JDK 21 dans les réglages Gradle et installez le SDK Android 36.
2. Lancez un émulateur depuis **Device Manager**. Gardez le backend ouvert sur votre PC.
3. Cliquez sur **Run ▶**. L’application appelle `http://10.0.2.2:8081/`, l’adresse de la boucle locale du PC vue depuis l’émulateur. [Réseau de l’émulateur Android](https://developer.android.com/studio/run/emulator-networking-address)

Android permet de s’inscrire, de se connecter et de consulter son profil. L’administration est disponible dans l’interface web. Pour générer un APK de développement depuis la racine :

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

L’APK apparaît dans `mobile/app/build/outputs/apk/debug/`. Pour changer le port de l’API, ajoutez par exemple `'-PGO_VOYAGE_API_URL=http://10.0.2.2:8082/'` à la commande Gradle.

## Tests

Importez `docs/GO-VOYAGE-Sprint-1.postman_collection.json` dans Postman et exécutez la collection dans l’ordre avec le backend en mode `dev`. Le scénario crée des comptes temporaires, teste l’inscription, les droits et la gestion des comptes, puis efface ses comptes. Vous pouvez aussi l’exécuter depuis la racine :

```powershell
npx.cmd --yes newman@6.2.1 run docs/GO-VOYAGE-Sprint-1.postman_collection.json
```

Dans [Swagger](http://127.0.0.1:8081/swagger-ui.html), **Authorize** accepte le `jeton` obtenu via `POST /api/auth/connexion`. Pour les tests automatisés du backend avec H2 :

```powershell
cd backend
.\mvnw.cmd test
```

## Partager avec votre équipe sur GitHub

Créez un dépôt **privé et vide** nommé `GoVoyage`. Dans le dossier du projet extrait, remplacez `VOTRE-COMPTE` par votre identifiant GitHub :

```powershell
git init -b main
git add .
git status --short
git commit -m "feat(sprint1): comptes et authentification GO VOYAGE"
git remote add origin https://github.com/VOTRE-COMPTE/GoVoyage.git
git push -u origin main
```

`.gitignore` exclut les dépendances installées, les fichiers compilés, les données PostgreSQL, les fichiers `.env` et les clés Android. Sur GitHub, ouvrez **Settings → Collaborators → Add people** pour inviter vos deux coéquipiers. Après acceptation, ils peuvent cloner le dépôt. [Importer le code local](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github) · [Inviter des collaborateurs](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository)

## Palette Vichy

| Couleur | Code | Usage |
| --- | --- | --- |
| Turquoise | `#05AD98` | Accent principal, actions et repères de voyage |
| Argent | `#BBBFBF` | Tracés et fonds neutres |
| Gris | `#878787` | Contours et référence pour le lettrage du logo |
| Blanc | `#FFFFFF` | Surfaces et détails du logo |

Les couleurs de base et leurs nuances sont regroupées dans `web/src/style.css` et `mobile/app/src/main/res/values/colors.xml`. Les nuances foncées maintiennent la lisibilité des textes et des contrôles.

Le logo conserve le repère de carte, l’avion et la trajectoire de GO VOYAGE. La version Vichy sur fond transparent a été recolorée avec ImageGen intégré et figure dans les applications web et Android.

> Recolor the existing GO VOYAGE logo. Input 1 is the logo to edit; input 2 is the Vichy palette reference only. Preserve the original horizontal lockup, the exact uppercase wording “GO VOYAGE”, the bold geometric letter shapes, map pin, cutout airplane, and sweeping trajectory. Change the pin fill to the exact flat turquoise #05AD98, the wordmark to flat medium neutral gray #878787, and the trajectory to flat silver gray #BBBFBF. Keep the cutout and the canvas genuinely transparent (alpha), so the aircraft reads white on white #FFFFFF. Use clean sharp edges and uniform solid fills, with no shadows, gradients, mockups, extra text, or reference-card graphics. Match the original proportions and spacing, keeping all artwork within the canvas.

> Finalize this GO VOYAGE logo with clean, flat brand-color fills. Input 1 is the layout and exact lettering to preserve; input 2 is the Vichy color swatch reference only. Match the pin to the bright turquoise swatch #05AD98, the wordmark to #878787, and the swoosh to #BBBFBF, sampled as pure opaque sRGB colors, no texture or shading. Make the plane inside the pin a clean solid #FFFFFF silhouette with no green or cyan fringe. Retain the exact spelling GO VOYAGE, horizontal layout, lettering shape, pin outline and trajectory geometry. Keep true alpha transparency outside the logo and in the letter counters. No extra objects, words, shadows, or reference card elements.

