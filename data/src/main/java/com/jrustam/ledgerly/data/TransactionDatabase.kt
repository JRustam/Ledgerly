package com.jrustam.ledgerly.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jrustam.ledgerly.data.local.dao.TransactionDao
import com.jrustam.ledgerly.data.local.dao.FinanceDao
import com.jrustam.ledgerly.data.local.entity.TransactionEntity
import com.jrustam.ledgerly.data.local.entity.WalletEntity
import com.jrustam.ledgerly.data.local.entity.TransferEntity
import com.jrustam.ledgerly.data.local.entity.PlanEntity
import com.jrustam.ledgerly.data.local.entity.ContributionEntity

@Database(entities = [TransactionEntity::class, WalletEntity::class, TransferEntity::class, PlanEntity::class, ContributionEntity::class], version = 3, exportSchema = true)
abstract class TransactionDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun financeDao(): FinanceDao

    companion object {
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN walletId TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS wallets (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, bank TEXT NOT NULL, kind TEXT NOT NULL, currency TEXT NOT NULL, openingMinor INTEGER NOT NULL, theme INTEGER NOT NULL, lastFour TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS transfers (id TEXT NOT NULL PRIMARY KEY, fromId TEXT NOT NULL, toId TEXT NOT NULL, debitMinor INTEGER NOT NULL, creditMinor INTEGER NOT NULL, dateEpochDay INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS plans (id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, name TEXT NOT NULL, currency TEXT NOT NULL, amountMinor INTEGER NOT NULL, category TEXT NOT NULL, dateEpochDay INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS contributions (id TEXT NOT NULL PRIMARY KEY, goalId TEXT NOT NULL, amountMinor INTEGER NOT NULL, dateEpochDay INTEGER NOT NULL)")
            }
        }

        // Version 1 already had a currency column, but all amounts represented USD cents.
        // This data migration makes that legacy meaning explicit without touching amounts/IDs.
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("UPDATE transactions SET currency = 'USD'")
            }
        }
    }
}
