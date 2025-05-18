package com.example.inf8405tf.utils

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.widget.ImageView

object PulsatingEffectUtils {
    fun startPulsatingEffect(networkStatusIcon: ImageView) {
        val scaleAnimation = ObjectAnimator.ofPropertyValuesHolder(
            networkStatusIcon,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.2f, 1f)
        )
        scaleAnimation.duration = 1000 // Duration in milliseconds
        scaleAnimation.repeatCount = ObjectAnimator.INFINITE
        scaleAnimation.start()
    }

    fun stopPulsatingEffect(networkStatusIcon: ImageView) {
        networkStatusIcon.animate().cancel()
        networkStatusIcon.scaleX = 1f
        networkStatusIcon.scaleY = 1f
    }
}