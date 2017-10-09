package com.tooploox.songapp.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.tooploox.songapp.BuildConfig
import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

class RemoteDataSource : DataSource {

    private val api: Api = setupApi()

    override fun search(query: String): Single<List<SongModel>> =
        if (query.isBlank()) {
            Single.just(emptyList())
        } else {
            api.search(query, API_RESPONSE_LIMIT)
                .subscribeOn(Schedulers.io())
                .flattenAsObservable(ApiSearchModel::results)
                .map { SongModel(it.trackName, it.artistName, it.releaseYear, it.artworkUrl100) }
                .toList()
        }

    private fun setupApi(): Api {
        val okHttpClientBuilder = OkHttpClient.Builder()
        withDebugInterceptor(okHttpClientBuilder)

        return Retrofit.Builder()
            .client(okHttpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(prepareGson()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(API_URL)
            .build().create(Api::class.java)
    }

    private fun prepareGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTypeAdapter())
        return gsonBuilder.create()
    }

    private fun withDebugInterceptor(httpClient: OkHttpClient.Builder) {
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger.DEFAULT)
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(interceptor)
        }
    }

    companion object {
        private const val API_URL = "https://itunes.apple.com/"
        private const val API_RESPONSE_LIMIT = 50
    }
}

private class LocalDateTypeAdapter : JsonDeserializer<LocalDateTime> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime? {
        val dateWithoutZone = dropZoneCharacter(json?.asString)
        return if (dateWithoutZone != null) LocalDateTime.parse(dateWithoutZone, DateTimeFormatter.ISO_LOCAL_DATE_TIME) else null
    }

    private fun dropZoneCharacter(dateRaw: String?): String? = dateRaw?.dropLastWhile({ it == 'Z' || it == 'z' })
}