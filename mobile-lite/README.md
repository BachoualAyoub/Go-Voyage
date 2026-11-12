# GO-VOYAGE Lite (beta)

Version allégée de démonstration de l'application mobile GO-VOYAGE, **100 % hors-ligne** :
aucune base de données, aucun serveur, aucune permission. Tout fonctionne avec des
**données fictives en mémoire** — les listes repartent de zéro à chaque relance de l'app
(comportement normal pour une beta).

Concepts utilisés uniquement : une seule Activity (`MainActivity` avec tiroir latéral),
**Fragments** partout, **ViewBinding**, **Navigation Component**, **ViewModel + LiveData**
(données créées dans le `init {}`), **RecyclerView** + Adapter, **AlertDialog** et **Toast**.

## Comptes de démonstration

| Compte | E-mail | Mot de passe | Rôle |
| --- | --- | --- | --- |
| Démo admin | `admin@govoyage.tn` | `admin123` | Administrateur |
| Démo voyageur | `sarra@govoyage.tn` | `sarra123` | Utilisateur |

L'écran d'inscription ajoute un nouveau compte dans la liste en mémoire (mot de passe
8 caractères minimum, e-mail unique).

## Écrans

- **Connexion / Inscription** (toolbar et tiroir masqués) ;
- **Explorer** : 8 destinations tunisiennes, recherche (nom, ville, région), filtres d'ambiance,
  circuits proposés ;
- **Fiche destination** et **fiche circuit** (itinéraire, jours, estimation en TND) ;
- **Mes circuits** : carnet du compte connecté — création, modification, suppression
  (chacun a son carnet, conservé en mémoire tant que l'app tourne) ;
- **Éditeur de circuit** : nom, budget, étapes (1 à 30 jours chacune, 10 étapes et 60 jours max),
  marge calculée en direct ;
- **Mon compte** : profil et déconnexion (confirmation par AlertDialog).

## Compilation et installation

```bash
cd mobile-lite
./gradlew :app:assembleDebug
```

APK généré : `app/build/outputs/apk/debug/app-debug.apk`
(`tn.govoyage.mobile.lite`, installable à côté de la version complète).

## Limites volontaires de la beta

- Aucune persistance : comptes créés, circuits et carnets disparaissent à la fermeture.
- Aucun appel API : les tarifs et itinéraires sont fictifs.
- La gestion des comptes ADMIN de la version complète n'existe pas ici.
