# GO VOYAGE · Sprint 2

| Besoin du backlog | Parcours livré sur le web et Android |
| --- | --- |
| US3 · Consulter les destinations et circuits proposés (5 points) | Explorer huit destinations tunisiennes, chercher par nom ou région, filtrer par ambiance, lire la fiche d’un lieu et parcourir trois circuits proposés. |
| US4 · Créer un circuit personnalisé (8 points) | Partir d’un modèle, d’une destination ou d’une page vierge. Ajouter, déplacer ou retirer des étapes, fixer leur durée et suivre le budget en direct. Enregistrer, consulter, modifier et supprimer ses circuits. |

Les destinations sont alimentées par le catalogue de démonstration. La gestion administrative du catalogue est prévue par l’US11 du backlog.

## Démarrage de l’équipe sur Windows

Démarrez Docker Desktop. Dans PowerShell, à la racine de GO VOYAGE, lancez le web, l’API et la base avec la même commande sur les trois postes.

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
docker compose --profile web up -d --build --wait
docker compose ps
```

- Ouvrez le [web](http://localhost:5173) ou [Swagger](http://localhost:8081/swagger-ui.html). Pour la démonstration, créez un compte ou utilisez le compte voyageur du [README](../README.md).
- Dans Android Studio, ouvrez `mobile`, sélectionnez un JDK 21 pour Gradle, installez le SDK 36 et lancez un émulateur avec **Run ▶**. L’application appelle l’API à `http://10.0.2.2:8081/`.
- Pour consulter PostgreSQL dans pgAdmin, créez une connexion sur `127.0.0.1:55433`, avec le nom de base, l’utilisateur et le mot de passe de votre `.env`. Dans **Schemas → public → Tables**, ouvrez `destination`, `circuit` et `etape_circuit`.
- Pour utiliser PostgreSQL installé sur le PC, suivez le [démarrage séparé](../README.md#démarrage-des-composants-séparés). Utilisez une seule instance d’API par port.

Flyway applique V2 et V3 à la base du Sprint 1 au démarrage. V2 crée les tables du voyage et V3 peuple le catalogue. Docker conserve les données dans le volume local de chaque poste.

## Démonstration en cinq minutes

1. Connectez-vous sur le web et sur Android avec le même compte. Dans **Explorer**, cherchez `sidi`, ouvrez Sidi Bou Saïd et lisez sa fiche.
2. Ouvrez **Bleu méditerranéen** et personnalisez-le. L’éditeur démarre avec deux jours à Sidi Bou Saïd et deux jours à Hammamet, soit quatre jours et 440 TND.
3. Donnez-lui un nom. Saisissez 400 TND comme budget cible pour voir les 40 TND de dépassement. Passez Hammamet à un jour pour voir une estimation de 315 TND et une marge de 85 TND. Inversez les étapes et enregistrez.
4. Ouvrez **Mes circuits** sur l’autre interface et cliquez sur **Actualiser**. Retrouvez le même ordre et les mêmes montants, modifiez le circuit et enregistrez de nouveau.
5. Connectez un autre compte pour vérifier que son carnet est distinct. Revenez au premier compte, confirmez la suppression et actualisez l’autre interface.

## De l’écran à PostgreSQL

Le JWT identifie le compte. Le backend passe du contrôleur au service, puis au DAO Spring Data JPA. Le service vérifie la propriété du circuit et calcule la durée totale et l’estimation à partir des tarifs par jour enregistrés en base. Les tarifs sont indicatifs et les montants sont en TND. Le dépassement du budget cible reste visible et la sauvegarde reste possible.

- `destination` contient la fiche, l’ambiance et le tarif journalier.
- `circuit` contient le nom, le budget cible éventuel et la version. Un circuit personnel est lié au compte qui l’a créé. Un circuit sans propriétaire sert de modèle. Personnaliser un modèle crée un circuit personnel distinct.
- `etape_circuit` associe une destination, une durée et un ordre à un circuit. L’API accepte jusqu’à dix étapes et soixante jours, avec un à trente jours par étape.

| Méthode | Route | Usage |
| --- | --- | --- |
| GET | `/api/destinations` et `/api/destinations/{id}` | Catalogue et fiche |
| GET | `/api/circuits/modeles` | Circuits proposés |
| GET | `/api/circuits` | Carnet du compte connecté |
| GET | `/api/circuits/{id}` | Modèle ou circuit personnel accessible |
| POST | `/api/circuits` | Création |
| PUT | `/api/circuits/{id}` | Modification avec le numéro de version courant |
| DELETE | `/api/circuits/{id}` | Suppression d’un circuit personnel |

Les deux clients calculent une prévisualisation locale. Le serveur recalcule la valeur officielle lors de la sauvegarde. Si deux appareils modifient le même circuit, une version devenue ancienne reçoit une réponse HTTP 409. Rouvrez l’éditeur depuis la liste actualisée pour repartir de la version courante.

Sur Android, Compose observe le `StateFlow` du ViewModel. Les repositories appellent l’API avec Retrofit et mettent à jour Room. Room conserve le jeton de session, le catalogue et les circuits chargés. La lecture du cache reste possible pendant une coupure. Les créations, modifications et suppressions nécessitent la connexion à l’API. La déconnexion retire la session et le carnet de l’appareil.

## Visuel

Les écrans reprennent le logo et la palette Vichy du Sprint 1. Le visuel de Sidi Bou Saïd est une illustration générée avec ImageGen, intégrée au web et à Android.

> Generate a polished editorial travel photograph for the GO VOYAGE Tunisia discovery dashboard. Authentic Sidi Bou Said whitewashed hillside buildings, characteristic cobalt blue doors, Mediterranean coast, warm subtle natural light, pale sky and turquoise sea, understated travel magazine style. Panoramic horizontal 2.5:1 composition, recognizable coastline and white-and-blue architecture on the right half, clean darker sea toward the left with room for application text that will be overlaid separately. Strong natural contrast, no writing, no logo, no watermarks, no people in close-up.
