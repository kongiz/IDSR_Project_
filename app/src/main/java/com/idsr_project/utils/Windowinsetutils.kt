package com.idsr_project.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

fun AppCompatActivity.applyWindowInsets(
    topView: View,
    bottomView: View? = null
) {
    ViewCompat.setOnApplyWindowInsetsListener(topView) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(0, systemBars.top, 0, 0)
        insets
    }

    bottomView?.let {
        ViewCompat.setOnApplyWindowInsetsListener(it) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 16.dpToPx(view.context)
            }
            insets
        }
    }
}

fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()