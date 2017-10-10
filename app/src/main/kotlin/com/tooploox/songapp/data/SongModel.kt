package com.tooploox.songapp.data

data class SongModel(
    val title: String,
    val artist: String,
    val year: String = "",
    val thumbnailUrl: String? = null,
    val genre: String = "",
    val album: String = ""
) {
    fun artistWithYear(): String = if (year.isNotBlank()) "$artist ($year)" else artist

    fun hasThumbnail() = thumbnailUrl?.isNotBlank() ?: false

    fun hasYear() = year.isNotBlank()

    fun hasAlbum() = album.isNotBlank()
}