package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;
import android.graphics.Rect;

import org.wakin.flexlayout.util.Comparator;

public class FlexItem implements Comparable<FlexItem> {

    private final FlexSection mSection;
    private int mItem;
    private Rect mFrame;
    private int mFlags = 0;

    public static final int ITEMFLAG_NONE = 0;
    public static final int ITEMFLAG_FULL_SPAN = 0x1;
    public static final int ITEMFLAG_PLACEHOLDER = 0x2;

    public FlexItem(FlexSection section, int item) {
        mSection = section;
        mItem = item;
        mFrame = new Rect();
    }

    public FlexItem(FlexItem src) {
        mSection = src.mSection;
        mItem = src.mItem;
        mFrame = new Rect(src.mFrame);
        mFlags = src.mFlags;
    }

    public void setItem(int item) {
        mItem = item;
    }

    public FlexItem clonePlaceHolder() {
        FlexItem placeHolder = new FlexItem(this);
        placeHolder.setPlaceHolder(true);

        return placeHolder;
    }

    public FlexItem(FlexSection section, int item, Point point, Size size) {
        mSection = section;
        mItem = item;
        mFrame = new Rect(point.x, point.y, point.x + size.width, point.y + size.height);
    }

    public final int getAdapterPosition() {
        return mSection.getPositionBase() + mItem;
    }

    public final void setFullSpan(boolean fullSpan) {
        mFlags = fullSpan ? (mFlags | ITEMFLAG_FULL_SPAN) : (mFlags & (~ITEMFLAG_FULL_SPAN));
    }

    public final void setPlaceHolder(boolean placeHolder) {
        mFlags = placeHolder ? (mFlags | ITEMFLAG_PLACEHOLDER) : (mFlags & (~ITEMFLAG_PLACEHOLDER));
    }

    public final boolean isFullSpan() {
        return (mFlags & ITEMFLAG_FULL_SPAN) == ITEMFLAG_FULL_SPAN;
    }

    public final boolean isPlaceHolder() {
        return (mFlags & ITEMFLAG_PLACEHOLDER) == ITEMFLAG_PLACEHOLDER;
    }

    @Override
    public int compareTo(FlexItem item) {
        return Integer.compare(getAdapterPosition(), item.getAdapterPosition());
    }

    public final int getSection () {
        return mSection.getSection();
    }

    public final int getItem() {
        return mItem;
    }

    public final Rect getFrame() {
        return mFrame;
    }

    public final void getFrameOnView(Rect rect) {
        rect.set(mFrame);
        rect.offset(mSection.getFrame().left, mSection.getFrame().top);
    }

    public final Rect offsetFrame(int dx, int dy) {
        return new Rect(mFrame.left + dx, mFrame.top + dy, mFrame.right + dx, mFrame.bottom + dy);
    }

    public final int getWidth() {
        return mFrame.width();
    }

    public final int getHeight() {
        return mFrame.height();
    }

    public static class RectVerticalComparator implements Comparator<FlexItem, Rect> {
        @Override
        public int compare(FlexItem lhs, Rect rhs) {
            if (lhs.getFrame().bottom < rhs.top) return -1;
            else if (lhs.getFrame().top > rhs.bottom) return 1;
            return 0;
        }

    }

    public static class RectHorizontalComparator implements Comparator<FlexItem, Rect> {
        @Override
        public int compare(FlexItem lhs, Rect rhs) {
            if (lhs.getFrame().right < rhs.left) return -1;
            else if (lhs.getFrame().left > rhs.right) return 1;
            return 0;
        }
    }

    public static class FlexItemComparator implements Comparator<FlexItem, Integer> {
        @Override
        public int compare(FlexItem lhs, Integer rhs) {
            return Integer.compare(lhs.getItem(), rhs.intValue());
        }
    }

    public static class FlexItemPositionComparator implements java.util.Comparator<FlexItem> {
        @Override
        public int compare(FlexItem lhs, FlexItem rhs) {
            return Integer.compare(lhs.getAdapterPosition(), rhs.getItem());
        }
    }
}
