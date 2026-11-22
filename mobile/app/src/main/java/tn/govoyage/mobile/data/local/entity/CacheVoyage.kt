package tn.govoyage.mobile.data.local.entity

import androidx.room.Entity

/** Instantane metier isole par compte et recherche, jamais un jeton ou mot de passe. */
@Entity(tableName = "cache_voyage", primaryKeys = ["compte", "cle"])
data class CacheVoyage(val compte: Long, val cle: String, val contenu: String, val date: Long)
