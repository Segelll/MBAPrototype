package com.example.mbaprototype.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Android emülatöründen bilgisayarınızdaki localhost'a erişmek için 10.0.2.2 adresi kullanılır.
    // API dokümantasyonunuzdaki http://127.0.0.1:8000 adresine karşılık gelir.
    private const val BASE_URL = "http://127.0.0.1:8000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}