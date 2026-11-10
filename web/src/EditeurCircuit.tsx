import { FormEvent, useState } from 'react';
import { Circuit, CircuitDemande, Destination, dinars, EtapeDemande } from './voyage';

type Props = {
  destinations: Destination[];
  circuit?: Circuit;
  destination?: Destination;
  attente: boolean;
  annuler: () => void;
  enregistrer: (demande: CircuitDemande) => void;
};

type EtapeEdition = EtapeDemande & { cle: number };
let cleEtape = 0;

/** Compose un circuit ordonne avant sauvegarde. */
export function EditeurCircuit({ destinations, circuit, destination, attente, annuler, enregistrer }: Props) {
  const [nom, definirNom] = useState(circuit ? (circuit.modele ? circuit.nom + ' · à ma façon' : circuit.nom) : '');
  const [etapes, definirEtapes] = useState<EtapeEdition[]>(() => {
    if (circuit) return circuit.etapes.map(e => ({ destinationId: e.destinationId, jours: e.jours, cle: ++cleEtape }));
    return destination ? [{ destinationId: destination.id, jours: 1, cle: ++cleEtape }] : [];
  });
  const [choix, definirChoix] = useState(destinations[0]?.id ?? 0);
  const duree = etapes.reduce((total, etape) => total + etape.jours, 0);
  const valide = nom.trim().length > 0 && etapes.length > 0 && duree <= 60 &&
    etapes.every(e => Number.isInteger(e.jours) && e.jours >= 1 && e.jours <= 30);

  /** Deplace une etape sans perdre son contenu. */
  function deplacer(index: number, sens: number) {
    const autre = index + sens;
    if (autre < 0 || autre >= etapes.length) return;
    const copie = [...etapes];
    [copie[index], copie[autre]] = [copie[autre], copie[index]];
    definirEtapes(copie);
  }

  /** Soumet uniquement les choix du voyageur. */
  function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (!valide || attente) return;
    enregistrer({
      nom: nom.trim(), budgetTnd: null,
      etapes: etapes.map(({ destinationId, jours }) => ({ destinationId, jours })),
      version: circuit && !circuit.modele ? circuit.version : undefined,
    });
  }

  return <form onSubmit={soumettre}>
    <div className="plan-champs">
      <label className="champ">Nom du circuit
        <input value={nom} onChange={e => definirNom(e.target.value)} maxLength={90}
          placeholder="Ex. Mon week-end sur la côte" required disabled={attente} />
      </label>
      <div className="plan-titre"><h3>Vos étapes</h3><span>{etapes.length} / 10</span></div>
      {etapes.length === 0 && <p className="secondaire">Ajoutez une première destination pour lancer le calcul.</p>}
      <ol className="liste-etapes">
        {etapes.map((etape, index) => {
          const lieu = destinations.find(d => d.id === etape.destinationId);
          return <li className="plan-etape" key={etape.cle}>
            <span className="numero-etape">{String(index + 1).padStart(2, '0')}</span>
            <div><strong>{lieu?.nom ?? 'Destination'}</strong><small>{dinars(lieu?.tarifJourTnd ?? 0)} / jour</small></div>
            <label className="champ mini-champ">Jours
              <input type="number" min={1} max={30} value={etape.jours || ''} required disabled={attente}
                aria-label={'Jours de l’étape ' + (index + 1)}
                onChange={e => definirEtapes(etapes.map(item => item.cle === etape.cle ? { ...item, jours: Number(e.target.value) } : item))} />
            </label>
            <div className="plan-actions">
              <button type="button" title="Monter" aria-label={'Monter l’étape ' + (index + 1)}
                onClick={() => deplacer(index, -1)} disabled={attente || index === 0}>↑</button>
              <button type="button" title="Descendre" aria-label={'Descendre l’étape ' + (index + 1)}
                onClick={() => deplacer(index, 1)} disabled={attente || index === etapes.length - 1}>↓</button>
              <button type="button" title="Retirer" aria-label={'Retirer l’étape ' + (index + 1)}
                onClick={() => definirEtapes(etapes.filter(e => e.cle !== etape.cle))} disabled={attente}>×</button>
            </div>
          </li>;
        })}
      </ol>
      <div className="ajout-etape">
        <select aria-label="Destination à ajouter" value={choix} onChange={e => definirChoix(Number(e.target.value))} disabled={attente}>
          {destinations.map(lieu => <option value={lieu.id} key={lieu.id}>{lieu.nom}</option>)}
        </select>
        <button className="bouton principal" type="button" disabled={!choix || etapes.length >= 10 || attente}
          onClick={() => definirEtapes([...etapes, { destinationId: choix, jours: 1, cle: ++cleEtape }])}>Ajouter</button>
      </div>
      {duree > 60 && <p className="message erreur" role="alert">60 jours maximum par circuit.</p>}
    </div>
    <div className="actions-dialogue">
      <button type="button" className="bouton secondaire-bouton" onClick={annuler} disabled={attente}>Annuler</button>
      <button type="submit" className="bouton principal" disabled={!valide || attente}>{attente ? 'Enregistrement…' : 'Enregistrer mon circuit'}</button>
    </div>
  </form>;
}
