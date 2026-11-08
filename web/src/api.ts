import axios from 'axios';

export type Role = 'USER' | 'ADMIN';

export type Profil = {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  tel: string;
  role: Role;
};

export type Coordonnees = Pick<Profil, 'nom' | 'prenom' | 'email' | 'tel'>;
export type Inscription = Coordonnees & { motDePasse: string };
export type Connexion = Pick<Inscription, 'email' | 'motDePasse'>;
export type Session = { jeton: string; utilisateur: Profil };
export type ErreurApi = { statut: number; message: string; erreurs?: Record<string, string> };

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://127.0.0.1:8081',
  timeout: 10000,
});

/** Construit l'en-tete d'une requete authentifiee. */
export function autorisation(jeton: string) {
  return { headers: { Authorization: 'Bearer ' + jeton } };
}

/** Renvoie le message et les erreurs de validation recus du serveur. */
export function erreurApi(erreur: unknown): ErreurApi {
  if (axios.isAxiosError<ErreurApi>(erreur)) {
    if (erreur.response?.data?.message) return erreur.response.data;
    if (!erreur.response) return { statut: 0, message: 'API injoignable. Vérifiez que le backend GO VOYAGE est démarré.' };
  }
  return { statut: 0, message: 'Une erreur est survenue. Réessayez dans un instant.' };
}
