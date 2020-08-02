package xyz.izadi.simplecurrencyconverter.data.api

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import retrofit2.http.GET
import xyz.izadi.simplecurrencyconverter.data.addCommas
import xyz.izadi.simplecurrencyconverter.data.round

interface CurrencyLayerApi {
    @GET("/live")
    suspend fun getRates(): Rates

    @GET("/list")
    suspend fun getCurrencies(): Currencies
}

@Parcelize
data class Currencies(
    val success: Boolean,
    var totalCurrencies: Int,
    val currencies: Map<String, String>
) : Parcelable

data class Rates(
    val success: Boolean,
    val timestamp: Long,
    val quotes: Map<String, Float>
) {
    fun convert(quantity: Double, from: String, to: String): String {
        // from --> usd --> to
        val fromRate = quotes["USD$from"] ?: error("No available origin rate")
        val toRate = quotes["USD$to"] ?: error("No available destination rate")
        val conversion = (quantity / fromRate.toDouble()) * toRate.toDouble()
        val roundedString = round(conversion, 4).toString()
        return addCommas(roundedString)
    }
}
