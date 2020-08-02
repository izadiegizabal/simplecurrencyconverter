package xyz.izadi.simplecurrencyconverter.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import xyz.izadi.simplecurrencyconverter.data.api.Currencies
import xyz.izadi.simplecurrencyconverter.data.api.Rates
import xyz.izadi.simplecurrencyconverter.data.db.DBCurrency
import java.util.*

class CurrenciesViewModel(application: Application) : AndroidViewModel(application) {

    private var repository = CurrencyRepository(application)
    var currenciesLiveData: MutableLiveData<Currencies> = MutableLiveData()
    var ratesLiveData: MutableLiveData<Rates> = MutableLiveData()

    init {
        getCurrencies()
    }

    // Get both the currency list and currencies if needed
    private fun getCurrencies() {
        viewModelScope.launch {
            try {
                val ratesDB = repository.getDbCurrencies()
                if (ratesDB.isEmpty()) {
                    val currencyRequest = repository.getCurrencies()
                    if (currencyRequest.success) {
                        currenciesLiveData.postValue(currencyRequest)
                    }
                } else {
                    currenciesLiveData.postValue(getCurrenciesFromDBFormat(ratesDB))
                }

                if (doWeUpdate()) {
                    val ratesRequest = repository.getRates()
                    if (ratesRequest.success) {
                        updateRates()
                    }
                } else {
                    ratesLiveData.postValue(getRatesFromDBFormat(ratesDB))
                }
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    // Fetch the rates and update the DB
    private suspend fun updateRates() {
        val ratesRequest = repository.getRates()
        if (ratesRequest.success) {
            ratesLiveData.postValue(ratesRequest)
            updateDB(ratesRequest)
            updateLastRetrievalDate(Date(System.currentTimeMillis()))
        }
    }

    // update the DB
    private fun updateDB(currentRates: Rates) {
        val currentCurrencies = currenciesLiveData.value
        val validCurrencyForDB: MutableList<DBCurrency> = mutableListOf()

        if (currentCurrencies?.currencies == null) return

        for ((code, name) in currentCurrencies.currencies) {
            currentRates.quotes["USD$code"]?.let {
                DBCurrency(
                    code, name,
                    it, currentRates.timestamp
                )
            }?.let {
                validCurrencyForDB.add(it)
            }
        }

        viewModelScope.launch {
            try {
                repository.addToDb(validCurrencyForDB)
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    // update the rates if the cool down time has passed
    fun updateRatesIfNeeded() {
        if (currenciesLiveData.value == null) {
            getCurrencies()
        } else if (doWeUpdate()) viewModelScope.launch {
            updateRates()
        }
    }

    // see if we need to update from the internet
    private fun doWeUpdate(): Boolean {
        val currentTime = Date(System.currentTimeMillis()).time
        // if less than 30mins since last update no need to update again
        val dateLastUpdate = getLastUpdateDate() ?: return true
        val minsSinceLastUpdate = ((currentTime - dateLastUpdate.time) / 1000) / 60
        return minsSinceLastUpdate > CHECK_UPDATES_EVERY
    }

    // Get the time of the last successful update of Rates
    fun getLastUpdateDate(): Date? {
        val sharedPref = getApplication<Application>().getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE
        )
        if (!sharedPref.contains(UPDATED_AT)) return null
        return Date(sharedPref.getLong(UPDATED_AT, Date(System.currentTimeMillis()).time))
    }

    // Set a new date as the last time when the rates where updated
    private fun updateLastRetrievalDate(date: Date) {
        val sharedPref = getApplication<Application>().getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE
        )
        with(sharedPref.edit()) {
            putLong(UPDATED_AT, date.time)
            apply()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CurrenciesViewModel(application) as T
        }
    }

    companion object {
        private const val PREFERENCE_NAME = "CurrencyPref"
        private const val UPDATED_AT = "updated_at"
        private const val CHECK_UPDATES_EVERY = 30 //minutes
    }
}