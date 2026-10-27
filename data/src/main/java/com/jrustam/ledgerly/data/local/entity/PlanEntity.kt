package com.jrustam.ledgerly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String, val kind: String, val name: String, val currency: String,
    val amountMinor: Long, val category: String, val dateEpochDay: Long
)
