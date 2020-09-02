package org.wakin.flexlayout.LayoutManager;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.wakin.flexlayout.util.Algorithm;
import org.wakin.flexlayout.util.Comparator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;


public class FlexLayoutManager extends RecyclerView.LayoutManager {

    static {
        System.loadLibrary("FlexLayout");
        initLayoutEnv(LayoutCallback.class);
    }

    private static final String TAG = "FlexLayoutManager";
    private static boolean DEBUG = true;
    private static boolean NATIVE = true;

    public static final int FLOWLAYOUT = 0;
    public static final int WATERFLAALAYOUT = 1;

    public static final int LAYOUT_INVALIDATION_NONE = 0;
    public static final int LAYOUT_INVALIDATION_EVERYTHING = 1;
    public static final int LAYOUT_INVALIDATION_DATASOURCECHANGED = 2;
    public static final int LAYOUT_INVALIDATION_PAGES = 4;

    public static final int UPDATE_ACTION_NONE = 0;
    public static final int UPDATE_ACTION_INSERT = 1;
    public static final int UPDATE_ACTION_DELETE = 2;
    public static final int UPDATE_ACTION_RELOAD = 3;
    public static final int UPDATE_ACTION_MOVE = 4;   // Not implemented yet

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;
    public static final int VERTICAL = OrientationHelper.VERTICAL;

    public static final int INVALID_OFFSET = Integer.MIN_VALUE;
    public static final Point INVALID_CONTENT_OFFSET = new Point(INVALID_OFFSET, INVALID_OFFSET);

    private int mOrientation = VERTICAL;
    private boolean mReverseLayout = false;

    private LayoutCallback mLayoutCallback;

    private int mLayoutInvalidation = LAYOUT_INVALIDATION_NONE;

    private int mMinimumPagableSection = NO_POSITION;
    // private Point mPagingOffset = new Point();
    protected int mPagingOffsetX = 0;
    protected int mPagingOffsetY = 0;
    protected int mMinPageOffset = 0;
    protected int mMaxPageOffset = 0;
    protected List<Integer> mPendingPages = null;

    /**
     * When LayoutManager needs to scroll to a position, it sets this variable and requests a
     * layout which will check this variable and re-layout accordingly.
     */
    int mPendingScrollPosition = NO_POSITION;
    /**
     * Used to keep the offset value when {@link #scrollToPositionWithOffset(int, int)} is
     * called.
     */
    int mPendingScrollPositionOffset = INVALID_OFFSET;

    List<UpdateItem> mPendingUpdateItems;

    SavedState mPendingSavedState = null;

    private int mContentSizeWidth;
    private int mContentSizeHeight;

    private Point mContentOffset;

    protected LayoutImpl mLayout = null;
    protected long mNativeLayout = 0;

    public FlexLayoutManager(LayoutCallback layoutCallback) {
        super();

        mOrientation = VERTICAL;
        mContentOffset = new Point();

        setLayoutCallback(layoutCallback);

        setMeasurementCacheEnabled(false);
    }

    public FlexLayoutManager(Context context, int orientation, boolean reverseLayout, LayoutCallback layoutCallback) {
        super();

        mOrientation = orientation;
        mContentOffset = new Point();

        setLayoutCallback(layoutCallback);

        setMeasurementCacheEnabled(false);
    }

    @Override
    protected void finalize() {
        if (mNativeLayout != 0) {
            releaseLayout(mNativeLayout);
        }
    }

    public LayoutCallback getLayoutCallback() {
        return mLayoutCallback;
    }

    public void setLayoutCallback(LayoutCallback layoutCallback) {

        if (NATIVE) {
            if (mNativeLayout != 0) {
                releaseLayout(mNativeLayout);
                mNativeLayout = 0;
            }
        } else {
            mLayout = null;
        }

        this.mLayoutCallback = layoutCallback;

        if (layoutCallback != null) {
            if (NATIVE) {
                mNativeLayout = createLayout(layoutCallback);
            } else {
                mLayout = new LayoutImpl(layoutCallback);
            }
        }

        mLayoutInvalidation |= LAYOUT_INVALIDATION_EVERYTHING;
    }

    /**
     * Returns the current orientaion of the layout.
     *
     * @return Current orientation.
     * @see #mOrientation
     * @see #setOrientation(int)
     */
    public int getOrientation() {
        return mOrientation;
    }
    /**
     * Sets the orientation of the layout.
     * will do its best to keep scroll position.
     *
     * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation:" + orientation);
        }
        assertNotInLayoutOrScroll(null);
        if (orientation == mOrientation) {
            return;
        }
        mOrientation = orientation;
        requestLayout();
    }

    public Point getContentOffset() {
        return mContentOffset;
    }

    public void addStickyItem(int section, int item) {
        if (NATIVE) {
            addStickyItem(mNativeLayout, section, item);
        } else {
            mLayout.addStickyItem(section, item);
        }
        requestLayout();
    }

    public void clearStickyItems() {
        if (NATIVE) {
            clearStickyItems(mNativeLayout);
        } else {
            mLayout.clearStickyItems();
        }
        requestLayout();
    }

    public boolean getStackedStickyItems() {
        if (NATIVE) {
            return isStackedStickyItems(mNativeLayout);
        } else {
            return mLayout.isStackedStickyItems();
        }
    }

    public void setStackedStickyItems(boolean stackedStickyItems) {
        if (NATIVE) {
            setStackedStickyItems(mNativeLayout, stackedStickyItems);
        } else {
            mLayout.setStackedStickyItems(stackedStickyItems);
        }
        requestLayout();
    }

    public void setMinimumPagableSection(int minimumPagableSection) {
        mMinimumPagableSection = minimumPagableSection;
    }

    public void clearPagingOffset() {
        mPagingOffsetX = 0;
        mPagingOffsetY = 0;
        mMinPageOffset = 0;
        mMaxPageOffset = 0;
        mPendingPages = null;

        mLayoutInvalidation |= LAYOUT_INVALIDATION_PAGES;

        //fillRect();
        requestLayout();
    }

    public void setPagingOffset(int pagingOffsetX, int pagingOffsetY) {
        mPagingOffsetX = pagingOffsetX;
        mPagingOffsetY = pagingOffsetY;

        // int pageSize = mOrientation == VERTICAL ? (getWidth() - getPaddingLeft() - getPaddingRight()) : (getHeight() - getPaddingTop() - getPaddingBottom());
        int pageSize = mOrientation == VERTICAL ? getWidth() : getHeight();
        updateActivePages(mOrientation == VERTICAL ? pagingOffsetX : pagingOffsetY, pageSize);
        mLayoutInvalidation |= LAYOUT_INVALIDATION_PAGES;

        requestLayout();
    }

    @Override
    public boolean performAccessibilityAction(RecyclerView.Recycler recycler, RecyclerView.State state, int action, Bundle args) {

        boolean result = super.performAccessibilityAction(recycler, state, action, args);

        if (action == 1) {
            mPagingOffsetX = args.getInt("offsetX");
            mPagingOffsetY = args.getInt("offsetY");

            // int pageSize = mOrientation == VERTICAL ? (getWidth() - getPaddingLeft() - getPaddingRight()) : (getHeight() - getPaddingTop() - getPaddingBottom());
            int pageSize = mOrientation == VERTICAL ? getWidth() : getHeight();
            boolean pageChanged = updateActivePages(mOrientation == VERTICAL ? mPagingOffsetX : mPagingOffsetY, pageSize);

            if (pageChanged) {
                mLayoutInvalidation |= LAYOUT_INVALIDATION_PAGES;
                requestLayout();
            } else {
                fillRect(recycler, state);
            }
            // requestLayout();
            // invalidate();
        }

        return result;
    }

    protected boolean updateActivePages(int pagingOffset, int pageSize) {

        if (pageSize == 0) {
            return false;
        }

        int page = mLayoutCallback.getPage();
        int numberOfPages = mLayoutCallback.getNumberOfPages();
        int pageOffset = pagingOffset > 0 ? (int)Math.ceil(pagingOffset / ((double)pageSize)) : (int)Math.floor(pagingOffset / ((double)pageSize));
        pageOffset = -pageOffset;

        int oldMinPageOffset = mMinPageOffset;
        int oldMaxPageOffset = mMaxPageOffset;

        mMinPageOffset = Math.max(-page, Math.min(pageOffset, mMinPageOffset));
        mMaxPageOffset = Math.min(numberOfPages - page - 1, Math.max(pageOffset, mMaxPageOffset));

        if (mMaxPageOffset - mMinPageOffset > 1) {
            Log.i("Flex", "calc page: min=" + mMinPageOffset + " max=" + mMaxPageOffset);
        }
        if (null == mPendingPages) {
            mPendingPages = new ArrayList<>(4);
        }

        return oldMinPageOffset != mMinPageOffset || oldMaxPageOffset != mMaxPageOffset;
    }

    /**
     * Returns if views are laid out from the opposite direction of the layout.
     *
     * @return If layout is reversed or not.
     * @see {@link #setReverseLayout(boolean)}
     */
    public boolean getReverseLayout() {
        return mReverseLayout;
    }

    public void setReverseLayout(boolean reverseLayout) {
        assertNotInLayoutOrScroll(null);
        if (reverseLayout == mReverseLayout) {
            return;
        }
        mReverseLayout = reverseLayout;

        requestLayout();
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
        /*
        if (mOrientation == HORIZONTAL) {
            return true;
        }

        return (null != mLayoutCallback) && (mLayoutCallback.getNumberOfPages() > 1);
         */
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
        /*
        if (mOrientation == VERTICAL) {
            return true;
        }

        return (null != mLayoutCallback) && (mLayoutCallback.getNumberOfPages() > 1);
        */
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mOrientation == VERTICAL) {
            return 0;
        }

        int actualDx = 0;
        if (dx > 0) {
            int remainingX = mContentSizeWidth - getWidth() - mContentOffset.x;
            actualDx = Math.min(dx, remainingX);
        } else {
            actualDx = -Math.min(-dx, mContentOffset.x);
        }
        mContentOffset.offset(actualDx, 0);

        if (actualDx != 0) {
            fillRect(recycler, state);
        }

        return actualDx;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mOrientation == HORIZONTAL) {
            return 0;
        }

        int actualDy = 0;
        if (dy > 0) {
            int remainingY = mContentSizeHeight - getHeight() - mContentOffset.y;
            actualDy = Math.min(dy, remainingY);
        } else {
            actualDy = -Math.min(-dy, mContentOffset.y);
        }

        mContentOffset.offset(0, actualDy);
        // Log.d("Flex", "Scroll: ContentSize=" + Integer.toString(mContentSizeHeight) + " ContentOffset.y=" + Integer.toString(mContentOffset.y) + " actualDy=" + Integer.toString(actualDy));

        if (actualDy != 0) {
            fillRect(recycler, state);
        }

        return actualDy;
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        return mContentOffset.x;
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return mContentOffset.y;
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        return getWidth();
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return getHeight();
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        return mContentSizeWidth;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return mContentSizeHeight;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        // return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return new RecyclerView.LayoutParams(0, 0);
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        // return super.isAutoMeasureEnabled();
        return false;
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {

        int oldWidth = getWidth();
        int oldHeight = getHeight();

        int width = chooseSize(widthSpec, getPaddingLeft() + getPaddingRight(), getMinimumWidth());
        int height = chooseSize(heightSpec, getPaddingTop() + getPaddingBottom(), getMinimumHeight());

        setMeasuredDimension(width, height);
        // super.layout(recycler, state, widthSpec, heightSpec);

        if (oldWidth != width || oldHeight != height) {
            // Size Changed
            mLayoutInvalidation |= LAYOUT_INVALIDATION_EVERYTHING;
        }
    }

    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        prepareLayout(recycler, state);

        if (mPendingSavedState != null) {
            // TODO: Consider deferring restore until getItemCount() > 0.
            // restoreFromSavedState(mPendingSavedState);
            mPendingSavedState = null;
        }

        applyPendingScrollPositionWithOffset();

        // Log.d("Flex", "onLayoutChildren: State=" + state.toString());

        fillRect(recycler, state);
    }

    private void offsetLayoutRectToChildViewRect(LayoutItem item, Rect rect) {
        // rect.offset(-mContentOffset.x + getPaddingLeft(), -mContentOffset.y + getPaddingTop());
        rect.offset(-mContentOffset.x, -mContentOffset.y);
        if (mMinimumPagableSection != NO_POSITION && item.getSection() >= mMinimumPagableSection && (mPagingOffsetX != 0 || mPagingOffsetY != 0)) {

            rect.offset(mPagingOffsetX, mPagingOffsetY);
        }
    }

    void fillRect(RecyclerView.Recycler recycler, RecyclerView.State state) {

        long debugStartTime = 0;
        long debugEndTime = 0;
        if (DEBUG) {
            debugStartTime = System.nanoTime();
        }

        if (getItemCount() <= 0 || state.isPreLayout()) {
            return;
        }

        List<LayoutItem> visibleItems = null;

        if (NATIVE) {
            int page = mLayoutCallback.getPage();
            int[] displayInfo = FlexLayoutHelper.makeDisplayInfo(this, mLayoutCallback, page + mMinPageOffset, page + mMaxPageOffset, mPagingOffsetX);
            int[] data = filterItems(mNativeLayout, displayInfo);

            Log.i("Flex", "fillRect page=" + page + "min=" + mMaxPageOffset + " max=" + mMaxPageOffset);

            List<LayoutItem> changingStickyItems = new ArrayList<>();
            visibleItems = FlexLayoutHelper.unserializeLayoutItemAndStickyItems(data, changingStickyItems);

            if (mMaxPageOffset >= mMinPageOffset) {
                for (LayoutItem layoutItem : visibleItems) {
                    Log.i("Flex", "visibleItems: page=" + layoutItem.getPage() + " pos=" + layoutItem.getPosition() + " section=" + layoutItem.getSection() +
                            " item=" + layoutItem.getItem() + " pt=" + layoutItem.getFrame().left + "," + layoutItem.getFrame().top);
                }
            }

            for (LayoutItem layoutItem : changingStickyItems) {
                if (layoutItem.isInSticky()) {
                    Point pt = new Point(layoutItem.getFrame().left, layoutItem.getFrame().top);
                    mLayoutCallback.onItemEnterStickyMode(layoutItem.getSection(), layoutItem.getItem(), layoutItem.getPosition(), pt);
                    Log.d(TAG, "EnterChange: Enter " + layoutItem.getSection() + " at: " + pt.toString());
                } else {
                    mLayoutCallback.onItemExitStickyMode(layoutItem.getSection(), layoutItem.getItem(), layoutItem.getPosition());
                    Log.d(TAG, "EnterChange Leave " + layoutItem.getSection());
                }
            }

        } else {
            visibleItems = mLayout.filterItems(this, getWidth(), getHeight(), getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom(),
                    mContentSizeWidth, mContentSizeHeight, mContentOffset.x, mContentOffset.y);

        }

        if (visibleItems == null || visibleItems.isEmpty())
        {
            removeAndRecycleAllViews(recycler);
            return;
        }

        if (DEBUG) {

            debugEndTime = System.nanoTime();

            Log.d("PERF", "fillRect filter takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
            debugStartTime = debugEndTime;
        }

        SortedMap<LayoutItem, View> visibleStickyItems = new TreeMap<>();

        // Rect rect = new Rect();
        int childCount = getChildCount();
        int removed = 0;
        View childView = null;

        // Go through child views (currently visible) first,
        // If it is not new-visible (list of visibleItems) and not-sticky, remove it
        // it they are still visible and not sticky, re-layout it to new position
        // For visible and sticky item, put the view object into visibleStickyItems
        Comparator<LayoutItem, Integer> layoutItemComparator = new Comparator<LayoutItem, Integer>() {
            @Override
            public int compare(LayoutItem o1, Integer o2) {
                return Integer.compare(o1.getPosition(), o2.intValue());
            }
        };

        Rect rect = new Rect();
        SectionPosition sectionPosition = new SectionPosition();
        LayoutItem layoutItem = null;
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            childView = getChildAt(childIndex - removed);
            int position = getPosition(childView);

            int itemIndex = Algorithm.binarySearch(visibleItems, new Integer(position), layoutItemComparator);
            if (itemIndex < 0) {
                // Currently Invisible and Not-Sticky, Remove it
                removeAndRecycleView(childView, recycler);
                removed++;
            } else {
                // Currently visible
                layoutItem = visibleItems.get(itemIndex);

                if (layoutItem.isInSticky() && layoutItem.isOriginChanged()) {
                    // Sticky
                    visibleStickyItems.put(layoutItem, childView);

                    // detachView(childView);
                    // removed++;

                    // Then remove it from visible list if it is in it
                    if (itemIndex >= 0) {
                        // visibleItems.remove(itemIndex);
                    }

                } else {

                    rect.set(layoutItem.getFrame());
                    offsetLayoutRectToChildViewRect(layoutItem, rect);

                    layoutDecorated(childView, rect.left, rect.top, rect.right, rect.bottom);

                    // detachAndScrapView(childView, recycler);
                    // removed++;
                    // If it is not-Sticky, it must be in visible list
                    visibleItems.remove(itemIndex);

                }
            }
        }

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "fillRect children takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
            debugStartTime = debugEndTime;
        }


        // detachAndScrapAttachedViews(recycler);
        // For new visible and not-sticky items, add them

        // Debug.startMethodTracing();
        HashMap<LayoutItem, View> views = new HashMap<>();
        for (LayoutItem item : visibleItems) {

            if (!item.isInSticky()) {
                int position = item.getPosition();

                try {
                    childView = recycler.getViewForPosition(position);
                }
                catch (Exception ex) {
                    childView = recycler.getViewForPosition(position);
                    Log.e("Flex", ex.getMessage());
                }
                views.put(item, childView);
            }
        }
        // Debug.stopMethodTracing();

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "fillRect create views takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
            debugStartTime = debugEndTime;
        }


        for (LayoutItem item : visibleItems) {
            if (item.isInSticky() && item.isOriginChanged()) {
                // If the origin is changing, we will layout the view later to put it higher in z-order
                // visibleStickyItems.put()
                if (!visibleStickyItems.containsKey(item)) {
                    visibleStickyItems.put(item, null);
                }
            } else {

                childView = recycler.getViewForPosition(item.getPosition());

                // childView = views.get(item);
                addView(childView);

                rect.set(item.getFrame());
                // item.getFrameOnView(rect);
                // TODO: decoration margins

                childView.measure(View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(rect.height(), View.MeasureSpec.EXACTLY));

                offsetLayoutRectToChildViewRect(item, rect);

                layoutDecorated(childView, rect.left, rect.top, rect.right, rect.bottom);
            }
        }

        /*
        for (FlexItem item : visibleItems) {

            if (!visibleStickyItems.containsKey(item)) {
                int position = item.getAdapterPosition();

                View childView = recycler.getViewForPosition(position);
                addView(childView);

                item.getFrameOnView(rect);
                // TODO: decoration margins

                childView.measure(View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(rect.height(), View.MeasureSpec.EXACTLY));

                offsetLayoutRectToChildViewRect(item, rect);

                Log.e(TAG, "Visible Item:" + item.getAdapterPosition() + " rect=" + rect.toString());
                layoutDecorated(childView, rect.left, rect.top, rect.right, rect.bottom);
            }
        }
        */

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "fillRect fill new item takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
            debugStartTime = debugEndTime;
        }


        // Sticky items
        // If it is in visible right now, detach/attach it and then re-layout it to new position
        // If it is not visible, add them and then layout it
        Iterator it = visibleStickyItems.entrySet().iterator();
        while (it.hasNext()) {
            SortedMap.Entry<LayoutItem, View> entry = (SortedMap.Entry<LayoutItem, View>)it.next();

            rect.set(entry.getKey().getFrame());
            Log.d(TAG, "LayoutItem: pos=" + entry.getKey().getPosition() + " rect=" + rect.toString());

            if (null != entry.getValue()) {
                // detachAndScrapView(entry.getValue().second, recycler);
                // addView(entry.getValue().second);
                detachView(entry.getValue());
                attachView(entry.getValue());

                layoutDecorated(entry.getValue(), rect.left, rect.top, rect.right, rect.bottom);
            } else {
                int position = entry.getKey().getPosition();

                childView = recycler.getViewForPosition(position);
                addView(childView);

                // TODO: decoration margins
                childView.measure(View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(rect.height(), View.MeasureSpec.EXACTLY));

                layoutDecorated(childView, rect.left, rect.top, rect.right, rect.bottom);
            }
        }

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "fillRect takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
        }
    }

    protected void prepareLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (null == mLayoutCallback || getItemCount() == 0) {
            return;
        }

        long debugStartTime = 0;
        long debugEndTime = 0;
        long startTime=System.nanoTime();
        if (DEBUG) {
            debugStartTime = System.nanoTime();
        }

        if (mLayoutInvalidation != LAYOUT_INVALIDATION_NONE) {

            if (NATIVE) {
                int[] layoutInfo = FlexLayoutHelper.makeLayoutAndSectionsInfo(this, mLayoutCallback);
                int page = mLayoutCallback.getPage();
                int pageStart = page + mMinPageOffset;
                int pageCount = page + mMaxPageOffset - pageStart + 1;
                prepareLayout(mNativeLayout, mLayoutCallback, pageStart, pageCount, layoutInfo);
            } else {
                mLayout.prepareLayout(this, getWidth(), getHeight(), getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
            }

            if (mPendingUpdateItems != null && !mPendingUpdateItems.isEmpty()) {
                for (UpdateItem updateItem : mPendingUpdateItems) {
                    switch (updateItem.getAction()) {
                        case UPDATE_ACTION_INSERT:
                            break;
                        case UPDATE_ACTION_RELOAD:
                            break;
                        case UPDATE_ACTION_DELETE:
                        /*
                        int sectionIndex = sectionFromAdapterPosition(updateItem.getPostionStart());
                        if (sectionIndex != -1) {
                            FlexSection section = mSections.get(sectionIndex);
                            int item = updateItem.getPostionStart() - section.getPositionBase();
                            int itemCountToDelete = updateItem.getItemCount();
                            while (itemCountToDelete > 0) {
                                int itemCount = Math.min(itemCountToDelete, section.getItemCount() - item);
                                itemCountToDelete -= itemCount;

                                if (itemCount == section.getItemCount()) {
                                    // Delete Current Section

                                } else {

                                }
                            }
                        }
                         */
                            break;
                    }
                }

                mPendingUpdateItems.clear();
            }

            mLayoutInvalidation = LAYOUT_INVALIDATION_NONE;
            // As we have done a complete layout, pending updates can be removed directly
            if (mPendingUpdateItems != null) {
                mPendingUpdateItems.clear();
            }
        }

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "prepareLayout takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
        }
    }


    @Override
    public void scrollToPosition(int position) {
        if (mPendingSavedState != null) {
            mPendingSavedState.invalidateAnchor();
        }
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = INVALID_OFFSET;

        requestLayout();
    }

    public void scrollToPositionWithOffset(int position, int offset) {
        if (mPendingSavedState != null) {
            mPendingSavedState.invalidateAnchor();
        }
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = offset;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        final PointF vector = computeScrollVectorForPosition(position);
        if (vector == null) {
            return;
        }
        LinearSmoothScroller linearSmoothScroller =
                new LinearSmoothScroller(recyclerView.getContext()) {
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return vector;
                    }

                    @Override
                    protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                        // final int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
                        // final int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
                        int distance = 0;
                        if (NATIVE) {
                            int[] layoutInfo = FlexLayoutHelper.makeLayoutInfo(FlexLayoutManager.this, mLayoutCallback, mMinPageOffset, mMaxPageOffset, null);
                            int contentOffset = computerContentOffsetToMakePositionTopVisible(mNativeLayout, layoutInfo, this.getTargetPosition(), 0);
                            if (getOrientation() == VERTICAL) {
                                distance = Math.abs(mContentOffset.y - contentOffset);
                            } else {
                                distance = Math.abs(mContentOffset.y - contentOffset);
                            }
                        } else {
                            distance = Math.abs(FlexLayoutManager.this.mLayout.computeScrollDistanceForPosition(FlexLayoutManager.this, this.getTargetPosition()));
                        }

                        int dx = 0;
                        int dy = 0;
                        if (getOrientation() == VERTICAL) {
                            dy = distance;
                        } else {
                            dx = distance;
                        }
                        final int time = calculateTimeForDeceleration(distance);
                        if (time > 0) {
                            action.update(dx, dy, time, mDecelerateInterpolator);
                        }
                    }
                };
        linearSmoothScroller.setTargetPosition(position);
        // linearSmoothScroller.
        startSmoothScroll(linearSmoothScroller);
    }

    /*
    public void onAttachedToWindow (RecyclerView view) {
        mLayoutInvalidation = true;
        super.onAttachedToWindow(view);
    }
     */

    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        mLayoutInvalidation |= LAYOUT_INVALIDATION_EVERYTHING;
        super.onAdapterChanged(oldAdapter, newAdapter);
    }

    public void onItemsChanged (RecyclerView recyclerView) {
        mLayoutInvalidation |= LAYOUT_INVALIDATION_EVERYTHING;
        super.onItemsChanged(recyclerView);
    }

    public void onItemsAdded (RecyclerView recyclerView, int positionStart, int itemCount) {
        // super.onItemsAdded(recyclerView, positionStart, itemCount);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        mPendingUpdateItems.add(new UpdateItem(UPDATE_ACTION_INSERT, positionStart, itemCount));
        mLayoutInvalidation |= LAYOUT_INVALIDATION_DATASOURCECHANGED;

    }

    public void onItemsRemoved (RecyclerView recyclerView, int positionStart, int itemCount) {
        // super.onItemsRemoved(recyclerView, positionStart, itemCount);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        mPendingUpdateItems.add(new UpdateItem(UPDATE_ACTION_DELETE, positionStart, itemCount));
        mLayoutInvalidation |= LAYOUT_INVALIDATION_DATASOURCECHANGED;
    }

    public void onItemsUpdated (RecyclerView recyclerView, int positionStart, int itemCount) {
        // super.onItemsUpdated(recyclerView, positionStart, itemCount);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        mPendingUpdateItems.add(new UpdateItem(UPDATE_ACTION_RELOAD, positionStart, itemCount));
        mLayoutInvalidation |= LAYOUT_INVALIDATION_DATASOURCECHANGED;
    }

    public void onItemsUpdated (RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        mPendingUpdateItems.add(new UpdateItem(UPDATE_ACTION_RELOAD, positionStart, itemCount));
        mLayoutInvalidation |= LAYOUT_INVALIDATION_DATASOURCECHANGED;
    }

    public int getVerticalVisibleHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected void setContentSize(int width, int height) {
        mContentSizeWidth = width + getPaddingLeft() + getPaddingRight();
        mContentSizeHeight = height + getPaddingTop() + getPaddingBottom();
    }

    protected PointF computeScrollVectorForPosition(int position) {
        PointF pt = null;
        if (NATIVE) {
            int[] data = getItemRect(mNativeLayout, 0, position);
            Rect rect = FlexLayoutHelper.intArrayToRect(data);
            if (rect != null) {
                if (getOrientation() == HORIZONTAL) {
                    int targetContentOffset = rect.left;
                    pt = new PointF(mContentOffset.x < targetContentOffset ? 1 : -1, 0);
                } else {
                    int targetContentOffset = rect.top;
                    pt = new PointF(0, mContentOffset.y < targetContentOffset ? 1 : -1);
                }
            }
        } else {
            pt = mLayout.computeScrollVectorForPosition(this, position, mContentOffset);
        }
        return pt;
    }

    protected void applyPendingScrollPositionWithOffset() {

        if (mPendingScrollPosition != NO_POSITION) {

            if (NATIVE) {
                if (mPendingScrollPositionOffset == INVALID_OFFSET) {
                    mPendingScrollPositionOffset = 0;
                }
                int[] layoutInfo = FlexLayoutHelper.makeLayoutInfo(this, mLayoutCallback);
                int contentOffset = computerContentOffsetToMakePositionTopVisible(mNativeLayout, layoutInfo, mPendingScrollPosition, mPendingScrollPositionOffset);
                if (contentOffset != INVALID_OFFSET) {
                    if (mOrientation == VERTICAL) {
                        if (contentOffset > mContentSizeHeight - getHeight()) {
                            mContentOffset.y = mContentSizeHeight - getHeight();
                        }
                        mContentOffset.y = contentOffset;
                    } else {
                        if (contentOffset > mContentSizeWidth - getWidth()) {
                            contentOffset = mContentSizeWidth - getWidth();
                        }
                        mContentOffset.x = contentOffset;
                    }
                }

            } else {

                int distance = mLayout.computeScrollDistanceForPosition(this, mPendingScrollPosition, mPendingScrollPositionOffset);

                if (mOrientation == VERTICAL) {
                    mContentOffset.y = distance;
                    if (mContentOffset.y > mContentSizeHeight - getHeight()) {
                        mContentOffset.y = mContentSizeHeight - getHeight();
                    }
                } else {
                    mContentOffset.x = distance;
                    if (mContentOffset.x > mContentSizeWidth - getWidth()) {
                        mContentOffset.y = mContentSizeWidth - getWidth();
                    }
                }
            }

            mPendingScrollPosition = NO_POSITION;
            mPendingScrollPositionOffset = INVALID_OFFSET;
        }
    }

    protected static native void initLayoutEnv(Class callbackClass);
    protected native long createLayout(LayoutCallback layoutCallback);
    protected native void addStickyItem(long layout, int section, int item);
    protected native void clearStickyItems(long layout);
    protected native void setStackedStickyItems(long layout, boolean stackedStickyItems);
    protected native boolean isStackedStickyItems(long layout);
    protected native void prepareLayout(long layout, LayoutCallback layoutCallback, int pageStart, int pageCount, int[] layoutInfo);
    // protected native int[] filterItems(long layout, int orientation, int width, int height, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom, int contentWidth, int contentHeight, int contentOffsetX, int contentOffsetY);
    protected native int[] filterItems(long layout, int[] layoutInfo);
    protected native int[] getItemRect(long layout, int page, int position);
    protected native int computerContentOffsetToMakePositionTopVisible(long layout, int[] layoutInfo, int position, int positionOffset);
    protected native void releaseLayout(long layout);

    static class SavedState implements Parcelable {
        int mAnchorPosition;
        int mAnchorOffset;
        boolean mAnchorLayoutFromEnd;
        public SavedState() {
        }
        SavedState(Parcel in) {
            mAnchorPosition = in.readInt();
            mAnchorOffset = in.readInt();
            mAnchorLayoutFromEnd = in.readInt() == 1;
        }
        public SavedState(SavedState other) {
            mAnchorPosition = other.mAnchorPosition;
            mAnchorOffset = other.mAnchorOffset;
            mAnchorLayoutFromEnd = other.mAnchorLayoutFromEnd;
        }
        boolean hasValidAnchor() {
            return mAnchorPosition >= 0;
        }
        void invalidateAnchor() {
            mAnchorPosition = NO_POSITION;
        }
        @Override
        public int describeContents() {
            return 0;
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mAnchorPosition);
            dest.writeInt(mAnchorOffset);
            dest.writeInt(mAnchorLayoutFromEnd ? 1 : 0);
        }
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }
            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static class UpdateItem {
        private int mAction;

        private int mPostionStart;
        private int mItemCount;

        public UpdateItem(int action, int positionStart, int itemCount) {
            mAction = action;
            mPostionStart = positionStart;
            mItemCount = itemCount;
        }

        public int getAction() {
            return mAction;
        }

        public int getPostionStart() {
            return mPostionStart;
        }

        public int getItemCount() {
            return mItemCount;
        }

    }

}
