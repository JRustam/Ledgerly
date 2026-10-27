package com.jrustam.ledgerly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String, val name: String, val bank: String, val kind: String,
    val currency: String, val openingMinor: Long, val theme: Int, val lastFour: String
)
