package com.tooploox.songapp.data

import io.reactivex.Single

interface DataSource {
    fun search(query: String): Single<List<SongModel>>
}