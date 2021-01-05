package org.wakin.flexlayout.layoutmanager.elements;

import android.graphics.Point;
import android.graphics.Rect;


import org.wakin.flexlayout.layoutmanager.graphics.Insets;
import org.wakin.flexlayout.layoutmanager.LayoutCallback;
import org.wakin.flexlayout.layoutmanager.graphics.Size;
import org.wakin.flexlayout.util.Algorithm;
import org.wakin.flexlayout.util.Comparator;

import java.util.ArrayList;
import java.util.List;

public class FlexFlowSection extends FlexSection {

    protected ArrayList<FlexRow> mRows;

    public FlexFlowSection(int section, int positionBase, LayoutCallback layoutCallback, Rect initialFrame) {
        super(section, positionBase, layoutCallback, initialFrame);

        mRows = new ArrayList<>();
    }

    public int mergeItemsInRect(List<FlexItem> items, final Rect rect, final boolean vertical) {

        Rect offsettedRect = new Rect(rect);
        offsettedRect.offset(-mFrame.left, -mFrame.top);
        Comparator<FlexRow, Rect> comparator = vertical ? new FlexRow.RectVerticalComparator() : new FlexRow.RectHorizontalComparator();

        int lowerRowBound = Algorithm.lowerBound(mRows, offsettedRect, comparator);
        if (lowerRowBound == -1) {
            return 0;
        }
        int upperRowBound = Algorithm.upperBound(mRows, offsettedRect, comparator);

        Comparator<FlexItem, Rect> rowItemComparator = vertical ? new FlexItem.RectHorizontalComparator() : new FlexItem.RectVerticalComparator();
        // Check if the item is in rect vertically
        Comparator<FlexItem, Rect> itemFilterComparator = new Comparator<FlexItem, Rect>() {
            @Override
            public int compare(FlexItem o1, Rect o2) {
                Rect itemRect = o1.getFrame();
                if (vertical) {
                    return itemRect.top < o2.bottom && o2.top < itemRect.bottom ? 0 : ((itemRect.top < o2.bottom) ? -1 : 1);
                } else {
                    return itemRect.left < o2.right && o2.left < itemRect.right ? 0 : ((itemRect.left < o2.left) ? -1 : 1);
                }
            }
        };
        int matchedItems = mRows.get(lowerRowBound).mergeItemsInBounds(items, offsettedRect, rowItemComparator, itemFilterComparator);
        for (int rowIndex = lowerRowBound + 1; rowIndex <= upperRowBound - 1; rowIndex ++) {
            matchedItems += mRows.get(rowIndex).mergeItemsInBounds(items, offsettedRect, rowItemComparator);
        }
        if (upperRowBound > lowerRowBound) {
            matchedItems += mRows.get(upperRowBound).mergeItemsInBounds(items, offsettedRect, rowItemComparator, itemFilterComparator);
        }

        return matchedItems;
    }

    public void prepareLayout(Rect bounds) {

        // Reset First
        mRows.clear();
        mFrame.bottom = mFrame.top;

        int itemCount = mLayoutCallback.getNumberOfItemsInSection(mSection);
        if (itemCount <= 0) {
            return;
        }

        Insets sectionInset = mLayoutCallback.getInsetsForSection(mSection);
        if (null == sectionInset) {
            sectionInset = Insets.NONE;
        }
        int maximalSizeOfRow = mFrame.width() - (sectionInset.left + sectionInset.right);
        int minimumInteritemSpacing = mLayoutCallback.getMinimumInteritemSpacingForSection(mSection);
        int minimumLineSpacing = mLayoutCallback.getMinimumLineSpacingForSection(mSection);
        
        Point pointOfItem = new Point();
        Size sizeOfItem = new Size();
        boolean hasFixedSize = mLayoutCallback.hasFixedItemSize(mSection, sizeOfItem);
        if (hasFixedSize) {
            // If all items have fixed size, fullSpac will be ignored
            // Calculate the number of items palced in one row
            int numberOfItemsInRow = (int)((maximalSizeOfRow + minimumInteritemSpacing) / (sizeOfItem.width + minimumInteritemSpacing));
            if (numberOfItemsInRow < 1) {
                numberOfItemsInRow = 1;
            }
            int numberOfRows = (int)(itemCount / numberOfItemsInRow  + 0.5);
            mRows.ensureCapacity(numberOfRows);
            Rect rectOfRow = new Rect(0, 0, numberOfItemsInRow * sizeOfItem.width + (numberOfItemsInRow - 1) * minimumInteritemSpacing, sizeOfItem.height);
            rectOfRow.offset(sectionInset.left, sectionInset.top);

            int itemIndexOfRow = 0;
            int itemIndex = 0;
            for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {
                FlexRow row = new FlexRow(numberOfItemsInRow);

                pointOfItem.y = rectOfRow.top;
                pointOfItem.x = rectOfRow.left;
                for (itemIndex = 0; itemIndex < numberOfItemsInRow && itemIndex + itemIndexOfRow < itemCount; itemIndex++) {
                    FlexItem item = new FlexItem(this, itemIndexOfRow + itemIndex, pointOfItem, sizeOfItem);
                    pointOfItem.x += sizeOfItem.width + minimumInteritemSpacing;
                }

                if (itemIndex + itemIndexOfRow >= itemCount && (itemCount % numberOfItemsInRow != 0)) {
                    // Adjust Size of Row When It is the Last Item
                    rectOfRow.right = pointOfItem.x - minimumInteritemSpacing;
                }
                row.getFrame().set(rectOfRow);
                mRows.add(row);
                rectOfRow.offset(0, sizeOfItem.height + minimumLineSpacing);
                itemIndexOfRow += numberOfItemsInRow;
            }
        } else {

            FlexRow row = null;

            int numberOfColumns = mLayoutCallback.getNumberOfColumnsForSection(mSection);
            if (numberOfColumns <= 0) {
                numberOfColumns = 1;
            }

            mRows.ensureCapacity((int)(itemCount / numberOfColumns + 0.5));
            Point pointOfRow = new Point(sectionInset.left, sectionInset.top);

            pointOfItem.set(pointOfRow.x, pointOfRow.y);

            boolean fullSpan = false;
            int availableSize = 0;
            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {

                fullSpan = mLayoutCallback.isFullSpanAtItem(mSection, itemIndex);
                mLayoutCallback.getSizeForItem(mSection, itemIndex, sizeOfItem);

                if (null != row)
                {
                    if (row.getItemCount() > 0) {
                        if (!fullSpan && (availableSize = (maximalSizeOfRow - row.getFrame().width() - minimumInteritemSpacing)) < sizeOfItem.width) {
                            // New Line
                            // row->alignItems(IS_CV_VERTICAL(m_layout));
                            mRows.add(row);
                            row = null;
                        } else {
                            pointOfItem.set(row.getFrame().right + minimumInteritemSpacing, row.getFrame().top);
                        }
                    }
                }

                if (null == row)
                {
                    if (mRows.size() > 0) {
                        pointOfRow.y += minimumLineSpacing + mRows.get(mRows.size() - 1).getFrame().height();
                    }

                    row = new FlexRow(numberOfColumns, pointOfRow);

                    pointOfItem.x = pointOfRow.x;
                    pointOfItem.y = pointOfRow.y;
                }

                // IS_CV_VERTICAL(m_layout) ? originOfItem.y = row->m_frame.origin.y : originOfItem.x = row->m_frame.origin.x;

                FlexItem item = new FlexItem(this, itemIndex, pointOfItem, sizeOfItem);

                mItems.add(item);
                row.addItemVertically(item);
            }

            if (null != row)
            {
                // Last row
                // row->alignItems(IS_CV_VERTICAL(m_layout));
                mRows.add(row);
            }

            mMinInvalidatedItem = mItems.size();

            mFrame.bottom = mFrame.top + row.getFrame().bottom - mRows.get(0).getFrame().top + sectionInset.bottom;
        }

    }

    @Override
    public int deleteItems(int itemStart, int itemCount) {
        return super.deleteItems(itemStart, itemCount);
    }

    @Override
    public int insertItems(int itemStart, int itemCount) {
        FlexItem item = mItems.get(itemStart);
        int pos = Algorithm.binarySearch(mRows, item, new FlexRow.FlexRowItemComparator());
        if (pos >= 0) {
            if (pos + 1 < mRows.size()) {
                mRows.subList(pos + 1, mRows.size()).clear();
            }

            FlexRow row = mRows.get(pos);
            row.deleteItemsFrom(item);
            if (!row.hasItems()) {
                mRows.remove(pos);
            } else {
                // Adjust frame of Row

            }
        }

        return super.insertItems(itemStart, itemCount);
    }

}
