package com.tooploox.songapp.data.local

import com.google.gson.annotations.SerializedName

internal data class LocalSongModel(
    @SerializedName("Song Clean") val trackName: String,
    @SerializedName("ARTIST CLEAN") val artistName: String,
    @SerializedName("Release Year") val releaseYear: String,
    @SerializedName("COMBINED") val combined: String
)