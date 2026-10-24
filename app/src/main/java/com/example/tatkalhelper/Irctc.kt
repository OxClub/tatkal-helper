package com.example.tatkalhelper

import android.content.Context
import android.content.Intent
import android.net.Uri

object Irctc {
    const val PKG = "cris.org.in.prs.ima" // IRCTC Rail Connect - verify on your phone
    const val WEB_HOST = "irctc.co.in"
    const val WEB_URL = "https://www.irctc.co.in/nget/train-search"

    /** Browsers the autofill service may act in, and only while an irctc.co.in page is open. */
    val BROWSERS = setOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.brave.browser",
        "org.mozilla.firefox"
    )

    fun appIntent(ctx: Context): Intent =
        (ctx.packageManager.getLaunchIntentForPackage(PKG) ?: websiteIntent())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens in your default browser. Autofill only works if that browser is in BROWSERS. */
    fun websiteIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
