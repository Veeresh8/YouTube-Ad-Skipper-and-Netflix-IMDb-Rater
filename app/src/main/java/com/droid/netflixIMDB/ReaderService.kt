package com.droid.netflixIMDB

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.droid.netflixIMDB.analytics.Analytics
import com.droid.netflixIMDB.notifications.NotificationManager
import com.droid.netflixIMDB.ratingView.RatingViewRenderer
import com.droid.netflixIMDB.reader.*
import com.droid.netflixIMDB.util.Prefs
import com.droid.netflixIMDB.util.ReaderConstants
import com.droid.netflixIMDB.util.ReaderConstants.Companion.PLAYSTORE_INIT


@Suppress("IMPLICIT_CAST_TO_ANY")
class ReaderService : AccessibilityService() {

    private val TAG: String = javaClass.simpleName

    private var title: String? = null
    private var year: String? = null
    private var type: String? = null

    private val readers = HashMap<String, Reader>()

    private var ratingView: RatingViewRenderer? = null

    companion object {
        var isConnected: Boolean = false
        var INSTANCE: ReaderService? = null
    }

    override fun onCreate() {
        super.onCreate()
        initReaders()
        initRatingView()
        INSTANCE = this
    }

    private fun initReaders() {
        readers.clear()
        readers[ReaderConstants.NETFLIX] = NetflixReader()
        readers[ReaderConstants.HOTSTAR] = HotstarReader()
        readers[ReaderConstants.PRIME] = AmazonPrimeReader()
        readers[ReaderConstants.YOUTUBE] = YoutubeReader()
    }

    private fun initRatingView() {
        ratingView = RatingViewRenderer()
        ratingView?.init(this)
    }

    private fun removeRatingView() {
        ratingView?.removeRatingView()
    }

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "Accessibility service un-bind")
        isConnected = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeRatingView()
    }

    override fun onServiceConnected() {
        Log.i(TAG, "Accessibility service connected")

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.packageNames = ReaderConstants.supportedPackages.toTypedArray()
        info.feedbackType = AccessibilityServiceInfo.DEFAULT
        info.notificationTimeout = 100

        this.serviceInfo = info

        isConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) {
                Log.i(TAG, "Null AccessibilityEvent")
                return
            }

            if (event.source == null) {
                Log.i(TAG, "Event source was NULL")
                return
            }

            if (event.source.packageName == null) {
                Log.i(TAG, "PackageName was NULL")
                return
            }

            if (!ReaderConstants.supportedPackages.contains(event.source.packageName)) {
                Log.i(TAG, "Not handling event from " + event.source.packageName)
                return
            }

            Prefs.getUserSupportedPackages()?.run {
                if (!this.contains(event.source.packageName)) {
                    Log.i(
                        TAG,
                        "Not handling event from " + event.source.packageName + " as user prefs"
                    )
                    return
                }
            }

            if (event.source.packageName == ReaderConstants.YOUTUBE && YoutubeReader.isTimerRunning) {
                Log.i(TAG, "Not handling event from Youtube when timer is running")
                return
            }

            val reader = readers[event.source.packageName]

            val readerPayload = reader?.payload(event.source)

            title = readerPayload?.title
            year = readerPayload?.year
            type = readerPayload?.type

            val payload = Payload(title, year, type)

            Log.i(TAG, "Scraped item: $payload")

            if (payload.title == null || (payload.year == null && payload.type == null)) {
                Log.i(TAG, "No title request")
                return
            }

            if (payload.title.equals(RatingRequester.lastTitle, true) &&
                payload.year.equals(RatingRequester.lastYear, true)
            ) {
                Log.i(TAG, "Already requested $payload")
                return
            }

            Analytics.postPayload(event.source.packageName.toString(), payload)

            if (Prefs.hasExceedLimit()) {
                Log.i(TAG, "Exceeded max events, user is not premium")
                Handler(Looper.getMainLooper()).post {
                    ratingView?.showBuyView()
                }
                return
            }

            RatingRequester.requestRating(
                payload,
                event.source.packageName.toString(),
                object : RatingRequester.RatingRequesterCallback {
                    override fun onFailure(message: String) {

                    }

                    override fun onSuccess(responsePayload: ResponsePayload) {
                        showRating(responsePayload)
                        incrementPushCount()
                    }

                    override fun onRequestException(exception: Exception) {

                    }
                })
        } catch (exception: Exception) {
            Log.d(TAG, "Exception in on event: ${exception.message}")
        }
    }

    private fun resetRequest(event: AccessibilityEvent) {
        if (event.packageName != null && event.className != null) {
            val componentName =
                ComponentName(event.packageName.toString(), event.className.toString())
            val activityInfo = tryGetActivity(componentName)
            if (activityInfo != null) {
                val currentActivity = componentName.flattenToShortString()
                Log.e(TAG, "Current Activity: $currentActivity")
            }
        }
    }

    private fun tryGetActivity(componentName: ComponentName): ActivityInfo? {
        return try {
            packageManager.getActivityInfo(componentName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun incrementPushCount() {
        try {
            Analytics.postUserProperties()
            Prefs.incrementRequestMade()
            Prefs.getPayloadTotalCount().run {
                if (this == PLAYSTORE_INIT) {
                    NotificationManager.createPlayStorePushNotification(
                        this@ReaderService,
                        "Enjoying ${application.getString(R.string.app_name)}?",
                        "We've served over $this hits. " +
                                "Please spread the word by giving us a honest rating at the PlayStore"
                    )
                }
            }
        } catch (exception: java.lang.Exception) {
            Log.e(TAG, "Exception showing push notification: ${exception.message}")
        }
    }

    private fun showConnectionErrorToast() {
        Toast.makeText(
            this,
            "Failed to fetch ratings, check your network connectivity",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showGenericErrorToast() {
        Toast.makeText(
            this,
            "Failed to fetch ratings, try again!",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showToastWithMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showRating(responsePayload: ResponsePayload) {
        Handler(Looper.getMainLooper()).post {
            ratingView?.showRating(responsePayload)
        }
    }

//    private fun checkNodeRecursively(node: AccessibilityNodeInfo) {
//        node.text?.let {
//            Log.d(TAG, "Text: " + node.text)
//        }
//
//        if (node.childCount > 0) {
//            (0 until node.childCount).forEach { index ->
//                val child = node.getChild(index)
//                if (child != null && child.isVisibleToUser) {
//                    checkNodeRecursively(child)
//                    child.recycle()
//                }
//            }
//        }
//    }
}