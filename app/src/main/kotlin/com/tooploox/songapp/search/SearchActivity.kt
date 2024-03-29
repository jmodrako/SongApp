package com.tooploox.songapp.search

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatRadioButton
import android.support.v7.widget.RecyclerView
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.jakewharton.rxbinding2.widget.RxTextView
import com.tooploox.songapp.R
import com.tooploox.songapp.common.addToDisposable
import com.tooploox.songapp.common.bindContentView
import com.tooploox.songapp.common.bold
import com.tooploox.songapp.common.clearAdapter
import com.tooploox.songapp.common.click
import com.tooploox.songapp.common.firstCheckedButton
import com.tooploox.songapp.common.gone
import com.tooploox.songapp.common.hasText
import com.tooploox.songapp.common.hideBottomSheet
import com.tooploox.songapp.common.hideBottomSheetsIfNeeded
import com.tooploox.songapp.common.hideKeyboard
import com.tooploox.songapp.common.prepareSpinnerUtil
import com.tooploox.songapp.common.retype
import com.tooploox.songapp.common.safeUnregisterAdapterDataObserver
import com.tooploox.songapp.common.setupInitialBottomSheet
import com.tooploox.songapp.common.toast
import com.tooploox.songapp.common.views
import com.tooploox.songapp.common.visible
import com.tooploox.songapp.common.withVerticalManager
import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import com.tooploox.songapp.data.local.AssetsProvider
import com.tooploox.songapp.data.local.LocalDataSource
import com.tooploox.songapp.data.remote.RemoteDataSource
import com.tooploox.songapp.databinding.ActivitySearchBinding
import com.tooploox.songapp.databinding.LayoutSpinnerWithLabelBinding
import com.tooploox.songapp.details.DetailsDialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.reflect.KProperty1

typealias FilterDefinition = (SongModel) -> Boolean
typealias SongPredicate = (SongModel, String) -> Boolean

class SearchActivity : AppCompatActivity(), SearchView {

    private lateinit var presenter: SearchPresenter
    private lateinit var binding: ActivitySearchBinding

    private val listAdapter by lazy { SearchAdapter(this) }

    private val settingsBottomSheet by lazy { BottomSheetBehavior.from(binding.bottomSheetSettings.bottomSheet) }
    private val filterBottomSheet by lazy { BottomSheetBehavior.from(binding.bottomSheetFilter.bottomSheet) }
    private val sortBottomSheet by lazy { BottomSheetBehavior.from(binding.bottomSheetSort.bottomSheet) }
    private val allBottomSheets by lazy { listOf(settingsBottomSheet, filterBottomSheet, sortBottomSheet) }

    private val compositeDisposable = CompositeDisposable()
    private var searchState = SearchState.newState()

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

        setupInitialBottomSheet(allBottomSheets, filterBottomSheet, binding.filter)
        setupInitialBottomSheet(allBottomSheets, sortBottomSheet, binding.sort)
        setupInitialBottomSheet(allBottomSheets, settingsBottomSheet, binding.settings)

        setupSorting()
        setupFiltering()
        setupLoadingIndicator()

        setupSearchInput()
        setupDataSourceSettings()
        setupSearchResultsList()
    }

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
        listAdapter.registerAdapterDataObserver(adapterDataObserver)
        rxifySearchInput()
    }

    override fun onStop() {
        listAdapter.safeUnregisterAdapterDataObserver(adapterDataObserver)
        presenter.detach()
        compositeDisposable.clear()
        super.onStop()
    }

    override fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    override fun showInitialEmptyView() {
        listAdapter.clearCurrentData()
        listAdapter.notifyDataSetChanged()
        showEmptyLayoutWithMessage(getString(R.string.type_query_to_find_song))
    }

    override fun showSearchResults(results: List<SongModel>) {
        listAdapter.clearCurrentData()

        if (results.isNotEmpty()) {
            createFilters(results)

            val sortedData = listAdapter.sort(results, chooseSortPredicate())
            listAdapter.updateOriginalData(sortedData)
        }

        listAdapter.notifyDataSetChanged()
    }

    private fun chooseSortPredicate(): KProperty1<SongModel, String> =
        when (searchState.sortBy) {
            SortBy.NONE, SortBy.TITLE -> SongModel::title
            SortBy.AUTHOR -> SongModel::artist
            SortBy.YEAR -> SongModel::year
        }

    override fun showSearchError() {
        toast(getString(R.string.something_went_wrong), Toast.LENGTH_SHORT)
    }

    override fun onBackPressed() {
        if (!hideBottomSheetsIfNeeded(allBottomSheets)) {
            super.onBackPressed()
        }
    }

    private fun showResultCountLabel(count: Int) {
        binding.searchResultCount.text = when (count) {
            0 -> getString(R.string.no_search_result)
            else -> getString(R.string.search_result_count, count)
        }
    }

    private fun setupLoadingIndicator() {
        binding.swipeRefresh.run {
            isRefreshing = false
            isEnabled = false
        }
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

            prepareSpinnerUtil(this, finalData, spinnerBinding.spinner,
                clickCallback = { clickedFilter ->
                    searchState.registerFilter(filterKey, { song -> songPredicate(song, clickedFilter) })
                    refreshListFromFilterBy()
                },
                defaultCallback = {
                    searchState.unregisterFilter(filterKey)
                    refreshListFromFilterBy()
                })

            spinnerBinding.root.visible()
        } else {
            spinnerBinding.root.gone()
        }
    }

    private fun setupSorting() {
        val radioGroup = binding.bottomSheetSort.sortRadioGroup
        radioGroup.setOnCheckedChangeListener({ _, _ ->
            val firstCheckedButton = radioGroup.firstCheckedButton()
            val checkedSortBy = firstCheckedButton.tag as SortBy

            searchState.updateSortBy(checkedSortBy)
            hideBottomSheet(sortBottomSheet)
            refreshListFromSortBy()
        })

        SortBy.values()
            .filter(SortBy::visible)
            .forEach {
                val radioBtn = AppCompatRadioButton(this).apply {
                    text = it.name.toLowerCase().capitalize()
                    val color = ContextCompat.getColor(context, R.color.primary_text_white)
                    setTextColor(color)
                    buttonTintList = ColorStateList.valueOf(color)


                    if (it.default) {
                        searchState.updateSortBy(it)
                    }

                    tag = it
                }

                radioGroup.addView(radioBtn)
            }

        selectDefaultSortBy(radioGroup)

        binding.bottomSheetSort.sortClear.click {
            clearSort()
            hideBottomSheet(sortBottomSheet)
        }

        binding.bottomSheetSort.sortClose.click {
            hideBottomSheet(sortBottomSheet)
        }
    }

    private fun selectDefaultSortBy(radioGroup: RadioGroup) {
        (radioGroup.views.first { (it.tag as SortBy).default } as RadioButton).isChecked = true
    }

    private fun setupFiltering() {
        binding.bottomSheetFilter.filterClose.click {
            hideBottomSheet(filterBottomSheet)
        }

        binding.bottomSheetFilter.filterClear.click {
            searchState.clearFilters()

            binding.bottomSheetFilter.run {
                filterArtist.spinner.setSelection(0)
                filterGenre.spinner.setSelection(0)
            }

            hideBottomSheet(filterBottomSheet)
        }
    }

    private fun refreshListFromFilterBy() {
        val filterValues = searchState.filtersDefinitions()
        val filteredData = listAdapter.filter(listAdapter.originalData, filterValues)
        val finalData = if (searchState.isSortActive()) listAdapter.sort(filteredData, chooseSortPredicate()) else filteredData

        listAdapter.updateData(finalData)
        listAdapter.notifyDataSetChanged()

        activateLabel(binding.filter, filterValues.isNotEmpty())
    }

    private fun refreshListFromSortBy() {
        val toSort = listAdapter.run { if (searchState.isFilterActive()) currentData else originalData }
        val sortedData = listAdapter.sort(toSort, chooseSortPredicate())

        listAdapter.updateData(sortedData)
        listAdapter.notifyDataSetChanged()

        activateLabel(binding.sort, searchState.isNotDefaultSort())
    }

    private fun clearFilters() {
        binding.bottomSheetFilter.run {
            filterArtist.spinner.clearAdapter()
            filterGenre.spinner.clearAdapter()

            filterArtist.root.gone()
            filterGenre.root.gone()
        }
        searchState.clearFilters()
        refreshListFromFilterBy()
    }

    private fun clearSort() {
        selectDefaultSortBy(binding.bottomSheetSort.sortRadioGroup)

        searchState.clearSort()
        refreshListFromSortBy()
    }

    private fun activateLabel(label: TextView, enable: Boolean) =
        label.run {
            setTextColor(ContextCompat.getColor(context, if (enable) R.color.accent else R.color.primary_text_white))
            bold(enable)
        }

    /**
     * Could be done via Dagger 2.
     */
    private fun createSearchPresenter() =
        SearchPresenter(mapOf(
            DataSource.Type.REMOTE to RemoteDataSource(),
            DataSource.Type.LOCAL to LocalDataSource(AssetsProvider(this))))

    private fun hideEmptyLayout() {
        binding.recyclerView.visible()
        binding.emptyLayout.root.gone()
    }

    private fun showEmptyLayoutWithMessage(newMessage: String) {
        binding.recyclerView.gone()
        binding.emptyLayout.run {
            message = newMessage
            root.visible()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchResultsList() {
        listAdapter.onItemClickListener = {
            val detailsDialog = DetailsDialogFragment()
            detailsDialog.withModel(it)
            detailsDialog.show(supportFragmentManager, "details-fragment")
        }

        binding.recyclerView.run {
            withVerticalManager()
            adapter = listAdapter
        }

        binding.recyclerView.onDownClickListener = {
            val input = binding.searchInput
            if (input.hasFocus()) {
                input.clearFocus()
                hideKeyboard(binding.searchInput)
            }

            hideBottomSheetsIfNeeded(allBottomSheets)
        }
    }

    /**
     * There is no easy way to create RadioGroup with dynamic buttons in it...
     */
    private fun setupDataSourceSettings() {
        val radioGroup = binding.bottomSheetSettings.dataSourceRadioGroup

        radioGroup.setOnCheckedChangeListener({ _, _ ->
            val firstCheckedButton = radioGroup.firstCheckedButton()
            val checkedDataSource = firstCheckedButton.tag as DataSource.Type

            searchState.updateDataSource(checkedDataSource)
            hideBottomSheet(settingsBottomSheet)
            binding.searchInput.retype()
        })

        DataSource.Type.values()
            .forEach {
                val radioBtn = AppCompatRadioButton(this).apply {
                    text = it.name.toLowerCase().capitalize()
                    val color = ContextCompat.getColor(context, R.color.primary_text_white)
                    setTextColor(color)
                    buttonTintList = ColorStateList.valueOf(color)

                    if (it.default) {
                        searchState.updateDataSource(it)
                    }

                    tag = it
                }

                radioGroup.addView(radioBtn)
            }

        (radioGroup.views.first { (it.tag as DataSource.Type).default } as RadioButton).isChecked = true
    }

    private fun setupSearchInput() {
        binding.searchInput.run {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        hideKeyboard(this)
                        false
                    }
                    else -> false
                }
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) hideBottomSheetsIfNeeded(allBottomSheets)
            }
        }

        binding.clearSearchInput.click {
            listAdapter.clearAllData()

            clearFilters()
            clearSort()

            binding.searchResultCount.text = ""
            binding.searchInput.setText("")
            binding.searchInput.requestFocus()
        }

        rxifySearchInput()
    }

    private fun rxifySearchInput() {
        RxTextView.textChanges(binding.searchInput)
            .skip(1)
            .map(CharSequence::toString)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                if (it.isEmpty()) binding.clearSearchInput.gone()
                else binding.clearSearchInput.visible()

            }
            .subscribe({ presenter.handleSearchQuery(it, searchState.dataSource) })
            .addToDisposable(compositeDisposable)
    }
}
