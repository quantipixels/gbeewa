package com.quantipixels.gbeewa.verification

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class OwnershipTest {
    @get:Rule val compose = createAndroidComposeRule<VerificationActivity>()

    @Test fun capturedOldOwnerCannotDeliverToReplacementAndCurrentResultIsConsumedOnce() {
        compose.waitForIdle()
        lateinit var stale: (String) -> Unit
        compose.activityRule.scenario.onActivity { stale = it.capturePickerCallback() }
        compose.onNodeWithTag("replace-owner").performClick()
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { stale("late") }
        compose.onNodeWithTag("deliveries").assertTextEquals("deliveries:0")
        compose.onNodeWithTag("publish").performClick()
        compose.onNodeWithTag("deliveries").assertTextEquals("deliveries:1")
        compose.onNodeWithTag("consume-again").performClick()
        compose.onNodeWithTag("deliveries").assertTextEquals("deliveries:1")
    }

    @Test fun restoredRouteFallsBackBeforeStartingWorkWithoutTransientHandoff() {
        compose.onNodeWithTag("open-detail").performClick()
        compose.onNodeWithTag("status").assertTextEquals("detail")
        compose.onNodeWithTag("work").assertTextEquals("work:1")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("status").assertTextEquals("fallback")
        compose.onNodeWithTag("work").assertTextEquals("work:0")
    }
}
