package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;

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

    // int getPageSize();

    void onItemEnterStickyMode(int section, int item, int position, Point point);
    void onItemExitStickyMode(int section, int item, int position);
}
