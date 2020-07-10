package org.wakin.flexlayout.LayoutManager;

import android.graphics.Point;

public interface LayoutCallback {
    int getSectionCount();
    int getLayoutModeForSection(int section);
    int getItemCountForSection(int section);

    Insets getInsetsForSection(int section);
    int getMinimumInteritemSpacingForSection(int section);
    int getMinimumLineSpacingForSection(int section);
    // If all items have the same size, we can optimize the layout
    // And in the most of cases, items have the same size
    boolean getFixedItemSizeForSection(int section, Size size);
    void getItemSize(int section, int item, Size size);
    boolean isItemFullSpan(int section, int item);

    int getColumnCountForSection(int section);

    // int getPageSize();

    void onItemEnterStickyMode(int section, int item, int position, Point point);
    void onItemExitStickyMode(int section, int item, int position);
}
