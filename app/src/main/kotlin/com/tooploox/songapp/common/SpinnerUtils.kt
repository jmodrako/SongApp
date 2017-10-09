package com.tooploox.songapp.common

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView

fun prepareSpinnerUtil(
    context: Context,
    data: List<String>, spinner: Spinner,
    clickCallback: (String) -> Unit,
    defaultCallback: () -> Unit) {

    val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, android.R.id.text1) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val result = super.getView(position, convertView, parent)
            result.findViewById<TextView>(android.R.id.text1).setTextColor(Color.WHITE)
            return result
        }
    }
    adapter.addAll(data)

    spinner.adapter = adapter
    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position == 0) {
                defaultCallback()
            } else {
                val clickedLabel = data[position]
                val clickedItem = clickedLabel.take(clickedLabel.lastIndexOf(" "))
                clickCallback(clickedItem)
            }
        }
    }
}