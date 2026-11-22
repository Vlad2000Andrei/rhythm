package com.rhythm.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import com.rhythm.habittracker.MainActivity
import com.rhythm.habittracker.R
import com.rhythm.habittracker.data.local.HabitDatabase
import com.rhythm.habittracker.data.local.toDomain
import com.rhythm.habittracker.domain.model.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

object RhythmWidgetUpdater {

    private val updateMutex = Mutex()

    private val UPCOMING_ROW_IDS = intArrayOf(
        R.id.widget_upcoming_row_1,
        R.id.widget_upcoming_row_2,
        R.id.widget_upcoming_row_3,
        R.id.widget_upcoming_row_4,
        R.id.widget_upcoming_row_5,
        R.id.widget_upcoming_row_6,
    )

    suspend fun updateAllWidgets(context: Context) = withContext(Dispatchers.IO) {
        val manager = AppWidgetManager.getInstance(context)
        val singleIds = manager.getAppWidgetIds(ComponentName(context, HabitCadenceWidgetProvider::class.java))
        val overallIds = manager.getAppWidgetIds(ComponentName(context, OverallCadenceWidgetProvider::class.java))
        val upcomingIds = manager.getAppWidgetIds(ComponentName(context, UpcomingTasksWidgetProvider::class.java))
        if (singleIds.isEmpty() && overallIds.isEmpty() && upcomingIds.isEmpty()) return@withContext

        val (habits, completedByDate) = loadSnapshot(context)
        updateMutex.withLock {
            val today = LocalDate.now()
            singleIds.forEach { id ->
                updateSingleHabitWidgetInternal(context, manager, id, habits, completedByDate, today)
            }
            overallIds.forEach { id ->
                updateOverallWidgetInternal(context, manager, id, habits, completedByDate, today)
            }
            upcomingIds.forEach { id ->
                updateUpcomingWidgetInternal(context, manager, id, habits, completedByDate, today)
            }
        }
    }

    suspend fun updateAllWidgetsWithSnapshot(
        context: Context,
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
    ) = withContext(Dispatchers.IO) {
        val manager = AppWidgetManager.getInstance(context)
        val singleIds = manager.getAppWidgetIds(ComponentName(context, HabitCadenceWidgetProvider::class.java))
        val overallIds = manager.getAppWidgetIds(ComponentName(context, OverallCadenceWidgetProvider::class.java))
        val upcomingIds = manager.getAppWidgetIds(ComponentName(context, UpcomingTasksWidgetProvider::class.java))
        if (singleIds.isEmpty() && overallIds.isEmpty() && upcomingIds.isEmpty()) return@withContext

        updateMutex.withLock {
            val today = LocalDate.now()
            singleIds.forEach { id ->
                updateSingleHabitWidgetInternal(context, manager, id, habits, completedInstancesByDate, today)
            }
            overallIds.forEach { id ->
                updateOverallWidgetInternal(context, manager, id, habits, completedInstancesByDate, today)
            }
            upcomingIds.forEach { id ->
                updateUpcomingWidgetInternal(context, manager, id, habits, completedInstancesByDate, today)
            }
        }
    }

    suspend fun updateSingleHabitWidgets(context: Context, appWidgetIds: IntArray) = withContext(Dispatchers.IO) {
        if (appWidgetIds.isEmpty()) return@withContext
        val manager = AppWidgetManager.getInstance(context)
        val (habits, completedByDate) = loadSnapshot(context)
        updateMutex.withLock {
            val today = LocalDate.now()
            appWidgetIds.forEach { id ->
                updateSingleHabitWidgetInternal(context, manager, id, habits, completedByDate, today)
            }
        }
    }

    suspend fun updateOverallWidgets(context: Context, appWidgetIds: IntArray) = withContext(Dispatchers.IO) {
        if (appWidgetIds.isEmpty()) return@withContext
        val manager = AppWidgetManager.getInstance(context)
        val (habits, completedByDate) = loadSnapshot(context)
        updateMutex.withLock {
            val today = LocalDate.now()
            appWidgetIds.forEach { id ->
                updateOverallWidgetInternal(context, manager, id, habits, completedByDate, today)
            }
        }
    }

    suspend fun updateUpcomingTasksWidgets(context: Context, appWidgetIds: IntArray) = withContext(Dispatchers.IO) {
        if (appWidgetIds.isEmpty()) return@withContext
        val manager = AppWidgetManager.getInstance(context)
        val (habits, completedByDate) = loadSnapshot(context)
        updateMutex.withLock {
            val today = LocalDate.now()
            appWidgetIds.forEach { id ->
                updateUpcomingWidgetInternal(context, manager, id, habits, completedByDate, today)
            }
        }
    }

    private suspend fun loadSnapshot(
        context: Context,
    ): Pair<List<Habit>, Map<LocalDate, Map<Long, Set<Int>>>> {
        val dao = HabitDatabase.getInstance(context).habitDao()
        val habits = dao.getAllHabitsWithDetails().map { it.toDomain() }
        val completions = dao.getAllCompletions()
        val byDate = completions
            .groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
            .mapValues { (_, list) ->
                list.groupBy { it.habitId }
                    .mapValues { (_, entries) -> entries.mapTo(mutableSetOf()) { it.instanceIndex } }
            }
        return habits to byDate
    }

    private fun updateSingleHabitWidgetInternal(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        today: LocalDate,
    ) {
        val themeMode = WidgetPreferencesStore.getThemeMode(context, appWidgetId)
        val configuredHabitId = WidgetPreferencesStore.getHabitId(context, appWidgetId)
        val data = WidgetDataBuilder.buildSingleHabitData(
            habits = habits,
            completedInstancesByDate = completedInstancesByDate,
            configuredHabitId = configuredHabitId,
            today = today,
        )
        val (wDp, hDp) = resolveWidgetSizeDp(context, manager, appWidgetId, 300, 180)
        val bitmap = WidgetBitmapRenderer.renderSingleHabitWidget(context, data, themeMode, wDp, hDp)

        val views = RemoteViews(context.packageName, R.layout.widget_canvas_single)
        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

        val targetHabit = data.habit
        val clickIntent = if (targetHabit != null) {
            openHabitPendingIntent(context, requestCode = 10_000 + appWidgetId, habitId = targetHabit.id)
        } else {
            openDashboardPendingIntent(context, requestCode = 10_000 + appWidgetId)
        }
        views.setOnClickPendingIntent(R.id.widget_canvas_image, clickIntent)
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun updateOverallWidgetInternal(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        today: LocalDate,
    ) {
        val themeMode = WidgetPreferencesStore.getThemeMode(context, appWidgetId)
        val data = WidgetDataBuilder.buildOverallMatrixData(
            habits = habits,
            completedInstancesByDate = completedInstancesByDate,
            today = today,
        )
        val (wDp, hDp) = resolveWidgetSizeDp(context, manager, appWidgetId, 300, 260)
        val bitmap = WidgetBitmapRenderer.renderOverallMatrixWidget(context, data, themeMode, wDp, hDp)

        val views = RemoteViews(context.packageName, R.layout.widget_canvas_single)
        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)
        views.setOnClickPendingIntent(
            R.id.widget_canvas_image,
            openDashboardPendingIntent(context, requestCode = 20_000 + appWidgetId),
        )
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun updateUpcomingWidgetInternal(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        today: LocalDate,
    ) {
        val themeMode = WidgetPreferencesStore.getThemeMode(context, appWidgetId)
        val data = WidgetDataBuilder.buildUpcomingTasksData(
            habits = habits,
            completedInstancesByDate = completedInstancesByDate,
            today = today,
        )
        val (wDp, hDp) = resolveWidgetSizeDp(context, manager, appWidgetId, 300, 280)

        val views = RemoteViews(context.packageName, R.layout.widget_upcoming_tasks)
        val bgRes = if (themeMode == WidgetThemeMode.DARK) {
            R.drawable.widget_card_bg_dark
        } else {
            R.drawable.widget_card_bg_light
        }
        views.setInt(R.id.widget_upcoming_root, "setBackgroundResource", bgRes)

        val headerBmp = WidgetBitmapRenderer.renderUpcomingTasksHeader(
            context = context,
            data = data,
            themeMode = themeMode,
            widthDp = wDp,
            drawBackground = false,
        )
        views.setImageViewBitmap(R.id.widget_upcoming_header, headerBmp)
        views.setOnClickPendingIntent(
            R.id.widget_upcoming_header,
            openDashboardPendingIntent(context, requestCode = 30_000 + appWidgetId),
        )

        val maxRows = WidgetBitmapRenderer.upcomingTasksMaxVisibleRows(hDp)
        val visibleTasks = data.orderedTasks.take(maxRows)
        val availRowsDp = (hDp - 122).coerceAtLeast(48)
        val rowDp = if (visibleTasks.isNotEmpty()) {
            (availRowsDp / visibleTasks.size).coerceIn(38, 48)
        } else {
            54
        }

        if (visibleTasks.isEmpty()) {
            val emptyBmp = WidgetBitmapRenderer.renderUpcomingTasksEmptyRow(context, themeMode, wDp, rowHeightDp = 52)
            views.setViewVisibility(UPCOMING_ROW_IDS[0], View.VISIBLE)
            views.setImageViewBitmap(UPCOMING_ROW_IDS[0], emptyBmp)
            views.setOnClickPendingIntent(
                UPCOMING_ROW_IDS[0],
                openDashboardPendingIntent(context, requestCode = 31_000 + appWidgetId),
            )
            for (i in 1 until UPCOMING_ROW_IDS.size) {
                views.setViewVisibility(UPCOMING_ROW_IDS[i], View.GONE)
            }
        } else {
            for (i in UPCOMING_ROW_IDS.indices) {
                val viewId = UPCOMING_ROW_IDS[i]
                if (i < visibleTasks.size) {
                    val task = visibleTasks[i]
                    val rowBmp = WidgetBitmapRenderer.renderUpcomingTaskRow(
                        context = context,
                        task = task,
                        themeMode = themeMode,
                        widthDp = wDp,
                        rowHeightDp = rowDp,
                    )
                    views.setViewVisibility(viewId, View.VISIBLE)
                    views.setImageViewBitmap(viewId, rowBmp)
                    views.setOnClickPendingIntent(
                        viewId,
                        toggleTaskPendingIntent(
                            context = context,
                            appWidgetId = appWidgetId,
                            rowIndex = i,
                            habitId = task.habitId,
                            instanceIndex = task.instanceIndex,
                        ),
                    )
                } else {
                    views.setViewVisibility(viewId, View.GONE)
                }
            }
        }

        val hiddenCount = (data.orderedTasks.size - maxRows).coerceAtLeast(0)
        val footerBmp = WidgetBitmapRenderer.renderUpcomingTasksFooter(
            context = context,
            data = data,
            themeMode = themeMode,
            widthDp = wDp,
            hiddenExtraCount = hiddenCount,
        )
        views.setImageViewBitmap(R.id.widget_upcoming_footer, footerBmp)
        views.setOnClickPendingIntent(
            R.id.widget_upcoming_footer,
            openDashboardPendingIntent(context, requestCode = 32_000 + appWidgetId),
        )

        manager.updateAppWidget(appWidgetId, views)
    }

    private fun resolveWidgetSizeDp(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        defaultWidthDp: Int,
        defaultHeightDp: Int,
    ): Pair<Int, Int> {
        val options = manager.getAppWidgetOptions(appWidgetId)
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val rawW = if (isLandscape) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        }
        val rawH = if (isLandscape) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        }
        val wDp = if (rawW > 0) rawW.coerceIn(120, 620) else defaultWidthDp
        val hDp = if (rawH > 0) rawH.coerceIn(110, 650) else defaultHeightDp
        return wDp to hDp
    }

    private fun openDashboardPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openHabitPendingIntent(context: Context, requestCode: Int, habitId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_HABIT_ID, habitId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun toggleTaskPendingIntent(
        context: Context,
        appWidgetId: Int,
        rowIndex: Int,
        habitId: Long,
        instanceIndex: Int,
    ): PendingIntent {
        val intent = Intent(context, UpcomingTasksWidgetProvider::class.java).apply {
            action = UpcomingTasksWidgetProvider.ACTION_TOGGLE_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(UpcomingTasksWidgetProvider.EXTRA_HABIT_ID, habitId)
            putExtra(UpcomingTasksWidgetProvider.EXTRA_INSTANCE_INDEX, instanceIndex)
        }
        val requestCode = 40_000 + appWidgetId * 10 + rowIndex
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
