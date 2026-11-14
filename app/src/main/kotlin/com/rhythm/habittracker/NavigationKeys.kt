package com.rhythm.habittracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute : NavKey

@Serializable data object AllHabitsRoute : NavKey

@Serializable
data class HabitDetailRoute(
    val habitId: Long,
) : NavKey

@Serializable
data class AddEditHabitRoute(
    val habitId: Long? = null,
    val instanceId: Long = 0L,
) : NavKey
