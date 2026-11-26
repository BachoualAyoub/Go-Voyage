import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { api, autorisation, erreurApi, ErreurApi, Profil, Session } from './api';
import { PilotagePage } from './PilotagePage';
import { ReservationsPage, CarnetReservations } from './ReservationsPage';
import { VolsPage } from './VolsPage';
import { ConnexionPage } from './ConnexionPage';
import { Champ, Message, lireCoordonnees } from './ChampsCompte';
import { EspaceVoyage } from './EspaceVoyage';
import { LivreOrFeed } from './LivreOrPage';

const cleSession = 'govoyage.jeton';

export function App() {
  const [jeton, definirJeton] = useState(() => sessionStorage.getItem(cleSession) ?? '');
  const [profil, definirProfil] = useState<Profil>();
  const [erreur, definirErreur] = useState<ErreurApi>();
  const [section, definirSection] = useState<'explorer' | 'circuits' | 'profil' | 'admin' | 'hotels' | 'vols' | 'reservations' | 'livre'>();
  const [motifConnexion, definirMotifConnexion] = useState<string>();
  const [editionProfil, definirEditionProfil] = useState(false);
  const [erreurProfil, definirErreurProfil] = useState<ErreurApi>();
  const [succesProfil, definirSuccesProfil] = useState('');
  const [attenteProfil, definirAttenteProfil] = useState(false);
  const dialogueProfil = useRef<HTMLDialogElement>(null);
  const onglet = section === 'admin' && profil?.role !== 'ADMIN' ? 'explorer' :
    section ?? (profil?.role === 'ADMIN' ? 'admin' : 'explorer');

  const deconnecter = useCallback(() => {
    sessionStorage.removeItem(cleSession);
    definirJeton('');
    definirProfil(undefined);
    definirErreur(undefined);
    definirSection(undefined);
    definirMotifConnexion(undefined);
  }, []);

  const actualiser = useCallback(async (signal?: AbortSignal) => {
    if (!jeton) return;
    definirErreur(undefined);
    try {
      const reponse = await api.get<Profil>('/api/auth/moi', { ...autorisation(jeton), signal });
      if (!signal?.aborted && sessionStorage.getItem(cleSession) === jeton) definirProfil(reponse.data);
    } catch (cause) {
      if (signal?.aborted || sessionStorage.getItem(cleSession) !== jeton) return;
      const retour = erreurApi(cause);
      if (retour.statut === 401) deconnecter();
      else definirErreur(retour);
    }
  }, [jeton, deconnecter]);

  useEffect(() => {
    const controle = new AbortController();
    void actualiser(controle.signal);
    return () => controle.abort();
  }, [actualiser]);

  useEffect(() => {
    if (editionProfil && !dialogueProfil.current?.open) dialogueProfil.current?.showModal();
  }, [editionProfil]);

  function connecter(session: Session) {
    sessionStorage.setItem(cleSession, session.jeton);
    definirProfil(session.utilisateur);
    definirJeton(session.jeton);
    definirMotifConnexion(undefined);
  }

  function demanderConnexion(motif: string) {
    definirMotifConnexion(motif);
  }

  function annulerConnexion() {
    definirMotifConnexion(undefined);
  }

  function ouvrirEditionProfil() {
    definirErreurProfil(undefined);
    definirSuccesProfil('');
    definirEditionProfil(true);
  }

  async function modifierProfil(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (!profil) return;
    const coordonnees = lireCoordonnees(new FormData(evenement.currentTarget));
    definirAttenteProfil(true);
    definirErreurProfil(undefined);
    try {
      const corps = { nom: coordonnees.nom, prenom: coordonnees.prenom, tel: coordonnees.tel };
      await api.put('/api/auth/moi', corps, autorisation(jeton));
      await actualiser();
      dialogueProfil.current?.close();
      definirSuccesProfil('Informations mises à jour.');
    } catch (cause) {
      definirErreurProfil(erreurApi(cause));
    } finally {
      definirAttenteProfil(false);
    }
  }

  function montrerSection(ancre: string) {
    definirMotifConnexion(undefined);
    definirSection('explorer');
    amenerSection(ancre, 10);
  }

  function amenerSection(ancre: string, tentatives: number) {
    window.setTimeout(() => {
      const cible = document.getElementById(ancre);
      const animer = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (!cible) {
        if (tentatives > 0) amenerSection(ancre, tentatives - 1);
        return;
      }
      const barres = document.querySelector('.barres-fixes');
      const decalage = barres instanceof HTMLElement ? barres.getBoundingClientRect().height : 0;
      const haut = cible.getBoundingClientRect().top + window.scrollY - decalage - 12;
      window.scrollTo({ top: haut, behavior: animer ? 'smooth' : 'auto' });
    }, 120);
  }

  return <div className="app">
    <div className="barres-fixes">
    <header className="barre">
      <a href="#contenu" className="marque"><img src="/logo-go-voyage.png" alt="GO VOYAGE" /></a>
      <div className="barre-droite">
        <nav className="nav-catalogue" aria-label="Navigation principale">
          <button type="button" onClick={() => { definirMotifConnexion(undefined); definirSection('explorer'); amenerSection('contenu', 10); }}>Accueil</button>
          <button type="button" onClick={() => montrerSection('destinations')}>Destinations</button>
          <button type="button" onClick={() => montrerSection('modeles')}>Circuits</button>
          <button type="button" onClick={() => { definirMotifConnexion(undefined); definirSection('hotels'); }}>Séjours</button>
          <button type="button" onClick={() => { definirMotifConnexion(undefined); definirSection('vols'); }}>Vols</button>
          <button type="button" onClick={() => montrerSection('livre-or')}>Livre d’or</button>
        </nav>
        {jeton ? <>
          {profil && <button type="button" className="compte-pilule" onClick={() => definirSection('profil')}
            title="Voir mon compte">
            <span className="avatar-compte" aria-hidden="true">{profil.prenom.charAt(0)}</span>
            {profil.prenom}
          </button>}
          <button type="button" className="bouton secondaire-bouton" onClick={deconnecter}>Déconnexion</button>
        </> : <button type="button" className="bouton principal" onClick={() => demanderConnexion('')}>Connectez-vous</button>}
      </div>
    </header>
    {profil && jeton && <nav className="nav-voyage" aria-label="Espace GO VOYAGE">
      {profil.role === 'ADMIN' && <button aria-current={onglet === 'admin' ? 'page' : undefined}
        onClick={() => definirSection('admin')}>Administration</button>}
      <button aria-current={onglet === 'explorer' ? 'page' : undefined} onClick={() => definirSection('explorer')}>Explorer</button>
      <button aria-current={onglet === 'circuits' ? 'page' : undefined} onClick={() => definirSection('circuits')}>Mes circuits</button>
      <button aria-current={onglet === 'reservations' ? 'page' : undefined} onClick={() => definirSection('reservations')}>Mes réservations</button>
    </nav>}
    </div>
    {motifConnexion !== undefined && !jeton ? <ConnexionPage connecter={connecter} motif={motifConnexion} annuler={annulerConnexion} /> :
      onglet === 'hotels' ? <ReservationsPage jeton={jeton} demanderConnexion={demanderConnexion} /> :
      onglet === 'vols' ? <VolsPage /> :
      onglet === 'livre' ? <main className="voyage-main" id="contenu">
        <button className="bouton secondaire-bouton retour-livre-or" type="button"
          onClick={() => { definirSection('explorer'); amenerSection('contenu', 10); }}>← Retour à l’accueil</button>
        <LivreOrFeed jeton={jeton} compteId={profil?.id} demanderConnexion={demanderConnexion} />
      </main> :
      !jeton ? (motifConnexion === undefined ?
      <EspaceVoyage jeton={jeton} compteId={profil?.id} onglet="explorer" afficherCircuits={() => definirSection('circuits')}
        actualiser={actualiser} demanderConnexion={demanderConnexion} voirLivreOr={() => definirSection('livre')} /> :
      <ConnexionPage connecter={connecter} motif={motifConnexion} annuler={annulerConnexion} />) :
      erreur ? <main className="espace" id="contenu">
        <Message erreur={erreur} />
        <button className="bouton secondaire-bouton" onClick={() => actualiser()}>Réessayer</button>
      </main> :
      !profil ? <main className="espace" id="contenu"><p role="status">Vérification de votre session…</p></main> :
      onglet === 'admin' ?
        <PilotagePage jeton={jeton} admin={profil} actualiser={actualiser} /> :
      onglet === 'reservations' ? <CarnetReservations jeton={jeton} /> :
      onglet !== 'profil' ?
        <EspaceVoyage jeton={jeton} compteId={profil?.id} onglet={onglet} actualiser={actualiser}
          afficherCircuits={() => definirSection('circuits')} demanderConnexion={demanderConnexion}
          voirLivreOr={() => definirSection('livre')} /> :
        <main className="espace" id="contenu">
          <div className="en-tete-espace"><div>
            <p className="surtitre"><i /> ESPACE VOYAGEUR</p>
            <h1>Bonjour, {profil.prenom}.</h1>
            <p className="secondaire">Retrouvez les informations de votre compte GO VOYAGE.</p>
          </div></div>
          <section className="panneau profil" aria-labelledby="titre-profil">
            <h2 id="titre-profil">Mon compte</h2>
            <dl>
              <div><dt>Nom complet</dt><dd>{profil.prenom} {profil.nom}</dd></div>
              <div><dt>Adresse e-mail</dt><dd>{profil.email}</dd></div>
              <div><dt>Téléphone</dt><dd>{profil.tel}</dd></div>
            </dl>
            <div className="actions-dialogue">
              <button className="bouton principal" type="button" onClick={ouvrirEditionProfil}>Modifier mes informations</button>
            </div>
            <Message enfants={succesProfil} />
          </section>
          <dialog ref={dialogueProfil} aria-labelledby="titre-edition-profil"
            onClose={() => definirEditionProfil(false)}
            onCancel={(evenement) => { if (attenteProfil) evenement.preventDefault(); }}>
            {editionProfil && <form onSubmit={modifierProfil}>
              <h2 id="titre-edition-profil">Modifier mes informations</h2>
              <Message erreur={erreurProfil} />
              <div className="duo">
                <Champ nom="prenom" label="Prénom" max={100} valeur={profil.prenom} erreur={erreurProfil?.erreurs?.prenom} />
                <Champ nom="nom" label="Nom" max={100} valeur={profil.nom} erreur={erreurProfil?.erreurs?.nom} />
              </div>
              <Champ nom="email" label="Adresse e-mail" type="email" valeur={profil.email} readOnly />
              <Champ nom="tel" label="Téléphone" type="tel" max={20} valeur={profil.tel} erreur={erreurProfil?.erreurs?.tel} />
              <div className="actions-dialogue">
                <button className="bouton secondaire-bouton" type="button"
                  onClick={() => dialogueProfil.current?.close()}>Annuler</button>
                <button className="bouton principal" type="submit" disabled={attenteProfil}>
                  {attenteProfil ? 'Enregistrement…' : 'Enregistrer'}
                </button>
              </div>
            </form>}
          </dialog>
        </main>}
    <footer className="pied"><span>GO VOYAGE</span><span>Votre espace voyage · Tunisie</span></footer>
  </div>;
}
