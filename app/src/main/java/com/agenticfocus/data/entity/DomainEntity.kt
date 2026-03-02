package com.agenticfocus.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "domains")
data class DomainEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String      // hex ex: "#4CAF50"
)
