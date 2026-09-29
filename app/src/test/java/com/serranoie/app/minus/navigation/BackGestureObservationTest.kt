package com.serranoie.app.minus.navigation

import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackGestureObservationTest {

    private class ConsumingHandler : NavigationEventHandler<NavigationEventInfo>(
        initialInfo = NavigationEventInfo.None,
        isBackEnabled = true,
        isForwardEnabled = false,
    ) {
        var completed = 0

        override fun onBackCompleted() {
            completed++
        }
    }

    private fun event(progress: Float, touchY: Float, edge: Int) = NavigationEvent(
        touchX = 0f,
        touchY = touchY,
        progress = progress,
        swipeEdge = edge,
    )

    @Test
    fun observerSeesGestureConsumedByAnotherHandler() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        dispatcher.addHandler(ConsumingHandler())

        input.backStarted(event(0f, 500f, NavigationEvent.EDGE_LEFT))
        input.backProgressed(event(0.4f, 620f, NavigationEvent.EDGE_LEFT))

        val state = dispatcher.transitionState.value
        assertTrue(state is NavigationEventTransitionState.InProgress)

        val inProgress = state as NavigationEventTransitionState.InProgress
        assertEquals(
            NavigationEventTransitionState.TRANSITIONING_BACK,
            inProgress.direction,
        )
        assertEquals(0.4f, inProgress.latestEvent.progress, 0.0001f)
        assertEquals(620f, inProgress.latestEvent.touchY, 0.0001f)
        assertEquals(NavigationEvent.EDGE_LEFT, inProgress.latestEvent.swipeEdge)
    }

    @Test
    fun transitionStateReturnsToIdleOnCompletion() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        dispatcher.addHandler(ConsumingHandler())

        input.backStarted(event(0f, 500f, NavigationEvent.EDGE_LEFT))
        input.backProgressed(event(0.6f, 500f, NavigationEvent.EDGE_LEFT))
        input.backCompleted()

        assertEquals(
            NavigationEventTransitionState.Idle,
            dispatcher.transitionState.value,
        )
    }

    @Test
    fun transitionStateReturnsToIdleOnCancel() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        dispatcher.addHandler(ConsumingHandler())

        input.backStarted(event(0f, 500f, NavigationEvent.EDGE_RIGHT))
        input.backProgressed(event(0.3f, 500f, NavigationEvent.EDGE_RIGHT))
        input.backCancelled()

        assertEquals(
            NavigationEventTransitionState.Idle,
            dispatcher.transitionState.value,
        )
    }
}
