package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;
import android.graphics.Rect;

import org.wakin.flexlayout.LayoutManager.Graphics.Insets;
import org.wakin.flexlayout.LayoutManager.Graphics.Size;

import java.util.ArrayList;
import java.util.List;

public class FlexLayoutHelper {

    protected static int writeLayoutInfo(int[] layoutInfo, int offsetStart, FlexLayoutManager layoutManager, LayoutCallback layoutCallback)
    {
        int offset = offsetStart;

        layoutInfo[offset++] = layoutManager.getOrientation();
        layoutInfo[offset++] = layoutManager.getWidth();
        layoutInfo[offset++] = layoutManager.getHeight();
        layoutInfo[offset++] = layoutManager.getPaddingLeft();
        layoutInfo[offset++] = layoutManager.getPaddingTop();
        layoutInfo[offset++] = layoutManager.getPaddingRight();
        layoutInfo[offset++] = layoutManager.getPaddingBottom();
        layoutInfo[offset++] = layoutManager.computeHorizontalScrollOffset(null);   // Not good?
        layoutInfo[offset++] = layoutManager.computeVerticalScrollOffset(null);     // Not good?

        int page = layoutCallback.getPage();
        int numberOfPages = layoutCallback.getNumberOfPages();

        layoutInfo[offset++] = page;
        layoutInfo[offset++] = numberOfPages;
        layoutInfo[offset++] = layoutCallback.getNumberOfFixedSections();
        for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
            layoutInfo[offset++] = layoutCallback.getNumberOfSectionsForPage(pageIndex);
        }

        layoutInfo[offset++] = 1;
        // int numberOfPendingPages;
        // std::vector<int> pendingPages;
        layoutInfo[offset++] = page;

        return offset - offsetStart;
    }

    public static int[] makeLayoutInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback) {
        int page = layoutCallback.getPage();
        return makeLayoutInfo(layoutManager, layoutCallback, page, page, null);
    }

    public static int[] makeLayoutInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback, int pageOffsetStart, int pageOffsetEnd, List<FlexLayoutManager.UpdateItem> updateItems) {
        int[] layoutInfo = null;

        int numberOfSections = layoutCallback.getNumberOfSections();
        int updateItemsLength = updateItems == null ? 0 : updateItems.size();

        int length = 1 + 9 + 1 + 3 + 2 + layoutCallback.getNumberOfPages() + updateItemsLength * 3 + 10;
        layoutInfo = new int[length];
        int offset = 0;
        layoutInfo[offset++] = length;

        offset += writeLayoutInfo(layoutInfo, offset, layoutManager, layoutCallback);
        layoutInfo[offset++] = updateItemsLength;
        for (int idx = 0; idx < updateItemsLength; idx++) {
            FlexLayoutManager.UpdateItem updateItem = updateItems.get(idx);
            layoutInfo[offset++] = updateItem.getAction();
            layoutInfo[offset++] = updateItem.getPostionStart();
            layoutInfo[offset++] = updateItem.getItemCount();
        }

        return layoutInfo;
    }

    public static int[] makeLayoutAndSectionsInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback) {
        int[] layoutInfo = null;

        int numberOfSections = layoutCallback.getNumberOfSections();

        // int length = 1 + 9 + 1 + 3 + layoutCallback.getNumberOfPages() + updateItemsLength * 3;

        int length = 1 + 11 + 3 + layoutCallback.getNumberOfPages() + 14 * numberOfSections + 10;
        layoutInfo = new int[length];
        int offset = 0;
        layoutInfo[offset++] = length;

        offset += writeLayoutInfo(layoutInfo, offset, layoutManager, layoutCallback);

        layoutInfo[offset++] = numberOfSections;
        layoutInfo[offset++] = 0; // sectionStart

        Size fixedItemSize = new Size();
        boolean hasFixedItemSize = false;
        int position = 0;
        for (int sectionIndex = 0; sectionIndex < numberOfSections; sectionIndex++) {
            int layoutMode = layoutCallback.getLayoutModeForSection(sectionIndex);
            int numberOfItems = layoutCallback.getNumberOfItemsInSection(sectionIndex);
            layoutInfo[offset++] = sectionIndex;
            layoutInfo[offset++] = position;
            layoutInfo[offset++] = layoutMode;
            Insets insets = layoutCallback.getInsetsForSection(sectionIndex);
            layoutInfo[offset++] = insets.left;
            layoutInfo[offset++] = insets.top;
            layoutInfo[offset++] = insets.right;
            layoutInfo[offset++] = insets.bottom;
            layoutInfo[offset++] = numberOfItems;
            layoutInfo[offset++] = layoutCallback.getNumberOfColumnsForSection(sectionIndex);
            layoutInfo[offset++] = layoutCallback.getMinimumLineSpacingForSection(sectionIndex);
            layoutInfo[offset++] = layoutCallback.getMinimumInteritemSpacingForSection(sectionIndex);

            fixedItemSize.set(0, 0);
            hasFixedItemSize = layoutCallback.hasFixedItemSize(sectionIndex, fixedItemSize);
            layoutInfo[offset++] = hasFixedItemSize ? 1 : 0;
            layoutInfo[offset++] = fixedItemSize.width;
            layoutInfo[offset++] = fixedItemSize.height;

            position += numberOfItems;
        }

        return layoutInfo;
    }

    public static int[] makeDisplayInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback, int pageOffsetStart, int pageOffsetEnd, int pagingOffset)
    {
        int[] displayInfo = null;

        int page = layoutCallback.getPage();
        int numberOfPages = layoutCallback.getNumberOfPages();
        if (pagingOffset == 0) {
            pageOffsetStart = page;
            pageOffsetEnd = page;
        } else if (pagingOffset < 0) {
            pageOffsetStart = page;
            pageOffsetEnd = page < (numberOfPages - 1) ? (page + 1) : page;
        } else {
            pageOffsetStart = page > 0 ? (page - 1) : page;
            pageOffsetEnd = page;
        }
        // int numberOfPendingPages = pageOffsetEnd - pageOffsetStart + 1;
        int numberOfPendingPages = pageOffsetEnd - pageOffsetStart + 1;

        int length = 1 + 9 + 1 + 3 + numberOfPages + numberOfPendingPages * 3 + 1;
        displayInfo = new int[length];

        int offset = 0;
        displayInfo[offset++] = length;

        displayInfo[offset++] = layoutManager.getOrientation();
        displayInfo[offset++] = layoutManager.getWidth();
        displayInfo[offset++] = layoutManager.getHeight();
        displayInfo[offset++] = layoutManager.getPaddingLeft();
        displayInfo[offset++] = layoutManager.getPaddingTop();
        displayInfo[offset++] = layoutManager.getPaddingRight();
        displayInfo[offset++] = layoutManager.getPaddingBottom();
        Point contentOffset = layoutManager.getContentOffset();
        if (null == contentOffset) {
            displayInfo[offset++] = 0;
            displayInfo[offset++] = 0;
        } else {
            displayInfo[offset++] = contentOffset.x;
            displayInfo[offset++] = contentOffset.y;
        }

        displayInfo[offset++] = page;
        displayInfo[offset++] = numberOfPages;
        displayInfo[offset++] = layoutCallback.getNumberOfFixedSections();
        for (int pageIndex = 0; pageIndex < numberOfPages; ++pageIndex) {
            displayInfo[offset++] = layoutCallback.getNumberOfSectionsForPage(pageIndex);
        }

        displayInfo[offset++] = pagingOffset;
        displayInfo[offset++] = numberOfPendingPages;



        for (int pendingPage = pageOffsetStart; pendingPage <= pageOffsetEnd; ++pendingPage) {
            displayInfo[offset++] = pendingPage;
            contentOffset = layoutCallback.getContentOffsetForPage(pendingPage);
            if (null == contentOffset) {
                displayInfo[offset++] = FlexLayoutManager.INVALID_OFFSET;
                displayInfo[offset++] = FlexLayoutManager.INVALID_OFFSET;
            } else {
                displayInfo[offset++] = contentOffset.x;
                displayInfo[offset++] = contentOffset.y;
            }
        }

        return displayInfo;
    }

    public static List<LayoutItem> unserializeLayoutItemAndStickyItems(int[] data, List<LayoutItem> changingStickyItems) {
        List<LayoutItem> visibleItems = null;

        if (data == null || data.length == 0) {
            return null;
        }

        int offset = 0;
        int numberOfItems = data[offset++];
        if (numberOfItems <= 0) {
            return visibleItems;
        }

        visibleItems = new ArrayList<>(numberOfItems);

        LayoutItem layoutItem = null;
        int page = 0;
        int section = 0;
        int item = 0;
        int position = 0;
        Rect rect = new Rect();
        int inSticky = 0;
        int originChanged = 0;

        for (int index = 0; index < numberOfItems; index++) {
            if (data.length - offset < 10) {
                break;
            }
            page = data[offset++];
            section = data[offset++];
            item = data[offset++];
            position = data[offset++];

            rect.left = data[offset++];
            rect.top = data[offset++];
            rect.right = rect.left + data[offset++];
            rect.bottom = rect.top + data[offset++];
            inSticky = data[offset++];
            originChanged = data[offset++];

            layoutItem = new LayoutItem(page, section, item, position, inSticky, originChanged, rect);

            visibleItems.add(layoutItem);
        }

        if (offset == data.length) {
            return visibleItems;
        }
        int numberOfChangingStickyItems = data[offset++];
        if (numberOfChangingStickyItems <= 0) {
            return visibleItems;
        }

        rect.set(0, 0, 0, 0);
        for (int index = 0; index < numberOfChangingStickyItems; index++) {
            if (data.length - offset < 7) {
                break;
            }

            section = data[offset++];
            item = data[offset++];
            position = data[offset++];
            inSticky = data[offset++];
            originChanged = data[offset++];
            rect.left = data[offset++];
            rect.top = data[offset++];

            rect.right = rect.left;
            rect.bottom = rect.top;

            layoutItem = new LayoutItem(section, item, position, inSticky, originChanged, rect);
            changingStickyItems.add(layoutItem);
        }

        return visibleItems;
    }

    public static Point longToPoint(long val) {
        if (val == (~0)) {
            return null;
        }

        Point pt = new Point(0, (int)val);
        pt.x = (int)(val >> 32);
        return pt;
    }

    public static Rect intArrayToRect(int[] intArray) {
        if (intArray == null || intArray.length < 4) {
            return null;
        }

        return new Rect(intArray[0], intArray[1], intArray[0] + intArray[2], intArray[1] + intArray[3]);
    }

}
