# GO VOYAGE — Sprints 3 et 4

Cette réalisation poursuit la version de l'équipe récupérée depuis origin/main au commit dd885c8, « fix auth local mobile ». L'authentification locale, les Fragments XML, Room et les circuits de Rayen sont conservés.

## Lancer sur le PC d'Ayoub

1. Ouvrir Docker Desktop et attendre « Engine running ».
2. Ouvrir GoVoyage-GitHub dans VS Code, puis Terminal → New Terminal.
3. À la racine du dépôt, dans PowerShell :

~~~powershell
Set-Location 'C:\Users\Adem\Documents\Codex\2026-11-04\a\outputs\GO-VOYAGE-Sprint-2\GO-VOYAGE-Sprint-2\GoVoyage-GitHub'
if (!(Test-Path .env)) { Copy-Item .env.example .env }
docker compose --profile web up -d --build --wait
docker compose --profile web ps
~~~

4. Ouvrir le site : [localhost:5173](http://localhost:5173).
5. Explorer Séjours, Vols et Livre d'or. Après connexion : Mes réservations et Mes circuits. Pour un administrateur : Administration → Vue d'ensemble / Hôtels / Destinations / Modération / Comptes.

La première construction télécharge et compile les dépendances. Les démarrages suivants réutilisent les images. Pour lire l'erreur d'un composant :

~~~powershell
docker compose logs --tail 80 backend
docker compose logs --tail 80 web
~~~

Pour arrêter les services en conservant les données :

~~~powershell
docker compose --profile web down
~~~

Flyway ajoute les nouvelles tables au démarrage. Ne supprimez pas le volume pour mettre le projet à jour. Le port PostgreSQL publié est 55433 dans .env.example, pour éviter un conflit avec le service PostgreSQL Windows.

## Android dans Android Studio

1. File → Open, choisir le dossier GoVoyage-GitHub\mobile.
2. Laisser la synchronisation Gradle se terminer. Le wrapper du projet est Gradle 9.3.1.
3. Settings → Build, Execution, Deployment → Build Tools → Gradle : utiliser un JDK complet. Cette réalisation a été compilée avec Temurin 25, installé sur ce PC dans C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot. Éviter le dossier JRE de l'extension Java de VS Code qui ne contient pas toujours jlink.exe. Sur un autre poste, utiliser Download JDK pour installer un JDK complet et sélectionner son emplacement.
4. Device Manager → Pixel 4 → ▶ pour lancer l'émulateur.
5. Sélectionner la configuration app, puis Run ▶.

L'émulateur joint le PC par http://10.0.2.2:8081/. L'API doit donc être démarrée avant une connexion serveur, une synchronisation ou une réservation.

Pour changer explicitement l'adresse API, ajouter dans le fichier local mobile/gradle.properties :

~~~properties
GO_VOYAGE_API_URL=http://10.0.2.2:8081/
~~~

Cette valeur est lue dans BuildConfig par data/remote/ApiVoyage.kt. Recompiler après un changement. Un port personnalisé dans .env doit être reporté ici.

Compilation et installation depuis PowerShell, depuis la racine du dépôt et avec l'émulateur déjà ouvert :

~~~powershell
Set-Location .\mobile
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
if (!$env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
.\gradlew.bat '-PGO_VOYAGE_API_URL=http://10.0.2.2:8081/' assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
~~~

Si la signature diffère d'une ancienne installation, conserver la même clé de développement pour la mise à jour afin de préserver Room. La migration 11 → 12 ajoute le cache des nouveaux parcours sans effacer les profils et circuits.

## Comptes de démonstration

Créés uniquement si la table des utilisateurs est vide, avec le profil de développement :

| Rôle | E-mail | Mot de passe de démonstration |
| --- | --- | --- |
| Administrateur | admin@gmail.com | admin1234 |
| Voyageur | ayoub@mail.tn | user12345 |
| Voyageuse | sarra@mail.tn | user12345 |
| Voyageur | radhouen@gmail.com | radhouen123 |

Si votre base contient déjà des comptes ou si leurs mots de passe ont changé, utilisez leurs identifiants actuels.

## Ce que les équipiers partagent

Le code, les migrations, les contrats d'API et la configuration Docker sont partagés. Chaque poste conserve sa propre base dans son volume Docker : un git pull ne synchronise pas les réservations ni les comptes.

Sur un même serveur, le web et Android lisent la même base et les mêmes réservations. Room garde les profils et circuits locaux ainsi que la dernière lecture du carnet. Une réservation ou une publication nécessite une session serveur et du réseau. La connexion locale seule ne confirme pas une réservation.

Pour récupérer une version intégrée à main, après avoir enregistré ou commité ses modifications locales :

~~~powershell
git switch main
git pull --ff-only origin main
if (!(Test-Path .env)) { Copy-Item .env.example .env }
docker compose --profile web up -d --build --wait
~~~

Le nouveau code est préparé sur une branche dédiée. Il devra être intégré puis poussé pour que les équipiers le récupèrent depuis main.

## Règles métier

**Sprint 3 — US5, US6 et US11, 21 points du Sprint 0.** Comparateur de vols jusqu'à quatre trajets, sélection et total indicatif. Séjours de 1 à 60 nuits, de 1 à 10 chambres et de 1 à 30 voyageurs, avec capacité vérifiée par chambre. Prix recalculé, conflit si le tarif change après sélection, verrouillage de l'hôtel contre la surréservation et clé de demande contre les doublons. Annulation avant le jour d'arrivée, réservée au propriétaire. Gestion du catalogue, archivage des hôtels et protection des destinations utilisées.

**Sprint 4 — US7 à US10 et US12, 21 points.** Récits avec photo ou vidéo facultative, JPEG/PNG/MP4 de 10 Mo maximum. Un avis noté de 1 à 5 par compte et publication, modifiable ; like réversible sans doublon ; recherche par pays ou ville ; signalement motivé ; examen, masquage et rétablissement par l'administrateur. Le média d'un récit masqué n'est plus public.

**Pilotage.** Indicateurs réels PostgreSQL : séjours et montant confirmés, voyageurs, circuits, récits, signalements, répartition par ville, séries journalières et dernières réservations. Graphiques animés avec prise en compte de la réduction des animations sur le web.

## Vérifications

- 22 tests backend réussis, incluant les 16 tests précédents.
- Essais PostgreSQL : concurrence sur la dernière chambre, répétition sans doublon, propriétaire, annulation, conflit de version, photos, recherche, avis, like, signalement, masquage et rétablissement.
- Compilation TypeScript/Vite et neuf parcours navigateur, dont réservation, annulation, avis, ajout et archivage d'hôtel, modération et navigation à 390 pixels. Aucune erreur JavaScript pendant ces parcours.
- APK compilé et exécuté sur émulateur : réservation serveur, carnet partagé, comparateur, livre d'or, avis et lecture Room hors connexion.

Les captures authentiques des rapports sont dans docs/captures/sprint3 et docs/captures/sprint4.

## Limites

Les offres sont des données de démonstration en base. Aucun fournisseur de voyages, achat de billet ou paiement en ligne n'est connecté. Les montants du tableau de bord représentent des réservations internes confirmées, pas des encaissements. Le cache Android ne garantit aucune disponibilité hors connexion.

Les essais et captures ont été faits sur une instance séparée. La base personnelle du projet n'a pas été remplacée.
