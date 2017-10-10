package com.tooploox.songapp.details

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.tooploox.songapp.R
import com.tooploox.songapp.data.SongModel
import com.tooploox.songapp.databinding.FragmentDetailsBinding

class DetailsDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentDetailsBinding
    private var model: SongModel? = null

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_details, container, false)

        if (model != null) {
            binding.model = model
        } else {
            dialog.dismiss()
        }

        return binding.root
    }

    fun withModel(newModel: SongModel) {
        model = newModel
    }
}