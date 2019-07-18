package com.droid.netflixIMDB

class ReaderConstants {

    companion object {
        const val NETFLIX = "com.netflix.mediaclient"
        const val HOTSTAR = "in.startv.hotstar"

        val supportedPackages = listOf("com.netflix.mediaclient", "in.startv.hotstar")

        /*Payload*/
        const val PACKAGE_NAME = "packageName"
        const val TITLE = "title"
        const val YEAR = "year"
        const val TYPE = "type"
        const val SEARCH = "search"

        /*Purchase*/
        const val PURCHASE_TYPE = "purchase_type"
        const val PURCHASE = "purchase"

        /*Click Events*/
        const val CLICK_TYPE = "click_type"
        const val CLICK = "click"

        const val BASE_URL = "https://www.omdbapi.com/"
    }
}