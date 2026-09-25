package org.fenixuz.ads

object AdsManager {
    fun requestAd(query: String?, onResult: (Long?, String?) -> Unit) {
        onResult(null, null)
    }
    fun clickAd(dialogId: Long) {}
    fun cancel() {}
}
