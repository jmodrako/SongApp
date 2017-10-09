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

typealias SearchQuery = Pair<DataSourceEnum, String>

class SearchPresenter(private val dataSourcesMap: Map<DataSourceEnum, DataSource>) : BasePresenter<SearchView>() {

    private val subject = PublishSubject.create<SearchQuery>()

    init {
        subject
            .debounce(400, TimeUnit.MILLISECONDS)
            .switchMap { chooseDataSource(it.first, it.second).toObservable() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                withView {
                    showLoading(false)
                    showSearchResults(it)
                }
            }, {
                withView {
                    showLoading(false)
                    showSearchError()
                }
            })
            .addToDisposable(disposables)
    }

    fun handleSearchQuery(query: String, dataSourceEnum: DataSourceEnum) {
        if (query.isEmpty()) {
            withView {
                showLoading(false)
                showInitialEmptyView()
            }
        } else {
            withView { showLoading(true) }
            subject.onNext(dataSourceEnum to query)
        }
    }

    fun sort(originalData: List<SongModel>, sortBy: SortBy): List<SongModel> {
        val predicate = when (sortBy) {
            SortBy.NONE, SortBy.TITLE -> SongModel::title
            SortBy.AUTHOR -> SongModel::artist
            SortBy.YEAR -> SongModel::year
        }

        return originalData.sortedBy(predicate)
    }

    fun filter(originalData: List<SongModel>, filterValues: MutableCollection<FilterDefinition>) =
        if (filterValues.isEmpty()) {
            originalData
        } else {
            originalData.filter { song -> filterValues.all { filter -> filter(song) } }
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
