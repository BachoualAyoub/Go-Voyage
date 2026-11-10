export type Ambiance = 'MER' | 'CULTURE' | 'NATURE';

export type Destination = {
  id: number; code: string; nom: string; ville: string; pays: string; region: string;
  ambiance: Ambiance; tarifJourTnd: number; accroche: string; description: string;
};
export type Etape = {
  destinationId: number; nom: string; ville: string; ambiance: Ambiance;
  jours: number; tarifJourTnd: number; coutTnd: number;
};
export type Circuit = {
  id: number; nom: string; description: string | null; modele: boolean;
  budgetTnd: number | null; dureeJours: number; estimationTnd: number; version: number; etapes: Etape[];
};
export type EtapeDemande = { destinationId: number; jours: number };
export type CircuitDemande = { nom: string; budgetTnd: number | null; etapes: EtapeDemande[]; version?: number };

export const ambiances: Record<Ambiance, string> = { MER: 'Bord de mer', CULTURE: 'Culture', NATURE: 'Nature' };
const nombre = new Intl.NumberFormat('fr-FR');

/** Affiche une estimation en dinars tunisiens. */
export function dinars(valeur: number) { return nombre.format(valeur) + ' TND'; }

/** Permet de retrouver une destination avec ou sans accents. */
export function normaliserRecherche(texte: string) {
  return texte.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}
