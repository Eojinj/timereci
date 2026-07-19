package com.timereci.focus.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks the context wrapper chain to the hosting [Activity], or null. */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
