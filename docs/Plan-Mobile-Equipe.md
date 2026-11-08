# Plan mobile de GO VOYAGE

Le brief de l’atelier demande une application Android native reliée au projet d’intégration. Le barème attribue 40 % à l’architecture et 60 % aux fonctionnalités. Le premier lot proposé est **Destinations et circuits à la carte** (US3 et US4 du Sprint 0).

## Point de départ

Sprint 1 livre les écrans Compose d’inscription et de connexion, le profil et le client Retrofit. Le ViewModel appelle actuellement Retrofit lui-même et garde le profil en mémoire. Le backend dispose des routes d’authentification et de gestion des comptes. Les routes métier de destination et de circuit restent à développer.

| Critère du brief | Travail à réaliser |
| --- | --- |
| MVVM et injection | Les écrans observent un `StateFlow` du ViewModel via `collectAsStateWithLifecycle()`. Le ViewModel reçoit son repository via `ViewModelProvider.Factory` et exécute les opérations dans `viewModelScope`. |
| Séparation des données | `AuthRepository` gère la session. `DestinationRepository` et `CircuitRepository` gèrent l’API et leurs données Room. Les écrans n’accèdent qu’aux ViewModels. |
| Session | Enregistrer le jeton et l’identifiant du compte dans Room. Vérifier `GET /api/auth/moi` au redémarrage, retirer la session à la déconnexion ou sur un `401` et présenter une action de réessai si le réseau est indisponible. |
| CRUD | Démontrer `GET`, `POST`, `PUT` et `DELETE` sur les entités métier retenues, avec les autorisations adaptées au rôle. Actualiser Room après une réponse réussie pour que la liste se mette à jour. |
| Fonction métier | Composer et réordonner les étapes d’un circuit, calculer sa durée et le coût estimé à partir des prix des étapes, puis signaler un dépassement du budget saisi. |
| Erreurs | Traiter les codes HTTP et les erreurs réseau dans les repositories. Les écrans affichent les états chargement, vide, erreur et succès. |

PostgreSQL conserve les données communes sur le serveur. Room garde la session et le cache sur chaque appareil. Spring Boot vérifie le JWT et les droits d’accès. Le backend déduit le propriétaire d’un circuit à partir de la session. Les opérations d’écriture du catalogue appartiennent au rôle ADMIN.

La première démonstration couvre la consultation du catalogue et le CRUD d’un circuit personnel. Si Destinations est retenue pour la note de CRUD, ajouter les écrans Android d’administration du catalogue. Rayen valide avec l’enseignant la liste complète des entités évaluées et présente Compose avec Flow comme l’équivalent réactif des exemples Activity/Fragment avec LiveData du brief.

## Premier contrat API à valider

| Opération | Route proposée |
| --- | --- |
| Consulter les destinations | `GET /api/destinations` |
| Consulter ses circuits | `GET /api/circuits` |
| Créer un circuit personnel | `POST /api/circuits` |
| Modifier un circuit personnel | `PUT /api/circuits/{id}` |
| Supprimer un circuit personnel | `DELETE /api/circuits/{id}` |
| Gérer le catalogue côté ADMIN | `POST /api/admin/destinations`, puis `PUT` et `DELETE /api/admin/destinations/{id}` |

Un circuit contient un nom, un budget en dinars et une liste ordonnée d’étapes. Chaque étape référence une destination et un nombre de jours. Les prix de référence viennent du catalogue. Les routes ci-dessus sont à développer.

## Répartition proposée

| Membre | À faire maintenant | Validation |
| --- | --- | --- |
| **Radwen** · Scrum Master | Créer `AuthRepository`, le stockage de session Room, le ViewModel injecté et la restauration de session. Suivre les blocages Android Studio et Docker dans l’équipe. Branche `feat/mobile-auth`. | Inscription, connexion, relance de l’app, déconnexion, mauvais mot de passe et jeton expiré. |
| **Rayen** · Product Owner | Confirmer les critères US3/US4 et les données à afficher. Créer les écrans Android de destinations et de circuits, leurs repositories métier et les ViewModels. Intégrer le formulaire d’étapes et le calcul du budget. Branche `feat/mobile-circuits`. | Présenter le parcours complet et vérifier le résultat sur l’émulateur avec des données API. |
| **Ayoub** · Développement et intégration | Faire valider le démarrage Docker sur les trois postes. Définir avec Rayen le contrat REST, ajouter les migrations Flyway et les endpoints des destinations et circuits, puis intégrer les scénarios Postman. Branche `feat/api-circuits`. | Vérifier les quatre opérations sur PostgreSQL et le refus d’accès au circuit d’un autre compte. |

Validez ensemble les DTO, les routes et les réponses d’erreur avant de coder. Ayoub fournit un exemple JSON afin que Rayen puisse travailler sur l’interface pendant le développement de l’API. Radwen prépare la session en parallèle. Utilisez des branches courtes et des pull requests vers la branche commune.

## Scénario à montrer

1. Depuis un clone propre, démarrez l’API et PostgreSQL avec Docker Compose. Ouvrez Swagger et vérifiez les routes.
2. Lancez Android Studio, ouvrez `mobile` et connectez-vous. Fermez et rouvrez l’application pour montrer la restauration de session.
3. Créez un circuit avec deux étapes, changez leur ordre et le nombre de jours, puis observez le nouveau total. Rechargez la liste et supprimez le circuit.
4. Essayez une saisie invalide, une panne de réseau et un accès à un circuit appartenant à un autre compte. Montrez la réaction de l’interface et de l’API.

Le [README](../README.md) contient les commandes Docker communes et les adresses utilisées depuis le PC et l’émulateur.
