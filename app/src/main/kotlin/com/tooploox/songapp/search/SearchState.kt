package com.tooploox.songapp.search

class SearchState {

    var sortBy: SortBy = SortBy.NONE
        private set

    var dataSource: DataSourceEnum = DataSourceEnum.default()
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

    fun updateDataSource(newDataSource: DataSourceEnum) {
        dataSource = newDataSource
    }

    fun updateSortBy(newSortBy: SortBy) {
        sortBy = newSortBy
    }

    companion object {
        fun newState(): SearchState = SearchState()
    }
}