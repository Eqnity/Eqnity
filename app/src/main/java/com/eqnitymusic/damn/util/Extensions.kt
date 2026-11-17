package com.eqnitymusic.damn.util

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.eqnitymusic.damn.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

fun View.showSnackbar(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).apply {
        actionLabel?.let { label ->
            setAction(label) { action?.invoke() }
        }
        setBackgroundTint(ContextCompat.getColor(context, R.color.surface_secondary_elevated))
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        setActionTextColor(ContextCompat.getColor(context, R.color.accent_primary))
        show()
    }
}

fun TextView.setTextAppearanceStyle(styleRes: Int) {
    setTextAppearance(styleRes)
}

fun Fragment.showSnackbar(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
    requireView().showSnackbar(message, actionLabel, action)
}

fun RecyclerView.showEmptyView(emptyView: View, hasItems: Boolean) {
    visibility = if (hasItems) View.VISIBLE else View.GONE
    emptyView.visibility = if (hasItems) View.GONE else View.VISIBLE
}

fun formatDuration(millis: Long): String {
    val minutes = millis / 60000
    val seconds = (millis % 60000) / 1000
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

fun Bitmap.getDominantColors(): Pair<Int, Int> {
    // Simple dominant color extraction
    var redSum = 0
    var greenSum = 0
    var blueSum = 0
    var pixelCount = 0

    val width = width
    val height = height
    val step = kotlin.math.max(1, width / 50) // Sample every N pixels

    for (x in 0 until width step step) {
        for (y in 0 until height step step) {
            val pixel = getPixel(x, y)
            redSum += Color.red(pixel)
            greenSum += Color.green(pixel)
            blueSum += Color.blue(pixel)
            pixelCount++
        }
    }

    val avgColor = Color.rgb(redSum / pixelCount, greenSum / pixelCount, blueSum / pixelCount)
    
    // Generate complementary color for gradient
    val hsv = FloatArray(3)
    Color.colorToHSV(avgColor, hsv)
    val complementColor = Color.HSVToColor(floatArrayOf((hsv[0] + 180) % 360, hsv[1], hsv[2]))

    return avgColor to complementColor
}

fun <T> CoroutineScope.launchMain(block: suspend CoroutineScope.() -> T): Job {
    return launch(Dispatchers.Main) { block() }
}

fun <T> CoroutineScope.launchIO(block: suspend CoroutineScope.() -> T): Job {
    return launch(Dispatchers.IO) { block() }
}