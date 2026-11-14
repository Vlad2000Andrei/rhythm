package com.rhythm.habittracker.ui.addhabit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import com.rhythm.habittracker.ui.components.RhythmBrandMark
import com.rhythm.habittracker.ui.components.RhythmProfileAvatar
import com.rhythm.habittracker.ui.components.RhythmTactileSwitch
import com.rhythm.habittracker.ui.theme.ConsistencyEmerald
import com.rhythm.habittracker.ui.theme.JetBrainsMonoFontFamily
import com.rhythm.habittracker.ui.theme.RhythmDeepGraphite
import com.rhythm.habittracker.ui.theme.RhythmError
import com.rhythm.habittracker.ui.theme.RhythmOnPrimary
import com.rhythm.habittracker.ui.theme.RhythmOnSurface
import com.rhythm.habittracker.ui.theme.RhythmOnSurfaceVariant
import com.rhythm.habittracker.ui.theme.RhythmOutlineVariant
import com.rhythm.habittracker.ui.theme.RhythmPrimary
import com.rhythm.habittracker.ui.theme.RhythmSecondary
import com.rhythm.habittracker.ui.theme.RhythmSurface
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainer
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerHigh
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLow
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLowest
import com.rhythm.habittracker.ui.theme.RhythmTheme
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class TimePickerRequest(
    val slotId: String?, // null = add new slot
    val initialHour: Int,
    val initialMinute: Int,
)

@Composable
fun AddEditHabitScreen(
    viewModel: AddEditHabitViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var commitToastSubtitle by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* User response handled gracefully; habit reminder preference remains saved */ }

    fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditHabitEvent.Committed -> {
                    commitToastSubtitle = event.message
                    delay(650)
                    onNavigateBack()
                }

                AddEditHabitEvent.Deleted -> {
                    onNavigateBack()
                }
            }
        }
    }

    BackHandler {
        viewModel.navigateBack(onNavigateBack)
    }

    AddEditHabitScreenContent(
        uiState = uiState,
        commitToastSubtitle = commitToastSubtitle,
        onBack = { viewModel.navigateBack(onNavigateBack) },
        onTitleChanged = viewModel::onTitleChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onSelectDomain = viewModel::selectDomain,
        onRemoveDomain = viewModel::removeDomainChip,
        onAddCustomDomain = viewModel::addCustomDomain,
        onSelectColor = viewModel::selectColor,
        onSelectCadenceType = viewModel::selectCadenceType,
        onToggleScheduledDay = viewModel::toggleScheduledDay,
        onApplyWeeklyPreset = viewModel::applyWeeklyPreset,
        onSelectWeeklyTargetMode = viewModel::selectWeeklyTargetMode,
        onSelectMonthlySubMode = viewModel::selectMonthlySubMode,
        onApplyMonthlyPreset = viewModel::applyMonthlyPreset,
        onToggleMonthlyDate = viewModel::toggleMonthlyDate,
        onSelectOrdinalWeek = viewModel::selectOrdinalWeek,
        onSelectOrdinalDayOfWeek = viewModel::selectOrdinalDayOfWeek,
        onSelectEndOfMonthBehavior = viewModel::selectEndOfMonthBehavior,
        onSelectCustomArchetype = viewModel::selectCustomArchetype,
        onAdjustCustomInterval = viewModel::adjustCustomInterval,
        onSelectCustomIntervalUnit = viewModel::selectCustomIntervalUnit,
        onAdjustCycleOn = viewModel::adjustCycleOn,
        onAdjustCycleOff = viewModel::adjustCycleOff,
        onSelectCustomStartDate = viewModel::selectCustomStartDate,
        onAdjustTargetPerPeriod = viewModel::adjustTargetPerPeriod,
        onToggleTimeSlotEnabled = { slotId ->
            val target = uiState.targetTimes.firstOrNull { it.id == slotId }
            if (target != null && !target.enabled) {
                ensureNotificationPermissionIfNeeded()
            }
            viewModel.toggleTimeSlotEnabled(slotId)
        },
        onUpdateTimeSlot = viewModel::updateTimeSlot,
        onAddTimeSlot = { hour, minute ->
            ensureNotificationPermissionIfNeeded()
            viewModel.addTimeSlot(hour, minute)
        },
        onRemoveTimeSlot = viewModel::removeTimeSlot,
        onToggleSkipReminder = viewModel::toggleSkipReminderIfCompleted,
        onCommitHabit = {
            if (uiState.targetTimes.any { it.enabled }) {
                ensureNotificationPermissionIfNeeded()
            }
            viewModel.commitHabit()
        },
        onDiscard = { viewModel.discardDraftAndExit(onNavigateBack) },
        onDeleteHabit = viewModel::deleteHabit,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreenContent(
    uiState: AddEditHabitUiState,
    commitToastSubtitle: String?,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSelectDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit,
    onAddCustomDomain: (String) -> Unit,
    onSelectColor: (HabitAccentColor) -> Unit,
    onSelectCadenceType: (CadenceType) -> Unit,
    onToggleScheduledDay: (DayOfWeek) -> Unit,
    onApplyWeeklyPreset: (WeeklyPreset) -> Unit,
    onSelectWeeklyTargetMode: (WeeklyTargetMode) -> Unit,
    onSelectMonthlySubMode: (MonthlySubMode) -> Unit,
    onApplyMonthlyPreset: (MonthlyDatePreset) -> Unit,
    onToggleMonthlyDate: (Int) -> Unit,
    onSelectOrdinalWeek: (Int) -> Unit,
    onSelectOrdinalDayOfWeek: (DayOfWeek) -> Unit,
    onSelectEndOfMonthBehavior: (EndOfMonthBehavior) -> Unit,
    onSelectCustomArchetype: (CustomCadenceArchetype) -> Unit,
    onAdjustCustomInterval: (Int) -> Unit,
    onSelectCustomIntervalUnit: (IntervalUnit) -> Unit,
    onAdjustCycleOn: (Int) -> Unit,
    onAdjustCycleOff: (Int) -> Unit,
    onAdjustTargetPerPeriod: (Int) -> Unit,
    onToggleTimeSlotEnabled: (String) -> Unit,
    onUpdateTimeSlot: (String, Int, Int) -> Unit,
    onAddTimeSlot: (Int, Int) -> Unit,
    onRemoveTimeSlot: (String) -> Unit,
    onToggleSkipReminder: () -> Unit,
    onCommitHabit: () -> Unit,
    onDiscard: () -> Unit,
    onDeleteHabit: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectCustomStartDate: (LocalDate) -> Unit = {},
) {
    val tokens = RhythmTheme.tokens
    var showCustomDomainDialog by remember { mutableStateOf(false) }
    var customDomainInput by remember { mutableStateOf("") }
    var domainPendingDeletion by remember { mutableStateOf<String?>(null) }
    var activeTimePicker by remember { mutableStateOf<TimePickerRequest?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showArchitectureInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RhythmSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AddEditTopHeader(
                isEditing = uiState.isEditing,
                draftSavedIndicator = uiState.draftSavedIndicator,
                onBack = onBack,
                onAvatarClick = { showArchitectureInfoDialog = true },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IdentityAndSignatureCard(
                    uiState = uiState,
                    onTitleChanged = onTitleChanged,
                    onDescriptionChanged = onDescriptionChanged,
                    onSelectDomain = onSelectDomain,
                    onRemoveDomain = { domain -> domainPendingDeletion = domain },
                    onOpenCustomDomainDialog = {
                        customDomainInput = ""
                        showCustomDomainDialog = true
                    },
                    onSelectColor = onSelectColor,
                )

                CadenceEngineCard(
                    uiState = uiState,
                    onSelectCadenceType = onSelectCadenceType,
                    onToggleScheduledDay = onToggleScheduledDay,
                    onApplyWeeklyPreset = onApplyWeeklyPreset,
                    onSelectWeeklyTargetMode = onSelectWeeklyTargetMode,
                    onSelectMonthlySubMode = onSelectMonthlySubMode,
                    onApplyMonthlyPreset = onApplyMonthlyPreset,
                    onToggleMonthlyDate = onToggleMonthlyDate,
                    onSelectOrdinalWeek = onSelectOrdinalWeek,
                    onSelectOrdinalDayOfWeek = onSelectOrdinalDayOfWeek,
                    onSelectEndOfMonthBehavior = onSelectEndOfMonthBehavior,
                    onSelectCustomArchetype = onSelectCustomArchetype,
                    onAdjustCustomInterval = onAdjustCustomInterval,
                    onSelectCustomIntervalUnit = onSelectCustomIntervalUnit,
                    onAdjustCycleOn = onAdjustCycleOn,
                    onAdjustCycleOff = onAdjustCycleOff,
                    onSelectCustomStartDate = onSelectCustomStartDate,
                    onAdjustTargetPerPeriod = onAdjustTargetPerPeriod,
                )

                TargetTimesCard(
                    targetTimes = uiState.targetTimes,
                    skipReminderIfCompleted = uiState.skipReminderIfCompleted,
                    onToggleSlotEnabled = onToggleTimeSlotEnabled,
                    onEditSlot = { slot ->
                        activeTimePicker = TimePickerRequest(
                            slotId = slot.id,
                            initialHour = slot.hour,
                            initialMinute = slot.minute,
                        )
                    },
                    onRemoveSlot = onRemoveTimeSlot,
                    onAddSlotClick = {
                        activeTimePicker = TimePickerRequest(
                            slotId = null,
                            initialHour = 12,
                            initialMinute = 0,
                        )
                    },
                    onToggleSkipReminder = onToggleSkipReminder,
                )

                FooterActionsSection(
                    isEditing = uiState.isEditing,
                    isSaving = uiState.isSaving,
                    onCommit = onCommitHabit,
                    onDiscard = onDiscard,
                    onRequestDelete = { showDeleteConfirmDialog = true },
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedVisibility(
                visible = commitToastSubtitle != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RhythmDeepGraphite,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ConsistencyEmerald),
                        )
                        Column {
                            Text(
                                text = "Habit Architecture Committed · 200 OK",
                                style = tokens.labelMd,
                                fontWeight = FontWeight.SemiBold,
                                color = RhythmOnPrimary,
                            )
                            if (!commitToastSubtitle.isNullOrBlank()) {
                                Text(
                                    text = commitToastSubtitle,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = RhythmOnPrimary.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDomainDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDomainDialog = false },
            containerColor = RhythmSurfaceContainerLowest,
            title = {
                Text(
                    text = "Add Custom Domain",
                    style = tokens.headlineMd,
                    color = RhythmOnSurface,
                )
            },
            text = {
                OutlinedTextField(
                    value = customDomainInput,
                    onValueChange = { customDomainInput = it.take(24) },
                    singleLine = true,
                    placeholder = { Text("e.g., Finance, Language, Recovery") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomDomain(customDomainInput)
                        showCustomDomainDialog = false
                    },
                    enabled = customDomainInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RhythmPrimary,
                        contentColor = RhythmOnPrimary,
                    ),
                ) {
                    Text("Add Domain")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDomainDialog = false }) {
                    Text("Cancel", color = RhythmOnSurfaceVariant)
                }
            },
        )
    }

    activeTimePicker?.let { request ->
        val pickerState = rememberTimePickerState(
            initialHour = request.initialHour,
            initialMinute = request.initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { activeTimePicker = null },
            containerColor = RhythmSurfaceContainerLowest,
            title = {
                Text(
                    text = if (request.slotId == null) "Add Reminder Cue" else "Edit Reminder Cue",
                    style = tokens.headlineMd,
                    color = RhythmOnSurface,
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(
                        state = pickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = RhythmPrimary,
                            timeSelectorSelectedContainerColor = RhythmPrimary,
                            timeSelectorSelectedContentColor = RhythmOnPrimary,
                            timeSelectorUnselectedContainerColor = RhythmSurfaceContainerLow,
                            timeSelectorUnselectedContentColor = RhythmOnSurface,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (request.slotId == null) {
                            onAddTimeSlot(pickerState.hour, pickerState.minute)
                        } else {
                            onUpdateTimeSlot(request.slotId, pickerState.hour, pickerState.minute)
                        }
                        activeTimePicker = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RhythmPrimary,
                        contentColor = RhythmOnPrimary,
                    ),
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeTimePicker = null }) {
                    Text("Cancel", color = RhythmOnSurfaceVariant)
                }
            },
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = RhythmSurfaceContainerLowest,
            title = {
                Text(
                    text = "Delete Habit?",
                    style = tokens.headlineMd,
                    color = RhythmOnSurface,
                )
            },
            text = {
                Text(
                    text = "This permanently removes \"${uiState.title.ifBlank { "this habit" }}\", its streak logs, and scheduled reminders.",
                    style = tokens.bodyMd,
                    color = RhythmOnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteHabit()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RhythmError,
                        contentColor = RhythmOnPrimary,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = RhythmOnSurfaceVariant)
                }
            },
        )
    }

    domainPendingDeletion?.let { domainToDelete ->
        AlertDialog(
            onDismissRequest = { domainPendingDeletion = null },
            containerColor = RhythmSurfaceContainerLowest,
            title = {
                Text(
                    text = "Delete Domain?",
                    style = tokens.headlineMd,
                    color = RhythmOnSurface,
                )
            },
            text = {
                Text(
                    text = "Remove \"$domainToDelete\" from your domains? Habits using this tag will become unassigned.",
                    style = tokens.bodyMd,
                    color = RhythmOnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveDomain(domainToDelete)
                        domainPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RhythmError,
                        contentColor = RhythmOnPrimary,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { domainPendingDeletion = null }) {
                    Text("Cancel", color = RhythmOnSurfaceVariant)
                }
            },
        )
    }

    if (showArchitectureInfoDialog) {
        AlertDialog(
            onDismissRequest = { showArchitectureInfoDialog = false },
            containerColor = RhythmSurfaceContainerLowest,
            title = {
                Text(
                    text = "Habit Architect Spec",
                    style = tokens.headlineMd,
                    color = RhythmOnSurface,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Every field autosaves locally so you can pause mid-design and resume later.",
                        style = tokens.bodyMd,
                        color = RhythmOnSurfaceVariant,
                    )
                    Text(
                        text = "Active cadence: ${uiState.cadenceType.label} · Color: ${uiState.selectedColor.hex}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = RhythmOnSurface,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchitectureInfoDialog = false }) {
                    Text("Done", color = RhythmPrimary)
                }
            },
        )
    }
}

@Composable
private fun AddEditTopHeader(
    isEditing: Boolean,
    draftSavedIndicator: Boolean,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    val tokens = RhythmTheme.tokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RhythmSurface.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to dashboard",
                    tint = RhythmOnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            RhythmBrandMark(size = 24.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isEditing) "Edit Habit" else "New Habit",
                style = tokens.headlineMd,
                fontWeight = FontWeight.SemiBold,
                color = RhythmOnSurface,
                modifier = Modifier.weight(1f),
            )
            RhythmProfileAvatar(onClick = onAvatarClick)
        }
        HorizontalDivider(thickness = 1.dp, color = RhythmSurfaceContainer)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RhythmSurfaceContainerLow.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isEditing) "ARCHITECT / EDIT ENTRY" else "ARCHITECT / NEW ENTRY",
                style = tokens.labelSm,
                fontFamily = JetBrainsMonoFontFamily,
                color = RhythmOnSurfaceVariant,
                letterSpacing = 0.8.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ConsistencyEmerald),
                )
                Text(
                    text = when {
                        isEditing -> "CHANGES AUTOSAVED"
                        draftSavedIndicator -> "DRAFT AUTOSAVED"
                        else -> "NEW DRAFT"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = RhythmOnSurfaceVariant,
                    letterSpacing = 0.6.sp,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = RhythmSurfaceContainer)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityAndSignatureCard(
    uiState: AddEditHabitUiState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSelectDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit,
    onOpenCustomDomainDialog: () -> Unit,
    onSelectColor: (HabitAccentColor) -> Unit,
) {
    val tokens = RhythmTheme.tokens
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Title Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "HABIT TITLE *",
                        style = tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.titleError) RhythmError else RhythmOnSurfaceVariant,
                        letterSpacing = 0.7.sp,
                    )
                    Text(
                        text = "${uiState.title.length}/48",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = RhythmOnSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChanged,
                    singleLine = true,
                    isError = uiState.titleError,
                    placeholder = {
                        Text(
                            text = "e.g., Deep Work, Morning Mobility",
                            style = tokens.bodyLg,
                            color = RhythmOnSurfaceVariant.copy(alpha = 0.55f),
                        )
                    },
                    textStyle = tokens.bodyLg.copy(
                        fontWeight = FontWeight.Medium,
                        color = RhythmOnSurface,
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RhythmSurfaceContainerLow.copy(alpha = 0.6f),
                        unfocusedContainerColor = RhythmSurfaceContainerLow.copy(alpha = 0.6f),
                        errorContainerColor = RhythmError.copy(alpha = 0.04f),
                        focusedBorderColor = RhythmPrimary,
                        unfocusedBorderColor = RhythmSurfaceContainerHigh,
                        errorBorderColor = RhythmError,
                        cursorColor = RhythmPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.titleError) {
                    Text(
                        text = "Habit title is required before committing.",
                        style = tokens.bodySm,
                        color = RhythmError,
                    )
                }
            }

            // Description / Cue Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DESCRIPTION & TRIGGER CUE",
                        style = tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurfaceVariant,
                        letterSpacing = 0.7.sp,
                    )
                    Text(
                        text = "CLEAR & MEASURABLE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = RhythmSecondary,
                        letterSpacing = 0.5.sp,
                    )
                }
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChanged,
                    minLines = 2,
                    maxLines = 4,
                    placeholder = {
                        Text(
                            text = "Optional stacking cue (e.g., After morning espresso, open notes for 90 min)...",
                            style = tokens.bodyMd,
                            color = RhythmOnSurfaceVariant.copy(alpha = 0.55f),
                        )
                    },
                    textStyle = tokens.bodyMd.copy(color = RhythmOnSurface),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RhythmSurfaceContainerLow.copy(alpha = 0.6f),
                        unfocusedContainerColor = RhythmSurfaceContainerLow.copy(alpha = 0.6f),
                        focusedBorderColor = RhythmPrimary,
                        unfocusedBorderColor = RhythmSurfaceContainerHigh,
                        cursorColor = RhythmPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Domains & Tags
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DOMAINS & TAGS",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                    letterSpacing = 0.7.sp,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.availableDomains.forEach { domain ->
                        val selected = domain == uiState.selectedDomain
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) RhythmPrimary else RhythmSurfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            ),
                            modifier = Modifier.clickable { onSelectDomain(domain) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = domain,
                                    style = tokens.labelMd,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove $domain tag",
                                    tint = if (selected) RhythmOnPrimary.copy(alpha = 0.7f) else RhythmOnSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { onRemoveDomain(domain) },
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, RhythmOutlineVariant),
                        modifier = Modifier.clickable(onClick = onOpenCustomDomainDialog),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = RhythmOnSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Custom",
                                style = tokens.labelMd,
                                fontWeight = FontWeight.Medium,
                                color = RhythmOnSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = RhythmSurfaceContainer)

            // Habit Color Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "HABIT COLOR",
                        style = tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurfaceVariant,
                        letterSpacing = 0.7.sp,
                    )
                    Text(
                        text = "Active: ${uiState.selectedColor.displayName} · ${uiState.selectedColor.hex}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = RhythmOnSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HabitAccentColor.entries.forEach { accent ->
                        val isSelected = accent == uiState.selectedColor
                        val swatchScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            label = "swatchScale",
                        )
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = swatchScale
                                    scaleY = swatchScale
                                }
                                .size(38.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, RhythmPrimary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(if (isSelected) 4.dp else 2.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = { onSelectColor(accent) },
                                )
                                .semantics {
                                    contentDescription = "${accent.displayName}${if (isSelected) ", selected" else ""}"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                ) + fadeIn(tween(150)),
                                exit = scaleOut(tween(120)) + fadeOut(tween(120)),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = RhythmOnPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CadenceEngineCard(
    uiState: AddEditHabitUiState,
    onSelectCadenceType: (CadenceType) -> Unit,
    onToggleScheduledDay: (DayOfWeek) -> Unit,
    onApplyWeeklyPreset: (WeeklyPreset) -> Unit,
    onSelectWeeklyTargetMode: (WeeklyTargetMode) -> Unit,
    onSelectMonthlySubMode: (MonthlySubMode) -> Unit,
    onApplyMonthlyPreset: (MonthlyDatePreset) -> Unit,
    onToggleMonthlyDate: (Int) -> Unit,
    onSelectOrdinalWeek: (Int) -> Unit,
    onSelectOrdinalDayOfWeek: (DayOfWeek) -> Unit,
    onSelectEndOfMonthBehavior: (EndOfMonthBehavior) -> Unit,
    onSelectCustomArchetype: (CustomCadenceArchetype) -> Unit,
    onAdjustCustomInterval: (Int) -> Unit,
    onSelectCustomIntervalUnit: (IntervalUnit) -> Unit,
    onAdjustCycleOn: (Int) -> Unit,
    onAdjustCycleOff: (Int) -> Unit,
    onSelectCustomStartDate: (LocalDate) -> Unit,
    onAdjustTargetPerPeriod: (Int) -> Unit,
) {
    val tokens = RhythmTheme.tokens
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "RECURRENCE & FREQUENCY",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                    letterSpacing = 0.7.sp,
                )
                Text(
                    text = "CADENCE ENGINE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = RhythmSecondary,
                    letterSpacing = 0.6.sp,
                )
            }

            // 4-way Segmented Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RhythmSurfaceContainerLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CadenceType.entries.forEach { cadence ->
                    val selected = cadence == uiState.cadenceType
                    val tabBg by animateColorAsState(
                        targetValue = if (selected) RhythmSurfaceContainerLowest else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "cadenceTabBg",
                    )
                    val tabTextColor by animateColorAsState(
                        targetValue = if (selected) RhythmOnSurface else RhythmOnSurfaceVariant,
                        animationSpec = tween(durationMillis = 200),
                        label = "cadenceTabText",
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tabBg,
                        border = if (selected) BorderStroke(1.dp, RhythmSurfaceContainerHigh) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                role = Role.Tab,
                                onClick = { onSelectCadenceType(cadence) },
                            ),
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = cadence.label,
                                style = tokens.labelMd,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = tabTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = uiState.cadenceType,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> direction * (fullWidth / 6) },
                    ) + fadeIn(animationSpec = tween(durationMillis = 220))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> -direction * (fullWidth / 8) },
                        ) + fadeOut(animationSpec = tween(durationMillis = 160)))
                },
                label = "cadenceEngineBody",
            ) { cadenceType ->
                when (cadenceType) {
                    CadenceType.DAILY -> DailyCadenceBody(
                        scheduledDays = uiState.scheduledDays,
                        accentColor = uiState.selectedColor.color,
                        onToggleDay = onToggleScheduledDay,
                    )

                    CadenceType.WEEKLY -> WeeklyCadenceBody(
                        scheduledDays = uiState.scheduledDays,
                        matchingPreset = uiState.matchingWeeklyPreset,
                        targetMode = uiState.weeklyTargetMode,
                        onToggleDay = onToggleScheduledDay,
                        onApplyPreset = onApplyWeeklyPreset,
                        onSelectTargetMode = onSelectWeeklyTargetMode,
                    )

                    CadenceType.MONTHLY -> MonthlyCadenceBody(
                        uiState = uiState,
                        onSelectSubMode = onSelectMonthlySubMode,
                        onApplyPreset = onApplyMonthlyPreset,
                        onToggleDate = onToggleMonthlyDate,
                        onSelectOrdinalWeek = onSelectOrdinalWeek,
                        onSelectOrdinalDay = onSelectOrdinalDayOfWeek,
                        onSelectEndOfMonthBehavior = onSelectEndOfMonthBehavior,
                    )

                    CadenceType.CUSTOM -> CustomCadenceBody(
                        uiState = uiState,
                        onSelectArchetype = onSelectCustomArchetype,
                        onAdjustInterval = onAdjustCustomInterval,
                        onSelectIntervalUnit = onSelectCustomIntervalUnit,
                        onAdjustCycleOn = onAdjustCycleOn,
                        onAdjustCycleOff = onAdjustCycleOff,
                        onSelectStartDate = onSelectCustomStartDate,
                        onAdjustTargetPerPeriod = onAdjustTargetPerPeriod,
                        onToggleDay = onToggleScheduledDay,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyCadenceBody(
    scheduledDays: Set<DayOfWeek>,
    accentColor: Color,
    onToggleDay: (DayOfWeek) -> Unit,
) {
    val tokens = RhythmTheme.tokens
    val letters = listOf(
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "T",
        DayOfWeek.WEDNESDAY to "W",
        DayOfWeek.THURSDAY to "T",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S",
        DayOfWeek.SUNDAY to "S",
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            letters.forEach { (day, letter) ->
                val selected = day in scheduledDays
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) RhythmPrimary else RhythmSurfaceContainerLow)
                        .border(
                            1.dp,
                            if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { onToggleDay(day) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = letter,
                        style = tokens.labelMd,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) accentColor else RhythmOutlineVariant.copy(alpha = 0.5f),
                            ),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(RhythmSurfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (scheduledDays.size == 7) {
                    "7 days / week · Daily selected"
                } else {
                    "${scheduledDays.size} days / week · Custom daily"
                },
                style = tokens.bodyMd,
                fontWeight = FontWeight.Medium,
                color = RhythmOnSurface,
            )
            Text(
                text = "ACTIVE",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConsistencyEmerald,
            )
        }

        Text(
            text = "Cadence automatically bound to Target Quantity & Daily Reminders.",
            style = tokens.bodySm,
            color = RhythmOnSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyCadenceBody(
    scheduledDays: Set<DayOfWeek>,
    matchingPreset: WeeklyPreset?,
    targetMode: WeeklyTargetMode,
    onToggleDay: (DayOfWeek) -> Unit,
    onApplyPreset: (WeeklyPreset) -> Unit,
    onSelectTargetMode: (WeeklyTargetMode) -> Unit,
) {
    val tokens = RhythmTheme.tokens
    val days = listOf(
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat",
        DayOfWeek.SUNDAY to "Sun",
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WEEKDAY SELECTOR",
                style = tokens.labelSm,
                fontWeight = FontWeight.SemiBold,
                color = RhythmOnSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = RhythmSurfaceContainer,
            ) {
                Text(
                    text = "${scheduledDays.size} DAYS SELECTED · ${scheduledDays.size}x / WEEK",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            days.forEach { (day, label) ->
                val selected = day in scheduledDays
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) RhythmPrimary else RhythmSurfaceContainerLow)
                        .border(
                            1.dp,
                            if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { onToggleDay(day) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = label,
                        style = tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) ConsistencyEmerald else RhythmOutlineVariant.copy(alpha = 0.55f),
                            ),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "QUICK PRESETS",
                style = tokens.labelSm,
                fontWeight = FontWeight.SemiBold,
                color = RhythmOnSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WeeklyPreset.entries.forEach { preset ->
                    val isMatched = preset == matchingPreset
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isMatched) RhythmPrimary else RhythmSurfaceContainerLow,
                        border = BorderStroke(
                            1.dp,
                            if (isMatched) RhythmPrimary else RhythmSurfaceContainerHigh,
                        ),
                        modifier = Modifier.clickable { onApplyPreset(preset) },
                    ) {
                        Text(
                            text = preset.label,
                            style = tokens.labelMd,
                            fontWeight = if (isMatched) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isMatched) RhythmOnPrimary else RhythmOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = RhythmSurfaceContainer)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "WEEKLY TARGET MODE",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                )
                Text(
                    text = "ALLOCATION LOGIC",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = RhythmSecondary,
                )
            }

            ArchitecturalRadioOption(
                selected = targetMode == WeeklyTargetMode.FIXED_SCHEDULE,
                title = "Target per selected day",
                subtitle = "Habit appears strictly on the selected weekdays above.",
                onClick = { onSelectTargetMode(WeeklyTargetMode.FIXED_SCHEDULE) },
            )
            ArchitecturalRadioOption(
                selected = targetMode == WeeklyTargetMode.FLEXIBLE_POOL,
                title = "Flexible weekly pool",
                subtitle = "Complete ${scheduledDays.size.coerceAtLeast(1)}x anywhere within Mon–Sun before reset.",
                onClick = { onSelectTargetMode(WeeklyTargetMode.FLEXIBLE_POOL) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthlyCadenceBody(
    uiState: AddEditHabitUiState,
    onSelectSubMode: (MonthlySubMode) -> Unit,
    onApplyPreset: (MonthlyDatePreset) -> Unit,
    onToggleDate: (Int) -> Unit,
    onSelectOrdinalWeek: (Int) -> Unit,
    onSelectOrdinalDay: (DayOfWeek) -> Unit,
    onSelectEndOfMonthBehavior: (EndOfMonthBehavior) -> Unit,
) {
    val tokens = RhythmTheme.tokens

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Monthly Sub-Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(RhythmSurfaceContainerLow)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                MonthlySubMode.SPECIFIC_DATES to "Specific Dates",
                MonthlySubMode.ORDINAL_DAYS to "Ordinal (1st Mon)",
            ).forEach { (mode, title) ->
                val selected = uiState.monthlySubMode == mode
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) RhythmSurfaceContainerLowest else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, RhythmSurfaceContainerHigh) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectSubMode(mode) },
                ) {
                    Text(
                        text = title,
                        style = tokens.labelSm,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) RhythmOnSurface else RhythmOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    )
                }
            }
        }

        if (uiState.monthlySubMode == MonthlySubMode.SPECIFIC_DATES) {
            // Cadence Presets 2x2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CADENCE PRESETS",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                )
                MonthlyDatePreset.entries.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowPresets.forEach { preset ->
                            val active = uiState.monthlyDates == preset.dates
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) RhythmPrimary else RhythmSurfaceContainerLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (active) RhythmPrimary else RhythmSurfaceContainerHigh,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onApplyPreset(preset) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = preset.title,
                                        style = tokens.bodySm,
                                        fontWeight = FontWeight.Medium,
                                        color = if (active) RhythmOnPrimary else RhythmOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = preset.badge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = if (active) RhythmOnPrimary.copy(alpha = 0.8f) else RhythmSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1..31 Calendar Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "MONTHLY CALENDAR MATRIX",
                        style = tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = RhythmSurfaceContainer,
                    ) {
                        Text(
                            text = "${uiState.monthlyDates.size} DATES SELECTED",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RhythmOnSurface,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }

                val rows = (1..31).chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { weekRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            weekRow.forEach { dayNum ->
                                val selected = dayNum in uiState.monthlyDates
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(if (selected) RhythmDeepGraphite else RhythmSurfaceContainerLow)
                                        .border(
                                            1.dp,
                                            if (selected) RhythmDeepGraphite else RhythmSurfaceContainerHigh,
                                            CircleShape,
                                        )
                                        .clickable { onToggleDate(dayNum) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                                    )
                                }
                            }
                            repeat(7 - weekRow.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RhythmSurfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ACTIVE DATES",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurfaceVariant,
                    )
                    Text(
                        text = uiState.activeDatesSummary,
                        style = tokens.bodySm,
                        fontWeight = FontWeight.Medium,
                        color = RhythmOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = RhythmSurfaceContainer)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "END-OF-MONTH SHORT DATES",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                )
                ArchitecturalRadioOption(
                    selected = uiState.endOfMonthBehavior == EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
                    title = "Execute on last day (e.g., Feb 28)",
                    subtitle = "Dates like 30 or 31 gracefully roll back to the month's final day.",
                    onClick = { onSelectEndOfMonthBehavior(EndOfMonthBehavior.EXECUTE_ON_LAST_DAY) },
                )
                ArchitecturalRadioOption(
                    selected = uiState.endOfMonthBehavior == EndOfMonthBehavior.SKIP_OCCURRENCE,
                    title = "Skip occurrence (months < 31 days)",
                    subtitle = "Only fire when the calendar month explicitly contains the date.",
                    onClick = { onSelectEndOfMonthBehavior(EndOfMonthBehavior.SKIP_OCCURRENCE) },
                )
            }
        } else {
            // Ordinal Days Picker
            val weekOptions = listOf(
                1 to "1st",
                2 to "2nd",
                3 to "3rd",
                4 to "4th",
                -1 to "Last",
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "WEEK OCCURRENCE",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    weekOptions.forEach { (value, label) ->
                        val selected = uiState.ordinalWeek == value
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) RhythmPrimary else RhythmSurfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectOrdinalWeek(value) },
                        ) {
                            Text(
                                text = label,
                                style = tokens.labelMd,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp),
                            )
                        }
                    }
                }

                Text(
                    text = "WEEKDAY",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = uiState.ordinalDayOfWeek == day
                        val shortName = day.getDisplayName(TextStyle.SHORT, Locale.US)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) RhythmPrimary else RhythmSurfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            ),
                            modifier = Modifier.clickable { onSelectOrdinalDay(day) },
                        ) {
                            Text(
                                text = shortName,
                                style = tokens.labelMd,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                val ordinalWeekLabel = weekOptions.firstOrNull { it.first == uiState.ordinalWeek }?.second ?: "1st"
                val weekdayLabel = uiState.ordinalDayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RhythmSurfaceContainerLow,
                ) {
                    Text(
                        text = "Executes on the $ordinalWeekLabel $weekdayLabel of every month",
                        style = tokens.bodyMd,
                        fontWeight = FontWeight.Medium,
                        color = RhythmOnSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCadenceBody(
    uiState: AddEditHabitUiState,
    onSelectArchetype: (CustomCadenceArchetype) -> Unit,
    onAdjustInterval: (Int) -> Unit,
    onSelectIntervalUnit: (IntervalUnit) -> Unit,
    onAdjustCycleOn: (Int) -> Unit,
    onAdjustCycleOff: (Int) -> Unit,
    onSelectStartDate: (LocalDate) -> Unit,
    onAdjustTargetPerPeriod: (Int) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
) {
    val tokens = RhythmTheme.tokens
    var showStartDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "CADENCE ARCHETYPE",
            style = tokens.labelSm,
            fontWeight = FontWeight.SemiBold,
            color = RhythmOnSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                CustomCadenceArchetype.INTERVAL to "Interval",
                CustomCadenceArchetype.WEEKLY_FIXED to "Weekly",
                CustomCadenceArchetype.CYCLE_ON_OFF to "Cycle",
            ).forEach { (archetype, label) ->
                val selected = uiState.customArchetype == archetype
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) RhythmPrimary else RhythmSurfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectArchetype(archetype) },
                ) {
                    Text(
                        text = label,
                        style = tokens.labelMd,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 9.dp),
                    )
                }
            }
        }

        when (uiState.customArchetype) {
            CustomCadenceArchetype.INTERVAL -> {
                StepperRow(
                    label = "Repeat Every",
                    valueText = uiState.customInterval.toString(),
                    onDecrement = { onAdjustInterval(-1) },
                    onIncrement = { onAdjustInterval(1) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IntervalUnit.entries.forEach { unit ->
                        val selected = uiState.customIntervalUnit == unit
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) RhythmPrimary else RhythmSurfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectIntervalUnit(unit) },
                        ) {
                            Text(
                                text = unit.label,
                                style = tokens.labelMd,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            CustomCadenceArchetype.WEEKLY_FIXED -> {
                DailyCadenceBody(
                    scheduledDays = uiState.scheduledDays,
                    accentColor = uiState.selectedColor.color,
                    onToggleDay = onToggleDay,
                )
            }

            CustomCadenceArchetype.CYCLE_ON_OFF -> {
                StepperRow(
                    label = "Work Sprint (Days On)",
                    valueText = uiState.cycleDaysOn.toString(),
                    onDecrement = { onAdjustCycleOn(-1) },
                    onIncrement = { onAdjustCycleOn(1) },
                )
                StepperRow(
                    label = "Recovery (Days Off)",
                    valueText = uiState.cycleDaysOff.toString(),
                    onDecrement = { onAdjustCycleOff(-1) },
                    onIncrement = { onAdjustCycleOff(1) },
                )
            }
        }

        // Start Date Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(RhythmSurfaceContainerLow)
                .clickable { showStartDatePicker = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Start Date",
                style = tokens.bodyMd,
                fontWeight = FontWeight.Medium,
                color = RhythmOnSurface,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = RhythmSurfaceContainerLowest,
                border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Choose start date",
                        tint = RhythmOnSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (uiState.anchorDate == today) {
                            "TODAY · ${uiState.anchorDate.format(dateFormatter).uppercase(Locale.ENGLISH)}"
                        } else {
                            uiState.anchorDate.format(dateFormatter).uppercase(Locale.ENGLISH)
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurface,
                    )
                }
            }
        }

        StepperRow(
            label = "Target Per Period",
            valueText = "${uiState.targetPerPeriod}x",
            onDecrement = { onAdjustTargetPerPeriod(-1) },
            onIncrement = { onAdjustTargetPerPeriod(1) },
        )

        // 30-Day Projection Matrix (6 columns x 5 rows = 30 upcoming days)
        val previewHabit = remember(uiState) {
            Habit(
                title = uiState.title.ifBlank { "Preview" },
                cadenceType = CadenceType.CUSTOM,
                scheduledDays = uiState.scheduledDays,
                customArchetype = uiState.customArchetype,
                customInterval = uiState.customInterval,
                customIntervalUnit = uiState.customIntervalUnit,
                cycleDaysOn = uiState.cycleDaysOn,
                cycleDaysOff = uiState.cycleDaysOff,
                anchorDate = uiState.anchorDate,
            )
        }
        val projectionStart = remember(uiState.anchorDate, today) {
            if (uiState.anchorDate.isAfter(today.plusDays(14))) uiState.anchorDate else today
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "30-DAY PROJECTION MATRIX",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                )
                Text(
                    text = if (projectionStart == today) {
                        "LIVE PREVIEW"
                    } else {
                        "FROM ${projectionStart.format(dateFormatter).uppercase(Locale.ENGLISH)}"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = RhythmSecondary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                (0 until 30).chunked(6).forEach { rowIndices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        rowIndices.forEach { offset ->
                            val date = projectionStart.plusDays(offset.toLong())
                            val scheduled = CadenceEvaluator.isHabitScheduledOn(previewHabit, date)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        if (scheduled) uiState.selectedColor.color.copy(alpha = 0.9f)
                                        else RhythmSurfaceContainerLow,
                                    )
                                    .border(
                                        1.dp,
                                        if (scheduled) uiState.selectedColor.color else RhythmSurfaceContainerHigh,
                                        RoundedCornerShape(5.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = if (scheduled) FontWeight.Bold else FontWeight.Normal,
                                    color = if (scheduled) RhythmOnPrimary else RhythmOnSurfaceVariant.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.anchorDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = RhythmSurfaceContainerLowest),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onSelectStartDate(picked)
                        }
                        showStartDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RhythmPrimary,
                        contentColor = RhythmOnPrimary,
                    ),
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = RhythmOnSurfaceVariant)
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = RhythmSurfaceContainerLowest,
                    selectedDayContainerColor = RhythmPrimary,
                    selectedDayContentColor = RhythmOnPrimary,
                    todayDateBorderColor = RhythmPrimary,
                    todayContentColor = RhythmPrimary,
                ),
            )
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val tokens = RhythmTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhythmSurfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = tokens.bodyMd,
            fontWeight = FontWeight.Medium,
            color = RhythmOnSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = RhythmSurfaceContainerLowest,
                border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onDecrement),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease $label",
                        tint = RhythmOnSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = valueText,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = RhythmOnSurface,
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.Center,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = RhythmSurfaceContainerLowest,
                border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onIncrement),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase $label",
                        tint = RhythmOnSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchitecturalRadioOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val tokens = RhythmTheme.tokens
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) RhythmSurfaceContainerLow else RhythmSurfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = RhythmPrimary,
                    unselectedColor = RhythmOutlineVariant,
                ),
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = tokens.bodyMd,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurface,
                )
                Text(
                    text = subtitle,
                    style = tokens.bodySm,
                    color = RhythmOnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TargetTimesCard(
    targetTimes: List<TargetTimeSlot>,
    skipReminderIfCompleted: Boolean,
    onToggleSlotEnabled: (String) -> Unit,
    onEditSlot: (TargetTimeSlot) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onAddSlotClick: () -> Unit,
    onToggleSkipReminder: () -> Unit,
) {
    val tokens = RhythmTheme.tokens
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TARGET TIMES",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                    letterSpacing = 0.7.sp,
                )
                Text(
                    text = "SCHEDULED ${targetTimes.size} ${if (targetTimes.size == 1) "TIME" else "TIMES"} A DAY",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = RhythmSecondary,
                    letterSpacing = 0.5.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                targetTimes.forEach { slot ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RhythmSurfaceContainerLow,
                        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onEditSlot(slot) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    tint = RhythmOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = slot.formatted24WithMeridiem(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (slot.enabled) RhythmOnSurface else RhythmOnSurfaceVariant,
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit time ${slot.formatted24WithMeridiem()}",
                                    tint = RhythmOnSurfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.size(14.dp),
                                )
                            }

                            if (targetTimes.size > 1) {
                                IconButton(
                                    onClick = { onRemoveSlot(slot.id) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove time slot",
                                        tint = RhythmOnSurfaceVariant,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            RhythmTactileSwitch(
                                checked = slot.enabled,
                                onCheckedChange = { onToggleSlotEnabled(slot.id) },
                                showBellIcon = true,
                                contentDescriptionText = "Toggle reminder at ${slot.formatted24WithMeridiem()}",
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onAddSlotClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, RhythmOutlineVariant),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = RhythmOnSurface,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Time",
                    style = tokens.labelMd,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurface,
                )
            }

            HorizontalDivider(thickness = 1.dp, color = RhythmSurfaceContainer)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleSkipReminder),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Skip reminder if already completed",
                        style = tokens.bodyMd,
                        fontWeight = FontWeight.Medium,
                        color = RhythmOnSurface,
                    )
                    Text(
                        text = "Suppresses later notifications once daily check-in is logged.",
                        style = tokens.bodySm,
                        color = RhythmOnSurfaceVariant,
                    )
                }
                RhythmTactileSwitch(
                    checked = skipReminderIfCompleted,
                    onCheckedChange = onToggleSkipReminder,
                    contentDescriptionText = "Skip reminder if already completed",
                )
            }
        }
    }
}

@Composable
private fun FooterActionsSection(
    isEditing: Boolean,
    isSaving: Boolean,
    onCommit: () -> Unit,
    onDiscard: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val tokens = RhythmTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onDiscard,
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = RhythmSurfaceContainerLowest,
                    contentColor = RhythmOnSurface,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Discard",
                    style = tokens.labelMd,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Button(
                onClick = onCommit,
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RhythmPrimary,
                    contentColor = RhythmOnPrimary,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1.6f),
            ) {
                Text(
                    text = when {
                        isSaving -> "Committing..."
                        isEditing -> "Save Changes"
                        else -> "Commit Habit"
                    },
                    style = tokens.labelMd,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (isEditing) {
            TextButton(
                onClick = onRequestDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = RhythmError,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete Habit",
                    style = tokens.labelMd,
                    fontWeight = FontWeight.Medium,
                    color = RhythmError,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1280)
@Composable
fun AddHabitDailyPreview() {
    RhythmTheme {
        AddEditHabitScreenContent(
            uiState = AddEditHabitUiState(
                title = "Deep Work (90 min)",
                cadenceType = CadenceType.DAILY,
            ),
            commitToastSubtitle = null,
            onBack = {},
            onTitleChanged = {},
            onDescriptionChanged = {},
            onSelectDomain = {},
            onRemoveDomain = {},
            onAddCustomDomain = {},
            onSelectColor = {},
            onSelectCadenceType = {},
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

@Preview(showBackground = true, widthDp = 412, heightDp = 1040)
@Composable
fun AddHabitWeeklyPreview() {
    RhythmTheme {
        AddEditHabitScreenContent(
            uiState = AddEditHabitUiState(
                title = "Strength & Mobility",
                selectedDomain = "Fitness",
                selectedColor = HabitAccentColor.EMERALD_GREEN,
                cadenceType = CadenceType.WEEKLY,
                scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            ),
            commitToastSubtitle = null,
            onBack = {},
            onTitleChanged = {},
            onDescriptionChanged = {},
            onSelectDomain = {},
            onRemoveDomain = {},
            onAddCustomDomain = {},
            onSelectColor = {},
            onSelectCadenceType = {},
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

@Preview(showBackground = true, widthDp = 412, heightDp = 1120)
@Composable
fun AddHabitMonthlyPreview() {
    RhythmTheme {
        AddEditHabitScreenContent(
            uiState = AddEditHabitUiState(
                title = "Monthly Architectural Review",
                selectedDomain = "Focus",
                selectedColor = HabitAccentColor.SLATE_GRAPHITE,
                cadenceType = CadenceType.MONTHLY,
                monthlySubMode = MonthlySubMode.SPECIFIC_DATES,
                monthlyDates = setOf(1, 15, 30),
                targetTimes = listOf(
                    TargetTimeSlot(id = "m_1", hour = 9, minute = 0, enabled = true),
                    TargetTimeSlot(id = "m_2", hour = 20, minute = 0, enabled = false),
                ),
            ),
            commitToastSubtitle = null,
            onBack = {},
            onTitleChanged = {},
            onDescriptionChanged = {},
            onSelectDomain = {},
            onRemoveDomain = {},
            onAddCustomDomain = {},
            onSelectColor = {},
            onSelectCadenceType = {},
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
