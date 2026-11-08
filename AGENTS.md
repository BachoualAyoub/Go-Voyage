# GO Voyage : règles du projet

## Nommage
Mots du métier en FRANÇAIS, rôles techniques avec les termes STANDARD internationaux (suffixes en anglais).
- Suffixes : Controller, Service, DAO, DTO/Request/Response, Config, Filter, Exception, Handler (GlobalExceptionHandler), Test. Entités sans suffixe (Utilisateur).
- Packages Java en minuscules, un par rôle : controller, service, dao, dto, entity, config, security, exception. Exemple : tn.govoyage.utilisateur.dao.UtilisateurDAO.
- Java : PascalCase (classes), camelCase (méthodes, variables), UPPER_SNAKE_CASE (constantes).
- Base de données : snake_case (table utilisateur, colonne id_u).
- Web (TypeScript) : composants et types en PascalCase (ConnexionPage, Navbar, UtilisateurFormDialog), fonctions et variables en camelCase.
- Mobile (Kotlin) : suffixes Screen et ViewModel (ConnexionScreen, ConnexionViewModel).
- Reste en français : noms du métier (Utilisateur, Inscription, Jeton), méthodes (inscrire, chercherParEmail, genererJeton), variables (listeUtilisateurs, motDePasse), messages d'erreur, textes de l'interface, commentaires, URLs d'API (/api/auth/inscription).
- Pas d'accents dans le code. Noms imposés par les frameworks inchangés (getX/setX, useState, @Override...).

## Style de code (niveau L3 DSI, ISET Rades)
Code simple et lisible, que trois étudiants peuvent expliquer à leur soutenance.
- Interdits : streams et lambdas enchaînées, génériques avancés, réflexion, annotations personnalisées, design patterns (builder, factory), bibliothèques de mapping, Lombok, Optional chaîné, ternaires imbriqués, one-liners astucieux.
- Autorisés : records Java pour les DTO. L'entité JPA est une classe normale avec getters/setters.
- Couches : Controller -> Service -> DAO. Services = classes concrètes, sans interface. Conversion entité/DTO écrite à la main.
- Méthodes courtes (environ 25 lignes max), une seule tâche par méthode.
- Web : TypeScript simple, composants fonctionnels avec useState/useEffect, axios, pas de Redux ni React Query.
- Mobile : Kotlin simple, un ViewModel par écran, pas de Hilt/Koin, Retrofit + Gson.
- Un court commentaire en français au-dessus de chaque classe et méthode.

## Sécurité et organisation
- Jamais de secrets dans git (variables d'environnement). Mots de passe hachés BCrypt, jamais renvoyés par l'API.
- Code organisé par fonctionnalité (utilisateur, auth, ...) pour pouvoir ajouter les sprints suivants sans refonte.
- Ne jamais ajouter de fonctionnalité hors du sprint en cours.
- Ne jamais dire que quelque chose marche sans l'avoir exécuté ; signaler ce qui n'a pas pu être vérifié.
- Un commit par phase : "feat(sprintN): <nom de la phase>".
