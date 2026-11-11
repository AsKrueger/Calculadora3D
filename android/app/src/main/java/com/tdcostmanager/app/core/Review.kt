package com.tdcostmanager.app.core

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Pide la valoración en Google Play cuando el usuario ya le ha sacado partido a la app
 * (3 piezas guardadas), como mucho una vez cada 90 días. Google decide si muestra el diálogo.
 */
object Review {
    private const val PREFS = "review"
    private const val SAVES = "saves"
    private const val LAST = "last_prompt"
    private const val MIN_SAVES = 3
    private const val COOLDOWN_MS = 90L * 24 * 60 * 60 * 1000

    fun onPieceSaved(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saves = prefs.getInt(SAVES, 0) + 1
        prefs.edit().putInt(SAVES, saves).apply()
        val now = System.currentTimeMillis()
        if (saves < MIN_SAVES || now - prefs.getLong(LAST, 0) < COOLDOWN_MS) return
        prefs.edit().putLong(LAST, now).apply()
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
        }
    }
}
