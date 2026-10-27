package com.jrustam.ledgerly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contributions")
data class ContributionEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val amountMinor: Long,
    val dateEpochDay: Long
)
