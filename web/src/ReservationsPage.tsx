import { FormEvent, useCallback, useEffect, useState } from 'react';
import { useDialogueVoyage } from './useDialogueVoyage';
import { api, autorisation, erreurApi } from './api';
import { Hotel, Reservation, dateSejour, imageSejour } from './sejour';
import { dinars } from './voyage';
import './sejour.css';

type Props = { jeton: string; demanderConnexion: (motif: string) => void };

/** Recherche les sejours et fait confirmer le prix avant la reservation. */
export function ReservationsPage({ jeton, demanderConnexion }: Props) {
  const [arrivee, setArrivee] = useState(dateSejour());
  const [depart, setDepart] = useState(dateSejour(3));
  const [voyageurs, setVoyageurs] = useState(2);
  const [chambres, setChambres] = useState(1);
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [ville, setVille] = useState('');
  const [tri, setTri] = useState('prix');
  const [message, setMessage] = useState('');
  const [attente, setAttente] = useState(false);
  const [recherche, setRecherche] = useState(0);
  const [confirmation, setConfirmation] = useState<{ hotel: Hotel; cle: string }>();
  const [envoi, setEnvoi] = useState(false);
  const fermer = useCallback(() => { if (!envoi) setConfirmation(undefined); }, [envoi]);
  useDialogueVoyage(!!confirmation, fermer);

  useEffect(() => {
    const controle = new AbortController();
    setAttente(true);
    api.get<Hotel[]>('/api/hotels', { params: { arrivee, depart, voyageurs, chambres }, signal: controle.signal })
      .then(r => { if (!controle.signal.aborted) setHotels(r.data); })
      .catch(e => { if (!controle.signal.aborted) { setMessage(erreurApi(e).message); setHotels([]); } })
      .finally(() => { if (!controle.signal.aborted) setAttente(false); });
    return () => controle.abort();
  }, [arrivee, depart, voyageurs, chambres, recherche]);

  /** Envoie la meme cle si une tentative doit etre rejouee. */
  async function reserver() {
    if (!confirmation || envoi) return;
    setEnvoi(true); setMessage('');
    try {
      await api.post('/api/reservations', {
        hotelId: confirmation.hotel.id_h, arrivee, depart, voyageurs, chambres, cle: confirmation.cle,
        montantAttendu: confirmation.hotel.total,
      }, autorisation(jeton));
      setConfirmation(undefined); setMessage('Réservation confirmée. Retrouvez-la dans Mes réservations.');
      setRecherche(n => n + 1);
    } catch (e) { setMessage(erreurApi(e).message); }
    finally { setEnvoi(false); }
  }
  const visibles = hotels.filter(h => !ville || h.ville === ville).sort((a, b) =>
    tri === 'etoiles' ? b.etoiles - a.etoiles : (a.total ?? 0) - (b.total ?? 0));

  return <main className="atelier-page" id="contenu">
    <section className="sejour-hero"><div><p className="surtitre">VOTRE PROCHAINE ESCALE</p>
      <h1>Un lieu pour ralentir.<br /><em>Un séjour à vous.</em></h1>
      <p>Choisissez vos dates et comparez les séjours disponibles.</p></div>
      <div className="hero-prix"><span>Votre sélection</span><strong>{voyageurs} voyageur{voyageurs > 1 ? 's' : ''}</strong>
        <span>{chambres} chambre{chambres > 1 ? 's' : ''} · Tunisie</span></div></section>
    <p className="note-source">Catalogue de démonstration · Prix en TND · Confirmation interne, sans paiement en ligne.</p>
    <form className="recherche-sejour" onSubmit={(e: FormEvent) => { e.preventDefault(); setRecherche(n => n + 1); }}>
      <label>Arrivée<input type="date" min={dateSejour(0)} value={arrivee} onChange={e => { setConfirmation(undefined); setArrivee(e.target.value); }} required /></label>
      <label>Départ<input type="date" min={arrivee} value={depart} onChange={e => { setConfirmation(undefined); setDepart(e.target.value); }} required /></label>
      <label>Voyageurs<input type="number" min="1" max="30" value={voyageurs} onChange={e => { setConfirmation(undefined); setVoyageurs(Number(e.target.value)); }} /></label>
      <label>Chambres<input type="number" min="1" max="10" value={chambres} onChange={e => { setConfirmation(undefined); setChambres(Number(e.target.value)); }} /></label>
      <button className="bouton principal" disabled={attente}>Actualiser</button>
    </form>
    {message && <p role="status" className="retour-action">{message}</p>}
    <div className="entete-selection"><h2>{attente ? 'Recherche des disponibilités…' : visibles.length + ' escales à découvrir'}</h2>
      <label>Ville<select value={ville} onChange={e => setVille(e.target.value)}><option value="">Toutes les villes</option>
        {[...new Set(hotels.map(h => h.ville))].map(v => <option key={v}>{v}</option>)}</select></label>
      <label>Trier<select value={tri} onChange={e => setTri(e.target.value)}><option value="prix">Prix croissant</option><option value="etoiles">Étoiles</option></select></label></div>
    <div className="cartes-hotels">{visibles.map(h => <article className="carte-hotel" key={h.id_h}>
      <img src={imageSejour(h.image)} alt={h.ville} onError={e => { e.currentTarget.src = '/voyage-sidi.png'; }} />
      <div className="hotel-contenu"><span className="etoiles" aria-label={h.etoiles + ' étoiles'}>{'★'.repeat(h.etoiles)} · {h.ville}</span>
        <h3>{h.nom}</h3><p>{h.description}</p><p>{h.disponibles} chambre(s) disponible(s) · {h.capacite} personnes par chambre</p>
        <div className="hotel-prix"><div><strong>{dinars(h.total ?? 0)}</strong><small> pour le séjour · {dinars(h.tarif_nuit)} / chambre / nuit</small></div>
          <button className="bouton principal" disabled={!h.reservable || attente} onClick={() => {
            if (!jeton) demanderConnexion('Connectez-vous pour confirmer votre réservation.');
            else { setMessage(''); setConfirmation({ hotel: h, cle: crypto.randomUUID() }); }
          }}>{h.reservable ? 'Choisir ce séjour' : 'Indisponible'}</button></div>
      </div></article>)}</div>
    {!attente && !visibles.length && <p className="vide-parcours">Aucun séjour ne correspond à cette recherche. Ajustez vos dates ou votre ville.</p>}
    {confirmation && <div className="voile-dialogue"><section role="dialog" aria-modal="true" aria-labelledby="titre-reserver" className="confirmation-sejour">
      <h2 id="titre-reserver">Votre escale est presque prête</h2><h3>{confirmation.hotel.nom}</h3>
      <p>Du {arrivee} au {depart} · {voyageurs} voyageur(s) · {chambres} chambre(s)</p>
      <strong className="total-sejour">{dinars(confirmation.hotel.total ?? 0)}</strong>
      <p>Le serveur vérifie à nouveau les chambres et le prix à la confirmation.</p>
      <p>Annulation possible avant le début du séjour depuis votre carnet.</p>
      {message && <p role="alert">{message}</p>}
      <div className="actions-dialogue"><button className="bouton secondaire-bouton" disabled={envoi} onClick={() => setConfirmation(undefined)}>Revenir aux séjours</button>
        <button className="bouton principal" disabled={envoi} onClick={reserver}>{envoi ? 'Confirmation…' : 'Confirmer la réservation'}</button></div>
    </section></div>}
  </main>;
}

/** Carnet serveur partage avec le mobile et annulation confirmee. */
export function CarnetReservations({ jeton }: { jeton: string }) {
  const [liste, setListe] = useState<Reservation[]>([]);
  const [erreur, setErreur] = useState('');
  const [revision, setRevision] = useState(0);
  const [annulation, setAnnulation] = useState<Reservation>();
  const [attente, setAttente] = useState(false);
  const fermer = useCallback(() => { if (!attente) setAnnulation(undefined); }, [attente]);
  useDialogueVoyage(!!annulation, fermer);
  useEffect(() => {
    const c = new AbortController();
    api.get<Reservation[]>('/api/reservations', { ...autorisation(jeton), signal: c.signal })
      .then(r => setListe(r.data)).catch(e => { if (!c.signal.aborted) setErreur(erreurApi(e).message); });
    return () => c.abort();
  }, [jeton, revision]);
  /** Annule seulement apres la confirmation du voyageur. */
  async function annuler() {
    if (!annulation) return;
    setAttente(true);
    try { await api.delete('/api/reservations/' + annulation.id_r, autorisation(jeton)); setAnnulation(undefined); setRevision(n => n + 1); }
    catch (e) { setErreur(erreurApi(e).message); } finally { setAttente(false); }
  }
  return <main className="atelier-page" id="contenu">
    <header className="entete-reservations">
      <div className="titre-reservations">
        <h1>Mes réservations</h1>
        <button className="bouton secondaire-bouton" onClick={() => { setErreur(''); setRevision(n => n + 1); }}>Actualiser</button>
      </div>
      <p>Les détails de vos prochaines escales, sur tous vos appareils.</p>
    </header>
    {erreur && <p role="alert" className="retour-action">{erreur}</p>}
    {!liste.length && <p className="vide-parcours">Votre carnet attend son premier séjour. Explorez les hôtels pour choisir votre prochaine escale.</p>}
    <div className="grille-reservations">{liste.map(r => <article className="billet-sejour" key={r.id_r}>
      <div className="billet-entete"><span>GO VOYAGE · Réservation #{r.id_r}</span><span className={'statut-' + r.statut.toLowerCase()}>{r.statut === 'CONFIRMEE' ? 'Confirmée' : 'Annulée'}</span></div>
      <h2>{r.hotel_nom}</h2><p>{r.ville} · {r.arrivee} → {r.depart}</p><p>{r.voyageurs} voyageur(s) · {r.chambres} chambre(s)</p>
      <strong>{dinars(r.montant)}</strong>
      {r.statut === 'CONFIRMEE' && r.arrivee > dateSejour(0) && <button className="bouton secondaire-bouton" onClick={() => setAnnulation(r)}>Annuler ce séjour</button>}
    </article>)}</div>
    {annulation && <div className="voile-dialogue"><section className="confirmation-sejour" role="dialog" aria-modal="true" aria-labelledby="titre-annuler">
      <h2 id="titre-annuler">Annuler votre séjour ?</h2><p>{annulation.hotel_nom} · {annulation.arrivee}</p>
      <div className="actions-dialogue"><button className="bouton secondaire-bouton" disabled={attente} onClick={() => setAnnulation(undefined)}>Conserver</button>
        <button className="bouton danger" disabled={attente} onClick={annuler}>Confirmer l’annulation</button></div></section></div>}
  </main>;
}
