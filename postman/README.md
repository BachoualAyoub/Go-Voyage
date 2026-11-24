# Postman - GO VOYAGE

Dossier pret a l'import dans Postman.

## Fichiers

- `GO-Voyage.postman_collection.json` : 9 dossiers, 48 requetes API.
- `GO-Voyage.postman_environment.json` : environnement `GO VOYAGE - Dev local`.

## Import

1. Dans Postman : **File > Import**.
2. Importer les deux fichiers JSON.
3. Selectionner l'environnement `GO VOYAGE - Dev local` en haut a droite. Il est optionnel : la collection contient deja les memes valeurs par defaut, prises sur la base de developpement.
4. Demarrer le backend sur `http://127.0.0.1:8081` depuis ton shell VS Code.
5. Executer d'abord `00 - Authentification > Connexion USER` et `Connexion ADMIN`.
6. Terminer par `00 - Authentification > Deconnexion` pour effacer les jetons.

Les scripts de ces deux connexions remplissent automatiquement les variables de collection `jeton` et `adminJeton`. Les autres requetes protegees utilisent ces variables. `Deconnexion` appelle `POST /api/auth/deconnexion` puis efface les deux jetons cote Postman. Le backend garde des JWT sans session : la route repond 204 sans revoquer le jeton cote serveur.

Les corps JSON contiennent des donnees directes, comme dans le projet SOA : comptes reels, photo deja present en base, hotel id 1, dates en clair. Aucun corps JSON n'utilise de variable `{{...}}`.

## Ordre conseille

1. `00 - Authentification` : connexions, `Mon compte USER`, `Mon compte ADMIN`, profil, deconnexion.
2. `01 - Catalogue public` : destinations et circuits modeles.
3. `02 - Circuits du voyageur` : creer, consulter, modifier, supprimer.
4. `03 - Medias` : importer une photo puis la lire.
5. `04 - Livre d or` : publier, modifier, avis, likes, signalement, retrait.
6. `05 - Hebergements, vols et reservations`.
7. `06 - Administration des comptes`.
8. `07 - Administration du catalogue`.
9. `08 - Administration de la moderation`.

Plusieurs requetes capturent automatiquement les identifiants cres : `circuitCreeId`, `utilisateurCreeId`, `hotelCreeId`, `destinationCreeId`, `publicationCreeId`, `reservationId`, `mediaUrl`, `mediaType` et `mediaId`. Il faut donc lancer la creation avant la modification ou la suppression correspondante.

## Variables a adapter

Les valeurs de l'environnement sont celles de la base de developpement :

- `baseUrl` : `http://127.0.0.1:8081`
- comptes de demonstration : `ayoub@mail.tn` / `user12345` et `admin@gmail.com` / `admin1234`
- `destinationId`, `publicationId`, `publicationAutreId`, `hotelId`, `montantAttendu`
- `cleReservation` : cle fixe pour rendre la reservation rejouable
- `cheminPhoto` : chemin absolu d'un JPG/PNG/MP4 a importer

Les dates sont ecrites en clair : `2026-12-24` au depart et `2026-12-27` au retour, soit 3 nuits. A les remplacer si la periode n'est plus valable.

## Verifications effectuees

Les deux fichiers JSON sont valides et analysables. Les 48 requetes, les deux jetons, les corps JSON directs et la deconnexion ont ete verifies statiquement. Les 48 URLs ont ete comparees aux mappings des controllers : aucune URL inconnue. Le backend a ete recompile avec la route `POST /api/auth/deconnexion`. Aucun appel API n'a ete relance : le backend reste ferme comme demande.
