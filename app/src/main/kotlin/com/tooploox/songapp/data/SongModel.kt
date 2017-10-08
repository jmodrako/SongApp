package com.tooploox.songapp.data

data class SongModel(
    val title: String,
    val artist: String,
    val year: String? = null,
    val thumbnailUrl: String? = null
) {
    fun artistWithYear(): String = if (year?.isNotBlank() == true) {
        "$artist ($year)"
    } else {
        artist
    }
}