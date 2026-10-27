package com.jrustam.ledgerly

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.jrustam.ledgerly.data.TransactionDatabase
import com.jrustam.ledgerly.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TransactionMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @get:Rule val helper = MigrationTestHelper(instrumentation,
        TransactionDatabase::class.java.canonicalName!!, FrameworkSQLiteOpenHelperFactory())

    @Test fun migrationPreservesLegacyValuesAndAssignsUsd() = runBlocking {
        val name = "migration-${java.util.UUID.randomUUID()}"
        try {
            helper.createDatabase(name, 1).apply {
                execSQL("INSERT INTO transactions VALUES ('legacy', 12345, 'USD', 'EXPENSE', 'FOOD', 20000, 'Lunch')")
                close()
            }
            helper.runMigrationsAndValidate(name, 3, true, TransactionDatabase.MIGRATION_1_2, TransactionDatabase.MIGRATION_2_3).close()
            val db = Room.databaseBuilder(instrumentation.targetContext, TransactionDatabase::class.java, name)
                .addMigrations(TransactionDatabase.MIGRATION_1_2, TransactionDatabase.MIGRATION_2_3).build()
            try {
                assertEquals(TransactionEntity("legacy", 12345, "USD", "EXPENSE", "FOOD", 20000, "Lunch"),
                    db.transactionDao().findById("legacy"))
                val azn = TransactionEntity("new", 9876, "AZN", "INCOME", "SALARY", 20001, null)
                db.transactionDao().insert(azn)
                assertEquals(listOf("new", "legacy"), db.transactionDao().observeAll().first().map { it.id })
                db.transactionDao().update(azn.copy(currency = "EUR", amountMinor = 5432))
                assertEquals("EUR", db.transactionDao().observeAll().first().first().currency)
                assertEquals(5432L, db.transactionDao().findById("new")!!.amountMinor)
                db.transactionDao().delete("new")
                assertEquals(listOf("legacy"), db.transactionDao().observeAll().first().map { it.id })
            } finally { db.close() }
        } finally { instrumentation.targetContext.deleteDatabase(name) }
    }
}
