package org.wakin.flexlayout.LayoutManager;

import android.graphics.Rect;

import java.util.List;

public class LayoutItem implements Comparable<LayoutItem> {

    private int mSection;
    private int mItem;
    private boolean mInSticky;
    private Rect mFrame;
    private int mPosition = 0;

    public LayoutItem(int section, int item, int position) {
        mSection = section;
        mItem = item;
        mPosition = position;
        mInSticky = false;
        mFrame = new Rect();
    }

    public LayoutItem(FlexItem item) {
        mSection = item.getSection();
        mItem = item.getItem();
        mInSticky = false;
        mPosition = item.getAdapterPosition();
        mFrame = new Rect();
        item.getFrameOnView(mFrame);
    }

    public LayoutItem(int section, int item, int position, boolean inSticky, int left, int top, int right, int bottom) {
        mSection = section;
        mItem = item;
        mPosition = position;
        mInSticky = inSticky;
        mFrame = new Rect(left, top, right, bottom);
    }

    public int getSection() {
        return mSection;
    }

    public int getItem() {
        return mItem;
    }

    public boolean isInSticky() {
        return mInSticky;
    }

    public void setInSticky(boolean inSticky) {
        mInSticky = inSticky;
    }

    public Rect getFrame() {
        return mFrame;
    }

    public void setFrame(Rect frame) {
        mFrame.set(frame);
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    @Override
    public int compareTo(LayoutItem layoutItem) {
        int result = Integer.compare(getSection(), layoutItem.getSection());
        if (result == 0) {
            result = Integer.compare(getItem(), layoutItem.getItem());
        }

        return result;
    }

    public static int search(List<LayoutItem> layoutItemList, FlexItem item) {
        if (layoutItemList.size() <= 4) {

        } else {

        }

        return 0;
    }
}
