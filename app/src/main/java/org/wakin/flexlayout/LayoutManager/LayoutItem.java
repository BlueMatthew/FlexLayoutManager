package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;
import android.graphics.Rect;

import java.util.List;

public class LayoutItem implements Comparable<LayoutItem> {

    private int mSection;
    private int mItem;
    private int mData;
    private Rect mFrame;
    private int mPosition = 0;
    private Point mOroginalPoint = null;

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

    public LayoutItem(int section, int item, int position, boolean inSticky, int left, int top, int right, int bottom) {
        mSection = section;
        mItem = item;
        mPosition = position;
        mData = 0;
        if (inSticky) {
            mData |= 1;
        }
        mFrame = new Rect(left, top, right, bottom);
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

    public void setOriginalPoint(int x, int y) {
        if (mOroginalPoint == null) {
            mOroginalPoint = new Point(x, y);
        } else {
            mOroginalPoint.set(x, y);
        }
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
