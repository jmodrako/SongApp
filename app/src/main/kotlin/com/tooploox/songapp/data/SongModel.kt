package com.tooploox.songapp.data

data class SongModel(
    val title: String,
    val artist: String,
    val year: String? = null
) {
    val hasYear
        get() = year?.isNotBlank()
}