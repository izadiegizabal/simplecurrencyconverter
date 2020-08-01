package xyz.izadi.simplecurrencyconverter.data.db

import androidx.room.*

@Dao
interface CurrencyDAO {
    @Query("SELECT * FROM dbcurrency")
    suspend fun getAll(): List<DBCurrency>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<DBCurrency>)
}