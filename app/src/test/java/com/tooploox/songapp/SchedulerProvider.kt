package com.tooploox.songapp

import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.jetbrains.spek.api.Spek

class SchedulerProvider {
    companion object {
        val testScheduler = TestScheduler()
        val rxSchedulersOverrideSpek = object : Spek({
            beforeGroup {
                // We have to mock real AndroidSchedulers.mainThread()...
                RxAndroidPlugins.setInitMainThreadSchedulerHandler({ SchedulerProvider.testScheduler })

                // ... and some from RxJava world.
                RxJavaPlugins.setInitIoSchedulerHandler({ SchedulerProvider.testScheduler })
                RxJavaPlugins.setInitComputationSchedulerHandler({ SchedulerProvider.testScheduler })
                RxJavaPlugins.setInitNewThreadSchedulerHandler({ SchedulerProvider.testScheduler })
            }
        }) {}
    }
}