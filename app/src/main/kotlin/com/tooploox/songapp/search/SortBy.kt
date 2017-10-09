package com.tooploox.songapp.search

enum class SortBy(val visible: Boolean = true) {
    TITLE, AUTHOR, YEAR, NONE(false)
}