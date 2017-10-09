package com.tooploox.songapp.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.jakewharton.rxbinding2.widget.RxTextView
import com.tooploox.songapp.R
import com.tooploox.songapp.common.addToDisposable
import com.tooploox.songapp.common.bindContentView
import com.tooploox.songapp.common.bold
import com.tooploox.songapp.common.click
import com.tooploox.songapp.common.gone
import com.tooploox.songapp.common.hasText
import com.tooploox.songapp.common.hideKeyboard
import com.tooploox.songapp.common.retype
import com.tooploox.songapp.common.toast
import com.tooploox.songapp.common.views
import com.tooploox.songapp.common.visible
import com.tooploox.songapp.common.withVerticalManager
import com.tooploox.songapp.data.SongModel
import com.tooploox.songapp.data.local.AssetsProvider
import com.tooploox.songapp.data.local.LocalDataSource
import com.tooploox.songapp.data.remote.RemoteDataSource
import com.tooploox.songapp.databinding.ActivitySearchBinding
import com.tooploox.songapp.databinding.LayoutSpinnerWithLabelBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

enum class SortBy(val visible: Boolean = true) {
    TITLE, AUTHOR, YEAR, NONE(false)
}

typealias FilterDefinition = (SongModel) -> Boolean
typealias SongPredicate = (SongModel, String) -> Boolean

data class AppState(
    var dataSource: DataSourceEnum = DataSourceEnum.REMOTE,
    var query: String = "",
    var sortBy: SortBy = SortBy.NONE,
    private var filtersMap: MutableMap<String, FilterDefinition> = mutableMapOf()) {

    fun isSortActive() = sortBy != SortBy.NONE

    fun isFilterActive() = filtersMap.isNotEmpty()

    fun registerFilter(filterKey: String, filter: FilterDefinition) {
        filtersMap[filterKey] = filter
    }

    fun unregisterFilter(filterKey: String) = filtersMap.remove(filterKey)

    fun clearFilters() = filtersMap.clear()

    fun filtersDefinitions(): MutableCollection<FilterDefinition> = filtersMap.values
}

class SearchActivity : AppCompatActivity(), SearchView {

    private lateinit var presenter: SearchPresenter
    private lateinit var binding: ActivitySearchBinding

    private val listAdapter by lazy { SearchAdapter(this) }

    private val settingsBottomSheet by lazy { BottomSheetBehavior.from(binding.bottomSheetSettings.bottomSheet) }
    private val filterBottomSheet by lazy { BottomSheetBehavior.from(binding.bottomSheetFilter.bottomSheet) }
    private val sortBottomSheet by lazy { BottomSheetBehavior.from(binding.bottomSheetSort.bottomSheet) }
    private val allBottomSheets by lazy { listOf(settingsBottomSheet, filterBottomSheet, sortBottomSheet) }

    private val compositeDisposable = CompositeDisposable()
    private var appState = AppState()

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            if (listAdapter.isEmpty()) {
                if (binding.searchInput.hasText) showResultCountLabel(0)

                showEmptyLayoutWithMessage(getString(R.string.cant_find_song))
            } else {
                showResultCountLabel(listAdapter.itemCount)

                hideEmptyLayout()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_search)
        presenter = createSearchPresenter()

        setupInitialBottomSheet(filterBottomSheet, binding.filter)
        setupInitialBottomSheet(sortBottomSheet, binding.sort)
        setupInitialBottomSheet(settingsBottomSheet, binding.settings)

        setupSorting()
        setupFiltering()
        setupLoadingIndicator()

        setupSearchInput()
        setupSearchDataSourceSpinner()
        setupSearchResultsList()
    }

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
    }

    override fun onStop() {
        listAdapter.unregisterAdapterDataObserver(adapterDataObserver)
        presenter.detach()
        compositeDisposable.clear()
        super.onStop()
    }

    override fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    override fun showInitialEmptyView() {
        listAdapter.clearData()
        listAdapter.notifyDataSetChanged()
        showEmptyLayoutWithMessage(getString(R.string.type_query_to_find_song))
    }

    override fun showSearchResults(results: List<SongModel>) {
        listAdapter.clearData()

        if (results.isEmpty()) {
            showEmptyLayoutWithMessage(getString(R.string.cant_find_song))
        } else {
            createFilters(results)
            hideEmptyLayout()

            listAdapter.updateOriginalData(results)
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun showSearchError() {
        toast(getString(R.string.something_went_wrong), Toast.LENGTH_SHORT)
    }

    override fun onBackPressed() {
        val isAnySheetOpened = allBottomSheets.any { it.state == BottomSheetBehavior.STATE_EXPANDED }

        if (isAnySheetOpened) {
            hideAllSheets()
        } else {
            super.onBackPressed()
        }
    }

    private fun showResultCountLabel(count: Int) {
        binding.searchResultCount.text = getString(R.string.search_result_count, count)
    }

    private fun setupLoadingIndicator() {
        binding.swipeRefresh.apply {
            isRefreshing = false
            isEnabled = false
        }
    }

    private fun setupSorting() {
        SortBy.values()
            .filter(SortBy::visible)
            .forEach {
                val radioBtn = RadioButton(this).apply {
                    text = it.name.toLowerCase().capitalize()
                    click {
                        appState.sortBy = it
                        refreshListFromSortBy()
                    }
                }

                binding.bottomSheetSort.sortRadioGroup.addView(radioBtn)
            }

        binding.bottomSheetSort.sortClear.click {
            binding.bottomSheetSort.sortRadioGroup.views
                .filterIsInstance(RadioButton::class.java)
                .forEach { it.isChecked = false }

            appState.sortBy = SortBy.NONE
            refreshListFromSortBy()

            hideBottomSheet(sortBottomSheet)
        }

        binding.bottomSheetSort.sortClose.click { hideBottomSheet(sortBottomSheet) }
    }

    private fun createFilters(data: List<SongModel>) {
        prepareFilterSpinner("artist", { song, clickedFilter -> song.artist == clickedFilter },
            data.groupBy(SongModel::artist), binding.bottomSheetFilter.filterArtist)

        prepareFilterSpinner("genre", { song, clickedFilter -> song.genre == clickedFilter },
            data.groupBy(SongModel::genre), binding.bottomSheetFilter.filterGenre)
    }

    private fun prepareFilterSpinner(
        filterKey: String,
        songPredicate: SongPredicate,
        dataMap: Map<String, List<SongModel>>,
        spinnerBinding: LayoutSpinnerWithLabelBinding) {

        val filteredMap = dataMap.filter { it.key.isNotBlank() }

        if (filteredMap.isNotEmpty()) {
            val spinnerData = filteredMap.map { "${it.key} (${it.value.size})" }.sorted()
            val finalData = listOf("${getString(R.string.all)} (${spinnerData.size})") + spinnerData

            prepareSpinner(finalData, spinnerBinding.spinner,
                clickCallback = { clickedFilter ->
                    appState.registerFilter(filterKey, { song -> songPredicate(song, clickedFilter) })
                    refreshListFromFilterBy()
                },
                defaultCallback = {
                    appState.unregisterFilter(filterKey)
                    refreshListFromFilterBy()
                })

            spinnerBinding.root.visible()
        } else {
            spinnerBinding.root.gone()
        }
    }

    private fun setupFiltering() {
        binding.bottomSheetFilter.filterClose.click {
            hideBottomSheet(filterBottomSheet)
        }

        binding.bottomSheetFilter.filterClear.click {
            appState.clearFilters()

            binding.bottomSheetFilter.filterArtist.spinner.setSelection(0)
            binding.bottomSheetFilter.filterGenre.spinner.setSelection(0)

            hideBottomSheet(filterBottomSheet)
        }
    }

    private fun prepareSpinner(
        data: List<String>, spinner: Spinner,
        clickCallback: (String) -> Unit,
        defaultCallback: () -> Unit) {

        val adapter = ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1, android.R.id.text1).apply {
            addAll(data)
        }

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    defaultCallback()
                } else {
                    val clickedLabel = data[position]
                    val clickedItem = clickedLabel.take(clickedLabel.lastIndexOf(" "))
                    clickCallback(clickedItem)
                }
            }
        }
    }

    private fun refreshListFromFilterBy() {
        val filterValues = appState.filtersDefinitions()
        val filteredData = presenter.filter(listAdapter.originalData, filterValues)
        val finalData = if (appState.isSortActive()) presenter.sort(filteredData, appState.sortBy) else filteredData

        listAdapter.updateData(finalData)
        listAdapter.notifyDataSetChanged()

        activateLabel(binding.filter, filterValues.isNotEmpty())
    }

    private fun refreshListFromSortBy() {
        val toSort = listAdapter.run { if (appState.isFilterActive()) currentData else originalData }
        val sortedData = presenter.sort(toSort, appState.sortBy)

        listAdapter.updateData(sortedData)
        listAdapter.notifyDataSetChanged()

        activateLabel(binding.sort, appState.sortBy != SortBy.NONE)
    }

    private fun activateLabel(label: TextView, enable: Boolean) {
        label.apply {
            setTextColor(ContextCompat.getColor(context, if (enable) R.color.red else R.color.primary_text))
            bold(enable)
        }
    }

    private fun createSearchPresenter() =
        SearchPresenter(mapOf(
            DataSourceEnum.LOCAL to LocalDataSource(AssetsProvider(this)),
            DataSourceEnum.REMOTE to RemoteDataSource()))

    private fun hideEmptyLayout() {
        binding.recyclerView.visible()
        binding.emptyLayout.root.gone()
    }

    private fun showEmptyLayoutWithMessage(newMessage: String) {
        binding.recyclerView.gone()
        binding.emptyLayout.apply {
            message = newMessage
            root.visible()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchResultsList() {
        listAdapter.registerAdapterDataObserver(adapterDataObserver)

        binding.recyclerView.apply {
            withVerticalManager()
            adapter = listAdapter
        }

        binding.recyclerView.setOnTouchListener { _, event ->
            val input = binding.searchInput
            if (event.action == MotionEvent.ACTION_MOVE && input.hasFocus()) {
                input.clearFocus()
                hideKeyboard(binding.searchInput)
            }
            false
        }
    }

    private fun setupInitialBottomSheet(bottomSheet: BottomSheetBehavior<*>, sheetTrigger: View) {
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN

        sheetTrigger.click {
            bottomSheet.state =
                if (bottomSheet.state != BottomSheetBehavior.STATE_HIDDEN) BottomSheetBehavior.STATE_HIDDEN
                else BottomSheetBehavior.STATE_EXPANDED

            hideAllSheets(bottomSheet)
        }
    }

    private fun hideAllSheets(bottomSheetToStayOpen: BottomSheetBehavior<*>? = null) {
        allBottomSheets
            .filter { it != bottomSheetToStayOpen }
            .forEach { it.state = BottomSheetBehavior.STATE_HIDDEN }
    }

    private fun hideBottomSheet(bottomSheet: BottomSheetBehavior<*>) {
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun setupSearchDataSourceSpinner() {
        val sources = DataSourceEnum.values()
        val items = sources.map { it.name.toLowerCase().capitalize() }
        val adapter = ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1, android.R.id.text1).apply {
            addAll(items)
        }

        binding.bottomSheetSettings.dataSourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                appState.dataSource = sources[position]
                hideBottomSheet(settingsBottomSheet)

                binding.searchInput.retype()
            }
        }
        binding.bottomSheetSettings.dataSourceSpinner.adapter = adapter
    }

    private fun setupSearchInput() {
        binding.searchInput.apply {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        hideKeyboard(this)
                        false
                    }
                    else -> false
                }
            }
        }

        binding.clearSearchInput.click {
            binding.searchResultCount.text = ""
            binding.searchInput.setText("")
            binding.searchInput.requestFocus()
        }

        RxTextView.textChanges(binding.searchInput)
            .skip(1)
            .map(CharSequence::toString)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                appState.query = it

                if (it.isEmpty()) binding.clearSearchInput.gone()
                else {
                    binding.clearSearchInput.visible()
                }
            }
            .subscribe({ presenter.handleSearchQuery(it, appState.dataSource) })
            .addToDisposable(compositeDisposable)
    }
}
