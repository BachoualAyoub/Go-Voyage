import { ReactNode } from 'react';
import { ErreurApi } from './api';

type ChampProps = {
  nom: string;
  label: string;
  type?: string;
  valeur?: string;
  autocomplete?: string;
  min?: number;
  max?: number;
  erreur?: string;
};

/** Affiche un champ et le retour de validation de l'API. */
export function Champ({ nom, label, type = 'text', valeur, autocomplete, min, max, erreur }: ChampProps) {
  return <label className="champ">
    <span>{label}</span>
    <input name={nom} type={type} required defaultValue={valeur} autoComplete={autocomplete}
      minLength={min} maxLength={max} aria-invalid={Boolean(erreur)}
      aria-describedby={erreur ? nom + '-erreur' : undefined} />
    {erreur && <small id={nom + '-erreur'} className="erreur-champ">{erreur}</small>}
  </label>;
}

/** Affiche les messages de l'API qui ne correspondent pas a un champ. */
export function Message({ erreur, enfants }: { erreur?: ErreurApi; enfants?: ReactNode }) {
  if (erreur) return <div className="message erreur" role="alert">
    {erreur.message}
    {erreur.erreurs?.motDePasseCompatible && <small>{erreur.erreurs.motDePasseCompatible}</small>}
    {erreur.erreurs?.['compte.motDePasseCompatible'] && <small>{erreur.erreurs['compte.motDePasseCompatible']}</small>}
  </div>;
  if (enfants) return <div className="message succes" role="status">{enfants}</div>;
  return null;
}

/** Lit et normalise les coordonnees du formulaire. */
export function lireCoordonnees(champs: FormData) {
  return {
    nom: String(champs.get('nom')).trim(),
    prenom: String(champs.get('prenom')).trim(),
    email: String(champs.get('email')).trim(),
    tel: String(champs.get('tel')).trim(),
  };
}
