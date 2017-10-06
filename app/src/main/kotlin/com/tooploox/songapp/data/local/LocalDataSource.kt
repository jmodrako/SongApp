package com.tooploox.songapp.data.local

import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class LocalDataSource(assetsProvider: AssetsProvider) : DataSource {

    private val localSongsList: Single<List<LocalSongModel>> by lazy(assetsProvider::localSongs)

    override fun search(query: String): Single<List<SongModel>> =
        if (query.isBlank()) {
            Single.just(emptyList())
        } else {
            localSongsList
                .subscribeOn(Schedulers.io())
                .flattenAsObservable { it }
                .filter { it.combined.contains(query, true) }
                .map { SongModel(it.trackName, it.artistName, it.releaseYear) }
                .toList()
        }
}