package com.rhythm.habittracker

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.rhythm.habittracker.ui.addhabit.AddEditHabitScreen
import com.rhythm.habittracker.ui.addhabit.AddEditHabitViewModel
import com.rhythm.habittracker.ui.allhabits.AllHabitsScreen
import com.rhythm.habittracker.ui.allhabits.AllHabitsViewModel
import com.rhythm.habittracker.ui.dashboard.DashboardScreen
import com.rhythm.habittracker.ui.dashboard.DashboardViewModel
import com.rhythm.habittracker.ui.habitdetail.HabitDetailScreen
import com.rhythm.habittracker.ui.habitdetail.HabitDetailViewModel

@Composable
fun MainNavigation(
  openHabitId: Long? = null,
  onOpenHabitConsumed: () -> Unit = {},
) {
  val backStack = rememberNavBackStack(DashboardRoute)
  val app = LocalContext.current.applicationContext as RhythmApplication
  val container = app.container

  LaunchedEffect(openHabitId) {
    if (openHabitId != null && openHabitId > 0L) {
      val targetRoute = HabitDetailRoute(habitId = openHabitId)
      if (backStack.lastOrNull() != targetRoute) {
        backStack.add(targetRoute)
      }
      onOpenHabitConsumed()
    }
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    transitionSpec = {
      (slideInHorizontally(
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> fullWidth / 4 },
      ) + fadeIn(animationSpec = tween(durationMillis = 260))) togetherWith
        (slideOutHorizontally(
          animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
          targetOffsetX = { fullWidth -> -fullWidth / 10 },
        ) + fadeOut(animationSpec = tween(durationMillis = 220)))
    },
    popTransitionSpec = {
      (slideInHorizontally(
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> -fullWidth / 10 },
      ) + fadeIn(animationSpec = tween(durationMillis = 240))) togetherWith
        (slideOutHorizontally(
          animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
          targetOffsetX = { fullWidth -> fullWidth / 4 },
        ) + fadeOut(animationSpec = tween(durationMillis = 220)))
    },
    predictivePopTransitionSpec = {
      (slideInHorizontally(
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> -fullWidth / 10 },
      ) + fadeIn(animationSpec = tween(durationMillis = 240))) togetherWith
        (slideOutHorizontally(
          animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
          targetOffsetX = { fullWidth -> fullWidth / 4 },
        ) + fadeOut(animationSpec = tween(durationMillis = 220)))
    },
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider =
      entryProvider {
        entry<DashboardRoute> {
          val viewModel: DashboardViewModel = viewModel(
            factory = DashboardViewModel.Factory(
              repository = container.habitRepository,
              reminderScheduler = container.reminderScheduler,
            ),
          )
          DashboardScreen(
            viewModel = viewModel,
            onCreateHabit = {
              backStack.add(AddEditHabitRoute(habitId = null, instanceId = System.currentTimeMillis()))
            },
            onEditHabit = { habitId ->
              backStack.add(AddEditHabitRoute(habitId = habitId, instanceId = System.currentTimeMillis()))
            },
            onOpenAllHabits = {
              backStack.add(AllHabitsRoute)
            },
            onOpenHabitDetail = { habitId ->
              backStack.add(HabitDetailRoute(habitId = habitId))
            },
          )
        }

        entry<AllHabitsRoute> {
          val viewModel: AllHabitsViewModel = viewModel(
            factory = AllHabitsViewModel.Factory(
              repository = container.habitRepository,
            ),
          )
          AllHabitsScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() },
            onOpenHabitDetail = { habitId ->
              backStack.add(HabitDetailRoute(habitId = habitId))
            },
            onEditHabit = { habitId ->
              backStack.add(AddEditHabitRoute(habitId = habitId, instanceId = System.currentTimeMillis()))
            },
            onCreateHabit = {
              backStack.add(AddEditHabitRoute(habitId = null, instanceId = System.currentTimeMillis()))
            },
          )
        }

        entry<HabitDetailRoute> { route ->
          val viewModel: HabitDetailViewModel = viewModel(
            key = "habit_detail_${route.habitId}",
            factory = HabitDetailViewModel.Factory(
              habitId = route.habitId,
              repository = container.habitRepository,
            ),
          )
          HabitDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() },
            onEditHabit = { habitId ->
              backStack.add(AddEditHabitRoute(habitId = habitId, instanceId = System.currentTimeMillis()))
            },
          )
        }

        entry<AddEditHabitRoute> { route ->
          val viewModel: AddEditHabitViewModel = viewModel(
            key = "add_edit_${route.habitId ?: "new"}_${route.instanceId}",
            factory = AddEditHabitViewModel.Factory(
              editingHabitId = route.habitId,
              repository = container.habitRepository,
              draftStore = container.draftHabitStore,
              reminderScheduler = container.reminderScheduler,
            ),
          )
          AddEditHabitScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() },
          )
        }
      },
  )
}
