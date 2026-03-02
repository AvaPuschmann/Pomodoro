package com.agenticfocus

import com.agenticfocus.viewmodel.Phase
import com.agenticfocus.viewmodel.PomodoroState
import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroPhaseTest {

    // --- Phase duration tests ---

    @Test
    fun `FOCUS phase has 1500 seconds`() {
        assertEquals(1500, Phase.FOCUS.durationSeconds)
    }

    @Test
    fun `SHORT_BREAK phase has 300 seconds`() {
        assertEquals(300, Phase.SHORT_BREAK.durationSeconds)
    }

    @Test
    fun `LONG_BREAK phase has 900 seconds`() {
        assertEquals(900, Phase.LONG_BREAK.durationSeconds)
    }

    // --- Phase transition logic (mirrors TimerService.onSessionComplete) ---

    private fun nextPhaseAfterFocus(completedPomodoros: Int): Phase {
        return if (completedPomodoros % 4 == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
    }

    @Test
    fun `After 1st focus session, next phase is SHORT_BREAK`() {
        assertEquals(Phase.SHORT_BREAK, nextPhaseAfterFocus(1))
    }

    @Test
    fun `After 2nd focus session, next phase is SHORT_BREAK`() {
        assertEquals(Phase.SHORT_BREAK, nextPhaseAfterFocus(2))
    }

    @Test
    fun `After 3rd focus session, next phase is SHORT_BREAK`() {
        assertEquals(Phase.SHORT_BREAK, nextPhaseAfterFocus(3))
    }

    @Test
    fun `After 4th focus session, next phase is LONG_BREAK`() {
        assertEquals(Phase.LONG_BREAK, nextPhaseAfterFocus(4))
    }

    @Test
    fun `After 8th focus session, next phase is LONG_BREAK`() {
        assertEquals(Phase.LONG_BREAK, nextPhaseAfterFocus(8))
    }

    // --- Time formatting ---

    private fun formatTime(seconds: Int): String =
        "%02d:%02d".format(seconds / 60, seconds % 60)

    @Test
    fun `formatTime 1500 seconds returns 25_00`() {
        assertEquals("25:00", formatTime(1500))
    }

    @Test
    fun `formatTime 300 seconds returns 05_00`() {
        assertEquals("05:00", formatTime(300))
    }

    @Test
    fun `formatTime 0 seconds returns 00_00`() {
        assertEquals("00:00", formatTime(0))
    }

    @Test
    fun `formatTime 65 seconds returns 01_05`() {
        assertEquals("01:05", formatTime(65))
    }

    // --- PomodoroState defaults ---

    @Test
    fun `Default PomodoroState starts with FOCUS phase`() {
        val state = PomodoroState()
        assertEquals(Phase.FOCUS, state.phase)
    }

    @Test
    fun `Default PomodoroState is not running`() {
        val state = PomodoroState()
        assertEquals(false, state.isRunning)
    }

    @Test
    fun `Default PomodoroState has 0 completed pomodoros`() {
        val state = PomodoroState()
        assertEquals(0, state.completedPomodoros)
    }
}
