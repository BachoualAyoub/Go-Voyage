import { FormEvent, useCallback, useEffect, useState } from 'react';
import { useDialogueVoyage } from './useDialogueVoyage';
import { api, autorisation, erreurApi, Profil } from './api';
import { AdministrationPage } from './AdministrationPage';
import { Hotel, Publication, Statistiques } from './sejour';
import { Destination, dinars } from './voyage';
import './sejour.css';

export function PilotagePage({ jeton, admin, actualiser }: { jeton: string; admin: Profil; actualiser: () => Promise<void> }) {
  const [onglet, setOnglet] = useState('synthese');
  const [stats, setStats] = useState<Statistiques>();
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [publications, setPublications] = useState<Publication[]>([]);
  const [revision, setRevision] = useState(0);
  const [erreur, setErreur] = useState('');
  const [attente, setAttente] = useState(false);
  const [hotel, setHotel] = useState<Partial<Hotel>>();
  const [destination, setDestination] = useState<Partial<Destination>>();
  const [motifs, setMotifs] = useState<{ motif: string; statut: string }[]>();
  const fermer = useCallback(() => { setHotel(undefined); setDestination(undefined); setMotifs(undefined); }, []);
  useDialogueVoyage(!!hotel || !!destination || !!motifs, fermer);
  useEffect(() => {
    const c = new AbortController();
    const config = { ...autorisation(jeton), signal: c.signal };
    setAttente(true);
    Promise.all([api.get('/api/admin/statistiques', config), api.get('/api/admin/hotels', config),
      api.get('/api/destinations', config), api.get('/api/admin/publications', config)])
      .then(([s, h, d, p]) => { setStats(s.data); setHotels(h.data); setDestinations(d.data); setPublications(p.data); })
      .catch(e => { if (!c.signal.aborted) setErreur(erreurApi(e).message); })
      .finally(() => { if (!c.signal.aborted) setAttente(false); });
    return () => c.abort();
  }, [jeton, revision]);

  async function agir(action: () => Promise<unknown>) {
    setErreur(''); setAttente(true);
    try { await action(); setRevision(v => v + 1); }
    catch (e) { setErreur(erreurApi(e).message); }
    finally { setAttente(false); }
  }

  function sauverHotel(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); if (!hotel) return;
    const d = Object.fromEntries(new FormData(e.currentTarget));
    const corps = { ...d, destinationId: Number(d.destinationId), etoiles: Number(d.etoiles),
      tarifNuit: Number(d.tarifNuit), chambres: Number(d.chambres), capacite: Number(d.capacite),
      actif: d.actif === 'on', version: hotel.version };
    void agir(async () => { if (hotel.id_h) await api.put('/api/admin/hotels/' + hotel.id_h, corps, autorisation(jeton));
      else await api.post('/api/admin/hotels', corps, autorisation(jeton)); setHotel(undefined); });
  }

  function sauverDestination(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); if (!destination) return;
    const d = Object.fromEntries(new FormData(e.currentTarget));
    const corps = { ...d, tarifJourTnd: Number(d.tarifJourTnd) };
    void agir(async () => { if (destination.id) await api.put('/api/admin/destinations/' + destination.id, corps, autorisation(jeton));
      else await api.post('/api/admin/destinations', corps, autorisation(jeton)); setDestination(undefined); });
  }
  const maximum = Math.max(1, ...(stats?.parVille.map(v => v.reservations) ?? []));
  const maxJour = Math.max(1, ...(stats?.parJour.map(j => j.nombre) ?? []));
  return <><main className="espace sejour pilotage" id="contenu">
    <div className="titre-voyage"><div><p className="surtitre">LE POULS DE GO VOYAGE</p><h1>Vos données.<br /><em>De nouvelles perspectives.</em></h1>
      <p>Bonjour {admin.prenom}. Voici l’activité réelle de votre plateforme.</p></div>
      <button className="bouton secondaire-bouton" disabled={attente} onClick={() => { setErreur(''); setRevision(v => v + 1); }}>{attente ? 'Actualisation…' : 'Actualiser les données'}</button></div>
    <nav className="onglets-pilotage" aria-label="Administration">{[['synthese','Vue d’ensemble'],['hotels','Hôtels'],['destinations','Destinations'],
      ['moderation','Modération'],['comptes','Comptes']].map(([id, texte]) => <button key={id} aria-current={onglet === id ? 'page' : undefined} onClick={() => setOnglet(id)}>{texte}{id === 'moderation' && !!stats?.signalements && <b>{stats.signalements}</b>}</button>)}</nav>
    {erreur && <p className="alerte erreur" role="alert">{erreur}</p>}
    {onglet === 'synthese' && stats && <>
      <section className="grille-indicateurs" aria-label="Indicateurs actuels">
        {[['Réservations confirmées', stats.reservations, 'Séjours enregistrés'],['Montant confirmé', dinars(stats.montant), 'Sans encaissement en ligne'],
          ['Voyageurs inscrits', stats.comptes, stats.circuits + ' circuits personnels'],['Récits publiés', stats.publications, stats.signalements + ' signalements ouverts']].map(([titre, valeur, sous]) =>
          <article className="indicateur" key={titre}><span>{titre}</span><strong>{valeur}</strong><small>{sous}</small></article>)}
      </section>
      <div className="grille-graphes"><section className="graphe-panel"><p className="surtitre">OÙ PARTENT NOS VOYAGEURS ?</p><h2>Les escales les plus choisies</h2>
        {!stats.parVille.length && <p>Les premières réservations feront apparaître ce graphique.</p>}
        {stats.parVille.map((v, i) => <div className="barre-ville" key={v.ville}><span><b>0{i+1}</b> {v.ville}<strong>{v.reservations}</strong></span>
          <div><i style={{ width: (v.reservations / maximum * 100) + '%' }} /></div><small>{dinars(v.montant)} confirmés</small></div>)}
      </section><section className="graphe-panel"><p className="surtitre">AU FIL DES JOURS</p><h2>Le rythme des réservations</h2>
        <p className="secondaire">Réservations actuellement confirmées, par date de création.</p>
        {!stats.parJour.length ? <p>Aucune réservation pour le moment.</p> : <div className="histogramme" role="img" aria-label="Nombre de réservations par jour">
          {stats.parJour.slice(-14).map(j => <div key={j.jour} title={j.jour + ' : ' + j.nombre + ' réservations'}>
            <strong>{j.nombre}</strong><i style={{ height: (j.nombre / maxJour * 130 + 10) + 'px' }} /><small>{j.jour.slice(5)}</small></div>)}</div>}
        <div className="resume-catalogue"><span><strong>{stats.hotels}</strong> hôtels actifs</span><span><strong>{stats.destinations}</strong> destinations</span><span><strong>{stats.annulations}</strong> annulations</span></div>
      </section></div>
      <section className="graphe-panel"><h2>Les dernières réservations</h2><div className="table-voyage"><table><thead><tr><th>Référence</th><th>Voyageur</th><th>Escale</th><th>Montant</th><th>État</th></tr></thead><tbody>
        {stats.recentes.map(r => <tr key={r.id_r}><td>GV-{r.id_r}</td><td>{r.prenom} {r.nom}</td><td>{r.hotel_nom}</td><td>{dinars(r.montant)}</td><td>{r.statut === 'CONFIRMEE' ? 'Confirmée' : 'Annulée'}</td></tr>)}
        {!stats.recentes.length && <tr><td colSpan={5}>Aucune réservation enregistrée.</td></tr>}</tbody></table></div></section>
    </>}
    {onglet === 'hotels' && <section className="graphe-panel"><div className="entete-selection"><h2>Le catalogue hôtelier</h2>
      <button className="bouton principal" onClick={() => setHotel({ actif: true, etoiles: 3, tarif_nuit: 100, chambres: 5, capacite: 2 })}>Ajouter un hôtel</button></div>
      <div className="table-voyage"><table><thead><tr><th>Hôtel</th><th>Ville</th><th>Nuit / chambre</th><th>Capacité</th><th>État</th><th>Actions</th></tr></thead><tbody>
        {hotels.map(h => <tr key={h.id_h}><td><strong>{h.nom}</strong></td><td>{h.ville}</td><td>{dinars(h.tarif_nuit)}</td><td>{h.chambres} chambres · {h.capacite} pers.</td><td>{h.actif ? 'En ligne' : 'Archivé'}</td>
          <td><button onClick={() => setHotel(h)}>Modifier</button>{h.actif && <button disabled={attente} onClick={() => { if (window.confirm('Archiver cet hôtel ? Les réservations existantes sont conservées.')) void agir(() => api.delete('/api/admin/hotels/' + h.id_h, autorisation(jeton))); }}>Archiver</button>}</td></tr>)}</tbody></table></div></section>}
    {onglet === 'destinations' && <section className="graphe-panel"><div className="entete-selection"><h2>Les destinations</h2><button className="bouton principal" onClick={() => setDestination({ pays: 'Tunisie', ambiance: 'MER' })}>Ajouter une destination</button></div>
      <div className="table-voyage"><table><thead><tr><th>Destination</th><th>Ville · Pays</th><th>Tarif journalier</th><th>Actions</th></tr></thead><tbody>
        {destinations.map(d => <tr key={d.id}><td><strong>{d.nom}</strong></td><td>{d.ville} · {d.pays}</td><td>{dinars(d.tarifJourTnd)}</td><td>
          <button onClick={() => setDestination(d)}>Modifier</button><button disabled={attente} onClick={() => { if (window.confirm('Supprimer cette destination inutilisée ?')) void agir(() => api.delete('/api/admin/destinations/' + d.id, autorisation(jeton))); }}>Supprimer</button></td></tr>)}</tbody></table></div></section>}
    {onglet === 'moderation' && <section className="graphe-panel"><h2>Un espace de confiance</h2><p>Examinez les motifs puis masquez ou rétablissez le récit. Cette action clôture ses signalements.</p>
      {!publications.length && <p>Aucun récit publié.</p>}
      {publications.map(p => <article className="moderation-ligne" key={p.id_p}><div><span className="etiquette">{p.statut === 'PUBLIEE' ? 'Visible' : 'Masqué'} · {p.signalements} signalement(s)</span>
        <h3>{p.titre} <small>— {p.auteur}</small></h3><p>{p.texte}</p><small>{p.ville} · {p.pays}</small></div>
        <button onClick={() => void agir(async () => { const r = await api.get('/api/admin/publications/' + p.id_p + '/signalements', autorisation(jeton)); setMotifs(r.data); })}>Voir les motifs</button>
        <button className="bouton secondaire-bouton" disabled={attente} onClick={() => void agir(() => api.put('/api/admin/publications/' + p.id_p, { actif: p.statut !== 'PUBLIEE' }, autorisation(jeton)))}>{p.statut === 'PUBLIEE' ? 'Masquer le récit' : 'Rétablir le récit'}</button></article>)}
    </section>}
    {hotel && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-hotel"><button className="fermer-dialogue" aria-label="Fermer" onClick={() => setHotel(undefined)}>×</button>
      <h2 id="titre-hotel">{hotel.id_h ? 'Modifier l’hôtel' : 'Nouvelle escale'}</h2>{erreur && <p role="alert">{erreur}</p>}
      <form className="formulaire-voyage" onSubmit={sauverHotel}>
        <label>Nom<input name="nom" required maxLength={120} defaultValue={hotel.nom} /></label>
        <div className="champs-ligne"><label>Destination<select name="destinationId" defaultValue={hotel.destination_id}>{destinations.map(d => <option value={d.id} key={d.id}>{d.nom}</option>)}</select></label><label>Ville<input name="ville" required maxLength={100} defaultValue={hotel.ville} /></label></div>
        <label>Description<textarea name="description" required maxLength={1200} defaultValue={hotel.description} rows={3} /></label>
        <div className="champs-ligne">{[['etoiles','Étoiles',hotel.etoiles,5],['tarifNuit','Prix par nuit (TND)',hotel.tarif_nuit,100000],['chambres','Chambres',hotel.chambres,500],['capacite','Personnes / chambre',hotel.capacite,10]].map(([nom,titre,valeur,max]) => <label key={nom}>{titre}<input name={String(nom)} type="number" min="1" max={Number(max)} required defaultValue={Number(valeur)} /></label>)}</div>
        <label>Photographie du catalogue<select name="image" defaultValue={hotel.image ?? 'sidi-bou-said'}>{['sidi-bou-said','hammamet','djerba','tozeur','tabarka','douz','el-jem','kairouan'].map(c => <option key={c}>{c}</option>)}</select></label>
        <label className="case-voyage"><input name="actif" type="checkbox" defaultChecked={hotel.actif} />Visible dans le catalogue</label>
        <button disabled={attente} className="bouton principal">Enregistrer l’hôtel</button></form></section></div>}
    {destination && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-destination"><button className="fermer-dialogue" aria-label="Fermer" onClick={() => setDestination(undefined)}>×</button>
      <h2 id="titre-destination">{destination.id ? 'Modifier la destination' : 'Nouvelle destination'}</h2>{erreur && <p role="alert">{erreur}</p>}
      <form className="formulaire-voyage" onSubmit={sauverDestination}>
        {(['nom','code','ville','pays','region','accroche'] as const).map(c => <label key={c}>{({nom:'Nom',code:'Code (minuscules, tirets)',ville:'Ville',pays:'Pays',region:'Région',accroche:'Accroche'})[c]}<input name={c} required maxLength={c === 'code' ? 40 : c === 'pays' ? 70 : c === 'accroche' ? 140 : 100} pattern={c === 'code' ? '[a-z0-9-]+' : undefined} defaultValue={destination[c]} /></label>)}
        <label>Ambiance<select name="ambiance" defaultValue={destination.ambiance}><option value="MER">Bord de mer</option><option value="CULTURE">Culture</option><option value="NATURE">Nature</option></select></label>
        <label>Tarif journalier (TND)<input name="tarifJourTnd" required type="number" min="1" max="100000" defaultValue={destination.tarifJourTnd ?? 100} /></label>
        <label>Description<textarea name="description" required maxLength={750} defaultValue={destination.description} rows={4} /></label>
        <button disabled={attente} className="bouton principal">Enregistrer la destination</button></form></section></div>}
    {motifs && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-motifs"><button className="fermer-dialogue" aria-label="Fermer" onClick={() => setMotifs(undefined)}>×</button><h2 id="titre-motifs">Motifs des signalements</h2>{!motifs.length && <p>Aucun signalement.</p>}{motifs.map((m,i) => <p key={i}>{m.motif} · {m.statut === 'OUVERT' ? 'À traiter' : 'Traité'}</p>)}</section></div>}
  {onglet === 'comptes' && <AdministrationPage jeton={jeton} admin={admin} actualiser={actualiser} />}
  </main></>;
}
