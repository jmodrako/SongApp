package com.tooploox.songapp.common

import android.app.Activity
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding

fun <T : ViewDataBinding> Activity.bindContentView(layoutId: Int): T =
    DataBindingUtil.setContentView(this, layoutId)