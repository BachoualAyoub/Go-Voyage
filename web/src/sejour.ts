/** Contrats communs aux parcours des Sprints 3 et 4. */
export type Hotel = {
  id_h: number; destination_id: number; nom: string; ville: string; description: string;
  etoiles: number; tarif_nuit: number; chambres: number; capacite: number; image: string;
  actif: boolean; version: number; disponibles?: number; reservable?: boolean; total?: number;
};
export type Reservation = {
  id_r: number; hotel_nom: string; ville: string; arrivee: string; depart: string; voyageurs: number;
  chambres: number; montant: number; statut: 'CONFIRMEE' | 'ANNULEE'; image: string;
};
export type Vol = {
  id_v: number; compagnie: string; categorie: string; depart: string; arrivee: string;
  heure_depart: string; duree_minutes: number; prix: number; bagage: string;
};
export type Publication = {
  id_p: number; utilisateur_id: number; titre: string; texte: string; pays: string; ville: string;
  auteur: string; media_url: string | null; media_type: string | null; cree_le: string;
  likes: number; avis: number; note: number; aime: number; statut: string; signalements?: number;
};
export type Avis = { id_a: number; auteur: string; note: number; texte: string; utilisateur_id: number };
export type Statistiques = {
  comptes: number; destinations: number; hotels: number; circuits: number; reservations: number;
  montant: number; annulations: number; publications: number; signalements: number;
  parVille: { ville: string; reservations: number; montant: number }[];
  parJour: { jour: string; nombre: number; montant: number }[];
  recentes: { id_r: number; prenom: string; nom: string; hotel_nom: string; montant: number; statut: string }[];
};
/** Produit une date locale utilisable par un champ HTML. */
export function dateSejour(decalage = 1) {
  const date = new Date();
  date.setDate(date.getDate() + decalage);
  return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
}
/** Retrouve la photographie du catalogue deja presente dans le projet. */
export function imageSejour(code: string) { return '/destinations/' + code + '.jpg'; }
