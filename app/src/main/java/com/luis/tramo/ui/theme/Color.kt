package com.luis.tramo.ui.theme

import androidx.compose.ui.graphics.Color

// --- Brand: deep pine-teal, derived from the Tramo identity ---
val TramoPine = Color(0xFF2F5D62)
val TramoPineContainer = Color(0xFFC9E4E7)
val TramoOnPineContainer = Color(0xFF12191A)

/** Reserved "progress" accent (amber). Used ONLY for streak, milestones and progress — never as a
 * primary/brand color or a button fill. Exposed via [com.luis.tramo.ui.theme.TramoTheme.progress]. */
val TramoProgress = Color(0xFFE8B75D)

// --- Neutrals / surfaces ---
val TramoBackgroundLight = Color(0xFFF5F3EE)
val TramoBackgroundDark = Color(0xFF12191A)
val TramoInk = Color(0xFF1A2224)          // onSurface (light)
val TramoError = Color(0xFFB4534B)

// Dark-scheme brand variants (lighter pine so it reads on the dark ink surface).
val TramoPineDark = Color(0xFF8FCBD1)
val TramoPineContainerDark = Color(0xFF14464B)

/**
 * The task card customization palette, relocated here as named tokens (was an inline list in the
 * tasks UI). Stored as ARGB longs because [com.luis.tramo.data.task.TaskEntity] persists a Long.
 */
val SwatchIndigo = 0xFF6366F1L
val SwatchTeal = 0xFF0EA5A4L
val SwatchAmber = 0xFFF59E0BL
val SwatchRed = 0xFFEF4444L
val SwatchGreen = 0xFF10B981L
val SwatchBlue = 0xFF3B82F6L
val SwatchPink = 0xFFEC4899L
val SwatchViolet = 0xFF8B5CF6L
val SwatchSlate = 0xFF64748BL
val SwatchOrange = 0xFFF97316L

val TaskSwatches: List<Long> = listOf(
    SwatchIndigo, SwatchTeal, SwatchAmber, SwatchRed, SwatchGreen,
    SwatchBlue, SwatchPink, SwatchViolet, SwatchSlate, SwatchOrange
)
