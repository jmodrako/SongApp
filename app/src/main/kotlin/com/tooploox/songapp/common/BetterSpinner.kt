package com.tooploox.songapp.common

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet

class BetterSpinner : android.support.v7.widget.AppCompatSpinner {

    constructor(context: Context) : super(context)

    constructor(context: Context, mode: Int) : super(context, mode)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, mode: Int) : super(context, attrs, defStyleAttr, mode)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, mode: Int, popupTheme: Resources.Theme) : super(context, attrs, defStyleAttr, mode, popupTheme)

    var isInitialSelection: Boolean = true
}