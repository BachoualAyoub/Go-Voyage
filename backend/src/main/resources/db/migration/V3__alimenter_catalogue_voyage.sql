-- Tarifs de référence pour la démonstration, exprimés en dinars par jour.
INSERT INTO destination (code, nom, ville, pays, region, ambiance, tarif_jour_tnd, accroche, description) VALUES
('sidi-bou-said', 'Sidi Bou Saïd', 'Sidi Bou Saïd', 'Tunisie', 'Grand Tunis', 'CULTURE', 95,
 'Un balcon bleu et blanc sur la Méditerranée.',
 'Promenez-vous dans les ruelles du village, découvrez ses portes bleues et prenez une pause face au golfe de Tunis. Une étape douce à combiner avec Tunis et Carthage.'),
('hammamet', 'Hammamet', 'Hammamet', 'Tunisie', 'Cap Bon', 'MER', 125,
 'Médina, jardins et longues plages.',
 'Commencez dans la médina, longez les remparts et gardez du temps pour la plage. Hammamet est une étape balnéaire accessible depuis Tunis.'),
('djerba', 'Djerba', 'Houmt Souk', 'Tunisie', 'Sud-Est', 'MER', 140,
 'Une île lumineuse au rythme tranquille.',
 'Découvrez Houmt Souk, le patrimoine de l’île et les plages. Répartissez vos journées entre les villages et le littoral.'),
('kairouan', 'Kairouan', 'Kairouan', 'Tunisie', 'Centre', 'CULTURE', 85,
 'Artisanat et patrimoine dans la médina.',
 'Prévoyez une journée pour découvrir les ruelles, les ateliers et les monuments. Les étapes dans le centre de la Tunisie offrent une autre lecture du voyage.'),
('tozeur', 'Tozeur', 'Tozeur', 'Tunisie', 'Djerid', 'NATURE', 110,
 'Palmeraies, briques ocres et horizons du Sud.',
 'Explorez la vieille ville et les oasis, puis prévoyez du temps pour les paysages du Djerid. Gardez les longues excursions pour les journées complètes.'),
('tabarka', 'Tabarka', 'Tabarka', 'Tunisie', 'Nord-Ouest', 'NATURE', 100,
 'La mer, les aiguilles rocheuses et les forêts.',
 'Combinez une balade côtière avec une sortie dans le Nord-Ouest. Tabarka se prête bien aux voyages qui alternent nature et bord de mer.'),
('el-jem', 'El Jem', 'El Jem', 'Tunisie', 'Sahel', 'CULTURE', 80,
 'Une étape autour de l’amphithéâtre.',
 'Découvrez l’amphithéâtre et le musée avant de reprendre la route. Une étape courte se combine bien avec le littoral du Sahel.'),
('douz', 'Douz', 'Douz', 'Tunisie', 'Sud', 'NATURE', 115,
 'La porte des dunes sahariennes.',
 'À Douz, le paysage change avec la lumière. Prévoyez une étape consacrée aux dunes et au rythme du désert.');

INSERT INTO circuit (nom, description) VALUES
('Bleu méditerranéen', 'Des ruelles de Sidi Bou Saïd aux plages du Cap Bon.'),
('Oasis et dunes', 'Deux haltes dans le Sud pour prendre le temps du désert.'),
('Patrimoine au fil de la route', 'Deux étapes entre médina, artisanat et héritage romain.');

INSERT INTO etape_circuit (circuit_id, ordre, destination_id, jours)
SELECT c.id_c, e.ordre, d.id_d, e.jours
FROM (VALUES
    ('Bleu méditerranéen', 0, 'sidi-bou-said', 2),
    ('Bleu méditerranéen', 1, 'hammamet', 2),
    ('Oasis et dunes', 0, 'tozeur', 2),
    ('Oasis et dunes', 1, 'douz', 2),
    ('Patrimoine au fil de la route', 0, 'kairouan', 2),
    ('Patrimoine au fil de la route', 1, 'el-jem', 1)
) AS e(nom, ordre, code, jours)
JOIN circuit c ON c.nom = e.nom AND c.proprietaire_id IS NULL
JOIN destination d ON d.code = e.code;
