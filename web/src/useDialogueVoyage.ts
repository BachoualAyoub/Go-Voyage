import { useEffect } from 'react';

/** Garde le clavier dans la boite ouverte et rend le focus au bouton de depart. */
export function useDialogueVoyage(ouvert: boolean, fermer: () => void) {
  useEffect(() => {
    if (!ouvert) return;
    const precedent = document.activeElement as HTMLElement | null;
    const boite = document.querySelector<HTMLElement>('[role="dialog"]');
    const defilement = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const selecteur = 'button:not(:disabled), input, select, textarea, a[href]';
    boite?.querySelector<HTMLElement>(selecteur)?.focus();
    function clavier(e: KeyboardEvent) {
      if (e.key === 'Escape') fermer();
      if (e.key !== 'Tab' || !boite) return;
      const champs = Array.from(boite.querySelectorAll<HTMLElement>(selecteur));
      const premier = champs[0], dernier = champs[champs.length - 1];
      if (e.shiftKey && document.activeElement === premier) { e.preventDefault(); dernier?.focus(); }
      else if (!e.shiftKey && document.activeElement === dernier) { e.preventDefault(); premier?.focus(); }
    }
    document.addEventListener('keydown', clavier);
    return () => { document.removeEventListener('keydown', clavier); document.body.style.overflow = defilement; precedent?.focus(); };
  }, [ouvert, fermer]);
}
