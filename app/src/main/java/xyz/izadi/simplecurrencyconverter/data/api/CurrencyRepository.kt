package xyz.izadi.simplecurrencyconverter.data.api

class CurrencyRepository {
    private var client: CurrencyLayerApi = RetrofitClient.apiService

    companion object Factory {
        val instance: CurrencyRepository
            @Synchronized get() {
                return CurrencyRepository()
            }
    }

    suspend fun getCurrencies() = client.getCurrencies()
    suspend fun getRates() = client.getRates()
}