package org.wakin.flexlayout.layoutmanager;

import android.graphics.Rect;

import org.wakin.flexlayout.layoutmanager.elements.FlexItem;

import java.util.List;

public class LayoutItem implements Comparable<LayoutItem> {

    private int mPage;
    private int mSection;
    private int mItem;
    private int mPosition = 0;
    private Rect mFrame;
    private boolean mInSticky = false;
    private boolean mOriginChanged = false;
    // private Point mOroginalPoint = null;

    public LayoutItem() {
        this(0, 0, 0);
    }

    public LayoutItem(int section, int item, int position) {
        this(0, section, item, position);
    }

    public LayoutItem(int page, int section, int item, int position) {
        mPage = page;
        mSection = section;
        mItem = item;
        mPosition = position;
        mInSticky = false;
        mOriginChanged = false;
        mFrame = new Rect();
    }

    public LayoutItem(FlexItem item) {
        mPage = 0;
        mSection = item.getSection();
        mItem = item.getItem();
        mInSticky = false;
        mOriginChanged = false;
        mPosition = item.getAdapterPosition();
        mFrame = new Rect();
        item.getFrameOnView(mFrame);
    }

    public LayoutItem(int section, int item, int position, int inSticky, int originChanged, int left, int top, int right, int bottom) {
        this(0, section, item, position, inSticky, originChanged, left, top, right, bottom);
    }

    public LayoutItem(int page, int section, int item, int position, int inSticky, int originChanged, int left, int top, int right, int bottom) {
        mPage = page;
        mSection = section;
        mItem = item;
        mPosition = position;
        mInSticky = (inSticky != 0);
        mOriginChanged = (originChanged != 0);
        mFrame = new Rect(left, top, right, bottom);
    }

    public LayoutItem(int section, int item, int position, int inSticky, int originChanged, Rect frame) {
        this(0, section, item, position, inSticky, originChanged, frame.left, frame.top, frame.right, frame.bottom);
    }

    public LayoutItem(int page, int section, int item, int position, int inSticky, int originChanged, Rect frame) {
        this(page, section, item, position, inSticky, originChanged, frame.left, frame.top, frame.right, frame.bottom);
    }

    public int getPage() { return mPage; }

    public int getSection() {
        return mSection;
    }

    public int getItem() {
        return mItem;
    }

    public boolean isInSticky() {
        return mInSticky;
    }
    public boolean isOriginChanged() {
        return mOriginChanged;
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
        mOriginChanged = true;
    }
    public void setInSticky() {
        mInSticky = true;
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
