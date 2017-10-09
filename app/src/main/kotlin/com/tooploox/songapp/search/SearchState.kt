package com.tooploox.songapp.search

import com.tooploox.songapp.data.DataSource

class SearchState {

    var sortBy: SortBy = SortBy.NONE
        private set

    var dataSource: DataSource.Type = DataSource.Type.default()
        private set

    private var filtersMap: MutableMap<String, FilterDefinition> = mutableMapOf()

    fun isSortActive() = sortBy != SortBy.NONE

    fun isFilterActive() = filtersMap.isNotEmpty()

    fun registerFilter(filterKey: String, filter: FilterDefinition) {
        filtersMap[filterKey] = filter
    }

    fun unregisterFilter(filterKey: String) = filtersMap.remove(filterKey)

    fun clearFilters() = filtersMap.clear()

    fun clearSort() {
        sortBy = SortBy.NONE
    }

    fun filtersDefinitions(): MutableCollection<FilterDefinition> = filtersMap.values

    fun updateDataSource(newDataSource: DataSource.Type) {
        dataSource = newDataSource
    }

    fun updateSortBy(newSortBy: SortBy) {
        sortBy = newSortBy
    }

    companion object {
        fun newState(): SearchState = SearchState()
    }
}