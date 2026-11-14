package com.rhythm.habittracker.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.rhythm.habittracker.R
import com.rhythm.habittracker.domain.MatrixCell
import com.rhythm.habittracker.domain.MatrixMonthLabel
import com.rhythm.habittracker.domain.model.HabitAccentColor
import java.time.LocalDate
import kotlin.math.roundToInt

object WidgetBitmapRenderer {

    private data class FontSet(
        val interRegular: Typeface,
        val interMedium: Typeface,
        val interSemiBold: Typeface,
        val interBold: Typeface,
        val monoRegular: Typeface,
        val monoMedium: Typeface,
        val monoSemiBold: Typeface,
        val monoBold: Typeface,
    )

    private data class WidgetPalette(
        val cardBg: Int,
        val cardBorder: Int,
        val subCardBg: Int,
        val subCardBorder: Int,
        val activeTaskBg: Int,
        val activeTaskBorder: Int,
        val completedTaskBg: Int,
        val completedTaskBorder: Int,
        val inkPrimary: Int,
        val inkSecondary: Int,
        val inkMuted: Int,
        val divider: Int,
        val progressTrack: Int,
        val progressFill: Int,
        val matrixEmpty: Int,
        val matrixX: Int,
        val checkboxBorder: Int,
        val checkboxBg: Int,
        val nowBadgeBg: Int,
        val nowBadgeText: Int,
        val emerald: Int,
    )

    private fun loadFonts(context: Context): FontSet {
        fun load(resId: Int, fallback: Typeface): Typeface =
            runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: fallback

        val sans = Typeface.SANS_SERIF
        val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val mono = Typeface.MONOSPACE
        val monoBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        return FontSet(
            interRegular = load(R.font.inter_regular, sans),
            interMedium = load(R.font.inter_medium, sans),
            interSemiBold = load(R.font.inter_semibold, sansBold),
            interBold = load(R.font.inter_bold, sansBold),
            monoRegular = load(R.font.jetbrains_mono_regular, mono),
            monoMedium = load(R.font.jetbrains_mono_medium, mono),
            monoSemiBold = load(R.font.jetbrains_mono_semibold, monoBold),
            monoBold = load(R.font.jetbrains_mono_bold, monoBold),
        )
    }

    private fun paletteFor(mode: WidgetThemeMode): WidgetPalette = when (mode) {
        WidgetThemeMode.LIGHT -> WidgetPalette(
            cardBg = 0xFFFFFFFF.toInt(),
            cardBorder = 0xFFE7E8EA.toInt(),
            subCardBg = 0xFFF5F6F8.toInt(),
            subCardBorder = 0xFFEDEEF0.toInt(),
            activeTaskBg = 0xFFFFFFFF.toInt(),
            activeTaskBorder = 0xFFD9DADC.toInt(),
            completedTaskBg = 0xFFF6F7F9.toInt(),
            completedTaskBorder = 0xFFECEEF1.toInt(),
            inkPrimary = 0xFF191C1E.toInt(),
            inkSecondary = 0xFF575F67.toInt(),
            inkMuted = 0xFF8A8E93.toInt(),
            divider = 0xFFEDEEF0.toInt(),
            progressTrack = 0xFFE5E7EB.toInt(),
            progressFill = 0xFF121417.toInt(),
            matrixEmpty = 0xFFEDEEF0.toInt(),
            matrixX = 0x8045474A.toInt(),
            checkboxBorder = 0xFFC6C6CA.toInt(),
            checkboxBg = 0xFFFFFFFF.toInt(),
            nowBadgeBg = 0xFFEFF6FF.toInt(),
            nowBadgeText = 0xFF1D4ED8.toInt(),
            emerald = 0xFF10B981.toInt(),
        )

        WidgetThemeMode.DARK -> WidgetPalette(
            cardBg = 0xFF121417.toInt(),
            cardBorder = 0xFF262A30.toInt(),
            subCardBg = 0xFF1B1E23.toInt(),
            subCardBorder = 0xFF2A2E35.toInt(),
            activeTaskBg = 0xFF1D2127.toInt(),
            activeTaskBorder = 0xFF323842.toInt(),
            completedTaskBg = 0xFF171A1F.toInt(),
            completedTaskBorder = 0xFF242830.toInt(),
            inkPrimary = 0xFFF3F4F6.toInt(),
            inkSecondary = 0xFFA1A7B0.toInt(),
            inkMuted = 0xFF707680.toInt(),
            divider = 0xFF252930.toInt(),
            progressTrack = 0xFF2A2F38.toInt(),
            progressFill = 0xFFF3F4F6.toInt(),
            matrixEmpty = 0xFF242830.toInt(),
            matrixX = 0x889CA3AF.toInt(),
            checkboxBorder = 0xFF4B5563.toInt(),
            checkboxBg = 0xFF121417.toInt(),
            nowBadgeBg = 0xFF1E3A5F.toInt(),
            nowBadgeText = 0xFF93C5FD.toInt(),
            emerald = 0xFF34D399.toInt(),
        )
    }

    private fun computeScale(context: Context, widthDp: Int, heightDp: Int): Float {
        val deviceDensity = context.resources.displayMetrics.density.coerceIn(1.5f, 3.0f)
        val maxSideDp = maxOf(widthDp, heightDp).coerceAtLeast(100)
        return minOf(deviceDensity, 960f / maxSideDp).coerceIn(1.25f, 2.75f)
    }

    private fun parseHexColor(hex: String?, fallback: Int = 0xFF2563EB.toInt()): Int {
        if (hex.isNullOrBlank()) return fallback
        return runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
    }

    private fun drawCardSurface(
        canvas: Canvas,
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        palette: WidgetPalette,
        scale: Float,
    ) {
        val rect = RectF(0.5f * scale, 0.5f * scale, widthPx - 0.5f * scale, heightPx - 0.5f * scale)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.cardBg
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, fillPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.cardBorder
            style = Paint.Style.STROKE
            strokeWidth = 1.0f * scale
        }
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, borderPaint)
    }

    private fun textPaint(
        typeface: Typeface,
        sizePx: Float,
        color: Int,
        align: Paint.Align = Paint.Align.LEFT,
        letterSpacing: Float = 0f,
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.textSize = sizePx
        this.color = color
        this.textAlign = align
        this.letterSpacing = letterSpacing
    }

    private fun ellipsize(text: String, paint: TextPaint, maxWidthPx: Float): String {
        if (maxWidthPx <= 0f) return ""
        return TextUtils.ellipsize(text, paint, maxWidthPx, TextUtils.TruncateAt.END).toString()
    }

    private fun drawFlameIcon(canvas: Canvas, cx: Float, cy: Float, sizePx: Float, color: Int) {
        val r = sizePx / 2f
        val path = Path().apply {
            moveTo(cx, cy - r)
            cubicTo(cx + r * 0.65f, cy - r * 0.35f, cx + r * 0.9f, cy + r * 0.15f, cx + r * 0.55f, cy + r * 0.68f)
            cubicTo(cx + r * 0.3f, cy + r, cx - r * 0.35f, cy + r, cx - r * 0.6f, cy + r * 0.62f)
            cubicTo(cx - r * 0.88f, cy + r * 0.2f, cx - r * 0.55f, cy - r * 0.35f, cx - r * 0.12f, cy - r * 0.62f)
            cubicTo(cx - r * 0.08f, cy - r * 0.25f, cx + r * 0.12f, cy - r * 0.15f, cx + r * 0.18f, cy - r * 0.42f)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)
    }

    private fun drawMatrixGridIcon(canvas: Canvas, left: Float, top: Float, sizePx: Float, color: Int, scale: Float) {
        val gap = sizePx * 0.16f
        val cell = (sizePx - gap) / 2f
        val radius = 1.5f * scale
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1.3f * scale
        }
        canvas.drawRoundRect(RectF(left, top, left + cell, top + cell), radius, radius, paint)
        canvas.drawRoundRect(RectF(left + cell + gap, top, left + sizePx, top + cell), radius, radius, paint)
        canvas.drawRoundRect(RectF(left, top + cell + gap, left + cell, top + sizePx), radius, radius, paint)
        canvas.drawRoundRect(RectF(left + cell + gap, top + cell + gap, left + sizePx, top + sizePx), radius, radius, paint)
    }

    private fun drawIntensityLegend(
        canvas: Canvas,
        rightX: Float,
        centerY: Float,
        activeColor: Int,
        palette: WidgetPalette,
        fonts: FontSet,
        scale: Float,
        labelSizeSp: Float = 8.5f,
    ) {
        val labelPaint = textPaint(fonts.monoMedium, labelSizeSp * scale, palette.inkMuted, letterSpacing = 0.04f)
        val moreText = "MORE"
        val lessText = "LESS"
        val moreW = labelPaint.measureText(moreText)
        val lessW = labelPaint.measureText(lessText)
        val box = 6.5f * scale
        val boxGap = 2.5f * scale
        val textGap = 4f * scale

        val baselineY = centerY - (labelPaint.descent() + labelPaint.ascent()) / 2f
        canvas.drawText(moreText, rightX - moreW, baselineY, labelPaint)

        val shades = listOf(
            palette.matrixEmpty,
            ColorUtils.blendARGB(palette.cardBg, activeColor, 0.32f),
            ColorUtils.blendARGB(palette.cardBg, activeColor, 0.62f),
            activeColor,
        )
        val boxesTotalW = shades.size * box + (shades.size - 1) * boxGap
        val boxesStartX = rightX - moreW - textGap - boxesTotalW
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val corner = 1.6f * scale

        shades.forEachIndexed { idx, shadeColor ->
            val bx = boxesStartX + idx * (box + boxGap)
            boxPaint.color = shadeColor
            canvas.drawRoundRect(
                RectF(bx, centerY - box / 2f, bx + box, centerY + box / 2f),
                corner,
                corner,
                boxPaint,
            )
        }

        canvas.drawText(lessText, boxesStartX - textGap - lessW, baselineY, labelPaint)
    }

    private fun matrixCellColor(
        cell: MatrixCell,
        activeColor: Int,
        palette: WidgetPalette,
    ): Int {
        if (cell.isFuture) return palette.matrixEmpty
        val ratio = cell.completionRatio.coerceIn(0f, 1f)
        return when {
            ratio <= 0f -> palette.matrixEmpty
            ratio >= 1f -> activeColor
            else -> ColorUtils.blendARGB(palette.cardBg, activeColor, 0.28f + 0.52f * ratio)
        }
    }

    private fun drawMatrixGrid(
        canvas: Canvas,
        bounds: RectF,
        columns: List<List<MatrixCell>>,
        monthLabels: List<MatrixMonthLabel>,
        today: LocalDate,
        activeColor: Int,
        palette: WidgetPalette,
        fonts: FontSet,
        scale: Float,
        showMonthLabels: Boolean,
    ) {
        if (columns.isEmpty() || bounds.width() <= 0f || bounds.height() <= 0f) return

        val monthRowH = if (showMonthLabels && monthLabels.isNotEmpty()) 13f * scale else 0f
        val dayColW = 11f * scale
        val dayToGridGap = 5f * scale

        val availGridW = (bounds.width() - dayColW - dayToGridGap).coerceAtLeast(24f * scale)
        val availGridH = (bounds.height() - monthRowH).coerceAtLeast(24f * scale)

        val gapY = (availGridH * 0.045f).coerceIn(2f * scale, 4f * scale)
        val maxCellFromHeight = ((availGridH - 6f * gapY) / 7f).coerceAtLeast(5f * scale)
        val targetCell = maxCellFromHeight.coerceIn(7f * scale, 15f * scale)
        val gapX = gapY

        val maxColsFit = ((availGridW + gapX) / (targetCell + gapX)).toInt().coerceIn(5, columns.size)
        val startColIndex = (columns.size - maxColsFit).coerceAtLeast(0)
        val visibleCols = columns.subList(startColIndex, columns.size)
        val colCount = visibleCols.size

        val cellByWidth = ((availGridW - (colCount - 1) * gapX) / colCount).coerceAtLeast(5f * scale)
        val cellSize = minOf(maxCellFromHeight, cellByWidth)
        val totalGridW = colCount * cellSize + (colCount - 1) * gapX
        val totalGridH = 7 * cellSize + 6 * gapY

        val gridLeft = bounds.left + dayColW + dayToGridGap + ((availGridW - totalGridW).coerceAtLeast(0f) / 2f)
        val gridTop = bounds.top + monthRowH + ((availGridH - totalGridH).coerceAtLeast(0f) / 2f)

        // Draw Month Labels if requested
        if (monthRowH > 0f) {
            val monthPaint = textPaint(fonts.monoMedium, 8.5f * scale, palette.inkMuted)
            val baseline = bounds.top + monthRowH - 4f * scale
            var lastLabelRight = -1f
            monthLabels.forEach { entry ->
                val relIndex = entry.weekColumnIndex - startColIndex
                if (relIndex in 0 until colCount) {
                    val labelX = gridLeft + relIndex * (cellSize + gapX)
                    val labelW = monthPaint.measureText(entry.label)
                    if (labelX > lastLabelRight + 6f * scale && labelX + labelW <= bounds.right + 2f * scale) {
                        canvas.drawText(entry.label, labelX, baseline, monthPaint)
                        lastLabelRight = labelX + labelW
                    }
                }
            }
        }

        // Draw all 7 weekday initials M T W T F S S
        val letters = listOf("M", "T", "W", "T", "F", "S", "S")
        val dayFontPx = (cellSize * 0.72f).coerceIn(7.5f * scale, 9.5f * scale)
        val dayPaint = textPaint(fonts.monoMedium, dayFontPx, palette.inkMuted, Paint.Align.CENTER)
        val dayX = bounds.left + dayColW / 2f
        val dayTextDy = -(dayPaint.descent() + dayPaint.ascent()) / 2f
        for (row in 0 until 7) {
            val cy = gridTop + row * (cellSize + gapY) + cellSize / 2f
            canvas.drawText(letters[row], dayX, cy + dayTextDy, dayPaint)
        }

        // Draw matrix cells
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.matrixX
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = (cellSize * 0.14f).coerceAtLeast(1.1f * scale)
        }
        val todayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeColor
            style = Paint.Style.STROKE
            strokeWidth = (1.5f * scale).coerceAtLeast(1.2f)
        }
        val corner = (cellSize * 0.22f).coerceAtLeast(1.5f * scale)

        visibleCols.forEachIndexed { colIdx, weekCells ->
            val x = gridLeft + colIdx * (cellSize + gapX)
            for (row in 0 until minOf(7, weekCells.size)) {
                val cell = weekCells[row]
                val y = gridTop + row * (cellSize + gapY)
                val rect = RectF(x, y, x + cellSize, y + cellSize)

                cellPaint.color = matrixCellColor(cell, activeColor, palette)
                canvas.drawRoundRect(rect, corner, corner, cellPaint)

                if (cell.isUnscheduledEmpty) {
                    val halfSpan = cellSize * 0.22f
                    val cx = rect.centerX()
                    val cy = rect.centerY()
                    canvas.drawLine(cx - halfSpan, cy - halfSpan, cx + halfSpan, cy + halfSpan, xPaint)
                    canvas.drawLine(cx + halfSpan, cy - halfSpan, cx - halfSpan, cy + halfSpan, xPaint)
                }

                if (cell.date == today) {
                    val inset = 0.6f * scale
                    val ringRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
                    canvas.drawRoundRect(ringRect, corner, corner, todayRingPaint)
                }
            }
        }
    }

    // ============================================================================================
    // WIDGET 1: SINGLE HABIT CADENCE MATRIX
    // ============================================================================================

    fun renderSingleHabitWidget(
        context: Context,
        data: SingleHabitWidgetData,
        themeMode: WidgetThemeMode,
        widthDp: Int,
        heightDp: Int,
    ): Bitmap {
        val safeW = widthDp.coerceAtLeast(120)
        val safeH = heightDp.coerceAtLeast(120)
        val scale = computeScale(context, safeW, safeH)
        val wPx = (safeW * scale).roundToInt().coerceAtLeast(180)
        val hPx = (safeH * scale).roundToInt().coerceAtLeast(180)

        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(themeMode)
        val fonts = loadFonts(context)
        val activeColor = parseHexColor(data.habit?.colorHex, HabitAccentColor.ROYAL_BLUE.colorValue.toInt())

        drawCardSurface(canvas, wPx.toFloat(), hPx.toFloat(), 26f * scale, palette, scale)

        val isTall = safeW < 235 && safeH >= 210
        val isCompactWide = safeW >= 235 && safeH < 235

        when {
            isTall -> drawSingleHabitTall(canvas, wPx.toFloat(), hPx.toFloat(), data, activeColor, palette, fonts, scale)
            isCompactWide -> drawSingleHabitWide(canvas, wPx.toFloat(), hPx.toFloat(), data, activeColor, palette, fonts, scale)
            else -> drawSingleHabitSquare(canvas, wPx.toFloat(), hPx.toFloat(), data, activeColor, palette, fonts, scale)
        }
        return bitmap
    }

    private fun drawSingleHabitTall(
        canvas: Canvas,
        w: Float,
        h: Float,
        data: SingleHabitWidgetData,
        activeColor: Int,
        palette: WidgetPalette,
        fonts: FontSet,
        scale: Float,
    ) {
        val pad = 16f * scale
        val contentLeft = pad
        val contentRight = w - pad
        val contentW = contentRight - contentLeft
        var y = pad

        // Top row: dot + domain tag | streak pill
        val streakStr = data.currentStreak.toString()
        val streakTextP = textPaint(fonts.monoBold, 10.5f * scale, palette.inkPrimary)
        val pillPadH = 7f * scale
        val pillH = 18f * scale
        val flameS = 9.5f * scale
        val pillW = pillPadH * 2f + flameS + 4f * scale + streakTextP.measureText(streakStr)
        val pillRect = RectF(contentRight - pillW, y, contentRight, y + pillH)
        canvas.drawRoundRect(
            pillRect,
            pillH / 2f,
            pillH / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg },
        )
        canvas.drawRoundRect(
            pillRect,
            pillH / 2f,
            pillH / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.subCardBorder
                style = Paint.Style.STROKE
                strokeWidth = 0.9f * scale
            },
        )
        drawFlameIcon(canvas, pillRect.left + pillPadH + flameS / 2f, pillRect.centerY(), flameS, activeColor)
        val pillTextY = pillRect.centerY() - (streakTextP.descent() + streakTextP.ascent()) / 2f
        canvas.drawText(streakStr, pillRect.left + pillPadH + flameS + 4f * scale, pillTextY, streakTextP)

        val dotR = 3.5f * scale
        val dotCy = pillRect.centerY()
        canvas.drawCircle(contentLeft + dotR, dotCy, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = activeColor })
        val badgePaint = textPaint(fonts.monoMedium, 9f * scale, palette.inkSecondary, letterSpacing = 0.05f)
        val badgeText = ellipsize(data.domainBadgeLabel, badgePaint, contentW - pillW - dotR * 2f - 10f * scale)
        canvas.drawText(
            badgeText,
            contentLeft + dotR * 2f + 5f * scale,
            dotCy - (badgePaint.descent() + badgePaint.ascent()) / 2f,
            badgePaint,
        )
        y += pillH + 10f * scale

        // Title & Subtitle
        val titleText = data.habit?.title ?: "Select Habit"
        val titlePaint = textPaint(fonts.interBold, 17f * scale, palette.inkPrimary)
        canvas.drawText(ellipsize(titleText, titlePaint, contentW), contentLeft, y - titlePaint.ascent(), titlePaint)
        y += titlePaint.fontSpacing + 2f * scale

        val subPaint = textPaint(fonts.interMedium, 10.5f * scale, palette.inkSecondary)
        canvas.drawText(ellipsize(data.subtitleLabel, subPaint, contentW), contentLeft, y - subPaint.ascent(), subPaint)
        y += subPaint.fontSpacing + 10f * scale

        // Divider + 2 compact stats rows (Rate & Target)
        val divPaint = Paint().apply {
            color = palette.divider
            strokeWidth = 1f * scale
        }
        canvas.drawLine(contentLeft, y, contentRight, y, divPaint)
        y += 8f * scale

        val keyP = textPaint(fonts.monoMedium, 9f * scale, palette.inkMuted, letterSpacing = 0.06f)
        val valRateP = textPaint(fonts.monoBold, 11f * scale, palette.inkPrimary, Paint.Align.RIGHT)
        val valTargetP = textPaint(fonts.monoSemiBold, 11f * scale, activeColor, Paint.Align.RIGHT)

        canvas.drawText("RATE", contentLeft, y - keyP.ascent(), keyP)
        canvas.drawText("${data.consistencyPercent}%", contentRight, y - valRateP.ascent(), valRateP)
        y += maxOf(keyP.fontSpacing, valRateP.fontSpacing) + 4f * scale

        canvas.drawText("TARGET", contentLeft, y - keyP.ascent(), keyP)
        canvas.drawText(data.primaryTargetLabel, contentRight, y - valTargetP.ascent(), valTargetP)
        y += maxOf(keyP.fontSpacing, valTargetP.fontSpacing) + 12f * scale

        // Matrix section
        val headerH = 14f * scale
        val headerLabelP = textPaint(fonts.monoMedium, 8.5f * scale, palette.inkMuted, letterSpacing = 0.06f)
        canvas.drawText("CADENCE MATRIX", contentLeft, y + headerH * 0.72f, headerLabelP)
        y += headerH + 6f * scale

        val footerH = if (h > y + 115f * scale) 20f * scale else 0f
        val matrixBottom = h - pad - footerH - if (footerH > 0f) 6f * scale else 0f
        drawMatrixGrid(
            canvas = canvas,
            bounds = RectF(contentLeft, y, contentRight, matrixBottom),
            columns = data.matrixColumns,
            monthLabels = emptyList(),
            today = data.today,
            activeColor = activeColor,
            palette = palette,
            fonts = fonts,
            scale = scale,
            showMonthLabels = false,
        )

        if (footerH > 0f) {
            val fy = h - pad - footerH
            canvas.drawLine(contentLeft, fy, contentRight, fy, divPaint)
            val statusPaint = textPaint(fonts.interMedium, 10f * scale, palette.inkSecondary)
            val dotColor = if (data.isCompletedToday) palette.emerald else activeColor
            val cy = fy + footerH * 0.65f
            canvas.drawCircle(contentLeft + 3f * scale, cy, 2.8f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor })
            canvas.drawText(
                ellipsize(data.statusLabel, statusPaint, contentW - 10f * scale),
                contentLeft + 10f * scale,
                cy - (statusPaint.descent() + statusPaint.ascent()) / 2f,
                statusPaint,
            )
        }
    }

    private fun drawSingleHabitWide(
        canvas: Canvas,
        w: Float,
        h: Float,
        data: SingleHabitWidgetData,
        activeColor: Int,
        palette: WidgetPalette,
        fonts: FontSet,
        scale: Float,
    ) {
        val padH = 18f * scale
        val padV = 14f * scale
        val left = padH
        val right = w - padH
        var y = padV

        // Streak pill on top right
        val streakStr = data.currentStreak.toString()
        val numP = textPaint(fonts.monoBold, 14f * scale, activeColor)
        val lblP = textPaint(fonts.monoSemiBold, 7.8f * scale, palette.inkMuted, Paint.Align.RIGHT, letterSpacing = 0.05f)
        val flameS = 11f * scale
        val topRowContentW = flameS + 4f * scale + numP.measureText(streakStr)
        val pillW = maxOf(topRowContentW + 16f * scale, lblP.measureText("DAYS ACTIVE") + 16f * scale)
        val pillH = 34f * scale
        val pillRect = RectF(right - pillW, y, right, y + pillH)
        canvas.drawRoundRect(pillRect, 11f * scale, 11f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg })
        canvas.drawRoundRect(
            pillRect,
            11f * scale,
            11f * scale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.subCardBorder
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            },
        )
        val numBaseline = pillRect.top + 16f * scale
        drawFlameIcon(
            canvas,
            pillRect.right - 8f * scale - numP.measureText(streakStr) - 4f * scale - flameS / 2f,
            numBaseline - 4.5f * scale,
            flameS,
            activeColor,
        )
        numP.textAlign = Paint.Align.RIGHT
        canvas.drawText(streakStr, pillRect.right - 8f * scale, numBaseline, numP)
        canvas.drawText("DAYS ACTIVE", pillRect.right - 8f * scale, pillRect.bottom - 6f * scale, lblP)

        // Left title stack
        val titleMaxW = right - left - pillW - 12f * scale
        val dotR = 3f * scale
        val dotCy = y + 6f * scale
        canvas.drawCircle(left + dotR, dotCy, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = activeColor })
        val badgeP = textPaint(fonts.monoMedium, 9f * scale, palette.inkSecondary, letterSpacing = 0.05f)
        canvas.drawText(
            ellipsize(data.domainBadgeLabel, badgeP, titleMaxW - 12f * scale),
            left + dotR * 2f + 5f * scale,
            dotCy - (badgeP.descent() + badgeP.ascent()) / 2f,
            badgeP,
        )
        val titleP = textPaint(fonts.interBold, 17f * scale, palette.inkPrimary)
        val titleY = y + 12f * scale - titleP.ascent()
        canvas.drawText(ellipsize(data.habit?.title ?: "Select Habit", titleP, titleMaxW), left, titleY, titleP)

        val subP = textPaint(fonts.interRegular, 10f * scale, palette.inkSecondary)
        canvas.drawText(
            ellipsize(data.subtitleLabel, subP, titleMaxW),
            left,
            titleY + titleP.descent() - subP.ascent() + 1f * scale,
            subP,
        )

        y = maxOf(pillRect.bottom, titleY + subP.fontSpacing + 4f * scale) + 8f * scale

        // Activity header + legend
        val actP = textPaint(fonts.monoSemiBold, 9f * scale, palette.inkSecondary, letterSpacing = 0.05f)
        val colsEst = ((right - left) / (12f * scale)).toInt().coerceIn(8, data.matrixColumns.size)
        canvas.drawText("ACTIVITY · PAST $colsEst WEEKS", left, y + 8f * scale, actP)
        drawIntensityLegend(canvas, right, y + 5f * scale, activeColor, palette, fonts, scale, 8f)
        y += 13f * scale

        val divPaint = Paint().apply {
            color = palette.divider
            strokeWidth = 1f * scale
        }
        canvas.drawLine(left, y, right, y, divPaint)
        y += 6f * scale

        val showFooter = h >= 190f * scale
        val footerH = if (showFooter) 22f * scale else 0f
        val matrixBottom = h - padV - footerH

        drawMatrixGrid(
            canvas = canvas,
            bounds = RectF(left, y, right, matrixBottom),
            columns = data.matrixColumns,
            monthLabels = emptyList(),
            today = data.today,
            activeColor = activeColor,
            palette = palette,
            fonts = fonts,
            scale = scale,
            showMonthLabels = false,
        )

        if (showFooter) {
            val fy = h - padV - footerH + 4f * scale
            canvas.drawLine(left, fy, right, fy, divPaint)
            val cy = (fy + h - padV) / 2f + 2f * scale
            val statusColor = if (data.isCompletedToday) palette.emerald else activeColor
            canvas.drawCircle(left + 3f * scale, cy, 2.8f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor })
            val stP = textPaint(fonts.interMedium, 10f * scale, palette.inkSecondary)
            val openP = textPaint(fonts.monoSemiBold, 9.5f * scale, palette.inkPrimary, Paint.Align.RIGHT, letterSpacing = 0.05f)
            val openText = "OPEN HABIT ›"
            val openW = openP.measureText(openText)
            canvas.drawText(
                ellipsize(data.statusLabel, stP, right - left - openW - 16f * scale),
                left + 10f * scale,
                cy - (stP.descent() + stP.ascent()) / 2f,
                stP,
            )
            canvas.drawText(openText, right, cy - (openP.descent() + openP.ascent()) / 2f, openP)
        }
    }

    private fun drawSingleHabitSquare(
        canvas: Canvas,
        w: Float,
        h: Float,
        data: SingleHabitWidgetData,
        activeColor: Int,
        palette: WidgetPalette,
        fonts: FontSet,
        scale: Float,
    ) {
        val pad = 18f * scale
        val left = pad
        val right = w - pad
        val contentW = right - left
        var y = pad

        // Header row: Dot + Domain | Streak Pill
        val streakNum = data.currentStreak.toString()
        val pillNumP = textPaint(fonts.monoSemiBold, 11f * scale, palette.inkPrimary)
        val pillDaysP = textPaint(fonts.monoMedium, 9f * scale, palette.inkMuted)
        val flameS = 10f * scale
        val pillPadH = 8f * scale
        val pillH = 22f * scale
        val pillW = pillPadH * 2f + flameS + 5f * scale + pillNumP.measureText(streakNum) + 4f * scale + pillDaysP.measureText("DAYS")
        val pillRect = RectF(right - pillW, y, right, y + pillH)
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg })
        canvas.drawRoundRect(
            pillRect,
            pillH / 2f,
            pillH / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.subCardBorder
                style = Paint.Style.STROKE
                strokeWidth = 0.9f * scale
            },
        )
        drawFlameIcon(canvas, pillRect.left + pillPadH + flameS / 2f, pillRect.centerY(), flameS, activeColor)
        val numX = pillRect.left + pillPadH + flameS + 5f * scale
        val pillBaseY = pillRect.centerY() - (pillNumP.descent() + pillNumP.ascent()) / 2f
        canvas.drawText(streakNum, numX, pillBaseY, pillNumP)
        canvas.drawText("DAYS", numX + pillNumP.measureText(streakNum) + 4f * scale, pillBaseY, pillDaysP)

        val dotR = 3.5f * scale
        val dotCy = pillRect.centerY()
        canvas.drawCircle(left + dotR, dotCy, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = activeColor })
        val domP = textPaint(fonts.monoMedium, 9.5f * scale, palette.inkSecondary, letterSpacing = 0.05f)
        canvas.drawText(
            ellipsize(data.domainBadgeLabel, domP, contentW - pillW - 16f * scale),
            left + dotR * 2f + 6f * scale,
            dotCy - (domP.descent() + domP.ascent()) / 2f,
            domP,
        )
        y += pillH + 10f * scale

        // Title & description
        val titleP = textPaint(fonts.interBold, 20f * scale, palette.inkPrimary)
        canvas.drawText(ellipsize(data.habit?.title ?: "Select Habit", titleP, contentW), left, y - titleP.ascent(), titleP)
        y += titleP.fontSpacing + 2f * scale

        val subP = textPaint(fonts.interMedium, 11f * scale, palette.inkSecondary)
        canvas.drawText(ellipsize(data.subtitleLabel, subP, contentW), left, y - subP.ascent(), subP)
        y += subP.fontSpacing + 12f * scale

        // Divider + 3 stat cards (Streak, Consistency, Target)
        val divPaint = Paint().apply {
            color = palette.divider
            strokeWidth = 1f * scale
        }
        canvas.drawLine(left, y, right, y, divPaint)
        y += 10f * scale

        val statGap = 7f * scale
        val statW = (contentW - statGap * 2f) / 3f
        val statH = 44f * scale
        val statLabels = listOf("CURRENT", "CONSISTENCY", "TARGET")
        val statValues = listOf("${data.currentStreak}d", "${data.consistencyPercent}%", data.primaryTargetLabel)
        val sLblP = textPaint(fonts.monoMedium, 8.5f * scale, palette.inkMuted, letterSpacing = 0.05f)
        val sValP = textPaint(fonts.monoBold, 14f * scale, palette.inkPrimary)

        for (i in 0 until 3) {
            val sx = left + i * (statW + statGap)
            val sRect = RectF(sx, y, sx + statW, y + statH)
            canvas.drawRoundRect(sRect, 10f * scale, 10f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg })
            canvas.drawRoundRect(
                sRect,
                10f * scale,
                10f * scale,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.subCardBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 0.9f * scale
                },
            )
            val innerPad = 8f * scale
            canvas.drawText(statLabels[i], sx + innerPad, y + 8f * scale - sLblP.ascent(), sLblP)
            val valText = ellipsize(statValues[i], sValP, statW - innerPad * 2f)
            canvas.drawText(valText, sx + innerPad, y + statH - 8f * scale - sValP.descent(), sValP)
        }
        y += statH + 12f * scale

        // Matrix section header + legend
        val matHdrP = textPaint(fonts.monoMedium, 9f * scale, palette.inkMuted, letterSpacing = 0.05f)
        val colsEst = (contentW / (14f * scale)).toInt().coerceIn(8, data.matrixColumns.size)
        canvas.drawText("ACTIVITY · PAST $colsEst WEEKS", left, y + 9f * scale, matHdrP)
        drawIntensityLegend(canvas, right, y + 5.5f * scale, activeColor, palette, fonts, scale, 8.5f)
        y += 16f * scale

        val footerH = 22f * scale
        val matrixBottom = h - pad - footerH - 8f * scale
        drawMatrixGrid(
            canvas = canvas,
            bounds = RectF(left, y, right, matrixBottom),
            columns = data.matrixColumns,
            monthLabels = emptyList(),
            today = data.today,
            activeColor = activeColor,
            palette = palette,
            fonts = fonts,
            scale = scale,
            showMonthLabels = false,
        )

        // Footer
        val fy = h - pad - footerH
        canvas.drawLine(left, fy, right, fy, divPaint)
        val fCy = fy + footerH * 0.68f
        val statusColor = if (data.isCompletedToday) palette.emerald else activeColor
        canvas.drawCircle(left + 3.5f * scale, fCy, 3f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor })
        val stP = textPaint(fonts.interMedium, 10.5f * scale, palette.inkSecondary)
        val openP = textPaint(fonts.monoSemiBold, 9.5f * scale, palette.inkPrimary, Paint.Align.RIGHT, letterSpacing = 0.05f)
        val openTxt = "OPEN HABIT ›"
        canvas.drawText(
            ellipsize(data.statusLabel, stP, contentW - openP.measureText(openTxt) - 18f * scale),
            left + 11f * scale,
            fCy - (stP.descent() + stP.ascent()) / 2f,
            stP,
        )
        canvas.drawText(openTxt, right, fCy - (openP.descent() + openP.ascent()) / 2f, openP)
    }

    // ============================================================================================
    // WIDGET 2: OVERALL CADENCE MATRIX
    // ============================================================================================

    fun renderOverallMatrixWidget(
        context: Context,
        data: OverallMatrixWidgetData,
        themeMode: WidgetThemeMode,
        widthDp: Int,
        heightDp: Int,
    ): Bitmap {
        val safeW = widthDp.coerceAtLeast(120)
        val safeH = heightDp.coerceAtLeast(120)
        val scale = computeScale(context, safeW, safeH)
        val wPx = (safeW * scale).roundToInt().coerceAtLeast(180)
        val hPx = (safeH * scale).roundToInt().coerceAtLeast(180)

        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(themeMode)
        val fonts = loadFonts(context)

        drawCardSurface(canvas, wPx.toFloat(), hPx.toFloat(), 26f * scale, palette, scale)

        val isTall = safeW < 235 && safeH >= 210
        if (isTall) {
            drawOverallMatrixTall(canvas, wPx.toFloat(), hPx.toFloat(), data, palette, fonts, scale)
        } else {
            drawOverallMatrixWideOrSquare(canvas, wPx.toFloat(), hPx.toFloat(), data, palette, fonts, scale, isCompactHeight = safeH < 280)
        }
        return bitmap
    }

    private fun drawOverallMatrixTall(
        canvas: Canvas,
        w: Float,
        h: Float,
        data: OverallMatrixWidgetData,
        palette: WidgetPalette,
        fonts: FontSet,
        scale: Float,
    ) {
        val pad = 16f * scale
        val left = pad
        val right = w - pad
        val contentW = right - left
        var y = pad

        // Header date + consistency pill
        val dateP = textPaint(fonts.monoMedium, 9f * scale, palette.inkMuted, letterSpacing = 0.05f)
        val consP = textPaint(fonts.monoSemiBold, 9.5f * scale, palette.emerald, Paint.Align.RIGHT)
        canvas.drawText(ellipsize("${data.dateShortLabel} · ${data.weekShortLabel}", dateP, contentW * 0.68f), left, y - dateP.ascent(), dateP)
        canvas.drawText("${data.consistencyPercent}%", right, y - consP.ascent(), consP)
        y += maxOf(dateP.fontSpacing, consP.fontSpacing) + 10f * scale

        // Stacked compact Morning / Afternoon / Evening progress bars
        val buckets = listOf(
            Triple("MORNING", data.morningCompleted, data.morningTotal),
            Triple("AFTERNOON", data.afternoonCompleted, data.afternoonTotal),
            Triple("EVENING", data.eveningCompleted, data.eveningTotal),
        )
        val bLblP = textPaint(fonts.monoMedium, 8.8f * scale, palette.inkMuted, letterSpacing = 0.05f)
        val bCntP = textPaint(fonts.monoBold, 9.5f * scale, palette.inkPrimary, Paint.Align.RIGHT)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.progressTrack }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val barW = (contentW * 0.36f).coerceIn(40f * scale, 68f * scale)
        val barH = 5f * scale

        buckets.forEach { (label, done, total) ->
            val rowH = 16f * scale
            val cy = y + rowH / 2f
            canvas.drawText(label, left, cy - (bLblP.descent() + bLblP.ascent()) / 2f, bLblP)
            canvas.drawText("$done/$total", right, cy - (bCntP.descent() + bCntP.ascent()) / 2f, bCntP)

            val fracW = bCntP.measureText("8/8") + 6f * scale
            val bRight = right - fracW
            val bLeft = bRight - barW
            val bTop = cy - barH / 2f
            canvas.drawRoundRect(RectF(bLeft, bTop, bRight, bTop + barH), barH / 2f, barH / 2f, trackPaint)
            val ratio = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            if (ratio > 0f) {
                fillPaint.color = if (ratio >= 1f) palette.emerald else palette.progressFill
                canvas.drawRoundRect(
                    RectF(bLeft, bTop, bLeft + barW * ratio, bTop + barH),
                    barH / 2f,
                    barH / 2f,
                    fillPaint,
                )
            }
            y += rowH + 4f * scale
        }

        val divPaint = Paint().apply {
            color = palette.divider
            strokeWidth = 1f * scale
        }
        y += 4f * scale
        canvas.drawLine(left, y, right, y, divPaint)
        y += 8f * scale

        // Grid header
        val gridHdrP = textPaint(fonts.monoMedium, 8.8f * scale, palette.inkMuted, letterSpacing = 0.05f)
        val streakP = textPaint(fonts.monoSemiBold, 8.8f * scale, palette.inkPrimary, Paint.Align.RIGHT)
        canvas.drawText("MATRIX", left, y - gridHdrP.ascent(), gridHdrP)
        canvas.drawText("${data.overallStreakDays}D STREAK", right, y - streakP.ascent(), streakP)
        y += maxOf(gridHdrP.fontSpacing, streakP.fontSpacing) + 6f * scale

        val footerH = if (h > y + 110f * scale) 20f * scale else 0f
        val matrixBottom = h - pad - footerH - if (footerH > 0f) 6f * scale else 0f
        drawMatrixGrid(
            canvas = canvas,
            bounds = RectF(left, y, right, matrixBottom),
            columns = data.matrixColumns,
            monthLabels = emptyList(),
            today = data.today,
            activeColor = palette.emerald,
            palette = palette,
            fonts = fonts,
            scale = scale,
            showMonthLabels = false,
        )

        if (footerH > 0f) {
            val fy = h - pad - footerH
            canvas.drawLine(left, fy, right, fy, divPaint)
            val fCy = fy + footerH * 0.68f
            val dotColor = if (data.hasPendingTasksToday) palette.emerald else palette.inkMuted
            canvas.drawCircle(left + 3f * scale, fCy, 2.5f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor })
            val nxtP = textPaint(fonts.monoMedium, 8.8f * scale, palette.inkSecondary)
            canvas.drawText(
                ellipsize("NEXT: ${data.nextTaskLabel}", nxtP, contentW - 10f * scale),
                left + 9f * scale,
                fCy - (nxtP.descent() + nxtP.ascent()) / 2f,
                nxtP,
            )
        }
    }

    private fun drawOverallMatrixWideOrSquare(
        canvas: Canvas,
        w: Float,
        h: Float,
        data: OverallMatrixWidgetData,
        palette: WidgetPalette,
        fonts: FontSet,
        scale: Float,
        isCompactHeight: Boolean,
    ) {
        val pad = 18f * scale
        val left = pad
        val right = w - pad
        val contentW = right - left
        var y = pad

        // Header bar: Date + Week | Emerald Consistency dot + label
        val dateStr = if (isCompactHeight) "${data.dateShortLabel} · ${data.weekShortLabel}" else "${data.dateLongLabel} · ${data.weekShortLabel}"
        val dateP = textPaint(fonts.monoMedium, 9.5f * scale, palette.inkMuted, letterSpacing = 0.05f)
        val consStr = "${data.consistencyPercent}% CONSISTENCY"
        val consP = textPaint(fonts.monoSemiBold, 9.5f * scale, palette.emerald, Paint.Align.RIGHT, letterSpacing = 0.04f)
        val consW = consP.measureText(consStr)
        val dotR = 2.8f * scale
        canvas.drawCircle(right - consW - 5f * scale - dotR, y + 5.5f * scale, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.emerald })
        canvas.drawText(consStr, right, y + 8.5f * scale, consP)
        canvas.drawText(ellipsize(dateStr, dateP, contentW - consW - 22f * scale), left, y + 8.5f * scale, dateP)
        y += 16f * scale

        val divPaint = Paint().apply {
            color = palette.divider
            strokeWidth = 1f * scale
        }
        canvas.drawLine(left, y, right, y, divPaint)
        y += if (isCompactHeight) 8f * scale else 10f * scale

        // 3 Time-block cards (Morning, Afternoon, Evening)
        val buckets = listOf(
            Triple("MORNING", data.morningCompleted, data.morningTotal),
            Triple("AFTERNOON", data.afternoonCompleted, data.afternoonTotal),
            Triple("EVENING", data.eveningCompleted, data.eveningTotal),
        )
        val cardGap = 7f * scale
        val cardW = (contentW - cardGap * 2f) / 3f
        val cardH = if (isCompactHeight) 32f * scale else 36f * scale
        val bLblP = textPaint(fonts.monoMedium, 8.5f * scale, palette.inkSecondary, letterSpacing = 0.04f)
        val bValP = textPaint(fonts.monoBold, 9.5f * scale, palette.inkPrimary, Paint.Align.RIGHT)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.progressTrack }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        buckets.forEachIndexed { idx, (label, done, total) ->
            val cx = left + idx * (cardW + cardGap)
            val rect = RectF(cx, y, cx + cardW, y + cardH)
            canvas.drawRoundRect(rect, 9f * scale, 9f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg })
            canvas.drawRoundRect(
                rect,
                9f * scale,
                9f * scale,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.subCardBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 0.9f * scale
                },
            )
            val innerPad = 8f * scale
            val textY = y + 8f * scale - bLblP.ascent()
            canvas.drawText(label, cx + innerPad, textY, bLblP)
            canvas.drawText("$done/$total", cx + cardW - innerPad, textY, bValP)

            val barH = 4.5f * scale
            val barTop = y + cardH - 8f * scale - barH
            val barW = cardW - innerPad * 2f
            canvas.drawRoundRect(RectF(cx + innerPad, barTop, cx + innerPad + barW, barTop + barH), barH / 2f, barH / 2f, trackPaint)
            val ratio = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            if (ratio > 0f) {
                fillPaint.color = if (ratio >= 1f) palette.emerald else palette.progressFill
                canvas.drawRoundRect(
                    RectF(cx + innerPad, barTop, cx + innerPad + barW * ratio, barTop + barH),
                    barH / 2f,
                    barH / 2f,
                    fillPaint,
                )
            }
        }
        y += cardH + if (isCompactHeight) 8f * scale else 10f * scale

        // Framed Matrix Container
        val footerH = 22f * scale
        val containerBottom = h - pad - footerH - 6f * scale
        val containerRect = RectF(left, y, right, containerBottom)
        canvas.drawRoundRect(containerRect, 14f * scale, 14f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg })
        canvas.drawRoundRect(
            containerRect,
            14f * scale,
            14f * scale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.subCardBorder
                style = Paint.Style.STROKE
                strokeWidth = 0.9f * scale
            },
        )

        val cPad = 10f * scale
        val cLeft = containerRect.left + cPad
        val cRight = containerRect.right - cPad
        var cy = containerRect.top + cPad

        // Matrix Card Header: Icon + CADENCE MATRIX | Streak · Month
        val iconSize = 10f * scale
        drawMatrixGridIcon(canvas, cLeft, cy, iconSize, palette.inkSecondary, scale)
        val matTitleP = textPaint(fonts.monoSemiBold, 9.2f * scale, palette.inkPrimary, letterSpacing = 0.05f)
        canvas.drawText("CADENCE MATRIX", cLeft + iconSize + 5f * scale, cy + 8.5f * scale, matTitleP)

        val streakSummary = "${data.overallStreakDays}D STREAK · ${data.completionsThisMonth} THIS MO"
        val sumP = textPaint(fonts.monoMedium, 8.8f * scale, palette.inkSecondary, Paint.Align.RIGHT)
        canvas.drawText(streakSummary, cRight, cy + 8.5f * scale, sumP)
        cy += 16f * scale

        val showLegendBottom = !isCompactHeight && (containerRect.height() >= 150f * scale)
        val legendH = if (showLegendBottom) 14f * scale else 0f
        val matrixBottom = containerRect.bottom - cPad - legendH - if (showLegendBottom) 4f * scale else 0f

        drawMatrixGrid(
            canvas = canvas,
            bounds = RectF(cLeft, cy, cRight, matrixBottom),
            columns = data.matrixColumns,
            monthLabels = data.monthLabels,
            today = data.today,
            activeColor = palette.emerald,
            palette = palette,
            fonts = fonts,
            scale = scale,
            showMonthLabels = true,
        )

        if (showLegendBottom) {
            drawIntensityLegend(
                canvas = canvas,
                rightX = cRight,
                centerY = containerRect.bottom - cPad - legendH / 2f,
                activeColor = palette.emerald,
                palette = palette,
                fonts = fonts,
                scale = scale,
                labelSizeSp = 8f,
            )
        }

        // Bottom Footer
        val fy = h - pad - footerH
        val fCy = fy + footerH * 0.65f
        val dotColor = if (data.hasPendingTasksToday) palette.emerald else palette.inkMuted
        canvas.drawCircle(left + 3.5f * scale, fCy, 3f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor })

        val openTxt = if (isCompactHeight) "OPEN ›" else "OPEN DASHBOARD ›"
        val openP = textPaint(fonts.monoSemiBold, 9.5f * scale, palette.inkPrimary, Paint.Align.RIGHT, letterSpacing = 0.05f)
        val openW = openP.measureText(openTxt)
        val nxtP = textPaint(fonts.monoMedium, 9.5f * scale, palette.inkSecondary)
        canvas.drawText(
            ellipsize("NEXT: ${data.nextTaskLabel}", nxtP, contentW - openW - 18f * scale),
            left + 11f * scale,
            fCy - (nxtP.descent() + nxtP.ascent()) / 2f,
            nxtP,
        )
        canvas.drawText(openTxt, right, fCy - (openP.descent() + openP.ascent()) / 2f, openP)
    }

    // ============================================================================================
    // WIDGET 3: UPCOMING TASKS (PIECEWISE FOR INTERACTIVE REMOTE VIEWS + COMBINED PREVIEW)
    // ============================================================================================

    fun upcomingTasksMaxVisibleRows(heightDp: Int): Int {
        val approxHeaderFooterDp = 128
        val rowSlotDp = 48
        val available = (heightDp - approxHeaderFooterDp).coerceAtLeast(rowSlotDp)
        return (available / rowSlotDp).coerceIn(2, 6)
    }

    fun renderUpcomingTasksHeader(
        context: Context,
        data: UpcomingTasksWidgetData,
        themeMode: WidgetThemeMode,
        widthDp: Int,
        drawBackground: Boolean = false,
        scaleOverride: Float? = null,
    ): Bitmap {
        val safeW = widthDp.coerceAtLeast(140)
        val isCompact = safeW < 235
        val headerH = if (isCompact) 74 else 76
        val scale = scaleOverride ?: computeScale(context, safeW, 300)
        val wPx = (safeW * scale).roundToInt()
        val hPx = (headerH * scale).roundToInt()

        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(themeMode)
        val fonts = loadFonts(context)
        if (drawBackground) {
            canvas.drawColor(palette.cardBg)
        }

        val padH = if (isCompact) 14f * scale else 18f * scale
        val topPad = 14f * scale
        val left = padH
        val right = wPx - padH
        val contentW = right - left

        // Top meta row: TODAY · SAT, OCT 21 | 2 / 5 DONE pill
        val metaText = if (isCompact) data.dateShortLabel else data.dateHeaderLabel
        val metaP = textPaint(fonts.monoSemiBold, 9f * scale, palette.inkMuted, letterSpacing = 0.05f)

        val fracStr = "${data.completedCount}/${data.totalCount}"
        val doneSuffix = if (isCompact) "" else " DONE"
        val fracP = textPaint(fonts.monoBold, 10f * scale, palette.inkPrimary)
        val doneP = textPaint(fonts.monoMedium, 8f * scale, palette.inkMuted)
        val pillW = 14f * scale + fracP.measureText(fracStr) + doneP.measureText(doneSuffix)
        val pillH = 18f * scale
        val pillRect = RectF(right - pillW, topPad - 2f * scale, right, topPad - 2f * scale + pillH)

        canvas.drawRoundRect(pillRect, 9f * scale, 9f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg })
        canvas.drawRoundRect(
            pillRect,
            9f * scale,
            9f * scale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.subCardBorder
                style = Paint.Style.STROKE
                strokeWidth = 0.9f * scale
            },
        )
        val pillBaseY = pillRect.centerY() - (fracP.descent() + fracP.ascent()) / 2f
        val fracX = pillRect.left + 7f * scale
        canvas.drawText(fracStr, fracX, pillBaseY, fracP)
        if (doneSuffix.isNotEmpty()) {
            canvas.drawText(doneSuffix, fracX + fracP.measureText(fracStr), pillBaseY, doneP)
        }

        canvas.drawText(
            ellipsize(metaText, metaP, contentW - pillW - 10f * scale),
            left,
            pillRect.centerY() - (metaP.descent() + metaP.ascent()) / 2f,
            metaP,
        )

        // Headline row: Today's Habits | 50% Completed
        val titleStr = if (isCompact) "Upcoming" else "Today's Habits"
        val titleP = textPaint(fonts.interBold, if (isCompact) 16.5f * scale else 19f * scale, palette.inkPrimary)
        val titleY = pillRect.bottom + 4f * scale - titleP.ascent()
        canvas.drawText(titleStr, left, titleY, titleP)

        if (!isCompact && data.totalCount > 0) {
            val pctP = textPaint(fonts.monoMedium, 9.5f * scale, palette.inkSecondary, Paint.Align.RIGHT)
            canvas.drawText("${data.completionPercent}% Done", right, titleY, pctP)
        }

        // Segmented multi-color progress bar
        val barTop = titleY + titleP.descent() + 7f * scale
        val barH = 4.5f * scale
        val segCount = data.segmentColorHexes.size.coerceAtLeast(1)
        val segGap = 4f * scale
        val segW = ((contentW - (segCount - 1) * segGap) / segCount).coerceAtLeast(4f * scale)
        val segPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (data.segmentColorHexes.isEmpty()) {
            segPaint.color = palette.progressTrack
            canvas.drawRoundRect(RectF(left, barTop, right, barTop + barH), barH / 2f, barH / 2f, segPaint)
        } else {
            data.segmentColorHexes.forEachIndexed { idx, hex ->
                val sx = left + idx * (segW + segGap)
                segPaint.color = if (hex != null) parseHexColor(hex) else palette.progressTrack
                canvas.drawRoundRect(RectF(sx, barTop, sx + segW, barTop + barH), barH / 2f, barH / 2f, segPaint)
            }
        }

        return bitmap
    }

    fun renderUpcomingTaskRow(
        context: Context,
        task: WidgetTaskItem,
        themeMode: WidgetThemeMode,
        widthDp: Int,
        rowHeightDp: Int = 46,
        scaleOverride: Float? = null,
    ): Bitmap {
        val safeW = widthDp.coerceAtLeast(140)
        val isCompact = safeW < 235
        val scale = scaleOverride ?: computeScale(context, safeW, 300)
        val wPx = (safeW * scale).roundToInt()
        val hPx = (rowHeightDp * scale).roundToInt()

        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(themeMode)
        val fonts = loadFonts(context)
        val accent = parseHexColor(task.accentColorHex)

        val padH = if (isCompact) 14f * scale else 18f * scale
        val cardRect = RectF(padH, 2.5f * scale, wPx - padH, hPx - 2.5f * scale)
        val cardRadius = 11f * scale

        val cardBg = if (task.isCompleted) palette.completedTaskBg else palette.activeTaskBg
        val cardBorder = when {
            task.isCompleted -> palette.completedTaskBorder
            task.isNowTarget -> palette.activeTaskBorder
            else -> palette.subCardBorder
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg })
        canvas.drawRoundRect(
            cardRect,
            cardRadius,
            cardRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cardBorder
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            },
        )

        val innerPadH = if (isCompact) 9f * scale else 11f * scale
        val boxSize = 15.5f * scale
        val boxLeft = cardRect.left + innerPadH
        val boxTop = cardRect.centerY() - boxSize / 2f
        val boxRect = RectF(boxLeft, boxTop, boxLeft + boxSize, boxTop + boxSize)
        val boxRadius = 4f * scale

        if (task.isCompleted) {
            canvas.drawRoundRect(boxRect, boxRadius, boxRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.9f * scale
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val checkPath = Path().apply {
                moveTo(boxRect.left + boxSize * 0.24f, boxRect.centerY() + boxSize * 0.02f)
                lineTo(boxRect.left + boxSize * 0.43f, boxRect.centerY() + boxSize * 0.21f)
                lineTo(boxRect.left + boxSize * 0.77f, boxRect.centerY() - boxSize * 0.19f)
            }
            canvas.drawPath(checkPath, checkPaint)
        } else {
            canvas.drawRoundRect(boxRect, boxRadius, boxRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.checkboxBg })
            canvas.drawRoundRect(
                boxRect,
                boxRadius,
                boxRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (task.isNowTarget) accent else palette.checkboxBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * scale
                },
            )
        }

        // Right-side accent signature dot
        val dotR = 3f * scale
        val dotCx = cardRect.right - innerPadH - dotR
        canvas.drawCircle(
            dotCx,
            cardRect.centerY(),
            dotR,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (task.isCompleted) ColorUtils.setAlphaComponent(accent, 115) else accent
            },
        )

        val textLeft = boxRect.right + if (isCompact) 8f * scale else 10f * scale
        val textRight = dotCx - dotR - 8f * scale
        val maxTextW = (textRight - textLeft).coerceAtLeast(30f * scale)

        val titleColor = if (task.isCompleted) palette.inkMuted else palette.inkPrimary
        val titleP = textPaint(
            typeface = if (task.isCompleted) fonts.interMedium else fonts.interSemiBold,
            sizePx = if (isCompact) 11.2f * scale else 12f * scale,
            color = titleColor,
        ).apply {
            isStrikeThruText = task.isCompleted
        }

        // Optional NOW badge next to title
        val nowW = if (task.isNowTarget && !task.isCompleted) {
            val nowText = "NOW"
            val nowP = textPaint(fonts.monoBold, 7.5f * scale, palette.nowBadgeText)
            nowP.measureText(nowText) + 12f * scale
        } else {
            0f
        }
        val titleMaxW = (maxTextW - nowW).coerceAtLeast(24f * scale)
        val drawnTitle = ellipsize(task.title, titleP, titleMaxW)
        val titleDrawnW = titleP.measureText(drawnTitle)

        val rowMidY = cardRect.centerY()
        val titleBaseline = rowMidY - 2.5f * scale
        canvas.drawText(drawnTitle, textLeft, titleBaseline, titleP)

        if (task.isNowTarget && !task.isCompleted) {
            val nowP = textPaint(fonts.monoBold, 7.5f * scale, palette.nowBadgeText)
            val nowText = "NOW"
            val bW = nowP.measureText(nowText) + 8f * scale
            val bH = 12f * scale
            val bLeft = textLeft + titleDrawnW + 5f * scale
            val bTop = titleBaseline - 9.5f * scale
            val bRect = RectF(bLeft, bTop, bLeft + bW, bTop + bH)
            canvas.drawRoundRect(bRect, 3.5f * scale, 3.5f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.nowBadgeBg })
            canvas.drawText(nowText, bLeft + 4f * scale, bRect.centerY() - (nowP.descent() + nowP.ascent()) / 2f, nowP)
        }

        val subText = if (isCompact) {
            if (task.isNowTarget && !task.isCompleted) "${task.timeShortLabel} · NOW" else task.timeShortLabel
        } else {
            task.subtitle
        }
        val subColor = if (task.isCompleted) palette.inkMuted else palette.inkSecondary
        val subP = textPaint(fonts.monoMedium, if (isCompact) 8.5f * scale else 9f * scale, subColor)
        val subBaseline = rowMidY + 10.5f * scale
        canvas.drawText(ellipsize(subText, subP, maxTextW), textLeft, subBaseline, subP)

        return bitmap
    }

    fun renderUpcomingTasksEmptyRow(
        context: Context,
        themeMode: WidgetThemeMode,
        widthDp: Int,
        rowHeightDp: Int = 54,
        scaleOverride: Float? = null,
    ): Bitmap {
        val safeW = widthDp.coerceAtLeast(140)
        val isCompact = safeW < 235
        val scale = scaleOverride ?: computeScale(context, safeW, 300)
        val wPx = (safeW * scale).roundToInt()
        val hPx = (rowHeightDp * scale).roundToInt()

        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(themeMode)
        val fonts = loadFonts(context)

        val padH = if (isCompact) 14f * scale else 18f * scale
        val rect = RectF(padH, 4f * scale, wPx - padH, hPx - 4f * scale)
        canvas.drawRoundRect(rect, 11f * scale, 11f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.subCardBg })
        canvas.drawRoundRect(
            rect,
            11f * scale,
            11f * scale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.subCardBorder
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            },
        )
        val txtP = textPaint(fonts.monoMedium, 10f * scale, palette.inkSecondary, Paint.Align.CENTER)
        val msg = ellipsize("REST DAY · NO HABITS SCHEDULED", txtP, rect.width() - 16f * scale)
        canvas.drawText(msg, rect.centerX(), rect.centerY() - (txtP.descent() + txtP.ascent()) / 2f, txtP)
        return bitmap
    }

    fun renderUpcomingTasksFooter(
        context: Context,
        data: UpcomingTasksWidgetData,
        themeMode: WidgetThemeMode,
        widthDp: Int,
        hiddenExtraCount: Int = 0,
        scaleOverride: Float? = null,
    ): Bitmap {
        val safeW = widthDp.coerceAtLeast(140)
        val isCompact = safeW < 235
        val scale = scaleOverride ?: computeScale(context, safeW, 300)
        val wPx = (safeW * scale).roundToInt()
        val hPx = (36 * scale).roundToInt()

        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(themeMode)
        val fonts = loadFonts(context)

        val padH = if (isCompact) 14f * scale else 18f * scale
        val left = padH
        val right = wPx - padH

        canvas.drawLine(
            left,
            4f * scale,
            right,
            4f * scale,
            Paint().apply {
                color = palette.divider
                strokeWidth = 1f * scale
            },
        )

        val cy = hPx * 0.56f
        val rightLabel = when {
            hiddenExtraCount > 0 -> "+$hiddenExtraCount MORE · OPEN ›"
            isCompact -> "OPEN ›"
            else -> "VIEW DASHBOARD ›"
        }
        val rightP = textPaint(fonts.monoSemiBold, 9.2f * scale, palette.inkPrimary, Paint.Align.RIGHT, letterSpacing = 0.04f)
        val rightW = rightP.measureText(rightLabel)
        canvas.drawText(rightLabel, right, cy - (rightP.descent() + rightP.ascent()) / 2f, rightP)

        val leftP = textPaint(fonts.monoMedium, 9.2f * scale, palette.inkSecondary)
        canvas.drawText(
            ellipsize(data.nextUpFooterLabel, leftP, right - left - rightW - 12f * scale),
            left,
            cy - (leftP.descent() + leftP.ascent()) / 2f,
            leftP,
        )
        return bitmap
    }

    fun renderUpcomingTasksFullPreview(
        context: Context,
        data: UpcomingTasksWidgetData,
        themeMode: WidgetThemeMode,
        widthDp: Int,
        heightDp: Int,
    ): Bitmap {
        val safeW = widthDp.coerceAtLeast(140)
        val safeH = heightDp.coerceAtLeast(160)
        val scale = computeScale(context, safeW, safeH)
        val wPx = (safeW * scale).roundToInt()
        val hPx = (safeH * scale).roundToInt()

        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(themeMode)

        drawCardSurface(canvas, wPx.toFloat(), hPx.toFloat(), 26f * scale, palette, scale)

        val headerBmp = renderUpcomingTasksHeader(
            context = context,
            data = data,
            themeMode = themeMode,
            widthDp = safeW,
            drawBackground = false,
            scaleOverride = scale,
        )
        canvas.drawBitmap(headerBmp, 0f, 0f, null)

        val maxRows = upcomingTasksMaxVisibleRows(safeH)
        val footerBmp = renderUpcomingTasksFooter(
            context = context,
            data = data,
            themeMode = themeMode,
            widthDp = safeW,
            hiddenExtraCount = (data.orderedTasks.size - maxRows).coerceAtLeast(0),
            scaleOverride = scale,
        )
        val footerTop = (hPx - footerBmp.height - 6f * scale).coerceAtLeast(headerBmp.height.toFloat())
        val availRowsH = (footerTop - headerBmp.height).coerceAtLeast(0f)

        val visibleTasks = data.orderedTasks.take(maxRows)
        if (visibleTasks.isEmpty()) {
            val emptyBmp = renderUpcomingTasksEmptyRow(context, themeMode, safeW, 52, scaleOverride = scale)
            if (emptyBmp.height <= availRowsH + 2f) {
                canvas.drawBitmap(emptyBmp, 0f, headerBmp.height.toFloat() + 6f * scale, null)
            }
            emptyBmp.recycle()
        } else {
            val rowDp = ((availRowsH / scale) / visibleTasks.size.coerceAtLeast(1)).toInt().coerceIn(38, 48)
            var curY = headerBmp.height.toFloat() + 2f * scale
            visibleTasks.forEach { task ->
                val rowBmp = renderUpcomingTaskRow(context, task, themeMode, safeW, rowDp, scaleOverride = scale)
                if (curY + rowBmp.height <= footerTop + 2f) {
                    canvas.drawBitmap(rowBmp, 0f, curY, null)
                    curY += rowBmp.height
                }
                rowBmp.recycle()
            }
        }

        canvas.drawBitmap(footerBmp, 0f, footerTop, null)
        headerBmp.recycle()
        footerBmp.recycle()
        return bitmap
    }
}
