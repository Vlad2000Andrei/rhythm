package com.rhythm.habittracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.ui.theme.ConsistencyEmerald
import com.rhythm.habittracker.ui.theme.RhythmDeepGraphite
import com.rhythm.habittracker.ui.theme.RhythmOnPrimary
import com.rhythm.habittracker.ui.theme.RhythmOnSurfaceVariant
import com.rhythm.habittracker.ui.theme.RhythmPrimary
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainer
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerHigh
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLowest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Geometric Swiss brand mark representing the 2x2 cadence grid.
 */
@Composable
fun RhythmBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(RhythmDeepGraphite)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(RhythmOnPrimary),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(RhythmOnPrimary.copy(alpha = 0.45f)),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(RhythmOnPrimary),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(RhythmOnPrimary),
                )
            }
        }
    }
}

@Composable
fun RhythmProfileAvatar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(RhythmPrimary)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Profile and statistics" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = RhythmOnPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun RhythmTactileSwitch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    showBellIcon: Boolean = false,
    width: Dp = 42.dp,
    height: Dp = 24.dp,
    contentDescriptionText: String = "Toggle",
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) RhythmPrimary else RhythmSurfaceContainerHigh,
        label = "switchTrack",
    )
    val thumbSize = height - 4.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - 2.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "switchThumb",
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = onCheckedChange,
            )
            .semantics { contentDescription = contentDescriptionText },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(RhythmSurfaceContainerLowest)
                .border(0.5.dp, trackColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (showBellIcon) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = if (checked) RhythmPrimary else RhythmPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

private enum class ConfettiShape {
    RIBBON,
    SQUARE,
    CIRCLE,
}

private data class ConfettiParticle(
    val startXFraction: Float,
    val startYFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val swayPhase: Float,
    val baseSizeDp: Float,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val tumbleSpeed: Float,
    val tumblePhase: Float,
    val color: Color,
    val shape: ConfettiShape,
    val delayFraction: Float,
)

/**
 * Non-interactive celebratory confetti burst rendered when the user completes
 * the final scheduled habit task of a day.
 */
@Composable
fun RhythmConfettiOverlay(
    triggerKey: Int,
    modifier: Modifier = Modifier,
) {
    if (triggerKey <= 0) return

    val progress = remember(triggerKey) { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing),
        )
    }

    val currentProgress = progress.value
    if (currentProgress >= 1f) return

    val particles = remember(triggerKey) {
        val rng = Random(triggerKey * 9973L + 42L)
        val palette = HabitAccentColor.entries.map { it.color } + ConsistencyEmerald
        val emitters = listOf(0.22f to 0.20f, 0.50f to 0.16f, 0.78f to 0.20f)

        List(76) { idx ->
            val (originX, originY) = emitters[idx % emitters.size]
            val spreadAngle = (-PI * 0.85 + rng.nextDouble() * PI * 0.70).toFloat()
            val speed = 0.38f + rng.nextFloat() * 0.55f
            ConfettiParticle(
                startXFraction = originX + (rng.nextFloat() - 0.5f) * 0.12f,
                startYFraction = originY + (rng.nextFloat() - 0.5f) * 0.06f,
                velocityX = cos(spreadAngle) * speed * 0.72f,
                velocityY = sin(spreadAngle) * speed - 0.15f,
                swayAmplitude = 0.015f + rng.nextFloat() * 0.03f,
                swayFrequency = 4f + rng.nextFloat() * 6f,
                swayPhase = rng.nextFloat() * (2f * PI.toFloat()),
                baseSizeDp = 6f + rng.nextFloat() * 5.5f,
                initialRotation = rng.nextFloat() * 360f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 720f,
                tumbleSpeed = 5f + rng.nextFloat() * 9f,
                tumblePhase = rng.nextFloat() * (2f * PI.toFloat()),
                color = palette[idx % palette.size],
                shape = ConfettiShape.entries[idx % ConfettiShape.entries.size],
                delayFraction = (idx % 6) * 0.018f,
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height
        val gravity = 1.48f

        particles.forEach { p ->
            val localT = ((currentProgress - p.delayFraction) / (1f - p.delayFraction))
                .coerceIn(0f, 1f)
            if (localT <= 0f || localT >= 1f) return@forEach

            val alpha = when {
                localT < 0.08f -> (localT / 0.08f).coerceIn(0f, 1f)
                localT > 0.68f -> ((1f - localT) / 0.32f).coerceIn(0f, 1f)
                else -> 1f
            }

            val sway = sin(localT * p.swayFrequency + p.swayPhase) * p.swayAmplitude
            val x = (p.startXFraction + p.velocityX * localT + sway) * widthPx
            val y = (p.startYFraction + p.velocityY * localT + 0.5f * gravity * localT * localT) * heightPx

            if (y > heightPx + 40f || x < -40f || x > widthPx + 40f) return@forEach

            val basePx = p.baseSizeDp.dp.toPx()
            val tumbleScale = abs(cos(localT * p.tumbleSpeed + p.tumblePhase)).coerceAtLeast(0.22f)
            val rotation = p.initialRotation + p.rotationSpeed * localT

            withTransform({
                translate(left = x, top = y)
                rotate(degrees = rotation, pivot = Offset.Zero)
            }) {
                when (p.shape) {
                    ConfettiShape.RIBBON -> {
                        val w = basePx * 1.45f * tumbleScale
                        val h = basePx * 0.62f
                        drawRoundRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(-w / 2f, -h / 2f),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        )
                    }

                    ConfettiShape.SQUARE -> {
                        val w = basePx * 0.95f * tumbleScale
                        val h = basePx * 0.95f
                        drawRoundRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(-w / 2f, -h / 2f),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                        )
                    }

                    ConfettiShape.CIRCLE -> {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = (basePx * 0.48f),
                            center = Offset.Zero,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Computes the fill color of a habit completion matrix square proportionally from
 * [completionRatio] (`0f` = uncompleted greyed out, `(0f, 1f)` = interpolated intensity tint,
 * `1f` = maximum [activeColor] intensity).
 */
fun rhythmMatrixCellColor(
    completionRatio: Float,
    activeColor: Color,
    emptyColor: Color = RhythmSurfaceContainer,
    futureColor: Color = emptyColor,
    isFuture: Boolean = false,
): Color {
    if (isFuture) return futureColor
    val clamped = completionRatio.coerceIn(0f, 1f)
    return when {
        clamped <= 0f -> emptyColor
        clamped >= 1f -> activeColor
        else -> activeColor.copy(alpha = 0.24f + 0.52f * clamped)
    }
}

/**
 * Shared completion matrix square rendered across Dashboard, Habit Detail, and future matrix grids.
 *
 * Scales background color intensity proportionally to [completionRatio] and draws a symmetric
 * grey `X` mark when [isUnscheduledEmpty] is true (days with zero scheduled tasks and no completions).
 */
@Composable
fun RhythmMatrixCell(
    completionRatio: Float,
    isUnscheduledEmpty: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    isPausedEmpty: Boolean = false,
    emptyColor: Color = RhythmSurfaceContainer,
    futureColor: Color = emptyColor,
    xColor: Color = RhythmOnSurfaceVariant.copy(alpha = 0.50f),
    pauseColor: Color = RhythmOnSurfaceVariant.copy(alpha = 0.62f),
    isFuture: Boolean = false,
    cornerRadius: Dp = 2.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    animateColor: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val targetColor = rhythmMatrixCellColor(
        completionRatio = completionRatio,
        activeColor = activeColor,
        emptyColor = emptyColor,
        futureColor = futureColor,
        isFuture = isFuture,
    )
    val cellColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = if (animateColor) 280 else 0),
        label = "rhythmMatrixCellColor",
    )
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(cellColor)
            .drawWithContent {
                drawContent()
                if (isPausedEmpty) {
                    val halfHeight = size.minDimension * 0.24f
                    val barOffset = size.minDimension * 0.14f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val strokeWidth = (size.minDimension * 0.14f).coerceAtLeast(1.1.dp.toPx())
                    drawLine(
                        color = pauseColor,
                        start = Offset(cx - barOffset, cy - halfHeight),
                        end = Offset(cx - barOffset, cy + halfHeight),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = pauseColor,
                        start = Offset(cx + barOffset, cy - halfHeight),
                        end = Offset(cx + barOffset, cy + halfHeight),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                } else if (isUnscheduledEmpty) {
                    val halfSpan = size.minDimension * 0.22f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val strokeWidth = (size.minDimension * 0.13f).coerceAtLeast(1.dp.toPx())
                    drawLine(
                        color = xColor,
                        start = Offset(cx - halfSpan, cy - halfSpan),
                        end = Offset(cx + halfSpan, cy + halfSpan),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = xColor,
                        start = Offset(cx + halfSpan, cy - halfSpan),
                        end = Offset(cx - halfSpan, cy + halfSpan),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        width = borderWidth,
                        color = borderColor,
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = !isFuture, onClick = onClick)
                } else {
                    Modifier
                }
            ),
    )
}
