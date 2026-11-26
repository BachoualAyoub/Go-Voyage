import { useState } from 'react';
import { api, erreurApi } from './api';
import { Vol, dateSejour } from './sejour';
import { dinars } from './voyage';
import './sejour.css';

type EtapeVol = { cle: number; depart: string; arrivee: string; date: string };
const villes = ['Paris', 'Lyon', 'Tunis', 'Djerba', 'Tozeur'];

export function VolsPage() {
  const [mode, setMode] = useState('simple');
  const [etapes, setEtapes] = useState<EtapeVol[]>([{ cle: 0, depart: 'Paris', arrivee: 'Tunis', date: dateSejour(7) }]);
  const [offres, setOffres] = useState<Vol[][]>([]);
  const [choix, setChoix] = useState<Record<number, Vol>>({});
  const [erreur, setErreur] = useState('');
  const [attente, setAttente] = useState(false);
  const [categorie, setCategorie] = useState('');

  function changerMode(valeur: string) {
    setMode(valeur); setOffres([]); setChoix({});
    const aller = etapes[0];
    setEtapes(valeur === 'simple' ? [aller] : [aller, { cle: 1, depart: aller.arrivee, arrivee: aller.depart, date: dateSejour(14) }]);
  }

  function changer(index: number, champ: keyof EtapeVol, valeur: string) {
    setEtapes(etapes.map((e, i) => i === index ? { ...e, [champ]: valeur } : e)); setOffres([]); setChoix({});
  }

  async function rechercher() {
    setAttente(true); setErreur(''); setChoix({}); setOffres([]);
    try {
      if (mode === 'retour' && (etapes[0].depart !== etapes[1].arrivee || etapes[0].arrivee !== etapes[1].depart)) throw new Error('Pour un aller-retour, inversez les villes du retour.');
      for (let i = 1; i < etapes.length; i++) if (etapes[i].date < etapes[i - 1].date) throw new Error('Respectez l’ordre chronologique des trajets.');
      const reponses = await Promise.all(etapes.map(e => api.get<Vol[]>('/api/vols', { params: e })));
      setOffres(reponses.map(r => r.data));
    } catch (e) { setErreur(e instanceof Error && !('isAxiosError' in e) ? e.message : erreurApi(e).message); }
    finally { setAttente(false); }
  }
  return <main className="atelier-page" id="contenu">
    <div className="titre-editorial"><p className="surtitre">L’ENVIE DE PARTIR</p><h1>Le voyage commence<br /><em>par un choix.</em></h1><p>Comparez les vols, étape par étape.</p></div>
    <p className="note-source">Offres de démonstration · Tarifs indicatifs par personne · Aucune billetterie réelle.</p>
    <div className="onglets-pilules">{[['simple','Aller simple'],['retour','Aller-retour'],['multiple','Multi-destinations']].map(([v,l]) =>
      <button aria-pressed={mode === v} key={v} onClick={() => changerMode(v)}>{l}</button>)}</div>
    <form className="plan-vols" onSubmit={e => { e.preventDefault(); void rechercher(); }}>
      {etapes.map((etape, i) => <div className="recherche-sejour" key={etape.cle}>
        <label>Départ<select value={etape.depart} onChange={e => changer(i,'depart',e.target.value)}>{villes.map(v => <option key={v}>{v}</option>)}</select></label>
        <label>Arrivée<select value={etape.arrivee} onChange={e => changer(i,'arrivee',e.target.value)}>{villes.map(v => <option key={v}>{v}</option>)}</select></label>
        <label>Date<input type="date" min={i ? etapes[i - 1].date : dateSejour(0)} value={etape.date} required onChange={e => changer(i,'date',e.target.value)} /></label>
        {mode === 'multiple' && i > 0 && <button className="bouton secondaire-bouton" type="button" onClick={() => { setEtapes(etapes.filter((_, n) => n !== i)); setOffres([]); setChoix({}); }}>Retirer</button>}
      </div>)}
      <div className="actions-dialogue">{mode === 'multiple' && etapes.length < 4 && <button className="bouton secondaire-bouton" type="button" onClick={() => {
        setEtapes([...etapes, { cle: Math.max(...etapes.map(e => e.cle)) + 1, depart: etapes.at(-1)!.arrivee, arrivee: 'Tunis', date: dateSejour(21) }]); setOffres([]); setChoix({});
      }}>Ajouter un trajet</button>}<button className="bouton principal" disabled={attente}>{attente ? 'Comparaison…' : 'Comparer les offres'}</button></div>
    </form>
    {erreur && <p className="retour-action" role="alert">{erreur}</p>}
    <div className="entete-selection"><label>Compagnie<select value={categorie} onChange={e => { setCategorie(e.target.value); setChoix({}); }}><option value="">Toutes les catégories</option>
      <option value="REGULIERE">Régulière</option><option value="LOW_COST">Low-cost</option><option value="CHARTER">Charter</option></select></label>
      <p aria-live="polite" className="total-selection">Total sélectionné <strong>{dinars(Object.values(choix).reduce((s,v) => s + v.prix,0))}</strong> · {Object.keys(choix).length}/{etapes.length} trajet(s)</p></div>
    {offres.map((liste,i) => <section key={i} className="resultats-vols"><h2>{etapes[i].depart} → {etapes[i].arrivee} <small>{etapes[i].date}</small></h2>
      {!liste.filter(v => !categorie || v.categorie === categorie).length && <p className="vide-parcours">Aucune offre pour ce trajet. Essayez une autre ville ou catégorie.</p>}
      {liste.filter(v => !categorie || v.categorie === categorie).map(v => <button key={v.id_v} className="billet-vol" aria-pressed={choix[i]?.id_v === v.id_v} onClick={() => setChoix({ ...choix, [i]: v })}>
        <span><b>{v.compagnie}</b><small>{v.bagage}</small></span><span><b>{v.heure_depart}</b><small>{v.duree_minutes} min · {v.categorie.replace('_',' ')}</small></span>
        <strong>{dinars(v.prix)}</strong><span>{choix[i]?.id_v === v.id_v ? '✓ Sélectionné' : 'Sélectionner'}</span></button>)}
    </section>)}
  </main>;
}
