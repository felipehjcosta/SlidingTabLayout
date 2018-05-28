package com.github.felipehjcosta.slidingtablayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout


internal class SlidingTabStrip @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var footerIndicatorHeight = 0.0f

    private val bottomBorderThickness: Int
    private val bottomBorderPaint: Paint

    private val selectedIndicatorThickness: Int
    private val selectedIndicatorPaint: Paint

    private val defaultBottomBorderColor: Int

    private val path: Path

    var scrollState: SlidingTabLayout.ScrollState = SlidingTabLayout.ScrollState.Idle(0, 0.0f)
        set(value) {
            field = value
            invalidate()
        }

    private var customTabColorizer: SlidingTabLayout.TabColorizer

    init {
        setWillNotDraw(false)

        footerIndicatorHeight = convertDpToPixel(20, context).toFloat()

        val density = resources.displayMetrics.density

        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorForeground, outValue, true)
        val themeForegroundColor = outValue.data

        defaultBottomBorderColor = setColorAlpha(themeForegroundColor,
                DEFAULT_BOTTOM_BORDER_COLOR_ALPHA)

        customTabColorizer = SlidingTabLayout.SimpleTabColorizer(intArrayOf(DEFAULT_SELECTED_INDICATOR_COLOR))

        bottomBorderThickness = (DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS * density).toInt()
        bottomBorderPaint = Paint()
        bottomBorderPaint.color = defaultBottomBorderColor

        selectedIndicatorThickness = (SELECTED_INDICATOR_THICKNESS_DIPS * density).toInt()
        selectedIndicatorPaint = Paint()
        path = Path()
    }

    fun setCustomTabColorizer(customTabColorizer: SlidingTabLayout.TabColorizer) {
        this.customTabColorizer = customTabColorizer
        invalidate()
    }

    fun getCustomTabColorizer(): SlidingTabLayout.TabColorizer? {
        return customTabColorizer
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = (measuredHeight + footerIndicatorHeight).toInt()

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val height = height
        val childCount = childCount
        val tabColorizer = customTabColorizer

        // Thick colored underline below the current selection
        val selectedPosition = scrollState.currentPosition
        val selectionOffset = scrollState.offset
        if (childCount > 0) {
            val selectedTitle = getChildAt(selectedPosition)
            var left = selectedTitle?.left ?: 0
            var color = tabColorizer.getIndicatorColor(selectedPosition - 1)

            val nextTitle = when (scrollState) {
                is SlidingTabLayout.ScrollState.Idle -> getChildAt(selectedPosition)
                else -> {
                    val nextColor = tabColorizer.getIndicatorColor(selectedPosition)
                    if (color != nextColor) {
                        color = blendColors(nextColor, color, selectionOffset)
                    }
                    getChildAt(selectedPosition + 1)
                }
            }
            val nextTitleWidth = nextTitle.width
            left = (selectionOffset * nextTitle.left + (1.0f - selectionOffset) * left).toInt() + nextTitleWidth / 2

            selectedIndicatorPaint.color = color
            val footerLineHeight = 0f
            val heightMinusLine = height - footerLineHeight

            path.reset()
            path.moveTo(left.toFloat(), heightMinusLine - footerIndicatorHeight)
            path.lineTo(left + footerIndicatorHeight, heightMinusLine)
            path.lineTo(left - footerIndicatorHeight, heightMinusLine)
            path.close()
            canvas.drawPath(path, selectedIndicatorPaint)
        }

    }

    /**
     * Set the alpha value of the `color` to be the given `alpha` value.
     */
    private fun setColorAlpha(color: Int, alpha: Byte): Int {
        return Color.argb(alpha.toInt(), Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Blend `color1` and `color2` using the given ratio.
     *
     * @param ratio of which to blend. 1.0 will return `color1`, 0.5 will give an even blend,
     * 0.0 will return `color2`.
     */
    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRation = 1.0f - ratio
        val r = Color.red(color1) * ratio + Color.red(color2) * inverseRation
        val g = Color.green(color1) * ratio + Color.green(color2) * inverseRation
        val b = Color.blue(color1) * ratio + Color.blue(color2) * inverseRation
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    companion object {

        private const val DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 0
        private const val DEFAULT_BOTTOM_BORDER_COLOR_ALPHA: Byte = 0x26
        private const val SELECTED_INDICATOR_THICKNESS_DIPS = 3
        private const val DEFAULT_SELECTED_INDICATOR_COLOR = -0xcc4a1b

        private const val BASE_DENSITY = 160.0f

        private fun convertDpToPixel(dp: Float, context: Context): Float {
            val resources = context.resources
            val metrics = resources.displayMetrics
            return dp * (metrics.densityDpi / BASE_DENSITY)
        }

        private fun convertDpToPixel(dp: Int, context: Context): Int {
            return convertDpToPixel(dp.toFloat(), context).toInt()
        }
    }

}

