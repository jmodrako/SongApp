package com.tooploox.songapp.data.local

import com.tooploox.songapp.SchedulerProvider
import io.reactivex.Single
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.include
import org.junit.Assert.assertTrue
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(JUnitPlatform::class)
class LocalDataSourceUnitTest : Spek({

    include(SchedulerProvider.rxSchedulersOverrideSpek)

    describe("empty search") {
        val mockAssetsProvider: AssetsProvider = mock(AssetsProvider::class.java)
        val sut = LocalDataSource(mockAssetsProvider)

        it("should give empty list") {
            val testObserver = sut.search("").test()
            SchedulerProvider.testScheduler.triggerActions()

            val callResult = testObserver.values().firstOrNull()
            if (callResult != null) {
                assertTrue(callResult.isEmpty())
            } else {
                error("Can't get call result during tests!")
            }
        }
    }

    describe("non empty search") {
        val mockAssetsProvider: AssetsProvider = mock(AssetsProvider::class.java)
        val mockList = listOf(LocalSongModel("track1", "artist1", "year1", "track1artist1year1"),
            LocalSongModel("track2", "artist2", "year2", "track2artist2year2"),
            LocalSongModel("track3", "artist3", "year3", "track3artist3year3"))
        val mockData = Single.fromCallable { mockList }

        `when`(mockAssetsProvider.localSongs()).thenReturn(mockData)

        val sut = LocalDataSource(mockAssetsProvider)

        it("should give one item for specific query") {
            val testObserver = sut.search("1").test()
            SchedulerProvider.testScheduler.triggerActions()

            val callResult = testObserver.values().firstOrNull()
            if (callResult != null) {
                assertTrue(callResult.size == 1)
            } else {
                error("Can't get call result during tests!")
            }
        }

        it("should give few items for specific query") {
            val testObserver = sut.search("artist").test()
            SchedulerProvider.testScheduler.triggerActions()

            val callResult = testObserver.values().firstOrNull()
            if (callResult != null) {
                assertTrue(callResult.size == mockList.size)
            } else {
                error("Can't get call result during tests!")
            }
        }
    }
})
