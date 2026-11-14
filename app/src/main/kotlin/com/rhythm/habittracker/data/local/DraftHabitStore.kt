package com.rhythm.habittracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.draftDataStore: DataStore<Preferences> by preferencesDataStore(name = "habit_draft_prefs")

@Serializable
data class HabitDraft(
    val title: String = "",
    val description: String = "",
    val domain: String = "",
    val colorHex: String = HabitAccentColor.ROYAL_BLUE.hex,
    val cadenceType: CadenceType = CadenceType.DAILY,
    val scheduledDayValues: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val weeklyTargetMode: WeeklyTargetMode = WeeklyTargetMode.FIXED_SCHEDULE,
    val monthlySubMode: MonthlySubMode = MonthlySubMode.SPECIFIC_DATES,
    val monthlyDates: List<Int> = listOf(1, 15, 30),
    val ordinalWeek: Int = 1,
    val ordinalDayOfWeek: Int = 1,
    val endOfMonthBehavior: EndOfMonthBehavior = EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
    val customArchetype: CustomCadenceArchetype = CustomCadenceArchetype.INTERVAL,
    val customInterval: Int = 3,
    val customIntervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val cycleDaysOn: Int = 5,
    val cycleDaysOff: Int = 2,
    val targetPerPeriod: Int = 1,
    val targetTimes: List<TargetTimeSlot> = listOf(
        TargetTimeSlot(id = "slot_1", hour = 8, minute = 0, enabled = true),
    ),
    val skipReminderIfCompleted: Boolean = true,
    val anchorDateEpochDay: Long? = null,
)

interface DraftHabitStore {
    suspend fun loadDraft(): HabitDraft?
    suspend fun saveDraft(draft: HabitDraft)
    suspend fun clearDraft()
}

class DataStoreDraftHabitStore(private val context: Context) : DraftHabitStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val draftKey = stringPreferencesKey("new_habit_draft_json")

    override suspend fun loadDraft(): HabitDraft? {
        val raw = context.draftDataStore.data.first()[draftKey] ?: return null
        return runCatching { json.decodeFromString<HabitDraft>(raw) }.getOrNull()
    }

    override suspend fun saveDraft(draft: HabitDraft) {
        context.draftDataStore.edit { prefs ->
            prefs[draftKey] = json.encodeToString(draft)
        }
    }

    override suspend fun clearDraft() {
        context.draftDataStore.edit { prefs ->
            prefs.remove(draftKey)
        }
    }
}
