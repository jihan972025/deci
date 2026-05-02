package com.deci.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

object InsetsHelper {

    fun applySystemBarPadding(topView: View? = null, bottomView: View? = null) {
        topView?.let { v ->
            ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(top = bars.top)
                insets
            }
            ViewCompat.requestApplyInsets(v)
        }
        bottomView?.let { v ->
            ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bars.bottom
                }
                insets
            }
            ViewCompat.requestApplyInsets(v)
        }
    }
}
