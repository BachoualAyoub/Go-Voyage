import { useEffect, useRef, useState } from 'react';
import { api, autorisation, erreurApi, ErreurApi } from './api';
import { Message } from './ChampsCompte';
import { EditeurCircuit } from './EditeurCircuit';
import { Ambiance, ambiances, Circuit, CircuitDemande, Destination, dinars, normaliserRecherche } from './voyage';
import './voyage.css';

type Dialogue = { type: 'edition'; circuit?: Circuit; destination?: Destination } |
  { type: 'suppression'; circuit: Circuit };
type Props = { jeton: string; onglet: 'explorer' | 'circuits'; afficherCircuits: () => void; actualiser: () => void; demanderConnexion: (motif: string) => void };

/** Affiche un circuit du catalogue ou du compte, avec ses actions. */
function CarteCircuit({ circuit, ouvrir, supprimer }: { circuit: Circuit; ouvrir: () => void; supprimer?: () => void }) {
  return <article className="carte-circuit">
    <div className="circuit-entete"><span className="voyage-etiquette">{circuit.modele ? 'INSPIRATION' : 'MON ITINÉRAIRE'}</span>
      <span>{circuit.dureeJours} j · {circuit.etapes.length} étapes</span></div>
    <h3>{circuit.nom}</h3>
    {circuit.description && <p className="secondaire">{circuit.description}</p>}
    <ol className="trace-circuit">{circuit.etapes.map((etape, index) =>
      <li key={index}><span>{etape.nom}</span><small>{etape.jours} j</small></li>)}</ol>
    <div className="circuit-bas"><div><strong>{dinars(circuit.estimationTnd)}</strong><small> estimés</small></div>
      <button className="bouton secondaire-bouton" type="button" onClick={ouvrir}>{circuit.modele ? 'Personnaliser' : 'Modifier'}</button>
      {supprimer && <button className="texte-supprimer" type="button" onClick={supprimer}>Supprimer</button>}
    </div>
  </article>;
}

/** Charge le catalogue public et, si connecte, les inspirations et les circuits personnels. */
export function EspaceVoyage({ jeton, onglet, afficherCircuits, actualiser, demanderConnexion }: Props) {
  const [destinations, definirDestinations] = useState<Destination[]>([]);
  const [modeles, definirModeles] = useState<Circuit[]>([]);
  const [circuits, definirCircuits] = useState<Circuit[]>([]);
  const [recherche, definirRecherche] = useState('');
  const [filtre, definirFiltre] = useState<Ambiance | ''>('');
  const [dialogue, definirDialogue] = useState<Dialogue>();
  const [erreur, definirErreur] = useState<ErreurApi>();
  const [erreurDialogue, definirErreurDialogue] = useState<ErreurApi>();
  const [succes, definirSucces] = useState('');
  const [chargement, definirChargement] = useState(true);
  const [attente, definirAttente] = useState(false);
  const [recharge, definirRecharge] = useState(0);
  const modale = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const controle = new AbortController();
    const config = jeton ? { ...autorisation(jeton), signal: controle.signal } : { signal: controle.signal };
    definirChargement(true);
    definirErreur(undefined);
    // Un visiteur sans compte consulte le catalogue public : jamais l'appel aux circuits personnels.
    const mesCircuits = jeton ? api.get<Circuit[]>('/api/circuits', config) : Promise.resolve(undefined);
    Promise.all([api.get<Destination[]>('/api/destinations', config),
      api.get<Circuit[]>('/api/circuits/modeles', config), mesCircuits])
      .then(([catalogue, inspirations, personnels]) => {
        if (controle.signal.aborted) return;
        definirDestinations(catalogue.data); definirModeles(inspirations.data);
        if (personnels) definirCircuits(personnels.data);
      }).catch(cause => {
        if (controle.signal.aborted) return;
        const retour = erreurApi(cause); definirErreur(retour);
        if (retour.statut === 401) actualiser();
      }).finally(() => { if (!controle.signal.aborted) definirChargement(false); });
    return () => controle.abort();
  }, [jeton, recharge, actualiser]);

  useEffect(() => {
    if (dialogue && !modale.current?.open) modale.current?.showModal();
  }, [dialogue]);

  /** Ouvre une action avec son propre etat d'erreur. */
  function ouvrir(action: Dialogue) { definirErreurDialogue(undefined); definirDialogue(action); }

  /** Ouvre l'atelier d'itineraire, ou oriente vers la connexion le visiteur sans compte. */
  function composer(circuit?: Circuit, destination?: Destination) {
    if (!jeton) {
      demanderConnexion('Connectez-vous ou créez votre compte pour personnaliser cet itinéraire.');
      return;
    }
    ouvrir({ type: 'edition', circuit, destination });
  }

  /** Met a jour la liste locale apres une reponse reussie. */
  async function enregistrer(demande: CircuitDemande) {
    if (dialogue?.type !== 'edition') return;
    const id = dialogue.circuit && !dialogue.circuit.modele ? dialogue.circuit.id : undefined;
    definirAttente(true); definirErreurDialogue(undefined);
    try {
      const reponse = id ? await api.put<Circuit>('/api/circuits/' + id, demande, autorisation(jeton)) :
        await api.post<Circuit>('/api/circuits', demande, autorisation(jeton));
      definirCircuits(liste => id ? liste.map(item => item.id === id ? reponse.data : item) : [reponse.data, ...liste]);
      modale.current?.close(); definirSucces(id ? 'Circuit mis à jour.' : 'Votre circuit est enregistré.');
      afficherCircuits();
    } catch (cause) {
      const retour = erreurApi(cause); definirErreurDialogue(retour);
      if (retour.statut === 401) actualiser();
      if (retour.statut === 409) definirRecharge(n => n + 1);
    } finally { definirAttente(false); }
  }

  /** Confirme la suppression avant d'enlever le circuit de la liste. */
  async function supprimer() {
    if (dialogue?.type !== 'suppression') return;
    const id = dialogue.circuit.id;
    definirAttente(true); definirErreurDialogue(undefined);
    try {
      await api.delete('/api/circuits/' + id, autorisation(jeton));
      definirCircuits(liste => liste.filter(item => item.id !== id));
      modale.current?.close(); definirSucces('Circuit supprimé.');
    } catch (cause) {
      const retour = erreurApi(cause); definirErreurDialogue(retour);
      if (retour.statut === 401) actualiser();
    } finally { definirAttente(false); }
  }

  const visibles = destinations.filter(lieu => (!filtre || lieu.ambiance === filtre) &&
    normaliserRecherche([lieu.nom, lieu.ville, lieu.pays, lieu.region].join(' ')).includes(normaliserRecherche(recherche)));

  return <main className="voyage-main" id="contenu">
    {onglet === 'explorer' ?
      <section className="hero-explorer">
        <div><span className="voyage-etiquette">GO VOYAGE · CARNET DE ROUTE</span>
          <h1>La Tunisie,<br />à votre rythme.</h1>
          <p>Une envie de mer, de médina ou de désert ? Choisissez vos étapes et dessinez le voyage qui vous ressemble.</p>
          <div className="hero-actions">
            <a className="bouton principal" href="#destinations">Explorer les destinations ↓</a>
            <a className="lien-hero" href="#modeles">Voir les circuits proposés</a>
          </div></div>
        <span className="hero-legende">SIDI BOU SAÏD · TUNISIE</span>
      </section> :
      <header className="titre-circuits">
        <div><p className="voyage-etiquette">VOTRE CARNET</p><h1>Mes circuits</h1>
          <p className="secondaire">Vos itinéraires, prêts à être repris et ajustés.</p></div>
        <div className="actions-dialogue">
          <button className="bouton secondaire-bouton" type="button" disabled={chargement || attente}
            onClick={() => definirRecharge(n => n + 1)}>Actualiser</button>
          <button className="bouton principal" type="button" disabled={chargement || !destinations.length}
            onClick={() => composer()}>Nouveau circuit</button>
        </div>
      </header>}
    <Message erreur={erreur} enfants={succes} />
    {chargement && <p role="status" className="etat-voyage">Chargement de votre carnet…</p>}
    {erreur && <button className="bouton secondaire-bouton" type="button" onClick={() => definirRecharge(n => n + 1)}>Réessayer</button>}
    {!chargement && !erreur && (onglet === 'explorer' ? <>
      <section className="section-voyage" id="destinations">
        <div className="titre-section"><div><p className="voyage-etiquette">01 · DESTINATIONS</p><h2>Où partir ?</h2></div>
          <label className="recherche-voyage"><span className="visuellement-cache">Rechercher une destination</span>
            <input type="search" placeholder="Ville, région ou pays…" value={recherche} onChange={e => definirRecherche(e.target.value)} /></label>
        </div>
        <div className="filtres-voyage" aria-label="Ambiance">
          <button type="button" aria-pressed={!filtre} onClick={() => definirFiltre('')}>Toutes</button>
          {(Object.keys(ambiances) as Ambiance[]).map(ambiance =>
            <button type="button" key={ambiance} aria-pressed={filtre === ambiance} onClick={() => definirFiltre(ambiance)}>{ambiances[ambiance]}</button>)}
          <span aria-live="polite">{visibles.length} destination{visibles.length === 1 ? '' : 's'}</span>
        </div>
        {visibles.length ? <div className="grille-destinations">{visibles.map(lieu =>
          <article className="carte-destination" key={lieu.id}>
            <div className={'scene-destination scene-' + lieu.ambiance.toLowerCase()}>
              <img src={'/destinations/' + lieu.code + '.jpg'} alt={'Photo de ' + lieu.nom} loading="lazy" />
              <span>{ambiances[lieu.ambiance]}</span>
            </div>
            <div className="carte-corps"><span className="voyage-etiquette">{lieu.region} · {lieu.pays}</span>
              <h3>{lieu.nom}</h3><p>{lieu.accroche}</p>
              <div><strong>{dinars(lieu.tarifJourTnd)}<small> / jour</small></strong>
                <button type="button" className="bouton principal"
                  onClick={() => composer(undefined, lieu)}>Commencer un circuit</button></div>
            </div>
          </article>)}</div> : <p className="etat-voyage">Aucune destination ne correspond. Essayez une autre recherche.</p>}
      </section>
      <section className="section-voyage" id="modeles"><div className="titre-section"><div><p className="voyage-etiquette">02 · CIRCUITS PROPOSÉS</p>
        <h2>Des idées de départ</h2></div></div>
        <div className="grille-circuits">{modeles.map(circuit => <CarteCircuit key={circuit.id} circuit={circuit}
          ouvrir={() => composer(circuit)} />)}</div>
      </section>
    </> : <section aria-label="Circuits personnels">
      {!circuits.length ? <p className="etat-voyage">Votre carnet est vide. Créez un circuit ou personnalisez une inspiration du catalogue.</p> :
        <div className="grille-circuits">{circuits.map(circuit => <CarteCircuit key={circuit.id} circuit={circuit}
          ouvrir={() => composer(circuit)} supprimer={() => ouvrir({ type: 'suppression', circuit })} />)}</div>}
    </section>)}
    <dialog ref={modale} className={dialogue?.type === 'edition' ? 'dialogue-circuit' : 'dialogue-voyage'}
      aria-labelledby="titre-dialogue-voyage" onClose={() => definirDialogue(undefined)}
      onCancel={e => { if (attente) e.preventDefault(); }}>
      {dialogue?.type === 'edition' && <>
        <p className="voyage-etiquette">ATELIER D’ITINÉRAIRE</p>
        <h2 id="titre-dialogue-voyage">{dialogue.circuit && !dialogue.circuit.modele ? 'Modifier le circuit' : 'Composer un circuit'}</h2>
        <Message erreur={erreurDialogue} />
        <EditeurCircuit destinations={destinations} circuit={dialogue.circuit} destination={dialogue.destination}
          attente={attente} annuler={() => modale.current?.close()} enregistrer={enregistrer} />
      </>}
      {dialogue?.type === 'suppression' && <>
        <h2 id="titre-dialogue-voyage">Supprimer ce circuit ?</h2>
        <p>{dialogue.circuit.nom} sera retiré de votre carnet.</p>
        <Message erreur={erreurDialogue} />
        <div className="actions-dialogue"><button className="bouton secondaire-bouton" disabled={attente}
          onClick={() => modale.current?.close()}>Annuler</button>
          <button className="bouton danger" onClick={supprimer} disabled={attente}>Supprimer le circuit</button></div>
      </>}
    </dialog>
  </main>;
}
