package com.agenticfocus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import com.agenticfocus.ui.screen.PomodoroScreen
import com.agenticfocus.ui.theme.AgenticFocusTheme

class PomodoroScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchScreen() {
        composeTestRule.setContent {
            AgenticFocusTheme {
                PomodoroScreen()
            }
        }
    }

    @Test
    fun timerDisplaysInitialTime() {
        launchScreen()
        composeTestRule.onNodeWithText("25:00").assertIsDisplayed()
    }

    @Test
    fun focusButtonIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Focus").assertIsDisplayed()
    }

    @Test
    fun breakButtonIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Break").assertIsDisplayed()
    }

    @Test
    fun playButtonIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun pomodoroCounterDisplaysZeroInitially() {
        launchScreen()
        composeTestRule.onNodeWithText("🍅 × 0").assertIsDisplayed()
    }

    @Test
    fun appTitleIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Agentic Focus").assertIsDisplayed()
    }
}
