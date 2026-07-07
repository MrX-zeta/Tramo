package com.luis.tramo.util

import kotlin.time.Duration.Companion.seconds

/**
 * Formats a raw seconds value into a compact human string: "2h 15m", "15m", or "45s".
 * Uses [kotlin.time.Duration.toComponents] so there's no manual /3600 %60 arithmetic.
 */
fun formatDuration(totalSeconds: Int): String =
    totalSeconds.seconds.toComponents { hours, minutes, secs, _ ->
        when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${secs}s"
        }
    }
