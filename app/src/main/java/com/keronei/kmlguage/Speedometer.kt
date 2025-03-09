package com.keronei.kmlguage;

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.core.content.res.ResourcesCompat
//import androidx.core.animation.doOnEnd
import com.keronei.kmlgauge.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Speedometer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Attribute Defaults
    private var _maxSpeed = 160

    private var _maxRpm = 8000

    @Dimension
    private var _borderSize = 56f

    @Dimension
    private var _textGap = 30f

    @ColorInt
    private var _borderColor = Color.parseColor("#402c47")

    @ColorInt
    private var _fillColor = Color.parseColor("#d83a78")

    @ColorInt
    private var _textColor = Color.parseColor("#f5f5f5")

    private var _metricText = "km/h"

    // Dynamic Values
    private val indicatorBorderRect = RectF()

    private val tickBorderRect = RectF()

    private val textBounds = Rect()

    private var angle = MIN_ANGLE

    private var speed = 0

    private var rpm = 0

    private var speedFont: Typeface? = null

    // Dimension Getters
    private val centerX get() = width / 2f

    private val centerY get() = height / 2f

    // Core Attributes
    var maxSpeed: Int
        get() = _maxSpeed
        set(value) {
            _maxSpeed = value
        }

    var maxRpm: Int
        get() = _maxRpm
        set(value) {
            _maxRpm = value
            invalidate()
        }

    var borderSize: Float
        @Dimension get() = _borderSize
        set(@Dimension value) {
            _borderSize = value
            paintIndicatorBorder.strokeWidth = value
            paintIndicatorFill.strokeWidth = value
            invalidate()
        }

    var textGap: Float
        @Dimension get() = _textGap
        set(@Dimension value) {
            _textGap = value
            invalidate()
        }

    var metricText: String
        get() = _metricText
        set(value) {
            _metricText = value
            invalidate()
        }

    var borderColor: Int
        @ColorInt get() = _borderColor
        set(@ColorInt value) {
            _borderColor = value
            paintIndicatorBorder.color = value
            paintTickBorder.color = value
            paintMajorTick.color = value
            paintMinorTick.color = value
            invalidate()
        }

    var fillColor: Int
        @ColorInt get() = _fillColor
        set(@ColorInt value) {
            _fillColor = value
            paintIndicatorFill.color = value
            invalidate()
        }

    var textColor: Int
        @ColorInt get() = _textColor
        set(@ColorInt value) {
            _textColor = value
            paintTickText.color = value
            paintSpeed.color = value
            paintMetric.color = value
            invalidate()
        }

    // Paints
    private val paintIndicatorBorder = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = borderColor
        strokeWidth = borderSize
        strokeCap = Paint.Cap.ROUND
    }

    private val paintIndicatorFill = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = fillColor
        strokeWidth = borderSize
        strokeCap = Paint.Cap.ROUND
    }

    private val unlitPaint = Paint().apply {
        textSize = 148f
        color = Color.GRAY  // Dim color for unlit trace
        alpha = 100  // Make it semi-transparent
        style = Paint.Style.STROKE

    }

    private val paintTickBorder = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = borderColor
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintMajorTick = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = borderColor
        strokeWidth = MAJOR_TICK_WIDTH
        strokeCap = Paint.Cap.BUTT
    }

    private val paintMinorTick = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = borderColor
        strokeWidth = MINOR_TICK_WIDTH
        strokeCap = Paint.Cap.BUTT
    }

    private val paintTickText = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = textColor
        textSize = 28f
    }

    private val paintSpeed = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = textColor
        textSize = 168f
    }

    private val paintMetric = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = textColor
        textSize = 40f
    }

    // Animators
    private val animator = ValueAnimator.ofFloat().apply {
        interpolator = AccelerateDecelerateInterpolator()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, widthMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        indicatorBorderRect.set(
            borderSize / 2,
            borderSize / 2,
            width - borderSize / 2,
            width - borderSize / 2
        )

        tickBorderRect.set(
            borderSize + TICK_MARGIN,
            borderSize + TICK_MARGIN,
            width - borderSize - TICK_MARGIN,
            width - borderSize - TICK_MARGIN
        )
    }

    init {
        obtainStyledAttributes(attrs, defStyleAttr)
    }

    private fun obtainStyledAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.Speedometer,
            defStyleAttr,
            0
        )

        try {
            with(typedArray) {
                maxRpm = getInt(
                    R.styleable.Speedometer_maxRpm,
                    maxSpeed
                )
                borderSize = getDimension(
                    R.styleable.Speedometer_borderSize,
                    borderSize
                )
                textGap = getDimension(
                    R.styleable.Speedometer_textGap,
                    textGap
                )
                metricText = getString(
                    R.styleable.Speedometer_metricText
                ) ?: metricText
                borderColor = getColor(
                    R.styleable.Speedometer_borderColor,
                    borderColor
                )
                fillColor = getColor(
                    R.styleable.Speedometer_fillColor,
                    borderColor
                )
                textColor = getColor(
                    R.styleable.Speedometer_textColor,
                    borderColor
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        initFont()
        renderMajorTicks(canvas)
        renderMinorTicks(canvas)
        renderBorder(canvas)
        renderBorderFill(canvas)
        renderTickBorder(canvas)
        renderSpeedAndMetricText(canvas)
    }

    private fun initFont() {
        speedFont = ResourcesCompat.getFont(context, R.font.soul_daisy)
        speedFont?.let {
            paintSpeed.typeface = it
            paintTickText.typeface = it
            //unlitPaint.typeface = it
            unlitPaint.typeface = it
        }

        ResourcesCompat.getFont(context, R.font.soul_daisy)?.let {
            paintTickText.typeface = it
        }
    }

    private fun renderMinorTicks(canvas: Canvas) {
        for (s in MIN_RPM..maxRpm step 100) {
            if (s % 100 == 0) {
                canvas.drawLine(
                    centerX + (centerX - borderSize - MINOR_TICK_SIZE) * cos(mapRpmToAngle(s).toRadian()),
                    centerY - (centerY - borderSize - MINOR_TICK_SIZE) * sin(mapRpmToAngle(s).toRadian()),
                    centerX + (centerX - borderSize - TICK_MARGIN) * cos(mapRpmToAngle(s).toRadian()),
                    centerY - (centerY - borderSize - TICK_MARGIN) * sin(mapRpmToAngle(s).toRadian()),
                    paintMinorTick
                )
            }
        }
    }

    private fun renderMajorTicks(canvas: Canvas) {
        for (s in MIN_RPM..maxRpm step 1000) {
            canvas.drawLine(
                centerX + (centerX - borderSize - MAJOR_TICK_SIZE) * cos(mapRpmToAngle(s).toRadian()),
                centerY - (centerY - borderSize - MAJOR_TICK_SIZE) * sin(mapRpmToAngle(s).toRadian()),
                centerX + (centerX - borderSize - TICK_MARGIN) * cos(mapRpmToAngle(s).toRadian()),
                centerY - (centerY - borderSize - TICK_MARGIN) * sin(mapRpmToAngle(s).toRadian()),
                paintMajorTick
            )
            canvas.drawTextCentred(
                (s / 1000).toString(),
                centerX + (centerX - borderSize - MAJOR_TICK_SIZE - TICK_MARGIN - TICK_TEXT_MARGIN) * cos(
                    mapRpmToAngle(s).toRadian()
                ),
                centerY - (centerY - borderSize - MAJOR_TICK_SIZE - TICK_MARGIN - TICK_TEXT_MARGIN) * sin(
                    mapRpmToAngle(s).toRadian()
                ),
                paintTickText
            )
        }
    }

    private fun renderBorder(canvas: Canvas) {
        canvas.drawArc(
            indicatorBorderRect,
            140f,
            260f,
            false,
            paintIndicatorBorder
        )
    }

    private fun renderTickBorder(canvas: Canvas) {
        canvas.drawArc(
            tickBorderRect,
            START_ANGLE,
            SWEEP_ANGLE,
            false,
            paintTickBorder
        )
    }

    private fun renderBorderFill(canvas: Canvas) {
        canvas.drawArc(
            indicatorBorderRect,
            START_ANGLE,
            MIN_ANGLE - angle,
            false,
            paintIndicatorFill
        )
    }

    private fun renderSpeedAndMetricText(canvas: Canvas) {
        canvas.drawTextCentred(
            speed.toString(),
            width / 2f,
            (height / 2f) - (height * 0.05f),
            paintSpeed,
            true
        )
        canvas.drawTextCentred(
            metricText,
            width / 2f,
            height / 2f + paintSpeed.textSize / 2 + textGap,
            paintMetric
        )
    }

    private fun mapRpmToAngle(r: Int): Float {
        return (MIN_ANGLE + ((MAX_ANGLE - MIN_ANGLE) / (maxRpm - MIN_RPM)) * (r - MIN_RPM))
    }

    private fun mapAngleToRpm(angle: Float): Int {
        return (MIN_RPM + ((maxRpm - MIN_RPM) / (MAX_ANGLE - MIN_ANGLE)) * (angle - MIN_ANGLE)).toInt()
    }

    fun setSpeedAndRPM(s: Int, r: Int, d: Long, onEnd: (() -> Unit)? = null) {
        animator.apply {
            setFloatValues(mapRpmToAngle(r), mapRpmToAngle(r))

            addUpdateListener {
                angle = it.animatedValue as Float
                rpm = mapAngleToRpm(angle)
                speed = s
                invalidate()
            }
//            doOnEnd {
//                onEnd?.invoke()
//            }
            duration = d
            start()
        }
    }

    private fun Canvas.drawTextCentred(text: String, cx: Float, cy: Float, paint: Paint, isSpeed: Boolean = false) {
        var x = cx

        paint.getTextBounds(text, 0, text.length, textBounds)

        text.forEachIndexed { index, char ->

            val charWidth = paint.measureText(char.toString())

            if (char == '1' && index > 0) {
                x += 24f
            }

            drawText(
                char.toString(),
                x - textBounds.exactCenterX(),
                cy - textBounds.exactCenterY(),
                paint
            )

            x += charWidth
        }
    }

    private fun Float.toRadian(): Float {
        return this * (PI / 180).toFloat()
    }

    companion object {
        private const val MIN_ANGLE = 220f
        private const val MAX_ANGLE = -40f
        private const val START_ANGLE = 140f
        private const val SWEEP_ANGLE = 260f

        private const val MIN_SPEED = 0
        private const val MIN_RPM = 0
        private const val TICK_MARGIN = 10f
        private const val TICK_TEXT_MARGIN = 10f
        private const val MAJOR_TICK_SIZE = 30f
        private const val MINOR_TICK_SIZE = 20f
        private const val MAJOR_TICK_WIDTH = 4f
        private const val MINOR_TICK_WIDTH = 2f
    }
}
