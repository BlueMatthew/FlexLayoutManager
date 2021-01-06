package org.wakin.flexlayout.layoutmanager.layoutobjects;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Point;

import org.wakin.flexlayout.layoutmanager.FlexLayoutManager;
import org.wakin.flexlayout.layoutmanager.LayoutCallback;
import org.wakin.flexlayout.layoutmanager.graphics.Insets;
import org.wakin.flexlayout.layoutmanager.graphics.Size;

public class DisplayInfo {

    public class PageOffsetInfo
    {
        public int page;
        public Point offset;

        public PageOffsetInfo(int page, Point offset) {
            this.page = page;
            if (offset == null) {
                this.offset = new Point(FlexLayoutManager.INVALID_OFFSET, FlexLayoutManager.INVALID_OFFSET);
            } else {
                this.offset = offset;
            }
        }

        public int write(int[] buffer, int offsetStart) {
            int offset = offsetStart;

            buffer[offset++] = page;
            buffer[offset++] = this.offset.x;
            buffer[offset++] = this.offset.y;

            return offset;
        }
    }

    public int orientation;
    public Size size;
    public Insets padding;
    public Size contentSize;    // may be removed
    public Point contentOffset;

    public int page;
    public int numberOfPages;
    public int numberOfFixedSections;
    public int[] numberOfPageSections;
    public int pagingOffset;
    public int numberOfPendingPages;
    List<PageOffsetInfo> pendingPages;


    public DisplayInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback, int pageOffsetStart, int pageOffsetEnd, int pagingOffset) {

        orientation = layoutManager.getOrientation();
        size = new Size(layoutManager.getWidth(), layoutManager.getHeight());
        padding = Insets.of(layoutManager.getPaddingLeft(), layoutManager.getPaddingTop(), layoutManager.getPaddingRight(), layoutManager.getPaddingBottom());
        contentSize = new Size(layoutManager.computeHorizontalScrollRange(null), layoutManager.computeVerticalScrollRange(null));
        contentOffset = new Point(layoutManager.getContentOffset());

        page = layoutCallback.getPage();
        numberOfPages = layoutCallback.getNumberOfPages();
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

        numberOfFixedSections = layoutCallback.getNumberOfFixedSections();
        if (numberOfPages > 0) {
            numberOfPageSections = new int[numberOfPages];
            for (int pageIndex = 0; pageIndex < numberOfPages; ++pageIndex) {
                numberOfPageSections[pageIndex] = layoutCallback.getNumberOfSectionsForPage(pageIndex);
            }
        }

        this.pagingOffset = pagingOffset;
        numberOfPendingPages = pageOffsetEnd - pageOffsetStart + 1;
        if (numberOfPendingPages > 0) {
            pendingPages = new ArrayList<>(numberOfPendingPages);
            for (int pendingPage = pageOffsetStart; pendingPage <= pageOffsetEnd; ++pendingPage) {
                PageOffsetInfo pageOffsetInfo = new PageOffsetInfo(pendingPage, layoutCallback.getContentOffsetForPage(pendingPage));

                pendingPages.add(pageOffsetInfo);
            }
        }
    }

    public int calcBufferLength() {
        return 14 + numberOfPages + 2 + numberOfPendingPages * 3;
    }

    public int[] write() {
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
        buffer[offset++] = contentSize.width;
        buffer[offset++] = contentSize.height;
        buffer[offset++] = contentOffset.x;
        buffer[offset++] = contentOffset.y;

        buffer[offset++] = page;
        buffer[offset++] = numberOfPages;
        buffer[offset++] = numberOfFixedSections;
        for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
            buffer[offset++] = numberOfPageSections[pageIndex];
        }

        buffer[offset++] = pagingOffset;
        buffer[offset++] = numberOfPendingPages;
        for (PageOffsetInfo pageOffsetInfo : pendingPages) {
            offset = pageOffsetInfo.write(buffer, offset);
        }

        return offset;
    }
}
