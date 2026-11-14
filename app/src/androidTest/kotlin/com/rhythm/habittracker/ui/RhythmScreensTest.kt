package com.rhythm.habittracker.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.ui.addhabit.AddEditHabitScreenContent
import com.rhythm.habittracker.ui.addhabit.AddEditHabitUiState
import com.rhythm.habittracker.ui.theme.RhythmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RhythmScreensTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun addHabitScreen_switchesCadenceTabs() {
        var selectedCadence = CadenceType.DAILY
        composeTestRule.setContent {
            RhythmTheme {
                AddEditHabitScreenContent(
                    uiState = AddEditHabitUiState(cadenceType = selectedCadence),
                    commitToastSubtitle = null,
                    onBack = {},
                    onTitleChanged = {},
                    onDescriptionChanged = {},
                    onSelectDomain = {},
                    onRemoveDomain = {},
                    onAddCustomDomain = {},
                    onSelectColor = {},
                    onSelectCadenceType = { selectedCadence = it },
                    onToggleScheduledDay = {},
                    onApplyWeeklyPreset = {},
                    onSelectWeeklyTargetMode = {},
                    onSelectMonthlySubMode = {},
                    onApplyMonthlyPreset = {},
                    onToggleMonthlyDate = {},
                    onSelectOrdinalWeek = {},
                    onSelectOrdinalDayOfWeek = {},
                    onSelectEndOfMonthBehavior = {},
                    onSelectCustomArchetype = {},
                    onAdjustCustomInterval = {},
                    onSelectCustomIntervalUnit = {},
                    onAdjustCycleOn = {},
                    onAdjustCycleOff = {},
                    onAdjustTargetPerPeriod = {},
                    onToggleTimeSlotEnabled = {},
                    onUpdateTimeSlot = { _, _, _ -> },
                    onAddTimeSlot = { _, _ -> },
                    onRemoveTimeSlot = {},
                    onToggleSkipReminder = {},
                    onCommitHabit = {},
                    onDiscard = {},
                    onDeleteHabit = {},
                )
            }
        }

        composeTestRule.onNodeWithText("New Habit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly").performClick()
        assertEquals(CadenceType.WEEKLY, selectedCadence)
    }
}
