package com.droid.netflixIMDB.network

import com.droid.netflixIMDB.BuildConfig
import com.droid.netflixIMDB.OMDBResponse
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface NetworkManager {

    @GET("?apikey=38dc3955")
    open fun getRatingAsync(
        @Query("t") title: String?,
        @Query("type") type: String? = null,
        @Query("y") year: String? = null
    ): Deferred<Response<OMDBResponse>>


    companion object HTTPService {

        private var create: NetworkManager? = null
        private var retrofit: Retrofit? = null

        fun getInstance(): NetworkManager? {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl("https://www.omdbapi.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .client(getHTTPClient())
                    .build()
                create = retrofit?.create(
                    NetworkManager::class.java)
            }
            return create
        }

        private fun getHTTPClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()

            builder.connectTimeout(10, TimeUnit.SECONDS)
            builder.readTimeout(10, TimeUnit.SECONDS)
            builder.writeTimeout(10, TimeUnit.SECONDS)

            if (BuildConfig.DEBUG)
                builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))

            return builder.build()
        }
    }
}