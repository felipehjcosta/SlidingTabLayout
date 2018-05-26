package com.github.felipehjcosta.slidingtablayout;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.Space;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 * <p>
 * To use the component, simply add it to your view hierarchy. Then in your
 * {@link android.app.Activity} or {@link android.support.v4.app.Fragment} call
 * {@link #setViewPager(ViewPager)} providing it the ViewPager this layout is being used for.
 * <p>
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via {@link #setSelectedIndicatorColors(int...)}. The
 * alternative is via the {@link TabColorizer} interface which provides you complete control over
 * which color is used for any individual position.
 * <p>
 * The views used as tabs can be customized by calling {@link #setCustomTabView(int, int)},
 * providing the layout ID of your custom layout.
 */
public class SlidingTabLayout extends HorizontalScrollView {
    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * {@link #setCustomTabColorizer(TabColorizer)}.
     */
    public interface TabColorizer {

        /**
         * @return return the color of the indicator used when {@code position} is selected.
         */
        int getIndicatorColor(int position);

    }

    private static final int TITLE_OFFSET_DIPS = 24;
    private static final int TAB_VIEW_PADDING_DIPS = 16;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 12;

    private int titleOffset;
    private float mFooterIndicatorHeight;

    private int tabViewLayoutId;
    private int tabViewTextViewId;
    private boolean distributeEvenly;

    private ViewPager viewPager;
    private SparseArray<String> contentDescriptions = new SparseArray<>();
    private ViewPager.OnPageChangeListener viewPagerPageChangeListener;

    private final SlidingTabStrip tabStrip;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        tabStrip = new SlidingTabStrip(context);
        addView(tabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        init(context, attrs);
    }

    @TargetApi(21)
    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        tabStrip = new SlidingTabStrip(context);
        addView(tabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        titleOffset = (int) (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density);

        mFooterIndicatorHeight = convertDpToPixel(20, context);
        viewPagerPageChangeListener = new InternalViewPagerListener();
    }

    //
    public static final float BASE_DENSITY = 160.0f;

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / BASE_DENSITY);
    }

    public static int convertDpToPixel(int dp, Context context) {
        return (int) convertDpToPixel((float) dp, context);
    }

    /**
     * Set the custom {@link TabColorizer} to be used.
     * <p>
     * If you only require simple custmisation then you can use
     * {@link #setSelectedIndicatorColors(int...)} to achieve
     * similar effects.
     */
    public void setCustomTabColorizer(TabColorizer tabColorizer) {
        tabStrip.setCustomTabColorizer(tabColorizer);
    }

    public void setDistributeEvenly(boolean distributeEvenly) {
        this.distributeEvenly = distributeEvenly;
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same color.
     */
    public void setSelectedIndicatorColors(int... colors) {
        tabStrip.setSelectedIndicatorColors(colors);
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId  id of the {@link TextView} in the inflated view
     */
    public void setCustomTabView(@LayoutRes int layoutResId, @IdRes int textViewId) {
        tabViewLayoutId = layoutResId;
        tabViewTextViewId = textViewId;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    public void setViewPager(ViewPager viewPager) {
        tabStrip.removeAllViews();

        if (this.viewPager != null) {
            this.viewPager.removeOnPageChangeListener(viewPagerPageChangeListener);
        }

        this.viewPager = viewPager;
        if (viewPager != null) {
            viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
            populateTabStrip();
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via
     * {@link #setCustomTabView(int, int)}.
     */
    protected TextView createDefaultTabView(Context context) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true);
        textView.setBackgroundResource(outValue.resourceId);
        textView.setAllCaps(true);

        int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding, padding, padding);

        return textView;
    }

    private void populateTabStrip() {
        final PagerAdapter adapter = viewPager.getAdapter();
        final OnClickListener tabClickListener = new TabClickListener();

        if (adapter == null || adapter.getCount() == 0) {
            return;
        }

        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            View tabView = null;
            TextView tabTitleView = null;

            if (tabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(getContext()).inflate(tabViewLayoutId, tabStrip,
                        false);
                tabTitleView = (TextView) tabView.findViewById(tabViewTextViewId);
            }

            if (tabView == null) {
                tabView = createDefaultTabView(getContext());
            }

            if (tabTitleView == null && TextView.class.isInstance(tabView)) {
                tabTitleView = (TextView) tabView;
            }

            if (distributeEvenly) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
                lp.width = 0;
                lp.weight = 1;
            }

            tabTitleView.setText(adapter.getPageTitle(i), TextView.BufferType.SPANNABLE);
            int textColor = tabStrip.getCustomTabColorizer().getIndicatorColor(i);
            tabTitleView.setTextColor(textColor);
            tabView.setOnClickListener(tabClickListener);
            String desc = contentDescriptions.get(i, null);
            if (desc != null) {
                tabView.setContentDescription(desc);
            }

            tabStrip.addView(tabView);
            if (i == viewPager.getCurrentItem()) {
                tabView.setSelected(true);
            }
        }

        runOnGlobalLayout(tabStrip, new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View firstView = tabStrip.getChildAt(0);
                View lastView = tabStrip.getChildAt(tabStrip.getChildCount() - 1);
                tabStrip.addView(new Space(getContext()), 0,
                        new LayoutParams((getRight() - firstView.getWidth()) / 2, ViewGroup.LayoutParams.WRAP_CONTENT));
                tabStrip.addView(new Space(getContext()),
                        new LayoutParams((getRight() - lastView.getWidth()) / 2, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        });
    }

    public static void runOnGlobalLayout(final View view, final ViewTreeObserver.OnGlobalLayoutListener listener) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                listener.onGlobalLayout();
                removeOnGlobalLayoutListener(view, this);
            }
        });
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void removeOnGlobalLayoutListener(View view, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        } else {
            view.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        }
    }

    public void setContentDescription(int i, String desc) {
        contentDescriptions.put(i, desc);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (viewPager != null) {
            scrollToTab(viewPager.getCurrentItem() + 1, 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = tabStrip.getChildCount();
        if (tabStripChildCount == 1 || tabIndex < 1 || tabIndex >= tabStripChildCount) {
            return;
        }

        View selectedChild = tabStrip.getChildAt(tabIndex);
        if (selectedChild != null) {
            int center = getWidth() / 2;
            int viewLeft = selectedChild.getLeft();
            int viewWidth = selectedChild.getWidth();
            int targetScrollX = (viewLeft - center) + (viewWidth / 2) + positionOffset;

            scrollTo(targetScrollX, 0);
        }
    }

    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        private int mScrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            tabStrip.onViewPagerPageChanged(position + 1, positionOffset);

            View selectedTitle = tabStrip.getChildAt(position + 1);
            int extraOffset = (selectedTitle != null)
                    ? (int) (positionOffset * selectedTitle.getWidth())
                    : 0;
            scrollToTab(position + 1, extraOffset);

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mScrollState = state;
        }

        @Override
        public void onPageSelected(int position) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                View selectedView = tabStrip.getChildAt(position + 1);
                tabStrip.onViewPagerPageChanged(position + 1, selectedView.getWidth() / 2);
                scrollToTab(position + 1, 0);
            }
            for (int i = 1; i < tabStrip.getChildCount(); i++) {
                tabStrip.getChildAt(i).setSelected(position == i);
            }
        }
    }

    private class TabClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 1; i < tabStrip.getChildCount(); i++) {
                if (v == tabStrip.getChildAt(i)) {
                    viewPager.setCurrentItem(i - 1);
                    return;
                }
            }
        }
    }

    class SlidingTabStrip extends LinearLayout {

        private static final int DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 0;
        private static final byte DEFAULT_BOTTOM_BORDER_COLOR_ALPHA = 0x26;
        private static final int SELECTED_INDICATOR_THICKNESS_DIPS = 3;
        private static final int DEFAULT_SELECTED_INDICATOR_COLOR = 0xFF33B5E5;

        private final int bottomBorderThickness;
        private final Paint bottomBorderPaint;

        private final int selectedIndicatorThickness;
        private final Paint selectedIndicatorPaint;

        private final int defaultBottomBorderColor;

        private Path path;

        private int selectedPosition;
        private float selectionOffset;

        private SlidingTabLayout.TabColorizer customTabColorizer;
        private final SimpleTabColorizer defaultTabColorizer;

        SlidingTabStrip(Context context) {
            this(context, null);
        }

        SlidingTabStrip(Context context, AttributeSet attrs) {
            super(context, attrs);
            setWillNotDraw(false);

            final float density = getResources().getDisplayMetrics().density;

            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.colorForeground, outValue, true);
            final int themeForegroundColor = outValue.data;

            defaultBottomBorderColor = setColorAlpha(themeForegroundColor,
                    DEFAULT_BOTTOM_BORDER_COLOR_ALPHA);

            defaultTabColorizer = new SimpleTabColorizer();
            defaultTabColorizer.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR);

            bottomBorderThickness = (int) (DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS * density);
            bottomBorderPaint = new Paint();
            bottomBorderPaint.setColor(defaultBottomBorderColor);

            selectedIndicatorThickness = (int) (SELECTED_INDICATOR_THICKNESS_DIPS * density);
            selectedIndicatorPaint = new Paint();
            path = new Path();
        }

        void setCustomTabColorizer(SlidingTabLayout.TabColorizer customTabColorizer) {
            this.customTabColorizer = customTabColorizer;
            invalidate();
        }

        public TabColorizer getCustomTabColorizer() {
            return customTabColorizer;
        }

        void setSelectedIndicatorColors(int... colors) {
            // Make sure that the custom colorizer is removed
            customTabColorizer = null;
            defaultTabColorizer.setIndicatorColors(colors);
            invalidate();
        }

        void onViewPagerPageChanged(int position, float positionOffset) {
            selectedPosition = position;
            selectionOffset = positionOffset;
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = getMeasuredWidth();
            int height = (int) (getMeasuredHeight() + mFooterIndicatorHeight);

            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final int height = getHeight();
            final int childCount = getChildCount();
            final SlidingTabLayout.TabColorizer tabColorizer = customTabColorizer != null
                    ? customTabColorizer
                    : defaultTabColorizer;

            // Thick colored underline below the current selection
            if (childCount > 0) {
                View selectedTitle = getChildAt(selectedPosition);
                int left = selectedTitle.getLeft();
                int right = selectedTitle.getRight();
                int color = tabColorizer.getIndicatorColor(selectedPosition - 1);

                int nextTitleWidth = 0;
                View nextTitle = getChildAt(selectedPosition);
                Log.e(getClass().getSimpleName(), ">>> selectedPosition: " + selectedPosition + ", selectionOffset: " + selectionOffset);
                int tabStripCountWithoutSpaces = tabStrip.getChildCount() - 2;
                if (selectionOffset > 0f && selectedPosition < tabStripCountWithoutSpaces - 1) {
                    int indicatorColor = selectedPosition + (selectedPosition < tabStripCountWithoutSpaces - 1 ? 1 : 0);
                    int nextColor = tabColorizer.getIndicatorColor(indicatorColor);
                    if (color != nextColor) {
                        color = blendColors(nextColor, color, selectionOffset);
                    }

                    nextTitle = getChildAt(selectedPosition + 1);
                }
                nextTitleWidth = nextTitle.getWidth();
                left = (int) (selectionOffset * nextTitle.getLeft() +
                        (1.0f - selectionOffset) * left) + nextTitleWidth / 2;

                selectedIndicatorPaint.setColor(color);
                float footerLineHeight = 0;
                float heightMinusLine = height - footerLineHeight;

                path.reset();
                path.moveTo(left, heightMinusLine - mFooterIndicatorHeight);
                path.lineTo(left + mFooterIndicatorHeight, heightMinusLine);
                path.lineTo(left - mFooterIndicatorHeight, heightMinusLine);
                path.close();
                canvas.drawPath(path, selectedIndicatorPaint);
            }

        }

        /**
         * Set the alpha value of the {@code color} to be the given {@code alpha} value.
         */
        private int setColorAlpha(int color, byte alpha) {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }

        /**
         * Blend {@code color1} and {@code color2} using the given ratio.
         *
         * @param ratio of which to blend. 1.0 will return {@code color1}, 0.5 will give an even blend,
         *              0.0 will return {@code color2}.
         */
        private int blendColors(int color1, int color2, float ratio) {
            final float inverseRation = 1f - ratio;
            float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
            float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
            float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
            return Color.rgb((int) r, (int) g, (int) b);
        }

        private class SimpleTabColorizer implements SlidingTabLayout.TabColorizer {
            private int[] mIndicatorColors;

            @Override
            public final int getIndicatorColor(int position) {
                return mIndicatorColors[position % mIndicatorColors.length];
            }

            void setIndicatorColors(int... colors) {
                mIndicatorColors = colors;
            }
        }

    }
}

