package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A trainable extractor for free-form "normal" messages that customers send to
 * announce a payment ("Nimelipa 50,000, jina Asha, nipo Mbeya, system 200W").
 * Each rule pulls ONE field out of the body via a regex capture group. The
 * highest-priority matching rule per [fieldKey] wins.
 *
 * Common field keys: name, location, amount, system_size, phone.
 */
@Entity(tableName = "info_rules")
data class InfoRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** The key this rule fills, e.g. "name", "location", "amount", "system_size". */
    val fieldKey: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val isBuiltIn: Boolean = false,
    /** Regex whose capture group [group] holds the value. */
    val regex: String,
    val group: Int = 1
)
