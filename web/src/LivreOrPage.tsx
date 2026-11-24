import { FormEvent, useCallback, useEffect, useState } from 'react';
import { useDialogueVoyage } from './useDialogueVoyage';
import { api, autorisation, erreurApi } from './api';
import { Avis, Publication } from './sejour';
import './sejour.css';

/** Livre d'or commun au site et a l'application Android. */
export function LivreOrPage({ jeton, compteId, demanderConnexion }: {
  jeton: string; compteId?: number; demanderConnexion: (motif: string) => void;
}) {
  const [recits, setRecits] = useState<Publication[]>([]);
  const [recherche, setRecherche] = useState('');
  const [revision, setRevision] = useState(0);
  const [message, setMessage] = useState('');
  const [chargement, setChargement] = useState(false);
  const [ecriture, setEcriture] = useState(false);
  const [occupe, setOccupe] = useState(false);
  const [ouvert, setOuvert] = useState<Publication>();
  const [avis, setAvis] = useState<Avis[]>([]);
  const [motif, setMotif] = useState<Publication>();
  const fermer = useCallback(() => { setEcriture(false); setOuvert(undefined); setMotif(undefined); }, []);
  useDialogueVoyage(ecriture || !!ouvert || !!motif, fermer);
  useEffect(() => {
    const controle = new AbortController();
    setChargement(true);
    api.get<Publication[]>('/api/publications', { ...autorisation(jeton), params: { recherche }, signal: controle.signal })
      .then(r => { setRecits(r.data); setChargement(false); })
      .catch(e => { if (!controle.signal.aborted) { setMessage(erreurApi(e).message); setChargement(false); } });
    return () => controle.abort();
  }, [recherche, jeton, revision]);
  useEffect(() => {
    setAvis([]);
    if (!ouvert) return;
    const controle = new AbortController();
    api.get<Avis[]>('/api/publications/' + ouvert.id_p + '/avis', { signal: controle.signal })
      .then(r => setAvis(r.data)).catch(e => { if (!controle.signal.aborted) setMessage(erreurApi(e).message); });
    return () => controle.abort();
  }, [ouvert, revision]);
  /** Reserve une action a une session authentifiee. */
  function connecte() {
    if (jeton) return true;
    demanderConnexion('Connectez-vous pour participer au livre d’or.');
    return false;
  }
  /** Execute une interaction puis relit les compteurs du serveur. */
  async function agir(action: () => Promise<unknown>) {
    setOccupe(true); setMessage('');
    try { await action(); setRevision(v => v + 1); }
    catch (e) { setMessage(e instanceof Error && !('isAxiosError' in e) ? e.message : erreurApi(e).message); }
    finally { setOccupe(false); }
  }
  /** Transfere le media puis publie le recit avec sa reference. */
  async function publier(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const donnees = new FormData(e.currentTarget);
    await agir(async () => {
      const fichier = donnees.get('fichier') as File;
      let mediaUrl = null, mediaType = null;
      if (fichier?.size) {
        if (fichier.size > 10000000) throw new Error('Le média est limité à 10 Mo.');
        const transfert = new FormData(); transfert.append('fichier', fichier);
        const r = await api.post('/api/medias', transfert, autorisation(jeton));
        mediaUrl = r.data.url; mediaType = r.data.type;
      }
      await api.post('/api/publications', { titre: donnees.get('titre'), texte: donnees.get('texte'),
        pays: donnees.get('pays'), ville: donnees.get('ville'), mediaUrl, mediaType }, autorisation(jeton));
      setEcriture(false);
    });
  }
  /** Prefixe l'adresse API sans doubler le slash devant un media. */
  function sourceMedia(chemin: string): string {
    const base = (api.defaults.baseURL ?? '').replace(/\/$/, '');
    return base + (chemin.startsWith('/') ? chemin : '/' + chemin);
  }
  return <main className="espace sejour" id="contenu">
    <div className="titre-voyage"><div><p className="surtitre">LE LIVRE D’OR</p><h1>Les voyages passent.<br /><em>Les histoires restent.</em></h1>
      <p>Des lieux, des regards et des souvenirs partagés par notre communauté.</p></div>
      <button className="bouton principal" onClick={() => { if (connecte()) setEcriture(true); }}>＋ Raconter mon voyage</button></div>
    <label className="recherche-recit">Quel sera votre prochain souvenir ?
      <input type="search" placeholder="Rechercher un pays ou une ville…" value={recherche} onChange={e => setRecherche(e.target.value)} /></label>
    {message && <p className="alerte erreur" role="alert">{message}</p>}
    {chargement && <p role="status">À la rencontre des voyageurs…</p>}
    {!chargement && !recits.length && <div className="vide-voyage"><h2>Une page à écrire.</h2><p>Aucun récit pour cette recherche. Partagez le premier !</p></div>}
    <div className="grille-recits">{recits.map(r => <article className="carte-recit" key={r.id_p}>
      {r.media_url && (r.media_type === 'VIDEO' ? <video controls preload="metadata" src={sourceMedia(r.media_url)} /> :
        <img loading="lazy" src={sourceMedia(r.media_url)} alt={r.titre} />)}
      <div className="corps-recit"><span className="etiquette">{r.ville} · {r.pays}</span><h2>{r.titre}</h2>
        <p className="texte-recit">{r.texte}</p><p className="secondaire">Par {r.auteur} · {new Date(r.cree_le).toLocaleDateString('fr-FR')}</p>
        <div className="actions-recit">
          <button disabled={occupe} aria-pressed={r.aime > 0} onClick={() => { if (connecte()) void agir(() => api.put('/api/publications/' + r.id_p + '/like', { actif: !r.aime }, autorisation(jeton))); }}>
            {r.aime ? '♥' : '♡'} {r.likes}</button>
          <button onClick={() => setOuvert(r)}>☆ {Number(r.note).toFixed(1)} · {r.avis} avis</button>
          <button title="Signaler ce récit" onClick={() => { if (connecte()) setMotif(r); }}>Signaler</button>
          {compteId === r.utilisateur_id && <button disabled={occupe} onClick={() => {
            if (window.confirm('Supprimer définitivement votre récit ?')) void agir(() => api.delete('/api/publications/' + r.id_p, autorisation(jeton)));
          }}>Retirer</button>}
        </div>
      </div></article>)}</div>
    {ecriture && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-recit">
      <button className="fermer-dialogue" aria-label="Fermer" onClick={() => setEcriture(false)}>×</button>
      <p className="surtitre">VOTRE CARNET, EN IMAGES</p><h2 id="titre-recit">Un souvenir à partager</h2>
      {message && <p role="alert">{message}</p>}
      <form onSubmit={publier} className="formulaire-voyage">
        <label>Titre<input name="titre" required maxLength={120} /></label>
        <div className="champs-ligne"><label>Pays<input name="pays" defaultValue="Tunisie" required maxLength={80} /></label>
          <label>Ville<input name="ville" required maxLength={100} /></label></div>
        <label>Votre récit<textarea name="texte" required maxLength={3000} rows={5} /></label>
        <label>Photo ou vidéo <small>JPG, PNG ou MP4 · 10 Mo maximum</small><input name="fichier" type="file" accept="image/jpeg,image/png,video/mp4" /></label>
        <button className="bouton principal" disabled={occupe}>{occupe ? 'Publication…' : 'Publier mon récit'}</button>
      </form></section></div>}
    {ouvert && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-avis">
      <button className="fermer-dialogue" aria-label="Fermer" onClick={() => setOuvert(undefined)}>×</button>
      <h2 id="titre-avis">{ouvert.titre}</h2><p>Les impressions des voyageurs</p>
      {message && <p role="alert">{message}</p>}
      {!avis.length && <p>Soyez le premier à laisser un avis.</p>}
      {avis.map(a => <div className="avis-voyage" key={a.id_a}><strong>{a.auteur} · {'★'.repeat(a.note)}</strong><p>{a.texte}</p></div>)}
      <form className="formulaire-voyage" onSubmit={e => { e.preventDefault(); if (!connecte()) return;
        const d = new FormData(e.currentTarget); void agir(() => api.put('/api/publications/' + ouvert.id_p + '/avis',
          { texte: d.get('texte'), note: Number(d.get('note')) }, autorisation(jeton))); }}>
        <label>Votre note<select name="note" defaultValue="5">{[5,4,3,2,1].map(n => <option key={n} value={n}>{n} / 5</option>)}</select></label>
        <label>Votre avis<textarea name="texte" maxLength={1000} required rows={3} /></label>
        <small>Un avis par personne. Un nouvel envoi remplace votre précédent avis.</small>
        <button disabled={occupe} className="bouton principal">Enregistrer mon avis</button></form>
    </section></div>}
    {motif && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-signalement">
      <button className="fermer-dialogue" aria-label="Fermer" onClick={() => setMotif(undefined)}>×</button>
      <h2 id="titre-signalement">Signaler ce contenu</h2><p>Un administrateur examinera votre demande.</p>
      {message && <p role="alert">{message}</p>}
      <form className="formulaire-voyage" onSubmit={e => { e.preventDefault(); const d = new FormData(e.currentTarget);
        void agir(async () => { await api.post('/api/publications/' + motif.id_p + '/signalements', { motif: d.get('motif') }, autorisation(jeton)); setMotif(undefined); }); }}>
        <label>Motif<textarea name="motif" required maxLength={500} rows={4} /></label>
        <button className="bouton principal" disabled={occupe}>Envoyer le signalement</button></form>
    </section></div>}
  </main>;
}
