package com.tooploox.songapp.data.remote

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

internal data class ApiSongModel(
    @SerializedName("artistName") val artistName: String,
    @SerializedName("trackName") val trackName: String,
    @SerializedName("artworkUrl100") val artworkUrl100: String,
    @SerializedName("releaseDate") private val releaseDate: LocalDateTime? = null
) {
    val releaseYear
        get() = releaseDate?.year?.toString() ?: ""
}

internal data class ApiSearchModel(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results") val results: List<ApiSongModel>)