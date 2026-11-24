import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { useDialogueVoyage } from './useDialogueVoyage';
import { api, autorisation, erreurApi } from './api';
import { AimePar, Avis, Publication } from './sejour';
import './sejour.css';

type Props = { jeton: string; compteId?: number; demanderConnexion: (motif: string) => void; limite?: number; voirTous?: () => void };

/** Fil d'actualite du livre d'or, affiche dans la page d'accueil. */
export function LivreOrFeed({ jeton, compteId, demanderConnexion, limite, voirTous }: Props) {
  const [recits, setRecits] = useState<Publication[]>([]);
  const [recherche, setRecherche] = useState('');
  const [revision, setRevision] = useState(0);
  const [message, setMessage] = useState('');
  const [chargement, setChargement] = useState(false);
  const [recitEnFormulaire, setRecitEnFormulaire] = useState<Publication | 'nouveau' | undefined>();
  const [occupe, setOccupe] = useState(false);
  const [ouvert, setOuvert] = useState<Publication>();
  const [avis, setAvis] = useState<Avis[]>([]);
  const [motif, setMotif] = useState<Publication>();
  const [mediasEnErreur, setMediasEnErreur] = useState<number[]>([]);
  const [likesOuvert, setLikesOuvert] = useState<number>();
  const [likesVus, setLikesVus] = useState<Record<number, string[]>>({});
  const likesCharges = useRef<number[]>([]);
  const fermer = useCallback(() => { setRecitEnFormulaire(undefined); setOuvert(undefined); setMotif(undefined); }, []);
  useDialogueVoyage(!!recitEnFormulaire || !!ouvert || !!motif, fermer);

  /** Oublie les listes de likes apres chaque interaction pour les relire a jour. */
  useEffect(() => { likesCharges.current = []; setLikesOuvert(undefined); }, [revision]);

  /** Relit les recits publics a chaque recherche ou interaction. */
  useEffect(() => {
    const controle = new AbortController();
    setChargement(true);
    api.get<Publication[]>('/api/publications', { ...autorisation(jeton), params: { recherche }, signal: controle.signal })
      .then(r => { setRecits(r.data); setChargement(false); })
      .catch(e => { if (!controle.signal.aborted) { setMessage(erreurApi(e).message); setChargement(false); } });
    return () => controle.abort();
  }, [recherche, jeton, revision]);

  /** Charge les avis du recit ouvert. */
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

  /** Transfere un nouveau media si besoin, puis cree ou modifie le recit. */
  async function publier(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const recit = recitEnFormulaire;
    if (!recit) return;
    const nouveau = recit === 'nouveau';
    const idRecit = nouveau ? 0 : recit.id_p;
    const mediaActuelUrl = nouveau ? null : recit.media_url;
    const mediaActuelType = nouveau ? null : recit.media_type;
    const donnees = new FormData(e.currentTarget);
    const titre = String(donnees.get('titre') ?? '').trim();
    const texte = String(donnees.get('texte') ?? '').trim();
    const pays = String(donnees.get('pays') ?? '').trim();
    const ville = String(donnees.get('ville') ?? '').trim();
    await agir(async () => {
      if (!titre || !texte || !pays || !ville) throw new Error('Remplissez le titre, le pays, la ville et le recit.');
      const fichier = donnees.get('fichier') as File;
      let mediaUrl = mediaActuelUrl, mediaType = mediaActuelType;
      if (fichier?.size) {
        if (fichier.size > 10000000) throw new Error('Le media est limite a 10 Mo.');
        const transfert = new FormData(); transfert.append('fichier', fichier);
        const r = await api.post('/api/medias', transfert, autorisation(jeton));
        mediaUrl = r.data.url; mediaType = r.data.type;
      }
      const corps = { titre, texte, pays, ville, mediaUrl, mediaType };
      if (nouveau) await api.post('/api/publications', corps, autorisation(jeton));
      else await api.put('/api/publications/' + idRecit, corps, autorisation(jeton));
      setRecitEnFormulaire(undefined);
    });
  }

  /** Enregistre un avis apres verification des champs vides. */
  async function envoyerAvis(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!ouvert || !connecte()) return;
    const publication = ouvert;
    const donnees = new FormData(e.currentTarget);
    const texte = String(donnees.get('texte') ?? '').trim();
    const note = Number(donnees.get('note'));
    await agir(async () => {
      if (!texte) throw new Error('Ecrivez votre avis avant de l envoyer.');
      if (!Number.isInteger(note) || note < 1 || note > 5) throw new Error('Choisissez une note de 1 a 5.');
      await api.put('/api/publications/' + publication.id_p + '/avis', { texte, note }, autorisation(jeton));
    });
  }

  /** Transmet un signalement sans accepter un motif vide. */
  async function envoyerSignalement(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!motif) return;
    const publication = motif;
    const donnees = new FormData(e.currentTarget);
    const texte = String(donnees.get('motif') ?? '').trim();
    await agir(async () => {
      if (!texte) throw new Error('Indiquez le motif du signalement.');
      await api.post('/api/publications/' + publication.id_p + '/signalements', { motif: texte }, autorisation(jeton));
      setMotif(undefined);
    });
  }

  /** Prefixe l'adresse API sans doubler le slash devant un media. */
  function sourceMedia(chemin: string): string {
    const base = (api.defaults.baseURL ?? '').replace(/\/$/, '');
    return base + (chemin.startsWith('/') ? chemin : '/' + chemin);
  }

  /** Remplace un media indisponible par un message lisible. */
  function marquerMediaEnErreur(id: number) {
    if (!mediasEnErreur.includes(id)) setMediasEnErreur(ids => [...ids, id]);
  }

  /** Affiche le nom complet de l'auteur, ou son ancien alias si besoin. */
  function nomAuteur(r: Publication): string {
    const complet = [r.auteur_prenom, r.auteur_nom].filter(Boolean).join(' ').trim();
    return complet || r.auteur;
  }

  /** Initiale du prenom, comme sur la pastille du compte. */
  function initialeAuteur(r: Publication): string {
    return nomAuteur(r).charAt(0).toUpperCase();
  }

  /** Charge la liste des voyageurs ayant aime, une seule fois par recit. */
  async function chargerLikes(id: number) {
    setLikesOuvert(id);
    if (likesCharges.current.includes(id)) return;
    likesCharges.current.push(id);
    try {
      const reponse = await api.get<AimePar[]>('/api/publications/' + id + '/likes');
      const noms = reponse.data.map(u => [u.prenom, u.nom].filter(Boolean).join(' ').trim());
      setLikesVus(v => ({ ...v, [id]: noms }));
    } catch {
      setLikesVus(v => ({ ...v, [id]: [] }));
    }
  }

  /** Trie les recits par nombre de likes pour l'accueil. */
  const pubsAffichees = limite ? [...recits].sort((a, b) => b.likes - a.likes).slice(0, limite) : recits;
  const barreRecherche = <div className="recherche-livre-or">
    <label className="recherche-recit">Quel sera votre prochain souvenir ?
      <input type="search" placeholder="Rechercher un pays ou une ville…" value={recherche} onChange={e => setRecherche(e.target.value)} /></label>
    <div className="livre-or-actions">
      <button className="bouton principal" type="button" onClick={() => { if (connecte()) setRecitEnFormulaire('nouveau'); }}>
        Raconter mon voyage</button>
      {voirTous && <button className="bouton secondaire-bouton" type="button" onClick={voirTous}>Voir tous les récits</button>}
    </div>
  </div>;

  return <section className="livre-or" id="livre-or" aria-labelledby="titre-livre-or">
    {voirTous ? <h2 className="livre-or-titre-acceuil" id="titre-livre-or">Livre d’or</h2> : <div className="livre-or-entete">
      <div className="livre-or-intro"><p className="voyage-etiquette">LE LIVRE D’OR</p>
        <h2 id="titre-livre-or">Les voyages passent. <em>Les histoires restent.</em></h2>
        <p className="secondaire">Des lieux, des regards et des souvenirs partagés par notre communauté.</p></div>
    </div>}
    {barreRecherche}
    {message && <p className="alerte erreur" role="alert">{message}</p>}
    {chargement && <p role="status">À la rencontre des voyageurs…</p>}
    {!chargement && !recits.length && <div className="vide-voyage"><h3>Une page à écrire.</h3><p>Aucun récit pour cette recherche. Partagez le premier !</p></div>}
    <div className="fil-actualite">{pubsAffichees.map(r => <article className="publication" key={r.id_p}>
      <div className="publication-entete">
        <span className="avatar-auteur" aria-hidden="true">{initialeAuteur(r)}</span>
        <div className="auteur-identite">
          <strong>{nomAuteur(r)}</strong>
          <small>{r.ville} · {r.pays} · {new Date(r.cree_le).toLocaleDateString('fr-FR')}</small>
        </div>
      </div>
      <h3 className="publication-titre">{r.titre}</h3>
      <p className="publication-texte">{r.texte}</p>
      {r.media_url && !mediasEnErreur.includes(r.id_p) && (r.media_type === 'VIDEO' ?
        <video controls preload="metadata" src={sourceMedia(r.media_url)} onError={() => marquerMediaEnErreur(r.id_p)} /> :
        <img className="publication-media" loading="lazy" src={sourceMedia(r.media_url)} alt={r.titre} onError={() => marquerMediaEnErreur(r.id_p)} />)}
      {(!r.media_url || mediasEnErreur.includes(r.id_p)) &&
        <img className="publication-media media-par-defaut" src="/logo-go-voyage.png" alt="Logo GO VOYAGE" loading="lazy" />}
      <div className="publication-actions">
        <div className="like-popover" onMouseEnter={() => void chargerLikes(r.id_p)}
          onMouseLeave={() => setLikesOuvert(undefined)} onFocus={() => void chargerLikes(r.id_p)}
          onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget as Node)) setLikesOuvert(undefined); }}>
          <button disabled={occupe} aria-pressed={r.aime > 0} aria-label={r.likes + ' likes'} onClick={() => { if (connecte()) void agir(() => api.put('/api/publications/' + r.id_p + '/like', { actif: !r.aime }, autorisation(jeton))); }}>
            <span className="icone-action" aria-hidden="true">{r.aime ? '♥' : '♡'}</span><span>{r.likes}</span></button>
          {likesOuvert === r.id_p && likesVus[r.id_p] && <div className="liste-likes" role="tooltip">
            <strong>Aimé par</strong>
            {likesVus[r.id_p].length ? likesVus[r.id_p].map((nom, i) => <span key={i}>{nom}</span>) :
              <small>Personne n’a encore aimé ce récit.</small>}
          </div>}
        </div>
        <button className="bouton-avis" aria-label={r.avis + ' avis'} onClick={() => setOuvert(r)}>
          <span className="icone-action" aria-hidden="true">★</span><span>{r.avis}</span></button>
        {compteId !== r.utilisateur_id && <button className="bouton-signalement" title="Signaler ce récit" onClick={() => { if (connecte()) setMotif(r); }}>Signaler</button>}
        {compteId === r.utilisateur_id && <button className="bouton-modifier" type="button" onClick={() => { if (connecte()) setRecitEnFormulaire(r); }}>Modifier</button>}
        {compteId === r.utilisateur_id && <button className="bouton-retirer" disabled={occupe} onClick={() => {
          if (window.confirm('Supprimer définitivement votre récit ?')) void agir(() => api.delete('/api/publications/' + r.id_p, autorisation(jeton)));
        }}>Retirer</button>}
      </div>
    </article>)}</div>

    {recitEnFormulaire && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-recit">
      <button className="fermer-dialogue" aria-label="Fermer" onClick={() => setRecitEnFormulaire(undefined)}>×</button>
      <p className="voyage-etiquette">VOTRE CARNET, EN IMAGES</p><h2 id="titre-recit">
        {recitEnFormulaire === 'nouveau' ? 'Un souvenir à partager' : 'Modifier mon récit'}</h2>
      {message && <p role="alert">{message}</p>}
      <form onSubmit={publier} className="formulaire-voyage">
        <label>Titre<input name="titre" required maxLength={120}
          defaultValue={recitEnFormulaire === 'nouveau' ? '' : recitEnFormulaire.titre} /></label>
        <div className="champs-ligne"><label>Pays<input name="pays" required maxLength={70}
          defaultValue={recitEnFormulaire === 'nouveau' ? 'Tunisie' : recitEnFormulaire.pays} /></label>
          <label>Ville<input name="ville" required maxLength={100}
            defaultValue={recitEnFormulaire === 'nouveau' ? '' : recitEnFormulaire.ville} /></label></div>
        <label>Votre récit<textarea name="texte" required maxLength={2000} rows={5}
          defaultValue={recitEnFormulaire === 'nouveau' ? '' : recitEnFormulaire.texte} /></label>
        <label>Photo ou vidéo <small>{recitEnFormulaire === 'nouveau' ? 'JPG, PNG ou MP4 · 10 Mo maximum' :
          'Laissez vide pour garder la photo actuelle.'}</small>
          <input name="fichier" type="file" accept="image/jpeg,image/png,video/mp4" /></label>
        <button className="bouton principal" disabled={occupe}>
          {recitEnFormulaire === 'nouveau' ? (occupe ? 'Publication…' : 'Publier mon récit') :
            (occupe ? 'Enregistrement…' : 'Enregistrer les modifications')}</button>
      </form></section></div>}

    {ouvert && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-avis">
      <button className="fermer-dialogue" aria-label="Fermer" onClick={() => setOuvert(undefined)}>×</button>
      <div className="entete-dialogue">
        <span className="etoile-avis" aria-label={ouvert.avis + ' avis'}><span aria-hidden="true">★</span> {ouvert.avis}</span>
        <div><h2 id="titre-avis">{ouvert.titre}</h2><p>Les impressions des voyageurs</p></div>
      </div>
      {message && <p role="alert">{message}</p>}
      {!avis.length && <p>Soyez le premier à laisser un avis.</p>}
      {avis.map(a => <div className="avis-voyage" key={a.id_a}><strong>{a.auteur} · {'★'.repeat(a.note)}</strong><p>{a.texte}</p></div>)}
      <form className="formulaire-voyage" onSubmit={e => { void envoyerAvis(e); }}>
        <label>Votre note<select name="note" defaultValue="5">{[5,4,3,2,1].map(n => <option key={n} value={n}>{n} / 5</option>)}</select></label>
        <label>Votre avis<textarea name="texte" maxLength={1000} required rows={3} /></label>
        <small>Un avis par personne. Un nouvel envoi remplace votre précédent avis.</small>
        <button disabled={occupe} className="bouton principal">Enregistrer mon avis</button></form>
    </section></div>}

    {motif && <div className="voile-voyage"><section className="dialogue-voyage" role="dialog" aria-modal="true" aria-labelledby="titre-signalement">
      <button className="fermer-dialogue" aria-label="Fermer" onClick={() => setMotif(undefined)}>×</button>
      <h2 id="titre-signalement">Signaler ce contenu</h2><p>Un administrateur examinera votre demande.</p>
      {message && <p role="alert">{message}</p>}
      <form className="formulaire-voyage" onSubmit={e => { void envoyerSignalement(e); }}>
        <label>Motif<textarea name="motif" required maxLength={500} rows={4} /></label>
        <button className="bouton principal" disabled={occupe}>Envoyer le signalement</button></form>
    </section></div>}
  </section>;
}
