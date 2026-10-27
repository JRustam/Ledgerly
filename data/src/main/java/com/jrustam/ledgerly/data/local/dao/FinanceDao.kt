package com.jrustam.ledgerly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Upsert
import com.jrustam.ledgerly.data.local.entity.WalletEntity
import com.jrustam.ledgerly.data.local.entity.TransferEntity
import com.jrustam.ledgerly.data.local.entity.PlanEntity
import com.jrustam.ledgerly.data.local.entity.ContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM wallets ORDER BY name")
    fun wallets(): Flow<List<WalletEntity>>
    @Query("SELECT * FROM transfers ORDER BY dateEpochDay DESC, rowid DESC")
    fun transfers(): Flow<List<TransferEntity>>
    @Query("SELECT * FROM plans ORDER BY dateEpochDay, name")
    fun plans(): Flow<List<PlanEntity>>
    @Query("SELECT * FROM contributions ORDER BY dateEpochDay")
    fun contributions(): Flow<List<ContributionEntity>>
    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun wallet(id: String): WalletEntity?
    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun plan(id: String): PlanEntity?
    @Upsert
    suspend fun save(wallet: WalletEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transfer: TransferEntity)
    @Upsert
    suspend fun save(plan: PlanEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contribution: ContributionEntity)
    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransfer(id: String)
    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: String)
    @Query("DELETE FROM contributions WHERE goalId = :id")
    suspend fun deleteContributions(id: String)
}
