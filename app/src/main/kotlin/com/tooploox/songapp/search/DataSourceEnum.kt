package com.tooploox.songapp.search

enum class DataSourceEnum(val default: Boolean = false) {
    LOCAL, REMOTE, ALL(true);

    companion object {
        fun default(): DataSourceEnum = values().first(DataSourceEnum::default)
    }
}