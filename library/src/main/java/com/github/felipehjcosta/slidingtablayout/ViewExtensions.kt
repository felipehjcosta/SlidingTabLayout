package com.github.felipehjcosta.slidingtablayout

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver

internal inline fun <T : View> T.doOnGlobalLayout(crossinline block: (T) -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            block(this@doOnGlobalLayout)
            viewTreeObserver.removeOnGlobalLayoutListenerIncludingBellowJellyBean(this)
        }
    })
}

@SuppressLint("NewApi")
internal fun ViewTreeObserver.removeOnGlobalLayoutListenerIncludingBellowJellyBean(
        listener: ViewTreeObserver.OnGlobalLayoutListener
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        removeOnGlobalLayoutListener(listener)
    } else {
        removeGlobalOnLayoutListener(listener)
    }
}