package org.wakin.flexlayout.layoutmanager.layoutobjects;

import org.wakin.flexlayout.layoutmanager.FlexLayoutManager;
import org.wakin.flexlayout.layoutmanager.LayoutCallback;
import org.wakin.flexlayout.layoutmanager.graphics.Insets;
import org.wakin.flexlayout.layoutmanager.graphics.Size;

public class SectionInfo {

    public int section;    // For incremental update
    public int position;
    public Insets padding;
    public int numberOfItems;
    public int numberOfColumns;
    public int layoutMode;
    public int lineSpacing;
    public int interitemSpacing;
    public boolean hasFixedItemSize;
    Size fixedItemSize;

    public SectionInfo() {

    }

    public static int calcBufferLength() {
        return 14;
    }
    public SectionInfo(int section, int position, FlexLayoutManager layoutManager, LayoutCallback layoutCallback) {
        this.section = section;
        this.position = position;
        padding = layoutCallback.getInsetsForSection(section);
        numberOfItems = layoutCallback.getNumberOfItemsInSection(section);
        numberOfColumns = layoutCallback.getNumberOfColumnsForSection(section);
        layoutMode = layoutCallback.getLayoutModeForSection(section);
        lineSpacing = layoutCallback.getMinimumLineSpacingForSection(section);
        interitemSpacing = layoutCallback.getMinimumInteritemSpacingForSection(section);
        fixedItemSize = new Size();
        hasFixedItemSize = layoutCallback.hasFixedItemSize(section, fixedItemSize);
    }

    public int write(int[] buffer, int offsetStart) {

        int offset = offsetStart;

        buffer[offset++] = section;
        buffer[offset++] = position;
        buffer[offset++] = layoutMode;

        buffer[offset++] = padding.left;
        buffer[offset++] = padding.top;
        buffer[offset++] = padding.right;
        buffer[offset++] = padding.bottom;
        buffer[offset++] = numberOfItems;
        buffer[offset++] = numberOfColumns;
        buffer[offset++] = lineSpacing;
        buffer[offset++] = interitemSpacing;

        buffer[offset++] = hasFixedItemSize ? 1 : 0;
        buffer[offset++] = fixedItemSize.width;
        buffer[offset++] = fixedItemSize.height;

        return offset;
    }
}
