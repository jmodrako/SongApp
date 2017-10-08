package com.tooploox.songapp.search

import com.tooploox.songapp.common.BasePresenter
import com.tooploox.songapp.common.addToDisposable
import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

enum class DataSourceEnum {
    LOCAL, REMOTE, BOTH
}

typealias SearchQuery = Pair<DataSourceEnum, String>

class SearchPresenter(
    private val localDataSource: DataSource,
    private val remoteDataSource: DataSource) : BasePresenter<SearchView>() {

    private val subject = PublishSubject.create<SearchQuery>()

    init {
        subject
            .debounce(400, TimeUnit.MILLISECONDS)
            .switchMap { chooseDataSource(it.first, it.second).toObservable() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({ withView { showSearchResults(it) } }, { withView { showSearchError() } })
            .addToDisposable(disposables)
    }

    fun handleSearchQuery(query: String, dataSourceEnum: DataSourceEnum) {
        if (query.isEmpty()) {
            withView { showInitialEmptyView() }
        } else {
            subject.onNext(dataSourceEnum to query)
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
