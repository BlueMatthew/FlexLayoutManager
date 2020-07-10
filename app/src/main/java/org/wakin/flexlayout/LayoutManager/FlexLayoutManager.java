package org.wakin.flexlayout.LayoutManager;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import android.util.Log;
import android.view.View;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.wakin.flexlayout.util.Algorithm;
import org.wakin.flexlayout.util.Comparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;


public class FlexLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "FlexLayoutManager";
    private static boolean DEBUG = true;

    public static final int FLOWLAYOUT = 0;
    public static final int WATERFLAALAYOUT = 1;

    public static final int UpdateActionNone = 0;
    public static final int UpdateActionInsert = 1;
    public static final int UpdateActionDelete = 2;
    public static final int UpdateActionReload = 3;
    public static final int UpdateActionMove = 4;   // Not implemented yet


    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;
    public static final int VERTICAL = OrientationHelper.VERTICAL;

    public static final int INVALID_OFFSET = Integer.MIN_VALUE;

    private int mOrientation = VERTICAL;
    private boolean mReverseLayout = false;

    private LayoutCallback mLayoutCallback;

    OrientationHelper mOrientationHelper;

    private List<FlexSection> mSections;
    private boolean mLayoutInvalidated = false;

    private boolean mStackedStickyItems = true;
    private SortedMap<SectionPosition, Boolean> mStickyItems;

    private int mMinimumPagableSection = NO_POSITION;
    private Point mPagingOffset = new Point();

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

    public FlexLayoutManager() {
        super();

        mOrientation = VERTICAL;
        mContentOffset = new Point();
        mSections = new ArrayList<>();
        mStickyItems = new TreeMap<>();

        setMeasurementCacheEnabled(false);
    }

    public FlexLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super();

        mOrientation = orientation;
        mContentOffset = new Point();
        mSections = new ArrayList<>();
        mStickyItems = new TreeMap<>();

        setMeasurementCacheEnabled(false);
    }

    public LayoutCallback getLayoutCallback() {
        return mLayoutCallback;
    }

    public void setLayoutCallback(LayoutCallback layoutCallback) {
        this.mLayoutCallback = layoutCallback;
        mLayoutInvalidated = true;
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
        mOrientationHelper = null;
        requestLayout();
    }

    public void addStickyItem(int section, int item) {
        mStickyItems.put(new SectionPosition(section, item), Boolean.FALSE);
        requestLayout();
    }

    public void clearStickyItems() {
        mStickyItems.clear();
        requestLayout();
    }

    public boolean getStackedStickyItems() {
        return mStackedStickyItems;
    }

    public void setStackedStickyItems(boolean stackedStickyItems) {
        mStackedStickyItems = stackedStickyItems;
    }

    public void setMinimumPagableSection(int minimumPagableSection) {
        mMinimumPagableSection = minimumPagableSection;
    }

    public void setPagingOffset(Point pagingOffset) {
        if (pagingOffset == null) {
            mPagingOffset.set(0, 0);
            return;
        }
        mPagingOffset.set(pagingOffset.x, pagingOffset.y);
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

        return (null != mLayoutCallback) && (mLayoutCallback.getPageSize() > 1);
         */
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
        /*
        if (mOrientation == VERTICAL) {
            return true;
        }

        return (null != mLayoutCallback) && (mLayoutCallback.getPageSize() > 1);
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
    public RecyclerView.LayoutParams generateDefaultLayoutParams () {
        // return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return new RecyclerView.LayoutParams(0, 0);
    }

    @Override
    public boolean isAutoMeasureEnabled () {
        // return super.isAutoMeasureEnabled();
        return  false;
    }

    @Override
    public void onMeasure (RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {

        int oldWidth = getWidth();
        int oldHeight = getHeight();

        int width = chooseSize(widthSpec, getPaddingLeft() + getPaddingRight(), getMinimumWidth());
        int height = chooseSize(heightSpec, getPaddingTop() + getPaddingBottom(), getMinimumHeight());

        setMeasuredDimension(width, height);
        // super.layout(recycler, state, widthSpec, heightSpec);

        if (oldWidth != width || oldHeight != height) {
            // Size Changed
            mLayoutInvalidated = true;
        }
    }

    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        prepareLayout(recycler, state);


        if (mPendingSavedState != null) {
            // TODO: Consider deferring restore until getItemCount() > 0.
            // restoreFromSavedState(mPendingSavedState);
            mPendingSavedState = null;
        }

        if (mPendingScrollPosition != NO_POSITION) {
            FlexItem item = findItem(mPendingScrollPosition);
            if (item != null) {
                Point contentOffset = mContentOffset;
                PointF distance = computeScrollDistanceForPosition(mPendingScrollPosition);

                if (mPendingScrollPositionOffset == INVALID_OFFSET) {
                    mPendingScrollPositionOffset = 0;
                }

                if (mOrientation == VERTICAL) {
                    contentOffset.y = (int)(distance.y + 0.5) + mPendingScrollPositionOffset;
                    if (contentOffset.y > mContentSizeHeight - getHeight()) {
                        contentOffset.y = mContentSizeHeight - getHeight();
                    }
                } else {
                    contentOffset.x = (int)(distance.x + 0.5) + mPendingScrollPositionOffset;
                    if (contentOffset.x > mContentSizeWidth - getWidth()) {
                        contentOffset.y = mContentSizeWidth - getWidth();
                    }
                }
                mContentOffset = contentOffset;
            }

            mPendingScrollPosition = NO_POSITION;
            mPendingScrollPositionOffset = INVALID_OFFSET;
        }

        // Log.d("Flex", "onLayoutChildren: State=" + state.toString());

        fillRect(recycler, state);
    }

    private void offsetLayoutRectToChildViewRect(FlexItem item, Rect rect) {
        // rect.offset(-mContentOffset.x + getPaddingLeft(), -mContentOffset.y + getPaddingTop());
        rect.offset(-mContentOffset.x, -mContentOffset.y);
        if (mMinimumPagableSection != NO_POSITION && item.getSection() >= mMinimumPagableSection && !mPagingOffset.equals(0, 0)) {
            rect.offset(mPagingOffset.x, mPagingOffset.y);
        }
    }

    void fillRect(RecyclerView.Recycler recycler, RecyclerView.State state) {

        long debugStartTime = 0;
        long debugEndTime = 0;
        long startTime=System.nanoTime();
        if (DEBUG) {
            debugStartTime = System.nanoTime();
        }

        Rect visibleRect = getVisibleRect();

        if (getItemCount() <= 0 || state.isPreLayout()) {
            return;
        }

        List<FlexItem> visibleItems = new LinkedList<>();
        boolean vertical = mOrientation == VERTICAL;

        Comparator<FlexSection, Rect> comparator = vertical ? new FlexSection.RectVerticalComparator() : new FlexSection.RectHorizontalComparator();

        int lowerSectionBound = Algorithm.lowerBound(mSections, visibleRect, comparator);
        if (lowerSectionBound == -1) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        final int upperSectionBound = Algorithm.upperBound(mSections, visibleRect, comparator);

        mSections.get(lowerSectionBound).mergeItemsInRect(visibleItems, visibleRect, vertical);
        for (int sectionIndex = lowerSectionBound + 1; sectionIndex <= upperSectionBound - 1; sectionIndex++) {
            mSections.get(sectionIndex).mergeItems(visibleItems);
        }
        if (upperSectionBound > lowerSectionBound) {
            mSections.get(upperSectionBound).mergeItemsInRect(visibleItems, visibleRect, vertical);
        }

        // Go through sticky items first and put all visible and REALLY-STICKY items into a TreeMap of visibleStickyItems
        // with their sticky position, we will add the views of those STICKY items at last to make them higher in z-coordination
        // TODO: delay callback after layout?
        SortedMap<FlexItem, Pair<Rect, View>> visibleStickyItems = new TreeMap<>();

        Point stickyItemPoint = new Point(getPaddingLeft(), getPaddingTop());
        Rect rect = new Rect();
        Point origin = new Point();
        Point oldOrigin = new Point();

        Iterator it = mStickyItems.entrySet().iterator();
        while (it.hasNext()) {

            SortedMap.Entry<SectionPosition, Boolean> entry = (SortedMap.Entry<SectionPosition, Boolean>)it.next();

            int sectionIndex = entry.getKey().section;
            FlexSection section = mSections.get(sectionIndex);

            if (sectionIndex > upperSectionBound || (!mStackedStickyItems && sectionIndex < lowerSectionBound)) {
                if (entry.getValue().booleanValue()) {
                    entry.setValue(Boolean.FALSE);
                    mLayoutCallback.onItemExitStickyMode(sectionIndex, entry.getKey().item, section.getPositionBase() + entry.getKey().item);
                }
                continue;
            }

            FlexItem item = section.getItem(entry.getKey().item);
            int position = item.getAdapterPosition();

            item.getFrameOnView(rect);
            rect.offset(-mContentOffset.x, -mContentOffset.y);
            int stickyItemSize = rect.height();
            origin.set(rect.left, rect.top);
            oldOrigin.set(origin.x, origin.y);

            if (mStackedStickyItems) {
                // origin.y = Math.max(mContentOffset.y + stickyItemPoint.y, origin.y);
                origin.y = Math.max(stickyItemPoint.y, origin.y);

                rect.offsetTo(origin.x, origin.y);
            } else {
                Rect rectSection = section.getFrame();
                origin.y = Math.min(
                        Math.max(getPaddingTop() + getPaddingTop(), (rectSection.top - stickyItemSize)),
                        (rectSection.bottom - stickyItemSize)
                );

                rect.offsetTo(origin.x, origin.y);
            }

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            boolean stickyMode = entry.getValue().booleanValue() ? ((mContentOffset.y + getPaddingTop() < oldOrigin.y) ? false : true) : ((rect.top > oldOrigin.y) ? true : false);

            if (stickyMode != entry.getValue().booleanValue()) {
                // Notify caller if changed
                entry.setValue(Boolean.valueOf(stickyMode));
                if (stickyMode) {
                    mLayoutCallback.onItemEnterStickyMode(sectionIndex, item.getItem(), position, oldOrigin);
                } else {
                    mLayoutCallback.onItemExitStickyMode(sectionIndex, item.getItem(), position);
                }
            }

            if (stickyMode) {
                visibleStickyItems.put(item, new Pair<Rect, View>(new Rect(rect), null));
                int itemIndex = Collections.binarySearch(visibleItems, item);
                if (itemIndex < 0) {
                    visibleItems.add(-itemIndex - 1, item);
                }

                // layoutAttributes.zIndex = 1024 + it->first;  //
                stickyItemPoint.y += stickyItemSize;
            }

        }

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "fillRect filter takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
            debugStartTime = debugEndTime;
        }

        int childCount = getChildCount();
        int removed = 0;
        View childView = null;

        // Go through child views (currently visible) first,
        // If it is not new-visible (list of visibleItems) and not-sticky, remove it
        // it they are still visible and not sticky, re-layout it to new position
        // For visible and sticky item, put the view object into visibleStickyItems

        SectionPosition sectionPosition = new SectionPosition();
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            childView = getChildAt(childIndex - removed);
            int position = getPosition(childView);
            int sectionIndex = sectionFromAdapterPosition(position);
            FlexSection section = mSections.get(sectionIndex);
            sectionPosition.set(sectionIndex, position - section.getPositionBase());

            FlexItem item = section.getItem(position - section.getPositionBase());

            Pair<Rect, View> stickyItemData = visibleStickyItems.get(item);

            int itemIndex = Collections.binarySearch(visibleItems, item);
            if (itemIndex < 0 && null == stickyItemData) {
                // Currently Invisible and Not-Sticky, Remove it
                removeAndRecycleView(childView, recycler);
                removed++;
            } else {
                // itemIndex >= 0 || null != stickyItemData
                // Currently visible or Sticky
                if (null == stickyItemData) {
                    // Should still be visible after current layout

                    item.getFrameOnView(rect);

                    offsetLayoutRectToChildViewRect(item, rect);
                    layoutDecorated(childView, rect.left, rect.top, rect.right, rect.bottom);

                    // detachAndScrapView(childView, recycler);
                    // removed++;
                    // If it is not-Sticky, it must be in visible list
                    visibleItems.remove(itemIndex);
                    // Log.e(TAG, "Existed visible view:" + Integer.toString(itemIndex));
                } else {
                    // If it is sticky, cache the view
                    visibleStickyItems.put(item, new Pair<Rect, View>(stickyItemData.first, childView));
                    // detachView(childView);
                    // removed++;

                    // Then remove it from visible list if it is in it
                    if (itemIndex >= 0) {
                        visibleItems.remove(itemIndex);
                    }
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
        HashMap<FlexItem, View> views = new HashMap<>();
        for (FlexItem item : visibleItems) {

            if (!visibleStickyItems.containsKey(item)) {
                int position = item.getAdapterPosition();

                childView = recycler.getViewForPosition(position);
                views.put(item, childView);
            }
        }

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "fillRect create views takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
            debugStartTime = debugEndTime;
        }


        for (FlexItem item : visibleItems) {

            if (!visibleStickyItems.containsKey(item)) {

                childView = views.get(item);
                addView(childView);

                item.getFrameOnView(rect);
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
        it = visibleStickyItems.entrySet().iterator();
        while (it.hasNext()) {
            SortedMap.Entry<FlexItem, Pair<Rect, View>> entry = (SortedMap.Entry<FlexItem, Pair<Rect, View>>)it.next();

            Log.d(TAG, "StickyItem: pos=" + entry.getKey().getAdapterPosition() + " rect=" + entry.getValue().first.toString());

            if (null != entry.getValue().second) {
                // detachAndScrapView(entry.getValue().second, recycler);
                // addView(entry.getValue().second);
                detachView(entry.getValue().second);
                attachView(entry.getValue().second);
                layoutDecorated(entry.getValue().second, entry.getValue().first.left, entry.getValue().first.top, entry.getValue().first.right, entry.getValue().first.bottom);
            } else {
                int position = entry.getKey().getAdapterPosition();

                childView = recycler.getViewForPosition(position);
                addView(childView);

                rect = entry.getValue().first;
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

        if (null == mLayoutCallback) {
            return;
        }

        long debugStartTime = 0;
        long debugEndTime = 0;
        long startTime=System.nanoTime();
        if (DEBUG) {
            debugStartTime = System.nanoTime();
        }

        if (mLayoutInvalidated) {

            mSections.clear();

            Rect bounds = new Rect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
            mContentSizeWidth = getWidth();

            int sectionCount = mLayoutCallback.getSectionCount();
            int positionBase = 0;

            Rect rectOfSection = new Rect(getPaddingLeft(), getPaddingTop(), getPaddingLeft() + bounds.right, 0);
            for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
                int layoutMode = mLayoutCallback.getLayoutModeForSection(sectionIndex);

                FlexSection section = layoutMode == WATERFLAALAYOUT ? new FlexWaterfallSection(sectionIndex,  positionBase, mLayoutCallback, rectOfSection) : new FlexFlowSection(sectionIndex,  positionBase, mLayoutCallback, rectOfSection);
                section.prepareLayout(bounds);

                mSections.add(section);

                positionBase += section.getItemCount();
                rectOfSection.bottom = rectOfSection.top + section.getFrame().height();
                rectOfSection.top = rectOfSection.bottom;
            }

            mContentSizeHeight = (int)rectOfSection.bottom + getPaddingBottom();

            mLayoutInvalidated = false;
            // As we have done a complete layout, pending updates can be removed directly
            if (mPendingUpdateItems != null) {
                mPendingUpdateItems.clear();
            }
        }
        else if (mPendingUpdateItems != null && !mPendingUpdateItems.isEmpty()) {
            for (UpdateItem updateItem : mPendingUpdateItems) {
                switch (updateItem.getAction()) {
                    case UpdateActionInsert:
                        break;
                    case UpdateActionReload:
                        break;
                    case UpdateActionDelete:
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
                        break;
                }
            }

            mPendingUpdateItems.clear();
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
        LinearSmoothScroller linearSmoothScroller =
                new LinearSmoothScroller(recyclerView.getContext()) {
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return FlexLayoutManager.this.computeScrollVectorForPosition(targetPosition);
                    }

                    @Override
                    protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                        // final int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
                        // final int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
                        // this.getTargetPosition()
                        final PointF d = FlexLayoutManager.this.computeScrollDistanceForPosition(this.getTargetPosition());
                        final int distance = (int) Math.sqrt(d.x * d.x + d.y * d.y);
                        int dx = (int)d.x;
                        int dy = (int)d.y;
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

    public PointF computeScrollVectorForPosition(int targetPosition) {

        FlexItem targetItem = findItem(targetPosition);
        if (targetItem == null) {
            return null;
        }
        Rect rect = new Rect();
        targetItem.getFrameOnView(rect);
        if (mOrientation == HORIZONTAL) {
            int targetContentOffset = rect.left;
            return new PointF(mContentOffset.x < targetContentOffset ? 1 : -1, 0);
        } else {
            int targetContentOffset = rect.top;
            return new PointF(0, mContentOffset.y < targetContentOffset ? 1 : -1);
        }
    }

    public PointF computeScrollDistanceForPosition(int targetPosition) {
        return computeScrollDistanceForPosition(targetPosition, null);
    }

    public PointF computeScrollDistanceForPosition(int targetPosition, Point offset) {

        PointF distance = new PointF();
        FlexItem targetItem = findItem(targetPosition);


        if (targetItem == null) {
            return distance;
        }

        Point stickyItemPoint = new Point(getPaddingLeft(), getPaddingTop());
        Rect rect = new Rect();
        Point origin = new Point();
        Point oldOrigin = new Point();
        Iterator it = mStickyItems.entrySet().iterator();
        while (it.hasNext()) {

            SortedMap.Entry<SectionPosition, Boolean> entry = (SortedMap.Entry<SectionPosition, Boolean>)it.next();

            int sectionIndex = entry.getKey().section;
            FlexSection section = mSections.get(sectionIndex);

            if (sectionIndex > targetItem.getSection() || (!mStackedStickyItems && sectionIndex < targetItem.getSection())) {
                continue;
            }

            FlexItem item = section.getItem(entry.getKey().item);
            int position = item.getAdapterPosition();

            item.getFrameOnView(rect);
            rect.offset(-mContentOffset.x, -mContentOffset.y);
            int stickyItemSize = rect.height();
            origin.set(rect.left, rect.top);

            if (mStackedStickyItems) {
                // origin.y = Math.max(mContentOffset.y + stickyItemPoint.y, origin.y);
                origin.y = Math.max(stickyItemPoint.y, origin.y);

                rect.offsetTo(origin.x, origin.y);
            } else {
                Rect rectSection = section.getFrame();
                origin.y = Math.min(
                        Math.max(getPaddingTop() + getPaddingTop(), (rectSection.top - stickyItemSize)),
                        (rectSection.bottom - stickyItemSize)
                );

                rect.offsetTo(origin.x, origin.y);
            }

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            boolean stickyMode = entry.getValue().booleanValue() ? ((mContentOffset.y + getPaddingTop() < oldOrigin.y) ? false : true) : ((rect.top > oldOrigin.y) ? true : false);
            if (stickyMode) {
                // layoutAttributes.zIndex = 1024 + it->first;  //
                stickyItemPoint.y += stickyItemSize;
            }
        }

        targetItem.getFrameOnView(rect);
        if (mOrientation == HORIZONTAL) {
            distance.x = rect.left - mContentOffset.x - stickyItemPoint.x;
        } else {
            distance.y = rect.top - mContentOffset.y - stickyItemPoint.y;
        }

        if (offset != null) {
            distance.x += offset.x;
            distance.y += offset.y;
        }

        return distance;
    }

    public void onAttachedToWindow (RecyclerView view) {
        super.onAttachedToWindow(view);

        mLayoutInvalidated = true;
    }

    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);

        mLayoutInvalidated = true;
    }

    public void onItemsChanged (RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);

        mLayoutInvalidated = true;
    }

    public void onItemsAdded (RecyclerView recyclerView, int positionStart, int itemCount) {
        // super.onItemsAdded(recyclerView, positionStart, itemCount);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        // mPendingUpdateItems.add(new UpdateItem(UpdateActionInsert, positionStart, itemCount));
    }

    public void onItemsRemoved (RecyclerView recyclerView, int positionStart, int itemCount) {
        // super.onItemsRemoved(recyclerView, positionStart, itemCount);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        // mPendingUpdateItems.add(new UpdateItem(UpdateActionDelete, positionStart, itemCount));
    }

    public void onItemsUpdated (RecyclerView recyclerView, int positionStart, int itemCount) {
        // super.onItemsUpdated(recyclerView, positionStart, itemCount);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        // mPendingUpdateItems.add(new UpdateItem(UpdateActionReload, positionStart, itemCount));
    }

    public void onItemsUpdated (RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
        if (mPendingUpdateItems == null) {
            mPendingUpdateItems = new ArrayList<>();
        }

        // mPendingUpdateItems.add(new UpdateItem(UpdateActionReload, positionStart, itemCount));
    }

    private Rect getVisibleRect() {
        int x = mContentOffset.x;
        int y = mContentOffset.y;
        return new Rect(x, y, x + getWidth(), y + getHeight());
    }

    public int getVerticalVisibleHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int sectionFromAdapterPosition(int position) {
        Comparator<FlexSection, Integer> comparator = new Comparator<FlexSection, Integer>() {
            @Override
            public int compare(FlexSection o1, Integer o2) {
                return o1.getPositionBase() > o2.intValue() ? 1 : (o1.getPositionBase() + o1.getItemCount() < o2.intValue() ? - 1 : 0);
            }
        };

        return Algorithm.upperBound(mSections, new Integer(position), comparator);
    }

    FlexItem findItem(int position) {
        FlexSection section = null;
        int item = position;
        for (int sectionIndex = 0; sectionIndex < mSections.size(); sectionIndex++) {
            section = mSections.get(sectionIndex);
            if (item >= section.getItemCount()) {
                item -= section.getItemCount();
                continue;
            }

            break;
        }

        return (section == null || item >= section.getItemCount()) ? null : section.getItem(item);
    }

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
