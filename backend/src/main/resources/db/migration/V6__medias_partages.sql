CREATE TABLE media_voyage (
 id_m VARCHAR(36) PRIMARY KEY,
 utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id_u) ON DELETE CASCADE,
 type_contenu VARCHAR(40) NOT NULL,
 contenu BYTEA NOT NULL
);
