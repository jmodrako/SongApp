package com.tooploox.songapp.common

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun Disposable.addToDisposable(compositeDisposable: CompositeDisposable) = compositeDisposable.add(this)