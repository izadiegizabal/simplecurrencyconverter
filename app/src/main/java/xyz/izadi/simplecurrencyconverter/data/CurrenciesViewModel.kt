package xyz.izadi.simplecurrencyconverter.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import xyz.izadi.simplecurrencyconverter.data.api.Currencies
import xyz.izadi.simplecurrencyconverter.data.api.CurrencyRepository
import xyz.izadi.simplecurrencyconverter.data.api.Rates

class CurrenciesViewModel(application: Application): AndroidViewModel(application) {

    private var repository = CurrencyRepository.instance
    var currenciesLiveData: MutableLiveData<Currencies> = MutableLiveData()
    var ratesLiveData: MutableLiveData<Rates> = MutableLiveData()

    init {
        getCurrencies()
    }

    private fun getCurrencies() {
        viewModelScope.launch {
            try {
                val request = repository.getCurrencies()
                if (request.success) {
                    currenciesLiveData.postValue(request)
                }
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    fun updateRates() {
        getRates()
    }

    private fun getRates() {
        viewModelScope.launch {
            try {
                val request = repository.getRates()
                if (request.success) {
                    ratesLiveData.postValue(request)
                }
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CurrenciesViewModel(application) as T
        }
    }
}