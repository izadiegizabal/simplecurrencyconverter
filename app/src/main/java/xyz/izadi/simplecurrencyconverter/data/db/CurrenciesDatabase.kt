package xyz.izadi.simplecurrencyconverter.data.db

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [DBCurrency::class], version = 1)
abstract class CurrenciesDatabase : RoomDatabase() {
    abstract fun currencyDAO(): CurrencyDAO
}