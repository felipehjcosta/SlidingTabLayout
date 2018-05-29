package com.github.felipehjcosta.slidingtablayout

import android.content.Context
import android.graphics.Typeface
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.view.forEachIndexed
import com.github.felipehjcosta.slidingtablayout.SlidingTabLayout.TabColorizer
import android.view.ViewGroup.LayoutParams as ViewGroupLayoutParams
import android.widget.LinearLayout.LayoutParams as LinearLayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT as LinearLayoutMatchParent
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT as LinearLayoutWrapContent

//import android.widget.LinearLayout.LayoutParams.MATCH_PARENT as LinearLayoutMatchParent
//import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT as LinearLayoutWrapContent

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
class SlidingTabLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {

    private var titleOffset = 0

    private var tabViewLayoutId = 0
    private var tabViewTextViewId = 0
    private var shouldDistributeEvenly = false

    private var viewPager: ViewPager? = null
    private val contentDescriptions = SparseArray<String>()
    private var viewPagerPageChangeListener: ViewPager.OnPageChangeListener? = null

    private val tabStrip = SlidingTabStrip(context)

    init {
        addView(tabStrip, ViewGroupLayoutParams.MATCH_PARENT, ViewGroupLayoutParams.WRAP_CONTENT)
        // Disable the Scroll Bar
        isHorizontalScrollBarEnabled = false
        // Make sure that the Tab Strips fills this View
        isFillViewport = true

        titleOffset = (TITLE_OFFSET_DIPS * resources.displayMetrics.density).toInt()

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
    fun setTabColorizer(tabColorizer: TabColorizer) {
        tabStrip.tabColorizer = tabColorizer
    }

    fun setDistributeEvenly(distributeEvenly: Boolean) {
        this.shouldDistributeEvenly = distributeEvenly
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same color.
     */
//    fun setSelectedIndicatorColors(vararg colors: Int) {
//        tabStrip.setSelectedIndicatorColors(*colors)
//    }

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

        this.viewPager?.removeOnPageChangeListener(viewPagerPageChangeListener!!)

        this.viewPager = viewPager
        this.viewPager?.let {
            it.addOnPageChangeListener(viewPagerPageChangeListener!!)
            populateTabStrip()
        }
    }

    private fun extractResource(resIdRes: Int): Int {
        return with(TypedValue()) {
            context.theme.resolveAttribute(resIdRes, this, true)
            resourceId
        }
    }

    private fun populateTabStrip() {
        val adapter = viewPager!!.adapter
        val tabClickListener = TabClickListener()

        if (adapter.isNullOrEmpty()) {
            return
        }

        for (index in 0 until adapter!!.count) {
            val (tabView, tabTitleView) = createTabViewAndTitleView()

            if (shouldDistributeEvenly) {
                distributeEvenly(tabView)
            }

            tabTitleView?.setText(adapter.getPageTitle(index), TextView.BufferType.SPANNABLE)
            val textColor = tabStrip.tabColorizer.getIndicatorColor(index)
            tabTitleView?.setTextColor(textColor)
            tabView?.setOnClickListener(tabClickListener)
            val contentDescription = contentDescriptions.get(index, null)
            contentDescription?.let { tabView?.contentDescription = it }

            tabStrip.addView(tabView)
            if (index == viewPager?.currentItem) {
                tabView?.isSelected = true
            }
        }

        addSpaces()
    }

    private fun createTabViewAndTitleView(): Pair<View?, TextView?> {
        return when {
            shouldCreateTabViewFromLayoutId() -> createTabViewFromLayoutId()
            else -> createDefaultTabView()
        }
    }

    private fun shouldCreateTabViewFromLayoutId(): Boolean = tabViewLayoutId != 0

    private fun createTabViewFromLayoutId(): Pair<View?, TextView?> {
        val tabView = LayoutInflater.from(context).inflate(tabViewLayoutId, tabStrip, false)
        val tabTitleView = tabView!!.findViewById<View>(tabViewTextViewId) as TextView
        return Pair<View?, TextView?>(tabView, tabTitleView)
    }

    private fun createDefaultTabView(): Pair<View?, TextView?> {
        val tabTitleView = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextSize(COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayoutParams(LinearLayoutWrapContent, LinearLayoutWrapContent)

            setBackgroundResource(extractResource(android.R.attr.selectableItemBackground))
            setAllCaps(true)

            val padding = (TAB_VIEW_PADDING_DIPS * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        return Pair<View?, TextView?>(tabTitleView, tabTitleView)
    }

    private fun distributeEvenly(tabView: View?) {
        (tabView?.layoutParams as LinearLayout.LayoutParams).let {
            it.width = 0
            it.weight = 1.0f
        }
    }

    private fun PagerAdapter?.isNullOrEmpty(): Boolean = this == null || this.count == 0

    private fun addSpaces() {
        tabStrip.doOnGlobalLayout {
            val firstIndex = 0
            val lastIndex = tabStrip.childCount - 1
            val firstView = it.getChildAt(firstIndex)
            val lastView = it.getChildAt(lastIndex)
            val firstWidth = (right - firstView.width) / 2
            val lastWidth = (right - lastView.width) / 2
            val firstLayoutParams = LinearLayoutParams(firstWidth, LinearLayoutParams.WRAP_CONTENT)
            val lastLayoutParams = LinearLayoutParams(lastWidth, LinearLayoutParams.WRAP_CONTENT)
            it.addView(Space(context), 0, firstLayoutParams)
            it.addView(Space(context), lastLayoutParams)
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

        tabStrip.getChildAt(tabIndex)?.let { selectedChild ->
            val center = width / 2
            val viewLeft = selectedChild.left
            val viewWidth = selectedChild.width
            val targetScrollX = viewLeft - center + viewWidth / 2 + positionOffset

            scrollTo(targetScrollX, 0)
        }
    }

    private inner class InternalViewPagerListener : ViewPager.OnPageChangeListener {
        private var viewPagerScrollState: Int = 0
        private var sumPositionAndPositionOffset = 0.0f

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            val currentSumPositionAndPositionOffset = position + positionOffset
            tabStrip.scrollState = when {
                positionOffset == 0.0f -> ScrollState.Idle(position + 1, 0.0f)
                currentSumPositionAndPositionOffset > sumPositionAndPositionOffset ->
                    ScrollState.DraggingToLeft(position + 1, positionOffset)
                else -> ScrollState.DraggingToRight(position + 1, positionOffset)
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
            viewPagerScrollState = state
        }

        override fun onPageSelected(position: Int) {
            tabStrip.forEachIndexed { index, view -> view.isSelected = (position == index) }
        }
    }

    private inner class TabClickListener : View.OnClickListener {
        override fun onClick(clickView: View) {
            tabStrip.forEachIndexed { index, view ->
                if (clickView === view) {
                    viewPager?.currentItem = index - 1
                    return@forEachIndexed
                }
            }
        }
    }

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * [.setTabColorizer].
     */
    interface TabColorizer {

        /**
         * @return return the color of the indicator used when `position` is selected.
         */
        fun getIndicatorColor(position: Int): Int
    }

    class SimpleTabColorizer(private val indicatorColors: IntArray) : SlidingTabLayout.TabColorizer {
        override fun getIndicatorColor(position: Int): Int {
            return indicatorColors[position % indicatorColors.size]
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
        private const val TITLE_OFFSET_DIPS = 24
        private const val TAB_VIEW_PADDING_DIPS = 16
        private const val TAB_VIEW_TEXT_SIZE_SP = 12.0f
    }
}
