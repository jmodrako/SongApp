package com.tooploox.songapp.search

data class SearchState(
    var dataSource: DataSourceEnum,
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

    fun clearSort() {
        sortBy = SortBy.NONE
    }

    fun filtersDefinitions(): MutableCollection<FilterDefinition> = filtersMap.values

    fun updateDataSource(newDataSource: DataSourceEnum) {
        dataSource = newDataSource
    }

    fun updateSearchQuery(newSearchQuery: String) {
        query = newSearchQuery
    }

    companion object {
        fun newState(): SearchState = SearchState(DataSourceEnum.default())

    }
}