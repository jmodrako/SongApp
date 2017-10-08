package com.tooploox.songapp.search

import com.tooploox.songapp.common.BaseView
import com.tooploox.songapp.data.SongModel

interface SearchView : BaseView {
    fun showSearchResults(results: List<SongModel>)
    fun showSearchError()
    fun showInitialEmptyView()
}