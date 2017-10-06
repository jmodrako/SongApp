package com.tooploox.songapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.tooploox.songapp.data.local.AssetsProvider
import com.tooploox.songapp.data.local.LocalDataSource
import com.tooploox.songapp.data.remote.RemoteDataSource
import io.reactivex.android.schedulers.AndroidSchedulers

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dataSource = RemoteDataSource()
        dataSource.search("Loosely")
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d("BENY", "Result.remote: $it")
            }, {
                Log.d("BENY", "Error.remote: $it")
            })

        val localDataSource = LocalDataSource(AssetsProvider(this))
        localDataSource.search("Loosely")
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d("BENY", "Result.local: $it")
            }, {
                Log.d("BENY", "Error.local: $it")
            })
    }
}
