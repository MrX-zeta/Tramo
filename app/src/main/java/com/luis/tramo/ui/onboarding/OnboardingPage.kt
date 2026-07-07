package com.luis.tramo.ui.onboarding

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.luis.tramo.R

/**
 * A single full-screen onboarding step: value-prop title, subtitle and a Lottie illustration.
 */
data class OnboardingPage(
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    @param:RawRes val animationRes: Int
) {
    companion object {
        val pages = listOf(
            OnboardingPage(
                titleRes = R.string.onboarding_title_1,
                subtitleRes = R.string.onboarding_subtitle_1,
                animationRes = R.raw.onboarding_focus
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_2,
                subtitleRes = R.string.onboarding_subtitle_2,
                animationRes = R.raw.onboarding_break
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_3,
                subtitleRes = R.string.onboarding_subtitle_3,
                animationRes = R.raw.onboarding_progress
            )
        )
    }
}
