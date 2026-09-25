package org.fenixuz.analytics

import android.content.Context

object AnalyticsRemote {
    @JvmStatic fun addInstallIfFirstTime(context: Context) {}
    @JvmStatic fun addAccount() {}
    fun getInstallCount(cb: (Long?) -> Unit) { cb(0L) }
    fun getAccountCount(cb: (Long?) -> Unit) { cb(0L) }
    fun getInstallBaseOffset(cb: (Long?) -> Unit) { cb(0L) }
    fun getAccountBaseOffset(cb: (Long?) -> Unit) { cb(0L) }
    @JvmStatic fun cachedInstallsOrBase(context: Context): Long = 0L
    @JvmStatic fun cachedAccountsOrBase(context: Context): Long = 0L
    @JvmStatic fun getInstallsDisplay(context: Context, cb: (Long) -> Unit) { cb(0L) }
    @JvmStatic fun getAccountsDisplay(context: Context, cb: (Long) -> Unit) { cb(0L) }
}
