package com.tooploox.songapp.common

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView

fun RecyclerView.withVerticalManager() {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
}

fun View?.visible() {
    if (this != null) visibility = View.VISIBLE
}

fun View?.gone() {
    if (this != null) visibility = View.GONE
}

fun View.click(callback: () -> Unit) = this.setOnClickListener { callback.invoke() }
fun View?.clearClickListener() = this?.setOnClickListener(null)

val ViewGroup.views: List<View>
    get() = (0 until childCount).map(this::getChildAt)

fun EditText.retype() {
    val currentText = this.text.toString()
    this.setText(currentText)
    this.setSelection(currentText.length)
}

val TextView.hasText: Boolean
    get() = this.text?.isNotBlank() ?: false

fun TextView.bold(bold: Boolean = true) {
    this.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
}