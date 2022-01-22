package org.wakin.flexlayout.impl.elements;

import android.graphics.Point;
import android.graphics.Rect;

import org.wakin.flexlayout.util.Algorithm;
import org.wakin.flexlayout.util.Comparator;

import java.util.ArrayList;
import java.util.List;

public class FlexRow {
    private List<FlexItem> mItems;
    private Rect mFrame;

    public FlexRow(int estimatedItems) {
        mItems = new ArrayList<>(estimatedItems);
        mFrame = new Rect();
    }

    public FlexRow(int estimatedItems, Point point) {
        mItems = new ArrayList<>(estimatedItems <= 0 ? 1 : estimatedItems);
        mFrame = new Rect(point.x, point.y, 0, 0);
    }

    public final Rect getFrame() {
        return mFrame;
    }

    public final List<FlexItem> getItems() {
        return mItems;
    }

    public final boolean hasItems() {
        return !mItems.isEmpty();
    }

    public final int getItemCount() {
        return mItems.size();
    }

    public final void addItem(FlexItem item)
    {
        mItems.add(item);
    }

    public final void addItemVertically(FlexItem item)
    {
        mItems.add(item);
        mFrame.right += item.getWidth();
        if (mFrame.height() < item.getHeight()) mFrame.bottom = mFrame.top + item.getHeight();
    }

    public final void addItemHorizontally(FlexItem item)
    {
        mItems.add(item);
        mFrame.bottom += item.getHeight();
        if (mFrame.width() < item.getWidth()) mFrame.right = mFrame.left + item.getWidth();
    }

    public int deleteItemsFrom(FlexItem item) {
        if (!mItems.isEmpty()) {
            Integer itemToFind = new Integer(item.getItem() - 1);
            int pos = Algorithm.lowerBound(mItems, itemToFind, new FlexItem.FlexItemComparator());
            if (pos >= 0) {
                mItems.subList(pos, mItems.size()).clear();

                int maxHeight = 0;
                for (FlexItem it : mItems) {
                    if (it.getFrame().height() > maxHeight) {
                        maxHeight = it.getFrame().height();
                    }
                }

                mFrame.bottom = mFrame.top + maxHeight;
                return pos;
            }
        }

        return -1;
    }

    public int compareToItem(FlexItem item) {
        if (mItems.isEmpty()) return -1;
        if (mItems.get(0).getItem() > item.getItem()) return 1;
        else if (mItems.get(mItems.size() - 1).getItem() < item.getItem()) return -1;
        return 0;
    }

    public int mergeItemsInBounds(List<FlexItem> items, Rect rect, Comparator<FlexItem, Rect> boundComparator, Comparator<FlexItem, Rect> filterComparator) {
        int lowerBound = Algorithm.lowerBound(mItems, rect, boundComparator);
        if (lowerBound == -1) {
            return 0;
        }

        int upperBound = Algorithm.upperBound(mItems, rect, boundComparator);

        int matchedItems = 0;
        for (int itemIndex = lowerBound; itemIndex <= upperBound; itemIndex++) {
            if (filterComparator.compare(mItems.get(itemIndex), rect) == 0) {
                items.add(mItems.get(itemIndex));
                matchedItems++;
            }
        }

        return matchedItems;
    }

    public int mergeItemsInBounds(List<FlexItem> items, Rect rect, Comparator<FlexItem, Rect> boundComparator) {
        int lowerBound = Algorithm.lowerBound(mItems, rect, boundComparator);
        if (lowerBound == -1) {
            return 0;
        }

        int upperBound = Algorithm.upperBound(mItems, rect, boundComparator);

        for (int itemIndex = lowerBound; itemIndex <= upperBound; itemIndex++) {
            items.add(mItems.get(itemIndex));
        }
        return upperBound - lowerBound + 1;
    }

    public int mergeItems(List<FlexItem> items) {
        items.addAll(mItems);
        return mItems.size();
    }

    public static class FlexRowItemComparator implements Comparator<FlexRow, FlexItem> {
        @Override
        public int compare(FlexRow lhs, FlexItem rhs) {
            return lhs.compareToItem(rhs);
        }
    }

    public static class RectVerticalComparator implements Comparator<FlexRow, Rect> {
        @Override
        public int compare(FlexRow lhs, Rect rhs) {
            if (lhs.getFrame().bottom < rhs.top) return -1;
            else if (rhs.top > lhs.getFrame().bottom) return 1;
            return 0;
        }
    }

    public static class RectHorizontalComparator implements Comparator<FlexRow, Rect> {
        @Override
        public int compare(FlexRow lhs, Rect rhs) {
            if (lhs.getFrame().right < rhs.left) return -1;
            else if (rhs.left > lhs.getFrame().right) return 1;
            return 0;
        }
    }

}
