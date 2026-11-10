import { FormEvent, useState } from 'react';
import { api, erreurApi, ErreurApi, Session } from './api';
import { Champ, lireCoordonnees, Message } from './ChampsCompte';

/** Affiche l'inscription et la connexion sur une meme page. */
export function ConnexionPage({ connecter, motif, annuler }: { connecter: (session: Session) => void; motif?: string; annuler?: () => void }) {
  const [inscription, definirInscription] = useState(false);
  const [erreur, definirErreur] = useState<ErreurApi>();
  const [succes, definirSucces] = useState('');
  const [email, definirEmail] = useState('');
  const [attente, definirAttente] = useState(false);

  /** Change de formulaire et reinitialise ses messages. */
  function choisirMode(nouveau: boolean) {
    definirInscription(nouveau);
    definirErreur(undefined);
    definirSucces('');
  }

  /** Cree un compte ou ouvre une session via l'API. */
  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    const champs = new FormData(evenement.currentTarget);
    const identifiants = { email: String(champs.get('email')).trim(), motDePasse: String(champs.get('motDePasse')) };
    definirAttente(true);
    definirErreur(undefined);
    definirSucces('');
    try {
      if (inscription) {
        await api.post('/api/auth/inscription', { ...lireCoordonnees(champs), ...identifiants });
        definirEmail(identifiants.email);
        definirInscription(false);
        definirSucces('Votre compte est créé. Vous pouvez vous connecter.');
      } else {
        const reponse = await api.post<Session>('/api/auth/connexion', identifiants);
        connecter(reponse.data);
      }
    } catch (cause) {
      definirErreur(erreurApi(cause));
    } finally {
      definirAttente(false);
    }
  }

  return <main className="accueil" id="contenu">
    <section className="voyage">
      <div className="voyage-texte">
        <p className="surtitre"><i /> PLATEFORME VOYAGE</p>
        <h1>Le départ,<br /><em>c’est ici.</em></h1>
        <p>Votre espace personnel GO VOYAGE. Créez votre compte et préparez votre prochaine aventure.</p>
      </div>
      <div className="carte-vol" aria-hidden="true">
        <svg viewBox="0 0 440 220" fill="none">
          <path className="orbite" d="M26 144C83 31 229 24 327 80S397 192 277 200 43 191 26 144Z"/>
          <path className="orbite" d="M75 155C101 77 204 57 293 84S351 174 268 184 96 181 75 155Z"/>
          <path className="trace" d="M43 171C92 202 139 99 215 129S330 151 390 40"/>
          <circle className="repere-vol" cx="43" cy="171" r="6"/>
          <circle className="anneau-vol" cx="390" cy="40" r="12" strokeWidth="1.5"/>
          <circle className="repere-vol" cx="390" cy="40" r="4.5"/>
          <path className="avion-vol" d="m231 127-20 7-12-8-4 2 8 10-15 6 3 4 16-2 5 10 4-2-1-11 18-10c3-2 1-7-2-6Z"/>
        </svg>
        <span>TUNIS · POINT DE DÉPART</span>
      </div>
      <span className="voyage-index">01 / 01</span>
    </section>
    <section className="bloc-auth" aria-labelledby="titre-auth">
      <div className="bascule" aria-label="Accès au compte">
        <button type="button" aria-pressed={!inscription} disabled={attente} onClick={() => choisirMode(false)}>Connexion</button>
        <button type="button" aria-pressed={inscription} disabled={attente} onClick={() => choisirMode(true)}>Inscription</button>
      </div>
      <h2 id="titre-auth">{inscription ? 'Créez votre compte' : 'Content de vous revoir.'}</h2>
      <p className="secondaire">{inscription ? 'Renseignez vos coordonnées pour rejoindre GO VOYAGE.' : 'Connectez-vous pour retrouver votre espace.'}</p>
      {motif && <div className="message info" role="note">{motif}</div>}
      <Message erreur={erreur} enfants={succes} />
      <form key={String(inscription)} onSubmit={soumettre}>
        {inscription && <div className="duo">
          <Champ nom="prenom" label="Prénom" autocomplete="given-name" max={100} erreur={erreur?.erreurs?.prenom} />
          <Champ nom="nom" label="Nom" autocomplete="family-name" max={100} erreur={erreur?.erreurs?.nom} />
        </div>}
        <Champ nom="email" label="Adresse e-mail" type="email" valeur={email} autocomplete="email" max={100} erreur={erreur?.erreurs?.email} />
        {inscription && <Champ nom="tel" label="Téléphone" type="tel" autocomplete="tel" max={20} erreur={erreur?.erreurs?.tel} />}
        <Champ nom="motDePasse" label="Mot de passe" type="password" min={inscription ? 8 : undefined}
          max={72} autocomplete={inscription ? 'new-password' : 'current-password'} erreur={erreur?.erreurs?.motDePasse} />
        {inscription && <small className="secondaire">8 caractères minimum.</small>}
        <button className="bouton principal" type="submit" disabled={attente}>
          {attente ? 'Veuillez patienter…' : inscription ? 'Créer mon compte' : 'Me connecter'}
        </button>
      </form>
      {annuler && <button type="button" className="bouton secondaire-bouton lien-retour" onClick={annuler}>← Retour au catalogue</button>}
    </section>
  </main>;
}
