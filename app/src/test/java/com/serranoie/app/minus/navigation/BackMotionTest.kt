package com.serranoie.app.minus.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackMotionTest {

    @Test
    fun scrubTokens_stayWithinUsableRanges() {
        assertTrue(BackMotionTokens.PageScaleTarget > 0f)
        assertTrue(BackMotionTokens.PageScaleTarget <= 1f)
        assertTrue(BackMotionTokens.ScrubAlphaFloor >= 0f)
        assertTrue(BackMotionTokens.ScrubAlphaFloor <= 1f)
        assertTrue(BackMotionTokens.RevealDimAlpha in 0f..1f)
        assertTrue(BackMotionTokens.ClosingDepartureFraction >= 0f)
        assertTrue(BackMotionTokens.ExitSlideDivisor >= 1)
        assertTrue(BackMotionTokens.TransitionDurationMillis > 0)
    }

    @Test
    fun alphaFloorKeyframe_landsStrictlyInsideTheTimeline() {
        val at = (BackMotionTokens.TransitionDurationMillis * BackMotionTokens.ScrubAlphaFloorAt)
            .toInt()

        assertTrue(at > 0)
        assertTrue(at < BackMotionTokens.TransitionDurationMillis)
    }

    @Test
    fun easedProgress_spansZeroToOneAndClamps() {
        assertEquals(0f, easedProgress(0f), 0.0001f)
        assertEquals(1f, easedProgress(1f), 0.0001f)
        assertEquals(0f, easedProgress(-2f), 0.0001f)
        assertEquals(1f, easedProgress(4f), 0.0001f)
    }

    @Test
    fun revealDim_isDarkestBehindAHiddenPageAndClearsAsItArrives() {
        assertEquals(BackMotionTokens.RevealDimAlpha, revealDim(0f), 0.0001f)
        assertEquals(0f, revealDim(1f), 0.0001f)
        assertTrue(revealDim(0.5f) < BackMotionTokens.RevealDimAlpha)
        assertTrue(revealDim(0.5f) > 0f)
    }

    @Test
    fun popDirection_reportsTheLastRecordedNavigation() {
        val direction = PopDirection()

        assertTrue(!direction.isPop)
        direction.record(pop = true)
        assertTrue(direction.isPop)
        direction.record(pop = false)
        assertTrue(!direction.isPop)
    }

    @Test
    fun closingDeparture_staysPutWhileTheFingerIsDown() {
        assertEquals(0f, closingDeparture(1080f, 1f), 0.0001f)
    }

    @Test
    fun closingDeparture_movesOnlyAsTheCommitPlaysOut() {
        val half = closingDeparture(1080f, 0.5f)
        val done = closingDeparture(1080f, 0f)

        assertEquals(1080f * BackMotionTokens.ClosingDepartureFraction, done, 0.0001f)
        assertTrue(half > 0f)
        assertTrue(half < done)
    }

    @Test
    fun revealDim_clampsOutsideTheTransition() {
        assertEquals(BackMotionTokens.RevealDimAlpha, revealDim(-1f), 0.0001f)
        assertEquals(0f, revealDim(2f), 0.0001f)
    }
}
