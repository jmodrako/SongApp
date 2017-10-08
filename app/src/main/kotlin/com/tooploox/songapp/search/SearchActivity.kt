package com.tooploox.songapp.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.jakewharton.rxbinding2.widget.RxTextView
import com.tooploox.songapp.R
import com.tooploox.songapp.common.addToDisposable
import com.tooploox.songapp.common.bindContentView
import com.tooploox.songapp.common.click
import com.tooploox.songapp.common.gone
import com.tooploox.songapp.common.hideKeyboard
import com.tooploox.songapp.common.retype
import com.tooploox.songapp.common.toast
import com.tooploox.songapp.common.visible
import com.tooploox.songapp.common.withVerticalManager
import com.tooploox.songapp.data.SongModel
import com.tooploox.songapp.data.local.AssetsProvider
import com.tooploox.songapp.data.local.LocalDataSource
import com.tooploox.songapp.data.remote.RemoteDataSource
import com.tooploox.songapp.databinding.ActivitySearchBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SearchActivity : AppCompatActivity(), SearchView {

    private lateinit var presenter: SearchPresenter
    private lateinit var binding: ActivitySearchBinding

    private val listAdapter by lazy { SearchAdapter(this) }

    private val compositeDisposable = CompositeDisposable()
    private var dataSource = DataSourceEnum.REMOTE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_search)
        presenter = SearchPresenter(LocalDataSource(AssetsProvider(this)), RemoteDataSource())

        setupSettings()
        setupSearchInput()
        setupSearchDataSourceSpinner()
        setupSearchResultsList()
    }

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
    }

    override fun onStop() {
        presenter.detach()
        compositeDisposable.clear()
        super.onStop()
    }

    override fun showInitialEmptyView() {
        listAdapter.clearData()
        showEmptyLayoutWithMessage(getString(R.string.type_query_to_find_song))
    }

    override fun showSearchResults(results: List<SongModel>) {
        listAdapter.clearData()

        if (results.isEmpty()) {
            showEmptyLayoutWithMessage(getString(R.string.cant_find_song))
        } else {
            hideEmptyLayout()

            listAdapter.updateData(results)
            listAdapter.notifyDataSetChanged()
        }
    }

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

    override fun showSearchError() {
        toast(getString(R.string.something_went_wrong), Toast.LENGTH_SHORT)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchResultsList() {
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

    private fun setupSettings() {
        val bottomBeh = BottomSheetBehavior.from(binding.bottomSheet)
        bottomBeh.state = BottomSheetBehavior.STATE_HIDDEN

        binding.settings.click {
            bottomBeh.state =
                if (bottomBeh.state != BottomSheetBehavior.STATE_HIDDEN) BottomSheetBehavior.STATE_HIDDEN
                else BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun hideSettings() {
        BottomSheetBehavior.from(binding.bottomSheet).state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun setupSearchDataSourceSpinner() {
        val sources = DataSourceEnum.values()
        val items = sources.map { it.name.toLowerCase().capitalize() }
        val adapter = ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1, android.R.id.text1).apply {
            addAll(items)
        }

        binding.dataSourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                dataSource = sources[position]
                hideSettings()

                binding.searchInput.retype()
            }
        }
        binding.dataSourceSpinner.adapter = adapter
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
            binding.searchInput.setText("")
            binding.searchInput.requestFocus()
        }

        RxTextView.textChanges(binding.searchInput)
            .skip(1)
            .debounce(350, TimeUnit.MILLISECONDS)
            .map(CharSequence::toString)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isEmpty()) binding.clearSearchInput.gone()
                else binding.clearSearchInput.visible()

                presenter.handleSearchQuery(it, dataSource)
            })
            .addToDisposable(compositeDisposable)
    }
}
