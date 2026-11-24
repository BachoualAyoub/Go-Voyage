import { useEffect, useState } from 'react';
import './voyage.css';

/** Toutes les destinations du catalogue, en commençant par Sidi Bou Said. */
const diapositives = [
  { image: '/voyage-sidi.png', legende: 'SIDI BOU SAÏD · TUNISIE' },
  { image: '/destinations/hammamet.jpg', legende: 'HAMMAMET · TUNISIE' },
  { image: '/destinations/el-jem.jpg', legende: 'EL JEM · TUNISIE' },
  { image: '/destinations/douz.jpg', legende: 'DOUZ · TUNISIE' },
  { image: '/destinations/kairouan.jpg', legende: 'KAIROUAN · TUNISIE' },
  { image: '/destinations/tozeur.jpg', legende: 'TOZEUR · TUNISIE' },
  { image: '/destinations/tabarka.jpg', legende: 'TABARKA · TUNISIE' },
  { image: '/destinations/djerba.jpg', legende: 'DJERBA · TUNISIE' }
];

/** Affiche les photos de l'accueil qui defilent automatiquement. */
export function CarrouselAccueil() {
  const [index, definirIndex] = useState(0);
  const [enPause, definirEnPause] = useState(false);

  /** Avance d'une photo, en revenant au debut apres la derniere. */
  function suivant() {
    definirIndex(valeur => (valeur + 1) % diapositives.length);
  }

  /** Revient a la photo precedente, en boucle. */
  function precedent() {
    definirIndex(valeur => (valeur - 1 + diapositives.length) % diapositives.length);
  }

  useEffect(() => {
    if (enPause || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const minuteur = window.setInterval(() => definirIndex(valeur => (valeur + 1) % diapositives.length), 5000);
    return () => window.clearInterval(minuteur);
  }, [enPause]);

  return <div className="carrousel" role="group" aria-label="Photos de la Tunisie"
    onMouseEnter={() => definirEnPause(true)} onMouseLeave={() => definirEnPause(false)}
    onFocus={() => definirEnPause(true)}
    onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget as Node)) definirEnPause(false); }}>
    {diapositives.map((diapo, position) => <img key={diapo.image}
      className={'carrousel-photo' + (position === index ? ' actif' : '')}
      src={diapo.image} alt={position === index ? diapo.legende : ''}
      aria-hidden={position !== index} />)}
    <button className="carrousel-fleche precedente" type="button" aria-label="Photo precedente" onClick={precedent}>
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5 8 12l7 7" /></svg>
    </button>
    <button className="carrousel-fleche suivante" type="button" aria-label="Photo suivante" onClick={suivant}>
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>
    </button>
    <div className="carrousel-points" aria-hidden="true">{diapositives.map((diapo, position) =>
      <span key={diapo.image} className={position === index ? 'actif' : ''} />)}</div>
    <span className="hero-legende">{diapositives[index].legende}</span>
  </div>;
}
