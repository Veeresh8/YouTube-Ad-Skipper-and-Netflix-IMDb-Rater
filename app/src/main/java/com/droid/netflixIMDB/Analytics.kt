package com.droid.netflixIMDB

import android.os.Bundle

object Analytics {

    fun postPayload(packageName: String, payload: Payload) {
        val bundle = Bundle()
        bundle.putString(ReaderConstants.PACKAGE_NAME, packageName)
        bundle.putString(ReaderConstants.TITLE, payload.title ?: "")
        bundle.putString(ReaderConstants.TYPE, payload.type ?: "")
        bundle.putString(ReaderConstants.YEAR, payload.year ?: "")
        Application.firebaseAnalytics?.logEvent(ReaderConstants.SEARCH, bundle)
    }

    fun postPurchasePayload(purchaseType: String) {
        val bundle = Bundle()
        bundle.putString(ReaderConstants.PURCHASE_TYPE, purchaseType)
        Application.firebaseAnalytics?.logEvent(ReaderConstants.PURCHASE, bundle)
    }

    fun postClickEvents(clickTypes: ClickTypes) {
        val bundle = Bundle()
        bundle.putString(ReaderConstants.CLICK_TYPE, clickTypes.name)
        Application.firebaseAnalytics?.logEvent(ReaderConstants.CLICK, bundle)
    }

    enum class ClickTypes(name: String) {
        PLAYSTORE("playstore"),
        FEEDBACK("feedback"),
        PRIVACY_POLICY("privacy_policy"),
        CUSTOMIZE("customize"),
        SUPPORT("support"),

        OVERLAY("overlay_permission"),
        ACC_SERV("acc_serv"),
        WHITELIST("whitelist"),

        SMALL_PURCHASE("purchase_small"),
        MEDIUM_PURCHASE("purchase_medium"),
        HIGH_PURCHASE("purchase_high"),
    }
}