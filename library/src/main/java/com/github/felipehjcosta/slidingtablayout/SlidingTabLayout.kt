package com.github.felipehjcosta.slidingtablayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.v4.view.ViewPager
import android.support.v4.widget.Space
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.felipehjcosta.slidingtablayout.SlidingTabLayout.TabColorizer

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 *
 *
 * To use the component, simply add it to your view hierarchy. Then in your
 * [android.app.Activity] or [android.support.v4.app.Fragment] call
 * [.setViewPager] providing it the ViewPager this layout is being used for.
 *
 *
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via [.setSelectedIndicatorColors]. The
 * alternative is via the [TabColorizer] interface which provides you complete control over
 * which color is used for any individual position.
 *
 *
 * The views used as tabs can be customized by calling [.setCustomTabView],
 * providing the layout ID of your custom layout.
 */
class SlidingTabLayout : HorizontalScrollView {

    private var titleOffset = 0
    private var mFooterIndicatorHeight = 0.0f

    private var tabViewLayoutId: Int = 0
    private var tabViewTextViewId: Int = 0
    private var distributeEvenly: Boolean = false

    private var viewPager: ViewPager? = null
    private val contentDescriptions = SparseArray<String>()
    private var viewPagerPageChangeListener: ViewPager.OnPageChangeListener? = null

    private val tabStrip: SlidingTabStrip

    @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(context, attrs, defStyle) {
        tabStrip = SlidingTabStrip(context)
        addView(tabStrip, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        init(context, attrs)
    }


    private fun init(context: Context, attrs: AttributeSet?) {
        // Disable the Scroll Bar
        isHorizontalScrollBarEnabled = false
        // Make sure that the Tab Strips fills this View
        isFillViewport = true

        titleOffset = (TITLE_OFFSET_DIPS * resources.displayMetrics.density).toInt()

        mFooterIndicatorHeight = convertDpToPixel(20, context).toFloat()
        viewPagerPageChangeListener = InternalViewPagerListener()
    }

    /**
     * Set the custom [TabColorizer] to be used.
     *
     *
     * If you only require simple custmisation then you can use
     * [.setSelectedIndicatorColors] to achieve
     * similar effects.
     */
    fun setCustomTabColorizer(tabColorizer: TabColorizer) {
        tabStrip.setCustomTabColorizer(tabColorizer)
    }

    fun setDistributeEvenly(distributeEvenly: Boolean) {
        this.distributeEvenly = distributeEvenly
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same color.
     */
    fun setSelectedIndicatorColors(vararg colors: Int) {
        tabStrip.setSelectedIndicatorColors(*colors)
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId  id of the [TextView] in the inflated view
     */
    fun setCustomTabView(@LayoutRes layoutResId: Int, @IdRes textViewId: Int) {
        tabViewLayoutId = layoutResId
        tabViewTextViewId = textViewId
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    fun setViewPager(viewPager: ViewPager?) {
        tabStrip.removeAllViews()

        if (this.viewPager != null) {
            this.viewPager!!.removeOnPageChangeListener(viewPagerPageChangeListener!!)
        }

        this.viewPager = viewPager
        if (viewPager != null) {
            viewPager.addOnPageChangeListener(viewPagerPageChangeListener!!)
            populateTabStrip()
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via
     * [.setCustomTabView].
     */
    private fun createDefaultTabView(context: Context): TextView {
        val textView = TextView(context)
        textView.gravity = Gravity.CENTER
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP.toFloat())
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val outValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true)
        textView.setBackgroundResource(outValue.resourceId)
        textView.setAllCaps(true)

        val padding = (TAB_VIEW_PADDING_DIPS * resources.displayMetrics.density).toInt()
        textView.setPadding(padding, padding, padding, padding)

        return textView
    }

    private fun populateTabStrip() {
        val adapter = viewPager!!.adapter
        val tabClickListener = TabClickListener()

        if (adapter == null || adapter.count == 0) {
            return
        }

        var i = 0
        val count = adapter.count
        while (i < count) {
            var tabView: View? = null
            var tabTitleView: TextView? = null

            if (tabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(context).inflate(tabViewLayoutId, tabStrip,
                        false)
                tabTitleView = tabView!!.findViewById<View>(tabViewTextViewId) as TextView
            }

            if (tabView == null) {
                tabView = createDefaultTabView(context)
            }

            if (tabTitleView == null && TextView::class.java.isInstance(tabView)) {
                tabTitleView = tabView as TextView?
            }

            if (distributeEvenly) {
                val lp = tabView.layoutParams as LinearLayout.LayoutParams
                lp.width = 0
                lp.weight = 1f
            }

            tabTitleView!!.setText(adapter.getPageTitle(i), TextView.BufferType.SPANNABLE)
            val textColor = tabStrip.getCustomTabColorizer()!!.getIndicatorColor(i)
            tabTitleView.setTextColor(textColor)
            tabView.setOnClickListener(tabClickListener)
            val desc = contentDescriptions.get(i, null)
            if (desc != null) {
                tabView.contentDescription = desc
            }

            tabStrip.addView(tabView)
            if (i == viewPager!!.currentItem) {
                tabView.isSelected = true
            }
            i++
        }

        tabStrip.doOnGlobalLayout {
            val firstView = it.getChildAt(0)
            val lastView = it.getChildAt(tabStrip.childCount - 1)
            it.addView(Space(context), 0,
                    FrameLayout.LayoutParams((right - firstView.width) / 2, ViewGroup.LayoutParams.WRAP_CONTENT))
            it.addView(Space(context),
                    FrameLayout.LayoutParams((right - lastView.width) / 2, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    fun setContentDescription(i: Int, desc: String) {
        contentDescriptions.put(i, desc)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (viewPager != null) {
            scrollToTab(viewPager!!.currentItem + 1, 0)
        }
    }

    private fun scrollToTab(tabIndex: Int, positionOffset: Int) {
        val tabStripChildCount = tabStrip.childCount
        if (tabStripChildCount == 1 || tabIndex < 1 || tabIndex >= tabStripChildCount) {
            return
        }

        val selectedChild = tabStrip.getChildAt(tabIndex)
        if (selectedChild != null) {
            val center = width / 2
            val viewLeft = selectedChild.left
            val viewWidth = selectedChild.width
            val targetScrollX = viewLeft - center + viewWidth / 2 + positionOffset

            scrollTo(targetScrollX, 0)
        }
    }

    private inner class InternalViewPagerListener : ViewPager.OnPageChangeListener {
        private var mScrollState: Int = 0
        private var sumPositionAndPositionOffset = 0.0f

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            val currentSumPositionAndPositionOffset = position + positionOffset
            if (positionOffset == 0.0f) {
                tabStrip.scrollState = ScrollState.Idle(position + 1, 0.0f)
            } else if (currentSumPositionAndPositionOffset > sumPositionAndPositionOffset) {
                tabStrip.scrollState = ScrollState.DraggingToLeft(position + 1, positionOffset)
            } else {
                tabStrip.scrollState = ScrollState.DraggingToRight(position + 1, positionOffset)
            }
            sumPositionAndPositionOffset = currentSumPositionAndPositionOffset

            val selectedTitle = tabStrip.getChildAt(position + 1)
            val extraOffset = if (selectedTitle != null)
                (positionOffset * selectedTitle.width).toInt()
            else
                0
            scrollToTab(position + 1, extraOffset)
        }

        override fun onPageScrollStateChanged(state: Int) {
            mScrollState = state
        }

        override fun onPageSelected(position: Int) {
//            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
//                val selectedView = tabStrip.getChildAt(position + 1)
//                tabStrip.scrollState = ScrollState.Idle(position + 1, (selectedView.width / 2).toFloat())
//                scrollToTab(position + 1, 0)
//            }
            for (i in 1 until tabStrip.childCount) {
                tabStrip.getChildAt(i).isSelected = position == i
            }
        }
    }

    private inner class TabClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            for (i in 1 until tabStrip.childCount) {
                if (v === tabStrip.getChildAt(i)) {
                    viewPager!!.currentItem = i - 1
                    return
                }
            }
        }
    }

    internal inner class SlidingTabStrip @JvmOverloads constructor(
            context: Context,
            attrs: AttributeSet? = null
    ) : LinearLayout(context, attrs) {

        private val bottomBorderThickness: Int
        private val bottomBorderPaint: Paint

        private val selectedIndicatorThickness: Int
        private val selectedIndicatorPaint: Paint

        private val defaultBottomBorderColor: Int

        private val path: Path

        var scrollState: ScrollState = ScrollState.Idle(0, 0.0f)
            set(value) {
                field = value
                Log.e("TAG", ">>>> newState: $field")
                invalidate()
            }

        private var customTabColorizer: SlidingTabLayout.TabColorizer? = null
        private val defaultTabColorizer: SimpleTabColorizer

        init {
            setWillNotDraw(false)

            val density = resources.displayMetrics.density

            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorForeground, outValue, true)
            val themeForegroundColor = outValue.data

            defaultBottomBorderColor = setColorAlpha(themeForegroundColor,
                    DEFAULT_BOTTOM_BORDER_COLOR_ALPHA)

            defaultTabColorizer = SimpleTabColorizer()
            defaultTabColorizer.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR)

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

        fun getCustomTabColorizer(): TabColorizer? {
            return customTabColorizer
        }

        fun setSelectedIndicatorColors(vararg colors: Int) {
            // Make sure that the custom colorizer is removed
            customTabColorizer = null
            defaultTabColorizer.setIndicatorColors(*colors)
            invalidate()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            val width = measuredWidth
            val height = (measuredHeight + mFooterIndicatorHeight).toInt()

            setMeasuredDimension(width, height)
        }

        override fun onDraw(canvas: Canvas) {
            val height = height
            val childCount = childCount
            val tabColorizer = customTabColorizer ?: defaultTabColorizer

            // Thick colored underline below the current selection
            val selectedPosition = scrollState.currentPosition
            val selectionOffset = scrollState.offset
            if (childCount > 0) {
                val selectedTitle = getChildAt(selectedPosition)
                var left = selectedTitle.left
                var color = tabColorizer.getIndicatorColor(selectedPosition - 1)

                val nextTitle = when (scrollState) {
                    is ScrollState.Idle -> getChildAt(selectedPosition)
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
                path.moveTo(left.toFloat(), heightMinusLine - mFooterIndicatorHeight)
                path.lineTo(left + mFooterIndicatorHeight, heightMinusLine)
                path.lineTo(left - mFooterIndicatorHeight, heightMinusLine)
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
            val inverseRation = 1f - ratio
            val r = Color.red(color1) * ratio + Color.red(color2) * inverseRation
            val g = Color.green(color1) * ratio + Color.green(color2) * inverseRation
            val b = Color.blue(color1) * ratio + Color.blue(color2) * inverseRation
            return Color.rgb(r.toInt(), g.toInt(), b.toInt())
        }

        private inner class SimpleTabColorizer : SlidingTabLayout.TabColorizer {
            private var mIndicatorColors: IntArray? = null

            override fun getIndicatorColor(position: Int): Int {
                return mIndicatorColors!![position % mIndicatorColors!!.size]
            }

            internal fun setIndicatorColors(vararg colors: Int) {
                mIndicatorColors = colors
            }
        }

    }

    sealed class ScrollState(val currentPosition: Int, val offset: Float) {
        class DraggingToRight(currentPosition: Int, offset: Float) : ScrollState(currentPosition, offset)
        class DraggingToLeft(currentPosition: Int, offset: Float) : ScrollState(currentPosition, offset)
        class Idle(currentPosition: Int, offset: Float) : ScrollState(currentPosition, offset)

        override fun toString(): String {
            return "${javaClass.simpleName}(currentPosition=$currentPosition, offset=$offset)"
        }


    }

    companion object {

        private val DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 0
        private val DEFAULT_BOTTOM_BORDER_COLOR_ALPHA: Byte = 0x26
        private val SELECTED_INDICATOR_THICKNESS_DIPS = 3
        private val DEFAULT_SELECTED_INDICATOR_COLOR = -0xcc4a1b

        private val TITLE_OFFSET_DIPS = 24
        private val TAB_VIEW_PADDING_DIPS = 16
        private val TAB_VIEW_TEXT_SIZE_SP = 12

        //
        val BASE_DENSITY = 160.0f

        fun convertDpToPixel(dp: Float, context: Context): Float {
            val resources = context.resources
            val metrics = resources.displayMetrics
            return dp * (metrics.densityDpi / BASE_DENSITY)
        }

        fun convertDpToPixel(dp: Int, context: Context): Int {
            return convertDpToPixel(dp.toFloat(), context).toInt()
        }

        private inline fun <T : View> T.doOnGlobalLayout(crossinline block: (T) -> Unit) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    block(this@doOnGlobalLayout)
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        @SuppressLint("NewApi")
        private fun ViewTreeObserver.removeOnGlobalLayoutListener(listener: ViewTreeObserver.OnGlobalLayoutListener) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                removeOnGlobalLayoutListener(listener)
            } else {
                removeGlobalOnLayoutListener(listener)
            }
        }
    }

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * [.setCustomTabColorizer].
     */
    interface TabColorizer {

        /**
         * @return return the color of the indicator used when `position` is selected.
         */
        fun getIndicatorColor(position: Int): Int

    }
}

