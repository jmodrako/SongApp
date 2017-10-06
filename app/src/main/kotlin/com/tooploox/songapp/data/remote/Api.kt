package com.tooploox.songapp.data.remote

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

internal interface Api {

    @GET("search")
    fun search(@Query("term") query: String, @Query("limit") limit: Int): Single<ApiSearchModel>
}