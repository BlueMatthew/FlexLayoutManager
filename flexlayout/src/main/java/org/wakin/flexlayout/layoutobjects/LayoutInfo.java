package org.wakin.flexlayout.layoutobjects;

import android.graphics.Point;

import org.wakin.flexlayout.FlexLayoutManager;
import org.wakin.flexlayout.LayoutCallback;
import org.wakin.flexlayout.graphics.Insets;
import org.wakin.flexlayout.graphics.Size;

import java.util.List;

public class LayoutInfo {
    public int orientation;
    public Size size;
    public Insets padding;
    public Point contentOffset; // TODO: may be removed

    public int page;
    public int numberOfPages;
    public int numberOfFixedSections;
    public int[] numberOfPageSections;
    public int numberOfPendingPages;
    public int[] pendingPages;

    public LayoutInfo() {
        orientation = FlexLayoutManager.VERTICAL;
    }

    public LayoutInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback, int pageOffsetStart, int pageOffsetEnd, List<FlexLayoutManager.UpdateItem> updateItems) {
        orientation = layoutManager.getOrientation();
        size = new Size(layoutManager.getWidth(), layoutManager.getHeight());
        padding = Insets.of(layoutManager.getPaddingLeft(), layoutManager.getPaddingTop(), layoutManager.getPaddingRight(), layoutManager.getPaddingBottom());
        contentOffset = new Point(layoutManager.getContentOffset());

        page = layoutCallback.getPage();
        numberOfPages = layoutCallback.getNumberOfPages();
        numberOfFixedSections = layoutCallback.getNumberOfFixedSections();

        if (numberOfPages > 0) {
            numberOfPageSections = new int[numberOfPages];
            for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
                numberOfPageSections[pageIndex] = layoutCallback.getNumberOfSectionsForPage(pageIndex);
            }
        }

        numberOfPendingPages = 1;
        pendingPages = new int[1];
        pendingPages[0] = page;
    }

    public int calcBufferLength() {
        return 11 + 3 + numberOfPages + 1 + numberOfPendingPages;
    }

    public int[] build() {
        int length = calcBufferLength();
        int[] buffer = new int[length];

        write(buffer, 0);

        return buffer;
    }

    protected int write(int[] buffer, int offsetStart) {
        int offset = offsetStart;

        buffer[offset++] = orientation;
        buffer[offset++] = size.width;
        buffer[offset++] = size.height;
        buffer[offset++] = padding.left;
        buffer[offset++] = padding.top;
        buffer[offset++] = padding.right;
        buffer[offset++] = padding.bottom;
        buffer[offset++] = contentOffset.x;
        buffer[offset++] = contentOffset.y;

        buffer[offset++] = page;
        buffer[offset++] = numberOfPages;
        buffer[offset++] = numberOfFixedSections;
        for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
            buffer[offset++] = numberOfPageSections[pageIndex];
        }

        buffer[offset++] = numberOfPendingPages;
        for (int pageIndex = 0; pageIndex < numberOfPendingPages; pageIndex++) {
            buffer[offset++] = pendingPages[pageIndex];
        }

        return offset;
    }
}
