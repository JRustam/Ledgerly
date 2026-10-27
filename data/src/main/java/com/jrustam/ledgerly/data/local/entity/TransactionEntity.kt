package com.jrustam.ledgerly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountMinor: Long,
    val currency: String,
    val type: String,
    val category: String,
    val dateEpochDay: Long,
    val description: String?,
    val walletId: String? = null,
)
