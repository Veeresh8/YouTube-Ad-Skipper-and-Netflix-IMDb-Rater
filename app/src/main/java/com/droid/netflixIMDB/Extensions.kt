package com.droid.netflixIMDB

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val DEFAULT_DEBOUNCE_INTERVAL_MS = 1000L


fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun Context.toast(message: String) {
    GlobalScope.launch (Dispatchers.Main.immediate) {
        Toast.makeText(Application.instance, message, Toast.LENGTH_SHORT).show()
    }
}

fun Context.toastLong(message: String) {
    GlobalScope.launch (Dispatchers.Main.immediate) {
        Toast.makeText(Application.instance, message, Toast.LENGTH_LONG).show()
    }
}


fun View.setOnDebouncedClickListener(
    interval: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    onClick: (View) -> Unit
) {
    var lastClickTime = 0L
    val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
    bounceAnimation.setAnimationListener(
        object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                onClick(this@setOnDebouncedClickListener)
            }
            override fun onAnimationRepeat(animation: Animation) {}
        }
    )
    setOnClickListener {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime > interval) {
            lastClickTime = currentTime
            startAnimation(bounceAnimation)
        }
    }
}
