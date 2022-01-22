package org.wakin.flexlayout;

import android.graphics.Point;
import android.graphics.Rect;

import org.wakin.flexlayout.graphics.Insets;
import org.wakin.flexlayout.graphics.Size;

public interface LayoutCallback {
    int getNumberOfSections();
    int getLayoutModeForSection(int section);
    int getNumberOfItemsInSection(int section);

    Insets getInsetsForSection(int section);
    int getMinimumInteritemSpacingForSection(int section);
    int getMinimumLineSpacingForSection(int section);
    // If all items have the same size, we can optimize the layout
    // And in the most of cases, items have the same size
    boolean hasFixedItemSize(int section, Size size);
    void getSizeForItem(int section, int item, Size size);
    boolean isFullSpanAtItem(int section, int item);

    int getNumberOfColumnsForSection(int section);

    int getInfoForItemsBatchly(int section, int itemStart, int itemCount, int[] data);

    int getPage();
    int getNumberOfPages();
    int getNumberOfFixedSections();
    int getNumberOfSectionsForPage(int page);
    Point getContentOffsetForPage(int page);

    void onItemEnterStickyMode(int section, int item, int position, Rect frame);
    void onItemExitStickyMode(int section, int item, int position);
}
