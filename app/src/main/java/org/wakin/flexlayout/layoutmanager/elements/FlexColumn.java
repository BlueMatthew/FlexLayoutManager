package org.wakin.flexlayout.layoutmanager.elements;

import android.graphics.Rect;

import org.wakin.flexlayout.util.Algorithm;
import org.wakin.flexlayout.util.Comparator;

import java.util.ArrayList;
import java.util.List;

public class FlexColumn {
    private List<FlexItem> mItems;
    private Rect mFrame;

    public FlexColumn(int estimatedItems, Rect frame) {
        mItems = new ArrayList<>(estimatedItems);
        mFrame = new Rect(frame);
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

    public final void addItemVertically(FlexItem item) {
        mItems.add(item);
        mFrame.bottom = item.getFrame().bottom;
    }

    public final void addItemHorizontally(FlexItem item) {
        mItems.add(item);
        mFrame.right += item.getFrame().right;
    }

    // return the first item deleted in the column
    public int deleteItemsFrom(FlexItem item) {
        if (!mItems.isEmpty()) {
            Integer itemToFind = new Integer(item.getItem() - 1);
            int pos = Algorithm.lowerBound(mItems, itemToFind, new FlexItem.FlexItemComparator());
            if (pos >= 0) {
                mItems.subList(pos, mItems.size()).clear();
                if (mItems.isEmpty()) {
                    mFrame.bottom = mFrame.top;
                } else {
                    mFrame.bottom = mItems.get(mItems.size() - 1).getFrame().bottom;
                }
                return pos;
            }
        }

        return -1;
    }

    public int mergeItemsInBounds(List<FlexItem> items, Rect rect, org.wakin.flexlayout.util.Comparator<FlexItem, Rect> boundComparator, org.wakin.flexlayout.util.Comparator<FlexItem, Rect> filterComparator) {
        int lowerBound = Algorithm.lowerBound(mItems, rect, boundComparator);
        if (lowerBound == -1) {
            return 0;
        }

        int upperBound = Algorithm.upperBound(mItems, rect, boundComparator);

        int matchedItems = 0;
        for (int itemIndex = lowerBound; itemIndex <= upperBound; itemIndex++) {
            FlexItem item = mItems.get(itemIndex);
            if (!item.isPlaceHolder() && filterComparator.compare(mItems.get(itemIndex), rect) == 0) {
                items.add(mItems.get(itemIndex));
                matchedItems++;
            }
        }

        return matchedItems;
    }

    public int mergeItemsInBounds(List<FlexItem> items, Rect rect, org.wakin.flexlayout.util.Comparator<FlexItem, Rect> boundComparator) {
        int lowerBound = Algorithm.lowerBound(mItems, rect, boundComparator);
        if (lowerBound == -1) {
            return 0;
        }

        int upperBound = Algorithm.upperBound(mItems, rect, boundComparator);

        int matchedItems = 0;
        for (int itemIndex = lowerBound; itemIndex <= upperBound; itemIndex++) {
            FlexItem item = mItems.get(itemIndex);
            if (!item.isPlaceHolder()) {
                items.add(mItems.get(itemIndex));
                matchedItems++;
            }

        }
        return matchedItems;
    }

    public int mergeItems(List<FlexItem> items) {
        items.addAll(mItems);
        return mItems.size();
    }

    public static class RectVerticalComparator implements Comparator<FlexColumn, Rect> {
        @Override
        public int compare(FlexColumn lhs, Rect rhs) {
            if (lhs.getFrame().bottom < rhs.top) return -1;
            else if (lhs.getFrame().top > rhs.bottom) return 1;
            return 0;
        }
    }

    public static class RectHorizontalComparator implements Comparator<FlexColumn, Rect> {
        @Override
        public int compare(FlexColumn lhs, Rect rhs) {
            if (lhs.getFrame().right < rhs.left) return -1;
            else if (lhs.getFrame().left > rhs.right) return 1;
            return 0;
        }
    }

}
