package com.tooploox.songapp.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import java.io.InputStreamReader

class AssetsProvider(private val context: Context) {

    internal fun localSongs(): Single<List<LocalSongModel>> =
        Single.fromCallable {
            val jsonStream = context.assets.open(LOCAL_DATA_FILE_NAME)
            val type = object : TypeToken<List<LocalSongModel>>() {}.type
            val reader = InputStreamReader(jsonStream, Charsets.UTF_8)
            return@fromCallable Gson().fromJson<List<LocalSongModel>>(reader, type)
        }

    companion object {
        private const val LOCAL_DATA_FILE_NAME = "local_songs.json"
    }
}