import { FormEvent, useEffect, useRef, useState } from 'react';
import { api, autorisation, erreurApi, ErreurApi, Profil, Role } from './api';
import { Champ, lireCoordonnees, Message } from './ChampsCompte';

const route = '/api/admin/utilisateurs';
type Action = { type: 'ajout' } | { type: 'edition' | 'suppression'; compte: Profil };

/** Permet a un administrateur de gerer les comptes. */
export function AdministrationPage({ jeton, admin, actualiser }: { jeton: string; admin: Profil; actualiser: () => void }) {
  const [comptes, definirComptes] = useState<Profil[]>([]);
  const [filtre, definirFiltre] = useState('');
  const [action, definirAction] = useState<Action>();
  const [erreurListe, definirErreurListe] = useState<ErreurApi>();
  const [erreur, definirErreur] = useState<ErreurApi>();
  const [succes, definirSucces] = useState('');
  const [chargement, definirChargement] = useState(true);
  const [attente, definirAttente] = useState(false);
  const dialogue = useRef<HTMLDialogElement>(null);

  /** Charge les comptes a l'ouverture et apres une operation. */
  async function charger(signal?: AbortSignal) {
    definirChargement(true);
    definirErreurListe(undefined);
    try {
      const reponse = await api.get<Profil[]>(route, { ...autorisation(jeton), signal });
      definirComptes(reponse.data);
    } catch (cause) {
      if (signal?.aborted) return;
      const retour = erreurApi(cause);
      definirErreurListe(retour);
      if (retour.statut === 401 || retour.statut === 403) actualiser();
    } finally {
      if (!signal?.aborted) definirChargement(false);
    }
  }

  useEffect(() => {
    const controle = new AbortController();
    void charger(controle.signal);
    return () => controle.abort();
  }, [jeton]);

  useEffect(() => {
    if (action && !dialogue.current?.open) dialogue.current?.showModal();
  }, [action]);

  /** Ouvre le formulaire ou la confirmation sans conserver les erreurs precedentes. */
  function ouvrir(nouvelleAction: Action) {
    definirErreur(undefined);
    definirSucces('');
    definirAction(nouvelleAction);
  }

  /** Enregistre l'operation choisie puis recharge les comptes. */
  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (!action) return;
    const champs = new FormData(evenement.currentTarget);
    const config = autorisation(jeton);
    definirAttente(true);
    definirErreur(undefined);
    try {
      if (action.type === 'suppression') {
        await api.delete(route + '/' + action.compte.id, config);
        definirSucces('Compte supprimé.');
      } else if (action.type === 'edition') {
        await api.put(route + '/' + action.compte.id, { ...lireCoordonnees(champs), role: champs.get('role') }, config);
        definirSucces('Compte modifié.');
        if (action.compte.id === admin.id) actualiser();
      } else {
        const compte = { ...lireCoordonnees(champs), motDePasse: String(champs.get('motDePasse')) };
        await api.post(route, { compte, role: champs.get('role') }, config);
        definirSucces('Compte ajouté.');
      }
      dialogue.current?.close();
      definirAction(undefined);
      await charger();
    } catch (cause) {
      const retour = erreurApi(cause);
      definirErreur(retour);
      if (retour.statut === 401 || retour.statut === 403) actualiser();
    } finally {
      definirAttente(false);
    }
  }

  const recherche = filtre.trim().toLowerCase();
  const comptesVisibles = comptes.filter((compte) =>
    (compte.prenom + ' ' + compte.nom + ' ' + compte.email).toLowerCase().includes(recherche));
  const compteEdite = action && 'compte' in action ? action.compte : undefined;
  const titre = action?.type === 'suppression' ? 'Supprimer le compte ?' :
    action?.type === 'edition' ? 'Modifier le compte' : 'Ajouter un compte';
  const prefixe = action?.type === 'ajout' ? 'compte.' : '';
  const roleDefaut: Role = compteEdite?.role ?? 'USER';

  return <main className="espace" id="contenu">
    <div className="en-tete-espace">
      <div>
        <p className="surtitre"><i /> ADMINISTRATION</p>
        <h1>Comptes utilisateurs</h1>
        <p className="secondaire">Gérez les accès à GO VOYAGE.</p>
      </div>
      <button className="bouton principal" type="button" onClick={() => ouvrir({ type: 'ajout' })}>＋ Ajouter un compte</button>
    </div>
    <Message erreur={erreurListe} enfants={succes} />
    <section className="panneau" aria-label="Liste des comptes">
      <div className="outils">
        <span className="compteur">{comptes.length} compte{comptes.length === 1 ? '' : 's'}</span>
        <input aria-label="Rechercher un compte" type="search" placeholder="Rechercher un compte…" value={filtre}
          onChange={(evenement) => definirFiltre(evenement.target.value)} />
      </div>
      {chargement ? <p className="vide" role="status">Chargement des comptes…</p> :
        erreurListe ? <p className="vide"><button className="bouton secondaire-bouton" onClick={() => charger()}>Réessayer</button></p> :
        comptesVisibles.length === 0 ? <p className="vide">Aucun compte trouvé.</p> :
        <div className="table-scroll"><table>
          <thead><tr><th>Compte</th><th>Téléphone</th><th>Rôle</th><th>Actions</th></tr></thead>
          <tbody>{comptesVisibles.map((compte) => <tr key={compte.id}>
            <td><strong>{compte.prenom} {compte.nom}</strong><small>{compte.email}</small></td>
            <td>{compte.tel}</td>
            <td><span className={'badge ' + (compte.role === 'ADMIN' ? 'badge-admin' : '')}>
              {compte.role === 'ADMIN' ? 'Administrateur' : 'Utilisateur'}</span></td>
            <td className="actions">
              <button type="button" aria-label={'Modifier ' + compte.email} onClick={() => ouvrir({ type: 'edition', compte })}>Modifier</button>
              {compte.id !== admin.id && <button type="button" className="danger-texte" aria-label={'Supprimer ' + compte.email}
                onClick={() => ouvrir({ type: 'suppression', compte })}>Supprimer</button>}
            </td>
          </tr>)}</tbody>
        </table></div>}
    </section>
    <dialog ref={dialogue} aria-labelledby="dialogue-titre" onClose={() => definirAction(undefined)}
      onCancel={(evenement) => { if (attente) evenement.preventDefault(); }}>
      {action && <form onSubmit={soumettre}>
        <h2 id="dialogue-titre">{titre}</h2>
        <Message erreur={erreur} />
        {action.type === 'suppression' ?
          <p>Le compte <strong>{compteEdite?.email}</strong> sera supprimé.</p> :
          <>
            <div className="duo">
              <Champ nom="prenom" label="Prénom" max={100} valeur={compteEdite?.prenom} erreur={erreur?.erreurs?.[prefixe + 'prenom']} />
              <Champ nom="nom" label="Nom" max={100} valeur={compteEdite?.nom} erreur={erreur?.erreurs?.[prefixe + 'nom']} />
            </div>
            <Champ nom="email" label="Adresse e-mail" type="email" max={100} valeur={compteEdite?.email} erreur={erreur?.erreurs?.[prefixe + 'email']} />
            <Champ nom="tel" label="Téléphone" type="tel" max={20} valeur={compteEdite?.tel} erreur={erreur?.erreurs?.[prefixe + 'tel']} />
            {action.type === 'ajout' && <Champ nom="motDePasse" label="Mot de passe" type="password" autocomplete="new-password"
              min={8} max={72} erreur={erreur?.erreurs?.['compte.motDePasse']} />}
            <label className="champ">Rôle
              <select name="role" defaultValue={roleDefaut}>
                {compteEdite?.id !== admin.id && <option value="USER">Utilisateur</option>}
                <option value="ADMIN">Administrateur</option>
              </select>
            </label>
          </>}
        <div className="actions-dialogue">
          <button className="bouton secondaire-bouton" type="button" disabled={attente} onClick={() => dialogue.current?.close()}>Annuler</button>
          <button className={'bouton ' + (action.type === 'suppression' ? 'danger' : 'principal')} type="submit" disabled={attente}>
            {attente ? 'Veuillez patienter…' : action.type === 'suppression' ? 'Supprimer' : 'Enregistrer'}
          </button>
        </div>
      </form>}
    </dialog>
  </main>;
}
