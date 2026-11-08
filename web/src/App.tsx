import { useCallback, useEffect, useState } from 'react';
import { api, autorisation, erreurApi, ErreurApi, Profil, Session } from './api';
import { AdministrationPage } from './AdministrationPage';
import { ConnexionPage } from './ConnexionPage';
import { Message } from './ChampsCompte';

const cleSession = 'govoyage.jeton';

/** Affiche l'espace correspondant au role confirme par le serveur. */
export function App() {
  const [jeton, definirJeton] = useState(() => sessionStorage.getItem(cleSession) ?? '');
  const [profil, definirProfil] = useState<Profil>();
  const [erreur, definirErreur] = useState<ErreurApi>();

  /** Ferme la session dans cet onglet. */
  const deconnecter = useCallback(() => {
    sessionStorage.removeItem(cleSession);
    definirJeton('');
    definirProfil(undefined);
    definirErreur(undefined);
  }, []);

  /** Verifie la session et le role actuel. */
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

  /** Stocke le jeton le temps de la session de l'onglet. */
  function connecter(session: Session) {
    sessionStorage.setItem(cleSession, session.jeton);
    definirProfil(session.utilisateur);
    definirJeton(session.jeton);
  }

  return <div className="app">
    <header className="barre">
      <a href="#contenu" className="marque"><img src="/logo-go-voyage.png" alt="GO VOYAGE" /></a>
      {jeton ? <div className="barre-droite">
        {profil && <span>{profil.prenom} {profil.nom}</span>}
        <button type="button" className="bouton secondaire-bouton" onClick={deconnecter}>Déconnexion</button>
      </div> : <span className="barre-note">VOTRE ESPACE VOYAGE</span>}
    </header>
    {!jeton ? <ConnexionPage connecter={connecter} /> :
      erreur ? <main className="espace" id="contenu">
        <Message erreur={erreur} />
        <button className="bouton secondaire-bouton" onClick={() => actualiser()}>Réessayer</button>
      </main> :
      !profil ? <main className="espace" id="contenu"><p role="status">Vérification de votre session…</p></main> :
      profil.role === 'ADMIN' ?
        <AdministrationPage jeton={jeton} admin={profil} actualiser={actualiser} /> :
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
              <div><dt>Rôle</dt><dd><span className="badge">Utilisateur</span></dd></div>
            </dl>
          </section>
        </main>}
    <footer className="pied"><span>GO VOYAGE</span><span>Votre espace voyage · Tunisie</span></footer>
  </div>;
}
