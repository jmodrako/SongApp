package com.tooploox.songapp.data

data class SongModel(
    val title: String,
    val artist: String,
    val year: String = "",
    val thumbnailUrl: String? = null
) {
    fun artistWithYear(): String = if (year.isNotBlank()) "$artist ($year)" else artist
}