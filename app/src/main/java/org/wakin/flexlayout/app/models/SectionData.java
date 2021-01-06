package org.wakin.flexlayout.app.models;

import android.util.Log;

import org.wakin.flexlayout.layoutmanager.graphics.Insets;

import java.util.ArrayList;
import java.util.List;

public class SectionData {

    protected int mSection;
    protected int mPosition;
    protected boolean mWaterfallMode = false;
    protected int mColumns = 1;
    protected List<CellData> mItems;
    protected Insets mInsets = Insets.NONE;
    protected int mLineSpacing = 0;
    protected int mInteritemSpacing = 0;

    protected int mReservedItemCount = 0;

    public int getSection() {
        return mSection;
    }

    public void setSection(int section) {
        this.mSection = section;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }

    public boolean isWaterfallMode() {
        return mWaterfallMode;
    }

    public void setWaterfallMode(boolean waterfallMode) {
        mWaterfallMode = waterfallMode;
    }

    public int getColumns() {
        return mColumns;
    }

    public void setColumns(int columns) {
        this.mColumns = columns;
    }

    public Insets getInsets() {
        return mInsets;
    }

    public void setInsets(Insets insets) {
        this.mInsets = insets;
    }

    public int getLineSpacing() {
        return mLineSpacing;
    }

    public void setLineSpacing(int lineSpacing) {
        this.mLineSpacing = lineSpacing;
    }

    public int getInteritemSpacing() {
        return mInteritemSpacing;
    }

    public void setInteritemSpacing(int interitemSpacing) {
        this.mInteritemSpacing = interitemSpacing;
    }

    public SectionData(int section, int itemCount, int position) {
        mSection = section;
        mReservedItemCount = itemCount;
        mPosition = position;
        mItems = new ArrayList<>(itemCount);
    }

    public int getItemCount() {
        return mItems.size();
    }

    public int getReservedItemCount() {
        return mReservedItemCount;
    }

    public void addCellData(CellData cellData) {
        mItems.add(cellData);
    }

    public CellData getItem(int itemIndex) {
        if (itemIndex >= mItems.size()) {
            Log.e("ERR", "Error");
        }
        return mItems.get(itemIndex);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("");
        if (!mInsets.equals(Insets.NONE)) {
            sb.append("Insets:" + mInsets.toString() + "\r\n");
        }
        if (mLineSpacing != 0) {
            sb.append("lineSpacing:" + mLineSpacing + "\r\n");
        }
        if (mLineSpacing != 0) {
            sb.append("interitemSpacing:" + mInteritemSpacing + "\r\n");
        }

        return sb.toString();
    }

    public int calcFullSpanWidth(boolean vertical, int boundWidth) {
        int leftRightOfInsets = vertical ? (mInsets.left + mInsets.right) : (mInsets.top + mInsets.bottom);
        return (boundWidth - leftRightOfInsets);
    }

    public int calcCellWidth(boolean vertical, int boundWidth) {

        if (mColumns < 1) {
            mColumns = 1;
        }

        int leftRightOfInsets = vertical ? (mInsets.left + mInsets.right) : (mInsets.top + mInsets.bottom);

        int width = (mColumns == 1) ? (boundWidth - leftRightOfInsets) : ((boundWidth - leftRightOfInsets - (mColumns - 1) * mInteritemSpacing) / mColumns);

        return width;
    }

    public int calcCellWidth(boolean vertical, int boundWidth, int column) {

        if (mColumns < 1) {
            mColumns = 1;
        }

        int leftRightOfInsets = vertical ? (mInsets.left + mInsets.right) : (mInsets.top + mInsets.bottom);

        int width = (mColumns == 1) ? (boundWidth - leftRightOfInsets) : ((boundWidth - leftRightOfInsets - (mColumns - 1) * mLineSpacing) / mColumns);

        return width;
    }

}
