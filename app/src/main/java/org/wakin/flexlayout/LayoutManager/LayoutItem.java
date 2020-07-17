package org.wakin.flexlayout.LayoutManager;

import android.graphics.Rect;

import org.wakin.flexlayout.LayoutManager.Elements.FlexItem;

import java.util.List;

public class LayoutItem implements Comparable<LayoutItem> {

    private int mSection;
    private int mItem;
    private int mPosition = 0;
    private Rect mFrame;
    private int mData;
    // private Point mOroginalPoint = null;

    public LayoutItem() {
        this(0, 0, 0);
    }

    public LayoutItem(int section, int item, int position) {
        mSection = section;
        mItem = item;
        mPosition = position;
        mData = 0;
        mFrame = new Rect();
    }

    public LayoutItem(FlexItem item) {
        mSection = item.getSection();
        mItem = item.getItem();
        mData = 0;
        mPosition = item.getAdapterPosition();
        mFrame = new Rect();
        item.getFrameOnView(mFrame);
    }

    public LayoutItem(int section, int item, int position, int data, int left, int top, int right, int bottom) {
        mSection = section;
        mItem = item;
        mPosition = position;
        mData = data;
        mFrame = new Rect(left, top, right, bottom);
    }

    public LayoutItem(int section, int item, int position, int data, Rect frame) {
        this(section, item, position, data, frame.left, frame.top, frame.right, frame.bottom);
    }

    public int getSection() {
        return mSection;
    }

    public int getItem() {
        return mItem;
    }


    public boolean isInSticky() {
        return (mData & 1) == 1;
    }
    public boolean isOriginChanged() {
        return (mData & 2) == 2;
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

    public void setOriginChanged() {
        mData |= 2;
    }
    public void setInSticky() {
        mData |= 1;
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
