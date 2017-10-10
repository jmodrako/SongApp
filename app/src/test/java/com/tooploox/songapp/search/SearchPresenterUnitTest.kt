package com.tooploox.songapp.search

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.tooploox.songapp.SchedulerProvider
import com.tooploox.songapp.data.DataSource
import com.tooploox.songapp.data.SongModel
import io.reactivex.Single
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.include
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(JUnitPlatform::class)
class SearchPresenterUnitTest : Spek({

    include(SchedulerProvider.rxSchedulersOverrideSpek)

    describe("asset details presenter interactions") {
        val localDataSource = prepareDataSource()
        val remoteDataSource = prepareDataSource()
        val presenter = SearchPresenter(mapOf(
            DataSource.Type.LOCAL to localDataSource,
            DataSource.Type.REMOTE to remoteDataSource))

        val mockedView: SearchView = mock(SearchView::class.java)

        beforeEachTest {
            clearInvocations(mockedView)
            presenter.detach()
        }

        context("attaching view") {
            it("should have view implementation") {
                presenter.attach(mockedView)

                assertNotNull(presenter.view)
                assertEquals(presenter.view, mockedView)
            }
        }

        context("attaching null view") {
            it("should throw exception, because argument type is not nullable") {
                try {
                    presenter.attach(null!!)
                } catch (exc: Exception) {
                    assertTrue(exc is KotlinNullPointerException)
                }
            }
        }

        context("detaching view") {
            it("shouldn't have view implementation") {
                presenter.detach()

                assertNull(presenter.view)
            }
        }

        context("empty query") {
            it("dismiss loading and show initial empty view") {
                presenter.attach(mockedView)
                presenter.handleSearchQuery("", DataSource.Type.ALL)

                verify(mockedView).showLoading(eq(false))
                verify(mockedView).showInitialEmptyView()
            }
        }

        context("non empty query") {
            it("show loading") {
                whenever(localDataSource.search(any())).thenReturn(Single.just(emptyList()))
                whenever(remoteDataSource.search(any())).thenReturn(Single.just(emptyList()))

                presenter.attach(mockedView)
                presenter.handleSearchQuery("some_query", DataSource.Type.ALL)

                SchedulerProvider.testScheduler.triggerActions()

                verify(mockedView).showLoading(eq(true))
            }

            it("pass data to view from local source") {
                val dataLocal = listOf(SongModel("title", "artist", "year", "url", "genre", "album"))
                val verification = {
                    verify(mockedView).showLoading(eq(true))
                    verify(mockedView).showSearchResults(argThat { size == 1 && any { it.title == "title" } })
                }

                checkLoadedDataFromSpecificSource(DataSource.Type.LOCAL, localDataSource, presenter,
                    mockedView, Single.just(dataLocal), verification)
            }

            it("pass data to view from remote source") {
                val dataRemote = listOf(SongModel("titleR", "artistR", "yearR", "urlR", "genreR", "albumR"))
                val verification = {
                    verify(mockedView).showLoading(eq(true))
                    verify(mockedView).showSearchResults(argThat { size == 1 && any { it.title == "titleR" } })
                }

                checkLoadedDataFromSpecificSource(DataSource.Type.REMOTE, remoteDataSource, presenter,
                    mockedView, Single.just(dataRemote), verification)
            }

            it("pass data to view from all sources") {
                val dataLocal = listOf(SongModel("title", "artist", "year", "url", "genre", "album"))
                val dataRemote = listOf(SongModel("titleR", "artistR", "yearR", "urlR", "genreR", "albumR"))

                whenever(localDataSource.search(any())).thenReturn(Single.just(dataLocal))
                whenever(remoteDataSource.search(any())).thenReturn(Single.just(dataRemote))

                presenter.attach(mockedView)
                presenter.handleSearchQuery("some_query", DataSource.Type.ALL)

                SchedulerProvider.testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS)

                verify(mockedView).showLoading(eq(true))
                verify(mockedView).showSearchResults(argThat { size == 2 })
            }

            it("show error when something went wrong with data loading") {
                val verification = {
                    verify(mockedView).showLoading(eq(false))
                    verify(mockedView).showSearchError()
                }

                checkLoadedDataFromSpecificSource(DataSource.Type.REMOTE, remoteDataSource, presenter,
                    mockedView, Single.error(IOException("No internet.")), verification)
            }
        }
    }

})

private fun checkLoadedDataFromSpecificSource(
    dataSourceType: DataSource.Type,
    dataSource: DataSource,
    presenter: SearchPresenter,
    mockedView: SearchView,
    resultSingle: Single<List<SongModel>>,
    verificationCode: () -> Unit) {

    whenever(dataSource.search(any())).thenReturn(resultSingle)

    presenter.attach(mockedView)
    presenter.handleSearchQuery("some_query", dataSourceType)

    SchedulerProvider.testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS)

    verificationCode()
}

private fun prepareDataSource(): DataSource = mock(DataSource::class.java)