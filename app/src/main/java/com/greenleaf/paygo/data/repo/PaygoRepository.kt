package com.greenleaf.paygo.data.repo

import com.greenleaf.paygo.data.db.PaygoDatabase
import com.greenleaf.paygo.data.db.entity.ContactGroupEntity
import com.greenleaf.paygo.data.db.entity.EventLogEntity
import com.greenleaf.paygo.data.db.entity.InfoRuleEntity
import com.greenleaf.paygo.data.db.entity.MessageCategory
import com.greenleaf.paygo.data.db.entity.MessageEntity
import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity
import com.greenleaf.paygo.data.db.entity.TransactionEntity
import com.greenleaf.paygo.parser.DefaultRules
import kotlinx.coroutines.flow.Flow

/** Single access point for all persistence. */
class PaygoRepository(private val db: PaygoDatabase) {

    // Messages ---------------------------------------------------------------
    fun observeMessages(): Flow<List<MessageEntity>> = db.messageDao().observeAll()
    fun observeMessagesByCategory(category: MessageCategory): Flow<List<MessageEntity>> =
        db.messageDao().observeByCategory(category.name)
    fun observeMessagesByGroup(groupKey: String): Flow<List<MessageEntity>> =
        db.messageDao().observeByGroup(groupKey)
    fun observeMessageCount(): Flow<Int> = db.messageDao().observeCount()
    suspend fun insertMessage(m: MessageEntity): Long = db.messageDao().insert(m)
    suspend fun updateMessage(m: MessageEntity) = db.messageDao().update(m)
    suspend fun getMessage(id: Long) = db.messageDao().getById(id)
    suspend fun allMessages() = db.messageDao().getAllOnce()

    // Transactions -----------------------------------------------------------
    fun observeTransactions(): Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    fun observeTotalReceived(): Flow<Double> = db.transactionDao().observeTotalReceived()
    fun observeTransactionCount(): Flow<Int> = db.transactionDao().observeCount()
    suspend fun insertTransaction(t: TransactionEntity): Long = db.transactionDao().insert(t)
    suspend fun allTransactions() = db.transactionDao().getAllOnce()

    // Rules ------------------------------------------------------------------
    fun observeParsingRules(): Flow<List<ParsingRuleEntity>> = db.parsingRuleDao().observeAll()
    suspend fun enabledParsingRules() = db.parsingRuleDao().getEnabled()
    suspend fun upsertParsingRule(rule: ParsingRuleEntity) = db.parsingRuleDao().upsert(rule)
    suspend fun deleteParsingRule(rule: ParsingRuleEntity) = db.parsingRuleDao().delete(rule)

    fun observeResponseRules(): Flow<List<ResponseRuleEntity>> = db.responseRuleDao().observeAll()
    suspend fun enabledResponseRules() = db.responseRuleDao().getEnabled()
    suspend fun upsertResponseRule(rule: ResponseRuleEntity) = db.responseRuleDao().upsert(rule)
    suspend fun deleteResponseRule(rule: ResponseRuleEntity) = db.responseRuleDao().delete(rule)

    fun observeInfoRules(): Flow<List<InfoRuleEntity>> = db.infoRuleDao().observeAll()
    suspend fun enabledInfoRules() = db.infoRuleDao().getEnabled()
    suspend fun upsertInfoRule(rule: InfoRuleEntity) = db.infoRuleDao().upsert(rule)
    suspend fun deleteInfoRule(rule: InfoRuleEntity) = db.infoRuleDao().delete(rule)

    // Contact groups ---------------------------------------------------------
    fun observeGroups(): Flow<List<ContactGroupEntity>> = db.contactGroupDao().observeAll()
    suspend fun getGroup(key: String) = db.contactGroupDao().getByKey(key)
    suspend fun upsertGroup(group: ContactGroupEntity) = db.contactGroupDao().upsert(group)

    // Event log --------------------------------------------------------------
    fun observeEvents(): Flow<List<EventLogEntity>> = db.eventLogDao().observeRecent()
    suspend fun logEvent(e: EventLogEntity) = db.eventLogDao().insert(e)
    suspend fun allEvents() = db.eventLogDao().getAllOnce()

    /** Seed built-in provider rules on first launch. */
    suspend fun seedIfEmpty() {
        if (db.parsingRuleDao().count() == 0) {
            DefaultRules.parsingRules().forEach { db.parsingRuleDao().upsert(it) }
        }
        if (db.responseRuleDao().count() == 0) {
            DefaultRules.responseRules().forEach { db.responseRuleDao().upsert(it) }
        }
        if (db.infoRuleDao().count() == 0) {
            DefaultRules.infoRules().forEach { db.infoRuleDao().upsert(it) }
        }
    }
}
