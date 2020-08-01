package xyz.izadi.simplecurrencyconverter.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import xyz.izadi.simplecurrencyconverter.BuildConfig

object RetrofitClient {
    private val authInterceptor = Interceptor { chain ->
        val newUrl = chain.request().url()
            .newBuilder()
            .addQueryParameter("access_key", BuildConfig.CURRENCYLAYER_API_KEY)
            .build()

        val newRequest = chain.request()
            .newBuilder()
            .url(newUrl)
            .build()

        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(authInterceptor)
        .build()

    val apiService: CurrencyLayerApi by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://api.currencylayer.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CurrencyLayerApi::class.java)
    }
}
