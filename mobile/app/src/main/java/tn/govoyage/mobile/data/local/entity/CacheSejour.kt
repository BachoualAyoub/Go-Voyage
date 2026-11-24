package tn.govoyage.mobile.data.local.entity

import androidx.room.Entity

/** Instantane local des sejours, isole par compte et par recherche. */
@Entity(tableName = "cache_voyage", primaryKeys = ["compte", "cle"])
data class CacheSejour(
    val compte: Long,
    val cle: String,
    val contenu: String,
    val date: Long
)
