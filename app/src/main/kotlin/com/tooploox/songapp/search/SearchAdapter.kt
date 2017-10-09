package com.tooploox.songapp.search

import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tooploox.songapp.R
import com.tooploox.songapp.data.SongModel
import com.tooploox.songapp.databinding.ListItemSongBinding

class SearchAdapter(context: Context) : RecyclerView.Adapter<BaseViewHolder>() {

    var originalData: List<SongModel> = listOf()
        private set

    var currentData: MutableList<SongModel> = mutableListOf()
        private set

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder =
        SongViewHolder(DataBindingUtil.inflate(layoutInflater, R.layout.list_item_song, parent, false))

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder) {
            is SongViewHolder -> holder.songBinding.model = currentData[position]
        }
    }

    override fun onViewRecycled(holder: BaseViewHolder?) {
        when (holder) {
            is SongViewHolder -> holder.clearView()
        }
    }

    override fun getItemCount() = currentData.size

    fun updateData(data: List<SongModel>) {
        currentData.clear()
        currentData.addAll(data)
    }

    fun updateOriginalData(results: List<SongModel>) {
        originalData = results
        updateData(originalData)
    }

    fun clearCurrentData() {
        currentData.clear()
    }

    fun clearAllData() {
        currentData.clear()
        originalData = emptyList()
        notifyDataSetChanged()
    }

    fun isEmpty() = currentData.isEmpty()

    fun sort(data: List<SongModel>, sortBy: SortBy): List<SongModel> {
        val predicate = when (sortBy) {
            SortBy.NONE, SortBy.TITLE -> SongModel::title
            SortBy.AUTHOR -> SongModel::artist
            SortBy.YEAR -> SongModel::year
        }

        return data.sortedBy(predicate)
    }

    fun filter(data: List<SongModel>, filterValues: MutableCollection<FilterDefinition>) =
        if (filterValues.isEmpty()) {
            data
        } else {
            data.filter { song -> filterValues.all { filter -> filter(song) } }
        }
}

sealed class BaseViewHolder(binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

class SongViewHolder(val songBinding: ListItemSongBinding) : BaseViewHolder(songBinding) {

    fun clearView() = songBinding.apply {
        listItemImage.setImageDrawable(null)
        listItemTitle.text = ""
        listItemSubtitle.text = ""
    }
}