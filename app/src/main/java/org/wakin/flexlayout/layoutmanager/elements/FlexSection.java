package org.wakin.flexlayout.layoutmanager.elements;

import android.graphics.Point;
import android.graphics.Rect;

import org.wakin.flexlayout.layoutmanager.LayoutCallback;
import org.wakin.flexlayout.layoutmanager.graphics.Size;
import org.wakin.flexlayout.util.Comparator;

import java.util.ArrayList;
import java.util.List;

public abstract class FlexSection {

    protected int mSection;
    protected int mPositionBase;
    protected LayoutCallback mLayoutCallback;
    protected Rect mFrame;
    protected ArrayList<FlexItem> mItems;
    protected int mMinInvalidatedItem;

    public FlexSection(int section, int positionBase, LayoutCallback layoutCallback, Rect initialFrame) {
        mSection = section;
        mPositionBase = positionBase;
        mLayoutCallback = layoutCallback;
        mFrame = new Rect(initialFrame);

        mItems = new ArrayList<>();
    }

    public final int getSection() {
        return mSection;
    }

    public final void setSection(int section) {
        mSection = section;
    }

    public abstract void prepareLayout(Rect bounds);

    public final void setPositonBase(int positonBase) {
        mPositionBase = positonBase;
    }

    public final int getPositionBase() {
        return mPositionBase;
    }

    public final int getItemCount() {
        return mItems.size();
    }

    public final FlexItem getItem(int item) {
        if (item < 0 || item >= mItems.size()) {
            return null;
        }

        return mItems.get(item);
    }

    public Rect getFrame() {
        return mFrame;
    }

    public int deleteItems(int itemStart, int itemCount) {
        mItems.subList(itemStart, itemStart + itemCount).clear();
        for (int itemIndex = itemStart; itemIndex < getItemCount(); itemIndex++) {
            FlexItem item = mItems.get(itemIndex);
            item.setItem(itemIndex - itemStart);
        }

        mMinInvalidatedItem = itemStart;
        return itemCount;
    }

    public int reloadItems(int itemStart, int itemCount) {
        int itemEnd = Math.min(itemStart + itemCount, mItems.size());
        Rect rect = null;
        Size size = new Size();
        boolean fullSpan = false;
        for (int itemIndex = itemStart; itemIndex < itemEnd; itemIndex++)
        {
            FlexItem item = mItems.get(itemIndex);
            mLayoutCallback.getSizeForItem(mSection, itemIndex, size);
            item.setFullSpan(mLayoutCallback.isFullSpanAtItem(mSection, itemIndex));
            rect = item.getFrame();
            rect.set(rect.left, rect.top, rect.left + size.width, rect.top + size.height);
        }

        mMinInvalidatedItem = itemStart;

        return itemEnd - itemStart;
    }

    public int insertItems(int itemStart, int itemCount) {
        mItems.ensureCapacity(mItems.size() + itemCount);
        ArrayList<FlexItem> items = new ArrayList<>(itemCount);
        int itemEnd = mItems.size();
        for (int itemIndex = itemStart; itemIndex < itemEnd; itemIndex++) {
            FlexItem item = mItems.get(itemIndex);
            item.setItem(itemIndex + itemCount);
        }
        itemEnd = itemStart + itemCount;
        Point origin = new Point();
        Size size = new Size();
        boolean isFullSpan = false;
        for (int itemIndex = itemStart; itemIndex < itemEnd; itemIndex++) {
            mLayoutCallback.getSizeForItem(mSection, itemIndex, size);
            FlexItem item = new FlexItem(this, itemIndex, origin, size);
            item.setFullSpan(mLayoutCallback.isFullSpanAtItem(mSection, itemIndex));

            items.add(item);
        }

        mItems.addAll(itemStart, items);

        mMinInvalidatedItem = itemStart;

        return itemCount;
    }

    public abstract int mergeItemsInRect(List<FlexItem> items, Rect rect, boolean vertical);

    public int mergeItems(List<FlexItem> items) {
        items.addAll(mItems);
        return mItems.size();
    }

    public static class RectVerticalComparator implements Comparator<FlexSection, Rect> {
        @Override
        public int compare(FlexSection lhs, Rect rhs) {
            if (lhs.getFrame().bottom < rhs.top) return -1;
            else if (lhs.getFrame().top > rhs.bottom) return 1;
            return 0;
        }

    }

    public static class RectHorizontalComparator implements Comparator<FlexSection, Rect> {
        @Override
        public int compare(FlexSection lhs, Rect rhs) {
            if (lhs.getFrame().right < rhs.left) return -1;
            else if (lhs.getFrame().left > rhs.right) return 1;
            return 0;
        }
    }

}
