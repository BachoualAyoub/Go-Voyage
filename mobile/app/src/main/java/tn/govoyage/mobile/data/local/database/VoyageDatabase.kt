package tn.govoyage.mobile.data.local.database

import android.database.Cursor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tn.govoyage.mobile.data.local.dao.CircuitDAO
import tn.govoyage.mobile.data.local.dao.DestinationDAO
import tn.govoyage.mobile.data.local.dao.UtilisateurDAO
import tn.govoyage.mobile.data.local.entity.CircuitLocal
import tn.govoyage.mobile.data.local.entity.DestinationLocale
import tn.govoyage.mobile.data.local.entity.EtapeLocale
import tn.govoyage.mobile.data.local.entity.UtilisateurLocal

/** Base privee du profil, du carnet et du catalogue, sans session persistante. */
@Database(
    entities = [UtilisateurLocal::class, DestinationLocale::class, CircuitLocal::class, EtapeLocale::class],
    version = 7,
    exportSchema = false
)
abstract class VoyageDatabase : RoomDatabase() {
    abstract fun utilisateurDAO(): UtilisateurDAO
    abstract fun circuitDAO(): CircuitDAO
    abstract fun destinationDAO(): DestinationDAO
}

/** V1 : ancienne session Room + circuit lie au compte. On garde circuits, etapes et destinations. */
val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        supprimerSession(db)
        reconstruireCircuitEtapes(db)
    }
}

/** V2 : schema intermediaire non versionne dans Git ; on couvre les deux formes possibles. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        supprimerSession(db)
        when {
            colonneExiste(db, "circuit", "compteId") -> reconstruireCircuitEtapes(db)
            !colonneExiste(db, "circuit", "favori") ->
                db.execSQL("ALTER TABLE circuit ADD COLUMN favori INTEGER NOT NULL DEFAULT 0")
        }
    }
}

/** V3 -> V4 : profil local, suivi de synchronisation et etat de suppression des circuits. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS utilisateur (
                cle INTEGER PRIMARY KEY NOT NULL,
                info_id INTEGER NOT NULL,
                info_nom TEXT NOT NULL,
                info_prenom TEXT NOT NULL,
                info_email TEXT NOT NULL,
                info_tel TEXT NOT NULL,
                info_role TEXT NOT NULL
            )
            """.trimIndent()
        )
        if (!colonneExiste(db, "circuit", "serveurId")) {
            db.execSQL("ALTER TABLE circuit ADD COLUMN serveurId INTEGER")
        }
        if (!colonneExiste(db, "circuit", "versionServeur")) {
            db.execSQL("ALTER TABLE circuit ADD COLUMN versionServeur INTEGER NOT NULL DEFAULT 0")
        }
        if (!colonneExiste(db, "circuit", "synchronise")) {
            db.execSQL("ALTER TABLE circuit ADD COLUMN synchronise INTEGER NOT NULL DEFAULT 1")
        }
        if (!colonneExiste(db, "circuit", "supprime")) {
            db.execSQL("ALTER TABLE circuit ADD COLUMN supprime INTEGER NOT NULL DEFAULT 0")
        }
    }
}

/** V4 -> V5 : session locale distincte du profil pour restaurer la connexion apres verification. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS session (
                cle INTEGER PRIMARY KEY NOT NULL,
                jeton TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

/** V5 -> V6 : on revient au jeton en memoire, la table session n'est plus utile. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        supprimerSession(db)
    }
}

/** V6 -> V7 : retire la colonne favori, inutilisee, en preservant circuits et etapes. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE circuit_sans_favori (
                id INTEGER PRIMARY KEY NOT NULL,
                nom TEXT NOT NULL,
                dureeJours INTEGER NOT NULL,
                estimationTnd INTEGER NOT NULL,
                serveurId INTEGER,
                versionServeur INTEGER NOT NULL DEFAULT 0,
                synchronise INTEGER NOT NULL DEFAULT 1,
                supprime INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO circuit_sans_favori
                (id, nom, dureeJours, estimationTnd, serveurId, versionServeur, synchronise, supprime)
            SELECT id, nom, dureeJours, estimationTnd, serveurId, versionServeur, synchronise, supprime
            FROM circuit
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE etape_sauvegarde AS SELECT * FROM etape")
        db.execSQL("DROP TABLE etape")
        db.execSQL("DROP TABLE circuit")
        db.execSQL("ALTER TABLE circuit_sans_favori RENAME TO circuit")
        db.execSQL(
            """
            CREATE TABLE etape (
                circuitId INTEGER NOT NULL,
                ordre INTEGER NOT NULL,
                donnee_destinationId INTEGER NOT NULL,
                donnee_nom TEXT NOT NULL,
                donnee_ville TEXT NOT NULL,
                donnee_ambiance TEXT NOT NULL,
                donnee_jours INTEGER NOT NULL,
                donnee_tarifJourTnd INTEGER NOT NULL,
                donnee_coutTnd INTEGER NOT NULL,
                PRIMARY KEY (circuitId, ordre),
                FOREIGN KEY (circuitId) REFERENCES circuit(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO etape (
                circuitId, ordre, donnee_destinationId, donnee_nom, donnee_ville,
                donnee_ambiance, donnee_jours, donnee_tarifJourTnd, donnee_coutTnd
            )
            SELECT circuitId, ordre, donnee_destinationId, donnee_nom, donnee_ville,
                   donnee_ambiance, donnee_jours, donnee_tarifJourTnd, donnee_coutTnd
            FROM etape_sauvegarde
            """.trimIndent()
        )
        db.execSQL("DROP TABLE etape_sauvegarde")
    }
}

/** Supprime uniquement l'ancienne table de session, jamais les circuits du voyageur. */
private fun supprimerSession(db: SupportSQLiteDatabase) {
    if (tableExiste(db, "session")) db.execSQL("DROP TABLE IF EXISTS session")
}

/** Recree circuit et etape en preservant les lignes existantes. */
private fun reconstruireCircuitEtapes(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE circuit_nouveau (
            id INTEGER PRIMARY KEY NOT NULL,
            nom TEXT NOT NULL,
            dureeJours INTEGER NOT NULL,
            estimationTnd INTEGER NOT NULL,
            favori INTEGER NOT NULL
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO circuit_nouveau (id, nom, dureeJours, estimationTnd, favori)
        SELECT id, nom, dureeJours, estimationTnd, 0 FROM circuit
        """.trimIndent()
    )
    db.execSQL("CREATE TABLE etape_sauvegarde AS SELECT * FROM etape")
    db.execSQL("DROP TABLE etape")
    db.execSQL("DROP TABLE circuit")
    db.execSQL("ALTER TABLE circuit_nouveau RENAME TO circuit")
    db.execSQL(
        """
        CREATE TABLE etape (
            circuitId INTEGER NOT NULL,
            ordre INTEGER NOT NULL,
            donnee_destinationId INTEGER NOT NULL,
            donnee_nom TEXT NOT NULL,
            donnee_ville TEXT NOT NULL,
            donnee_ambiance TEXT NOT NULL,
            donnee_jours INTEGER NOT NULL,
            donnee_tarifJourTnd INTEGER NOT NULL,
            donnee_coutTnd INTEGER NOT NULL,
            PRIMARY KEY (circuitId, ordre),
            FOREIGN KEY (circuitId) REFERENCES circuit(id) ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO etape (
            circuitId, ordre, donnee_destinationId, donnee_nom, donnee_ville,
            donnee_ambiance, donnee_jours, donnee_tarifJourTnd, donnee_coutTnd
        )
        SELECT circuitId, ordre, donnee_destinationId, donnee_nom, donnee_ville,
               donnee_ambiance, donnee_jours, donnee_tarifJourTnd, donnee_coutTnd
        FROM etape_sauvegarde
        """.trimIndent()
    )
    db.execSQL("DROP TABLE etape_sauvegarde")
}

/** Verifie la presence d'une colonne sans supposer le schema exact. */
private fun colonneExiste(db: SupportSQLiteDatabase, table: String, colonne: String): Boolean {
    var curseur: Cursor? = null
    try {
        curseur = db.query("PRAGMA table_info($table)")
        while (curseur.moveToNext()) {
            if (curseur.getString(curseur.getColumnIndexOrThrow("name")) == colonne) return true
        }
        return false
    } finally {
        curseur?.close()
    }
}

/** Verifie la presence d'une table avant de la supprimer. */
private fun tableExiste(db: SupportSQLiteDatabase, table: String): Boolean {
    var curseur: Cursor? = null
    try {
        curseur = db.query("PRAGMA table_info($table)")
        return curseur.count > 0
    } finally {
        curseur?.close()
    }
}
