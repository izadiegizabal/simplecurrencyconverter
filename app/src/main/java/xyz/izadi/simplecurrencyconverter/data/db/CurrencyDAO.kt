package xyz.izadi.simplecurrencyconverter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDAO {
    @Query("SELECT * FROM dbcurrency")
    suspend fun getAll(): List<DBCurrency>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<DBCurrency>)
}