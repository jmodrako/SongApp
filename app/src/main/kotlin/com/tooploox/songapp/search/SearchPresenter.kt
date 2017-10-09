package com.tooploox.songapp.search

import com.tooploox.songapp.common.BasePresenter
import com.tooploox.songapp.common.addToDisposable
import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

enum class DataSourceEnum {
    LOCAL, REMOTE, ALL
}

typealias SearchQuery = Pair<DataSourceEnum, String>

class SearchPresenter(private val dataSourcesMap: Map<DataSourceEnum, DataSource>) : BasePresenter<SearchView>() {

    private val subject = PublishSubject.create<SearchQuery>()

    init {
        subject
            .debounce(400, TimeUnit.MILLISECONDS)
            .switchMap { chooseDataSource(it.first, it.second).toObservable() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({ withView { showSearchResults(it) } }, {
                it.printStackTrace()
                withView { showSearchError() }
            })
            .addToDisposable(disposables)
    }

    fun handleSearchQuery(query: String, dataSourceEnum: DataSourceEnum) {
        if (query.isEmpty()) {
            withView { showInitialEmptyView() }
        } else {
            subject.onNext(dataSourceEnum to query)
        }
    }

    @SuppressWarnings("unchecked")
    private fun chooseDataSource(dataSourceEnum: DataSourceEnum, query: String): Single<List<SongModel>> =
        when (dataSourceEnum) {
            DataSourceEnum.ALL -> {
                val allSources = dataSourcesMap.map { it.value.search(query) }
                Single.zip(allSources, {
                    it.fold(emptyList<SongModel>(),
                        { acc, result -> acc + result as List<SongModel> })
                })
            }
            else -> {
                dataSourcesMap[dataSourceEnum]?.search(query) ?: chooseDataSource(DataSourceEnum.ALL, query)
            }
        }
}
