package com.jrustam.ledgerly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val id: String, val fromId: String, val toId: String,
    val debitMinor: Long, val creditMinor: Long, val dateEpochDay: Long
)
