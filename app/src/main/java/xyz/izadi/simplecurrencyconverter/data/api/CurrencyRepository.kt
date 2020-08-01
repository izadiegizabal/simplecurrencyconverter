package xyz.izadi.simplecurrencyconverter.data.api

import android.app.Application
import android.util.Log
import androidx.room.Room
import xyz.izadi.simplecurrencyconverter.data.db.CurrenciesDatabase
import xyz.izadi.simplecurrencyconverter.data.db.DBCurrency


class CurrencyRepository(app: Application) {
    private var client: CurrencyLayerApi = RetrofitClient.apiService
    private var db = Room.databaseBuilder(
        app,
        CurrenciesDatabase::class.java, "currencies-db"
    ).build()

    suspend fun getCurrencies() = client.getCurrencies()
    suspend fun getRates() = client.getRates()

    suspend fun getDbCurrencies() = db.currencyDAO().getAll()

    suspend fun addToDb(currencies: List<DBCurrency>) = db.currencyDAO().insertAll(currencies)
}