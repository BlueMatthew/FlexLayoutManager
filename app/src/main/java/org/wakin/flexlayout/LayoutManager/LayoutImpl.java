package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.View;

import org.wakin.flexlayout.LayoutManager.Elements.FlexFlowSection;
import org.wakin.flexlayout.LayoutManager.Elements.FlexItem;
import org.wakin.flexlayout.LayoutManager.Elements.FlexSection;
import org.wakin.flexlayout.LayoutManager.Elements.FlexWaterfallSection;
import org.wakin.flexlayout.util.Algorithm;
import org.wakin.flexlayout.util.Comparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class LayoutImpl {

    private LayoutCallback mLayoutCallback;
    private List<FlexSection> mSections;
    private boolean mStackedStickyItems = true;
    private SortedMap<SectionPosition, Boolean> mStickyItems;

    public LayoutImpl(LayoutCallback layoutCallback) {
        mLayoutCallback = layoutCallback;

        mSections = new ArrayList<>();
        mStickyItems = new TreeMap<>();

    }

    public void addStickyItem(int section, int item) {
        mStickyItems.put(new SectionPosition(section, item), Boolean.FALSE);
    }
    public void clearStickyItems() {
        mStickyItems.clear();
    }
    public void setStackedStickyItems(boolean stackedStickyItems) {
        mStackedStickyItems = stackedStickyItems;
    }
    public boolean isStackedStickyItems() {
        return mStackedStickyItems;
    }


    public int sectionFromAdapterPosition(int position) {
        Comparator<FlexSection, Integer> comparator = new Comparator<FlexSection, Integer>() {
            @Override
            public int compare(FlexSection o1, Integer o2) {
                return o1.getPositionBase() > o2.intValue() ? 1 : (o1.getPositionBase() + o1.getItemCount() < o2.intValue() ? - 1 : 0);
            }
        };

        return Algorithm.upperBound(mSections, new Integer(position), comparator);
    }

    public FlexItem findItem(int position) {
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

    public PointF computeScrollVectorForPosition(FlexLayoutManager layoutManager, int targetPosition, Point contentOffset) {

        FlexItem targetItem = findItem(targetPosition);
        if (targetItem == null) {
            return null;
        }
        Rect rect = new Rect();
        targetItem.getFrameOnView(rect);
        if (layoutManager.getOrientation() == FlexLayoutManager.HORIZONTAL) {
            int targetContentOffset = rect.left;
            return new PointF(contentOffset.x < targetContentOffset ? 1 : -1, 0);
        } else {
            int targetContentOffset = rect.top;
            return new PointF(0, contentOffset.y < targetContentOffset ? 1 : -1);
        }
    }

    public int computeScrollDistanceForPosition(FlexLayoutManager layoutManager, int targetPosition) {
        return computeScrollDistanceForPosition(layoutManager, targetPosition, 0);
    }

    public int computeScrollDistanceForPosition(FlexLayoutManager layoutManager, int targetPosition, int offset) {
        return (layoutManager.getOrientation() == FlexLayoutManager.VERTICAL) ?
                computeScrollDistanceForPositionVertically(layoutManager, targetPosition, offset) :
                computeScrollDistanceForPositionHorizontally(layoutManager, targetPosition, offset);
    }

    public int computeScrollDistanceForPositionVertically(FlexLayoutManager layoutManager, int targetPosition, int offset) {

        FlexItem targetItem = findItem(targetPosition);
        if (targetItem == null) {
            return 0;
        }

        int distance = 0;
        Point stickyItemPoint = new Point(layoutManager.getPaddingLeft(), layoutManager.getPaddingTop());
        Rect rect = new Rect();
        Point origin = new Point();
        Point oldOrigin = new Point();
        Iterator it = mStickyItems.entrySet().iterator();

        int contentOffsetX = layoutManager.getOrientation() == FlexLayoutManager.VERTICAL ? (targetItem.getFrame().left - layoutManager.getPaddingLeft()) : 0;
        int contentOffsetY = layoutManager.getOrientation() == FlexLayoutManager.VERTICAL ? 0 : (targetItem.getFrame().top - layoutManager.getPaddingTop());

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
            rect.offset(-contentOffsetX, -contentOffsetY);
            int stickyItemSize = rect.height();
            origin.set(rect.left, rect.top);

            if (mStackedStickyItems) {
                // origin.y = Math.max(mContentOffset.y + stickyItemPoint.y, origin.y);
                origin.y = Math.max(stickyItemPoint.y, origin.y);

                rect.offsetTo(origin.x, origin.y);
            } else {
                Rect rectSection = section.getFrame();
                origin.y = Math.min(
                        Math.max(layoutManager.getPaddingTop() + layoutManager.getPaddingTop(), (rectSection.top - stickyItemSize)),
                        (rectSection.bottom - stickyItemSize)
                );

                rect.offsetTo(origin.x, origin.y);
            }

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            boolean stickyMode = entry.getValue().booleanValue() ? ((contentOffsetY + layoutManager.getPaddingTop() < oldOrigin.y) ? false : true) : ((rect.top > oldOrigin.y) ? true : false);
            if (stickyMode) {
                // layoutAttributes.zIndex = 1024 + it->first;  //
                stickyItemPoint.y += stickyItemSize;
            }
        }

        targetItem.getFrameOnView(rect);
        distance = rect.top - contentOffsetY - stickyItemPoint.y;

        return distance + offset;
    }

    public int computeScrollDistanceForPositionHorizontally(FlexLayoutManager layoutManager, int targetPosition, int offset) {

        return 0;
    }

    public void prepareLayout(FlexLayoutManager layoutManager, int width, int height, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        mSections.clear();

        Rect bounds = new Rect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);

        int sectionCount = mLayoutCallback.getNumberOfSections();
        int positionBase = 0;

        Rect rectOfSection = new Rect(layoutManager.getPaddingLeft(), layoutManager.getPaddingTop(), layoutManager.getPaddingLeft() + bounds.right, 0);
        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            int layoutMode = mLayoutCallback.getNumberOfItemsInSection(sectionIndex);

            FlexSection section = layoutMode == FlexLayoutManager.WATERFLAALAYOUT ? new FlexWaterfallSection(sectionIndex,  positionBase, mLayoutCallback, rectOfSection) : new FlexFlowSection(sectionIndex,  positionBase, mLayoutCallback, rectOfSection);
            section.prepareLayout(bounds);

            mSections.add(section);

            positionBase += section.getItemCount();
            rectOfSection.bottom = rectOfSection.top + section.getFrame().height();
            rectOfSection.top = rectOfSection.bottom;
        }

        layoutManager.setContentSize(width,rectOfSection.bottom + paddingBottom);
    }

    public List<LayoutItem> filterItems(FlexLayoutManager layoutManager, int width, int height, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom, int contentWidth, int contentHeight, int contentOffsetX, int contentOffsetY) {


        List<FlexItem> items = new LinkedList<>();

        boolean vertical = layoutManager.getOrientation() == FlexLayoutManager.VERTICAL;
        final Rect visibleRect = new Rect(contentOffsetX, contentOffsetY, contentOffsetX + width, contentOffsetY + height);

        Comparator<FlexSection, Rect> comparator = vertical ?
                new FlexSection.RectVerticalComparator() :
                new FlexSection.RectHorizontalComparator();

        int lowerSectionBound = Algorithm.lowerBound(mSections, visibleRect, comparator);
        if (lowerSectionBound == -1) {
            return null;
        }
        int upperSectionBound = Algorithm.upperBound(mSections, visibleRect, comparator);

        mSections.get(lowerSectionBound).mergeItemsInRect(items, visibleRect, vertical);
        for (int sectionIndex = lowerSectionBound + 1; sectionIndex <= upperSectionBound - 1; sectionIndex++) {
            mSections.get(sectionIndex).mergeItems(items);
        }
        if (upperSectionBound > lowerSectionBound) {
            mSections.get(upperSectionBound).mergeItemsInRect(items, visibleRect, vertical);
        }

        List<LayoutItem> visibleItems = new ArrayList<LayoutItem>(items.size() + 2);

        LayoutItem layoutItem = null;
        for (FlexItem flexItem : items) {
            layoutItem = new LayoutItem(flexItem);

            visibleItems.add(layoutItem);
        }

        SortedMap<LayoutItem, View> visibleStickyItems = new TreeMap<>();

        // Go through sticky items first and put all visible and REALLY-STICKY items into a TreeMap of visibleStickyItems
        // with their sticky position, we will add the views of those STICKY items at last to make them higher in z-coordination
        // TODO: delay callback after layout?

        Point stickyItemPoint = new Point(layoutManager.getPaddingLeft(), layoutManager.getPaddingTop());
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
            rect.offset(-contentOffsetX, -contentOffsetY);
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
                        Math.max(layoutManager.getPaddingTop(), (rectSection.top - stickyItemSize)),
                        (rectSection.bottom - stickyItemSize)
                );

                rect.offsetTo(origin.x, origin.y);
            }

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            boolean stickyMode = entry.getValue().booleanValue() ? ((contentOffsetY + layoutManager.getPaddingTop() < oldOrigin.y) ? false : true) : ((rect.top > oldOrigin.y) ? true : false);

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
                layoutItem = new LayoutItem(item);
                layoutItem.getFrame().set(rect);
                layoutItem.setInSticky();

                visibleStickyItems.put(layoutItem, null);
                int itemIndex = Collections.binarySearch(visibleItems, layoutItem);
                if (itemIndex < 0) {
                    visibleItems.add(-itemIndex - 1, layoutItem);
                } else {
                    visibleItems.get(itemIndex).setInSticky();
                }

                // layoutAttributes.zIndex = 1024 + it->first;  //
                stickyItemPoint.y += stickyItemSize;
            }

        }

        return visibleItems;
    }

}
