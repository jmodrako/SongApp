package com.tooploox.songapp.search

enum class SortBy(
    val visible: Boolean = true,
    val default: Boolean = false) {

    TITLE(default = true), AUTHOR, YEAR, NONE(visible = false);

    companion object {
        fun default(): SortBy = SortBy.values().first(SortBy::default)
    }
}