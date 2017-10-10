package com.tooploox.songapp.search

import com.tooploox.songapp.common.BasePresenter
import com.tooploox.songapp.common.addToDisposable
import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

typealias SearchQuery = Pair<DataSource.Type, String>

class SearchPresenter(private val dataSourcesMap: Map<DataSource.Type, DataSource>) : BasePresenter<SearchView>() {

    private val subject by lazy { PublishSubject.create<SearchQuery>() }

    fun handleSearchQuery(query: String, dataSourceEnum: DataSource.Type) {
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

    override fun onAttached() {
        subject
            .debounce(400, TimeUnit.MILLISECONDS)
            .switchMap { chooseDataSource(it.first, it.second).toObservable() }
            .observeOn(AndroidSchedulers.mainThread())
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

    @SuppressWarnings("unchecked")
    private fun chooseDataSource(dataSourceType: DataSource.Type, query: String): Single<List<SongModel>> =
        when (dataSourceType) {
            DataSource.Type.ALL -> {
                val allSources = dataSourcesMap.map { it.value.search(query) }
                Single.zip(allSources, {
                    it.fold(emptyList<SongModel>(),
                        { acc, result -> acc + result as List<SongModel> })
                })
            }
            else -> {
                dataSourcesMap[dataSourceType]?.search(query) ?: chooseDataSource(DataSource.Type.ALL, query)
            }
        }
}
