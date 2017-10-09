package com.tooploox.songapp.data

import io.reactivex.Single

interface DataSource {

    fun search(query: String): Single<List<SongModel>>

    enum class Type(val default: Boolean = false) {
        LOCAL, REMOTE, ALL(true);

        companion object {
            fun default(): Type = values().first(Type::default)
        }
    }
}