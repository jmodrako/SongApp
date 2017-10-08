package com.tooploox.songapp.search

import com.tooploox.songapp.common.BasePresenter
import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

enum class DataSourceEnum {
    LOCAL, REMOTE, BOTH
}

class SearchPresenter(
    val localDataSource: DataSource,
    val remoteDataSource: DataSource) : BasePresenter<SearchView>() {

    fun handleSearchQuery(query: String, dataSourceEnum: DataSourceEnum) {
        if (query.isEmpty()) {
            withView { showInitialEmptyView() }
        } else {
            chooseDataSource(dataSourceEnum, query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ withView { showSearchResults(it) } },
                    { withView { showSearchError() } })
        }
    }

    private fun chooseDataSource(dataSourceEnum: DataSourceEnum, query: String): Single<List<SongModel>> =
        when (dataSourceEnum) {
            DataSourceEnum.LOCAL -> localDataSource.search(query)
            DataSourceEnum.REMOTE -> remoteDataSource.search(query)
            DataSourceEnum.BOTH -> Single.zip(localDataSource.search(query), remoteDataSource.search(query),
                BiFunction { localData, remoteData -> localData.plus(remoteData) })
        }
}
