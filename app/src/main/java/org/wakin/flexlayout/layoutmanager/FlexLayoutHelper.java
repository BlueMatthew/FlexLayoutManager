package org.wakin.flexlayout.layoutmanager;

import android.graphics.Point;
import android.graphics.Rect;

import org.wakin.flexlayout.layoutmanager.layoutobjects.DisplayInfo;
import org.wakin.flexlayout.layoutmanager.layoutobjects.LayoutAndSectionsInfo;
import org.wakin.flexlayout.layoutmanager.layoutobjects.LayoutInfo;

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

        LayoutInfo layoutInfo = new LayoutInfo(layoutManager, layoutCallback, pageOffsetStart, pageOffsetEnd, updateItems);
        return layoutInfo.build();
    }

    public static int[] makeLayoutAndSectionsInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback) {
        int page = layoutCallback.getPage();
        LayoutAndSectionsInfo layoutAndSectionsInfo = new LayoutAndSectionsInfo(layoutManager, layoutCallback, page, page, null);
        return layoutAndSectionsInfo.build();
    }

    public static int[] makeDisplayInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback, int pageOffsetStart, int pageOffsetEnd, int pagingOffset)
    {
        DisplayInfo displayInfo = new DisplayInfo(layoutManager, layoutCallback, pageOffsetStart, pageOffsetEnd, pagingOffset);
        return displayInfo.build();
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
            if (data.length - offset < 9) {
                break;
            }

            section = data[offset++];
            item = data[offset++];
            position = data[offset++];
            inSticky = data[offset++];
            originChanged = data[offset++];
            rect.left = data[offset++];
            rect.top = data[offset++];

            rect.right = rect.left + data[offset++];
            rect.bottom = rect.top + data[offset++];

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
