package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A "training" rule that tells the parser how to read a payment SMS for a given
 * provider. Everything is regex driven so the user can add/adjust providers from
 * the UI without a code change.
 *
 * A rule matches when [senderPattern] matches the SMS sender address AND
 * [bodyPattern] matches the SMS text. The remaining regexes then extract fields;
 * the value is taken from capture group [*Group] (1 by default).
 */
@Entity(tableName = "parsing_rules")
data class ParsingRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val name: String,
    /** Provider slug this rule produces, e.g. "mpesa". */
    val provider: String,
    val enabled: Boolean = true,
    /** Higher priority rules are evaluated first. */
    val priority: Int = 0,
    val isBuiltIn: Boolean = false,

    /** Regex matched against the SMS sender line (case-insensitive). */
    val senderPattern: String,
    /** Regex that must be found in the SMS body for this rule to apply. */
    val bodyPattern: String,

    /** RECEIVED / SENT — direction assigned to matches of this rule. */
    val direction: String = TxnDirection.RECEIVED.name,

    val amountRegex: String? = null,
    val amountGroup: Int = 1,
    val nameRegex: String? = null,
    val nameGroup: Int = 1,
    val numberRegex: String? = null,
    val numberGroup: Int = 1,
    val referenceRegex: String? = null,
    val referenceGroup: Int = 1,
    val balanceRegex: String? = null,
    val balanceGroup: Int = 1
)
