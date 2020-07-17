package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;
import android.graphics.Rect;

import org.wakin.flexlayout.LayoutManager.FlexLayoutManager;
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

        return offset - offsetStart;
    }

    public static int[] makeLayoutInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback) {
        int[] layoutInfo = null;

        int numberOfSections = layoutCallback.getNumberOfSections();

        int length = 1 + 9;
        layoutInfo = new int[length];
        int offset = 0;
        layoutInfo[offset++] = length;

        offset += writeLayoutInfo(layoutInfo, offset, layoutManager, layoutCallback);

        return layoutInfo;
    }


    public static int[] makeLayoutAndSectionsInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback) {
        int[] layoutInfo = null;

        int numberOfSections = layoutCallback.getNumberOfSections();

        int length = 1 + 11 + 14 * numberOfSections;
        layoutInfo = new int[length];
        int offset = 0;
        layoutInfo[offset++] = length;

        offset += writeLayoutInfo(layoutInfo, offset, layoutManager, layoutCallback);

        layoutInfo[offset++] = numberOfSections;
        layoutInfo[offset++] = 0; // sectionStart

        Size fixedItemSize = new Size();
        boolean hasFixedItemSize = false;
        for (int sectionIndex = 0; sectionIndex < numberOfSections; sectionIndex++) {
            int layoutMode = layoutCallback.getLayoutModeForSection(sectionIndex);
            layoutInfo[offset++] = sectionIndex;
            layoutInfo[offset++] = layoutMode;
            Insets insets = layoutCallback.getInsetsForSection(sectionIndex);
            layoutInfo[offset++] = insets.left;
            layoutInfo[offset++] = insets.top;
            layoutInfo[offset++] = insets.right;
            layoutInfo[offset++] = insets.bottom;
            layoutInfo[offset++] = layoutCallback.getNumberOfItemsInSection(sectionIndex);
            layoutInfo[offset++] = layoutCallback.getNumberOfColumnsForSection(sectionIndex);
            layoutInfo[offset++] = layoutCallback.getMinimumLineSpacingForSection(sectionIndex);
            layoutInfo[offset++] = layoutCallback.getMinimumInteritemSpacingForSection(sectionIndex);

            fixedItemSize.set(0, 0);
            hasFixedItemSize = layoutCallback.hasFixedItemSize(sectionIndex, fixedItemSize);
            layoutInfo[offset++] = hasFixedItemSize ? 1 : 0;
            layoutInfo[offset++] = fixedItemSize.width;
            layoutInfo[offset++] = fixedItemSize.height;
        }

        return layoutInfo;
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
        int section = 0;
        int item = 0;
        int position = 0;
        Rect rect = new Rect();
        int stickyData = 0;

        for (int index = 0; index < numberOfItems; index++) {
            if (data.length - offset < 8) {
                break;
            }
            section = data[offset++];
            item = data[offset++];
            position = data[offset++];

            rect.left = data[offset++];
            rect.top = data[offset++];
            rect.right = rect.left + data[offset++];
            rect.bottom = rect.top + data[offset++];
            stickyData = data[offset++];

            layoutItem = new LayoutItem(section, item, position, stickyData, rect);

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
            if (data.length - offset < 6) {
                break;
            }

            section = data[offset++];
            item = data[offset++];
            position = data[offset++];
            stickyData = data[offset++];
            rect.left = data[offset++];
            rect.top = data[offset++];

            rect.right = rect.left;
            rect.bottom = rect.top;

            layoutItem = new LayoutItem(section, item, position, stickyData, rect);
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
