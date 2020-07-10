package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;
import android.graphics.Rect;

import androidx.recyclerview.widget.RecyclerView;

import org.wakin.flexlayout.util.Algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FlexWaterfallSection extends FlexSection {

    protected List<FlexColumn> mColumns;

    public FlexWaterfallSection(int section, int positionBase, LayoutCallback layoutCallback, Rect initialFrame) {
        super(section, positionBase, layoutCallback, initialFrame);

        mColumns = new ArrayList<>();
    }

    private FlexColumn searchColumn(Comparator<FlexColumn> comparator) {
        FlexColumn columnToFind = null;
        for (FlexColumn column : mColumns) {
            if (null == columnToFind) {
                columnToFind = column;
            } else {
                if (comparator.compare(columnToFind, column) > 0) {
                    columnToFind = column;
                }
            }
        }

        return columnToFind;
    }

    public int mergeItemsInRect(List<FlexItem> items, Rect rect, final boolean vertical) {

        Rect offsettedRect = new Rect(rect);
        offsettedRect.offset(-mFrame.left, -mFrame.top);
        org.wakin.flexlayout.util.Comparator<FlexColumn, Rect> comparator = vertical ? new FlexColumn.RectHorizontalComparator() : new FlexColumn.RectVerticalComparator();

        int lowerRowBound = Algorithm.lowerBound(mColumns, offsettedRect, comparator);
        if (lowerRowBound == -1) {
            return 0;
        }
        int upperRowBound = Algorithm.upperBound(mColumns, offsettedRect, comparator);

        org.wakin.flexlayout.util.Comparator<FlexItem, Rect> rowItemComparator = vertical ? new FlexItem.RectVerticalComparator() : new FlexItem.RectHorizontalComparator();
        // Check if the item is in rect vertically
        org.wakin.flexlayout.util.Comparator<FlexItem, Rect> itemFilterComparator = new org.wakin.flexlayout.util.Comparator<FlexItem, Rect>() {
            @Override
            public int compare(FlexItem o1, Rect o2) {
                Rect itemRect = o1.getFrame();
                if (vertical) {
                    return itemRect.left < o2.right && o2.left < itemRect.right ? 0 : ((itemRect.left < o2.left) ? -1 : 1);
                } else {
                    return itemRect.top < o2.bottom && o2.top < itemRect.bottom ? 0 : ((itemRect.top < o2.bottom) ? -1 : 1);

                }
            }
        };

        List<FlexItem> itemsInSection = new ArrayList<>();
        mColumns.get(lowerRowBound).mergeItemsInBounds(itemsInSection, offsettedRect, rowItemComparator, itemFilterComparator);
        for (int columnIndex = lowerRowBound + 1; columnIndex <= upperRowBound - 1; columnIndex ++) {
            mColumns.get(columnIndex).mergeItemsInBounds(itemsInSection, offsettedRect, rowItemComparator);
        }
        if (upperRowBound > lowerRowBound) {
            mColumns.get(upperRowBound).mergeItemsInBounds(itemsInSection, offsettedRect, rowItemComparator, itemFilterComparator);
        }

        Collections.sort(itemsInSection);

        items.addAll(itemsInSection);

        return itemsInSection.size();
    }

    public void prepareLayout(Rect bounds) {

        // Reset first
        mColumns.clear();
        mFrame.bottom = mFrame.top;

        int numberOfItems = mLayoutCallback.getItemCountForSection(mSection);
        if (numberOfItems <= 0) {
            return;
        }

        Insets sectionInset = mLayoutCallback.getInsetsForSection(mSection);
        if (null == sectionInset) {
            sectionInset = Insets.NONE;
        }

        int minimumInteritemSpacing = mLayoutCallback.getMinimumInteritemSpacingForSection(mSection);
        int minimumLineSpacing = mLayoutCallback.getMinimumLineSpacingForSection(mSection);

        int numberOfColumns = mLayoutCallback.getColumnCountForSection(mSection);
        if (numberOfColumns < 1) {
            numberOfColumns = 1;
        }
        int estimatedItems = (int)Math.ceil(numberOfItems / numberOfColumns);

        int sizeOfSection = mFrame.width() - (sectionInset.left + sectionInset.right);
        Point columnOrigin = new Point(sectionInset.left, sectionInset.top);

        int sizeOfColumn = 0;
        int availableSizeOfSection = sizeOfSection;
        for (int columnIndex = 0; columnIndex < numberOfColumns; columnIndex++)
        {
            if (columnIndex == numberOfColumns - 1) {
                sizeOfColumn = availableSizeOfSection;
            } else {
                sizeOfColumn = (int)((availableSizeOfSection - (numberOfColumns - columnIndex - 1) * minimumInteritemSpacing) / (numberOfColumns - columnIndex) + 0.5);
                availableSizeOfSection -= sizeOfColumn + minimumInteritemSpacing;
            }

            FlexColumn column = new FlexColumn(estimatedItems, new Rect(columnOrigin.x, columnOrigin.y, sizeOfColumn, 0));
            mColumns.add(column);

            columnOrigin.x += sizeOfColumn + minimumInteritemSpacing;
        }

        Comparator<FlexColumn> minComparator = new Comparator<FlexColumn>() {
            @Override
            public int compare(FlexColumn o1, FlexColumn o2) {
                return Integer.compare(o1.getFrame().height(), o2.getFrame().height());
            }
        };

        Comparator<FlexColumn> maxComparator = new Comparator<FlexColumn>() {
            @Override
            public int compare(FlexColumn o1, FlexColumn o2) {
                return Integer.compare(o2.getFrame().height(), o1.getFrame().height());
            }
        };

        Point originOfItem = new Point();
        Size size = new Size();
        // loadItems();
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++)
        {
            boolean fullSpan = mLayoutCallback.isItemFullSpan(mSection, itemIndex);

            // find column with minimal size
            FlexColumn column = searchColumn(fullSpan ? maxComparator : minComparator);

            originOfItem.set(fullSpan ? mColumns.get(0).getFrame().left : column.getFrame().left, column.getFrame().bottom + (column.getFrame().bottom == column.getFrame().top ? 0 : minimumLineSpacing));
            //: CGPointMake((*columnItOfMinimalSize)->m_frame.origin.x + (*columnItOfMinimalSize)->m_frame.size.width + (itemIndexInColumn == 0 ? 0.0f : minimumLineSpacing), (*columnItOfMinimalSize)->m_frame.origin.y);

            mLayoutCallback.getItemSize(mSection, itemIndex, size);
            FlexItem item = new FlexItem(this, itemIndex, originOfItem, size);

            mItems.add(item);

            if (fullSpan) {
                item.setFullSpan(true);
                for (FlexColumn c : mColumns) {
                    if (column != c) {
                        // PlaceHolder
                        FlexItem placeHolder = item.clonePlaceHolder();
                        int newTop = column.getFrame().bottom;
                        if (column.hasItems()) {
                            newTop += minimumLineSpacing;
                        }
                        placeHolder.getFrame().offsetTo(placeHolder.getFrame().left, newTop);
                        c.addItemVertically(placeHolder);
                    } else {
                        c.addItemVertically(item);
                    }
                    // c.addItemVertically(item, (column != c));
                }
            } else {
                column.addItemVertically(item);
            }
        }

        mMinInvalidatedItem = mItems.size();

        // find column of the biggest size
        FlexColumn columnOfMaximalSize = searchColumn(maxComparator);

        // bottom is initialized to top on the begin of the function
        mFrame.bottom += columnOfMaximalSize.getFrame().height() + sectionInset.top + sectionInset.bottom;
    }

    void deleteItemsFrom(int itemStart) {
        // delete all items from itemStart
        FlexItem item = mItems.get(itemStart);
        for (FlexColumn column : mColumns) {
            column.deleteItemsFrom(item);
        }
    }

    @Override
    public int deleteItems(int itemStart, int itemCount) {
        deleteItemsFrom(itemStart);
        return super.deleteItems(itemStart, itemCount);
    }

    @Override
    public int insertItems(int itemStart, int itemCount) {
        deleteItemsFrom(itemStart);
        return super.insertItems(itemStart, itemCount);
    }

    @Override
    public int reloadItems(int itemStart, int itemCount) {
        deleteItemsFrom(itemStart);
        return super.reloadItems(itemStart, itemCount);
    }
}
