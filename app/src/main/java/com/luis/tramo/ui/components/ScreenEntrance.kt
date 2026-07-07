package com.luis.tramo.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * The shared screen-entrance animation, originally defined on Home. Reused across destinations so
 * every screen animates in consistently. A staggered fade + slide keyed on [index]; skipped
 * entirely when the system reduced-motion setting is on.
 */
@Composable
fun ScreenEntrance(
    index: Int,
    visible: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (reduceMotion) {
        Box(modifier) { content() }
        return
    }
    val delay = index * 90
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(400, delayMillis = delay)) +
            slideInVertically(tween(400, delayMillis = delay)) { it / 8 }
    ) {
        content()
    }
}

/** True when the system animator duration scale is 0 (reduced motion / "remove animations"). */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
