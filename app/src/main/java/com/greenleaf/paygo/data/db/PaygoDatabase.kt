package com.greenleaf.paygo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.greenleaf.paygo.data.db.dao.ContactGroupDao
import com.greenleaf.paygo.data.db.dao.EventLogDao
import com.greenleaf.paygo.data.db.dao.MessageDao
import com.greenleaf.paygo.data.db.dao.ParsingRuleDao
import com.greenleaf.paygo.data.db.dao.ResponseRuleDao
import com.greenleaf.paygo.data.db.dao.TransactionDao
import com.greenleaf.paygo.data.db.entity.ContactGroupEntity
import com.greenleaf.paygo.data.db.entity.EventLogEntity
import com.greenleaf.paygo.data.db.entity.MessageEntity
import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity
import com.greenleaf.paygo.data.db.entity.TransactionEntity

@Database(
    entities = [
        MessageEntity::class,
        TransactionEntity::class,
        ParsingRuleEntity::class,
        ResponseRuleEntity::class,
        ContactGroupEntity::class,
        EventLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PaygoDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun transactionDao(): TransactionDao
    abstract fun parsingRuleDao(): ParsingRuleDao
    abstract fun responseRuleDao(): ResponseRuleDao
    abstract fun contactGroupDao(): ContactGroupDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        @Volatile private var INSTANCE: PaygoDatabase? = null

        fun get(context: Context): PaygoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PaygoDatabase::class.java,
                    "paygo.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
