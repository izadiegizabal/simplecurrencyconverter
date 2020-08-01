package xyz.izadi.simplecurrencyconverter.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DBCurrency(
    @PrimaryKey val code: String,
    val name: String,
    val rate: Float,
    val timestamp: Long
)
