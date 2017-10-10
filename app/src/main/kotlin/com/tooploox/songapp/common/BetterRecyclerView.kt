package com.tooploox.songapp.common

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent

typealias OnDownClickListener = () -> Unit

class BetterRecyclerView : RecyclerView {

    var onDownClickListener: OnDownClickListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && onDownClickListener != null) onDownClickListener!!.invoke()

        return super.dispatchTouchEvent(ev)
    }
}
