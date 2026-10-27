package com.jrustam.ledgerly

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jrustam.ledgerly.data.TransactionDatabase
import com.jrustam.ledgerly.data.local.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TransactionDatabaseTest {
    @Test fun transactionSurvivesReopenAndRetryDoesNotDuplicate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-${java.util.UUID.randomUUID()}.db"
        val entity = TransactionEntity("stable-id", 1234L, "USD", "EXPENSE", "FOOD", 20000L, "Lunch")
        try {
            Room.databaseBuilder(context, TransactionDatabase::class.java, name).build().let { db ->
                try {
                    db.transactionDao().insert(entity)
                    assertEquals(-1L, db.transactionDao().insert(entity))
                } finally { db.close() }
            }
            Room.databaseBuilder(context, TransactionDatabase::class.java, name).build().let { db ->
                try { assertEquals(entity, db.transactionDao().findById(entity.id)) }
                finally { db.close() }
            }
        } finally { context.deleteDatabase(name) }
    }
}
