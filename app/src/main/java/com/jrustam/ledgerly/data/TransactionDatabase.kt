package com.jrustam.ledgerly.data

import androidx.room.*

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountMinor: Long,
    val currency: String,
    val type: String,
    val category: String,
    val dateEpochDay: Long,
    val description: String?,
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, rowid DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<TransactionEntity>>

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String): Int

    // Stable draft IDs make retries idempotent, including after process recreation.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: String): TransactionEntity?
}

@Database(entities = [TransactionEntity::class], version = 2, exportSchema = true)
abstract class TransactionDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        // Version 1 already had a currency column, but all amounts represented USD cents.
        // This data migration makes that legacy meaning explicit without touching amounts/IDs.
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("UPDATE transactions SET currency = 'USD'")
            }
        }
    }
}
