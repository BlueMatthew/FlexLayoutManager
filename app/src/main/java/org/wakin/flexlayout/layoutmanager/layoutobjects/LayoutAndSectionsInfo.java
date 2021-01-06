package org.wakin.flexlayout.layoutmanager.layoutobjects;

import android.graphics.Point;

import org.wakin.flexlayout.layoutmanager.FlexLayoutManager;
import org.wakin.flexlayout.layoutmanager.LayoutCallback;
import org.wakin.flexlayout.layoutmanager.graphics.Insets;
import org.wakin.flexlayout.layoutmanager.graphics.Size;

import java.util.ArrayList;
import java.util.List;

public class LayoutAndSectionsInfo extends LayoutInfo {

    public int numberOfSections;
    public int sectionStart;

    public List<SectionInfo> sections;

    public LayoutAndSectionsInfo() {
        super();
    }

    public LayoutAndSectionsInfo(FlexLayoutManager layoutManager, LayoutCallback layoutCallback, int pageOffsetStart, int pageOffsetEnd, List<FlexLayoutManager.UpdateItem> updateItems) {
        super(layoutManager, layoutCallback, pageOffsetStart, pageOffsetEnd, updateItems);

        numberOfSections = layoutCallback.getNumberOfSections();
        sectionStart = 0;

        Size fixedItemSize = new Size();
        boolean hasFixedItemSize = false;
        int position = 0;
        sections = (numberOfSections > 0) ? (new ArrayList<SectionInfo>(numberOfSections)) : (new ArrayList<SectionInfo>());
        for (int sectionIndex = 0; sectionIndex < numberOfSections; sectionIndex++) {

            SectionInfo sectionInfo = new SectionInfo(sectionIndex, position, layoutManager, layoutCallback);
            sections.add(sectionInfo);

            position += sectionInfo.numberOfItems;
        }

    }

    public int calcBufferLength() {
        return super.calcBufferLength() + 2 + SectionInfo.calcBufferLength() * numberOfSections;
    }

    public int[] build() {

        int length = calcBufferLength();

        int[] buffer = new int[length];

        write(buffer, 0);

        return buffer;
    }

    protected int write(int[] buffer, int offsetStart) {
        int offset = super.write(buffer, offsetStart);

        buffer[offset++] = numberOfSections;
        buffer[offset++] = sectionStart;

        for (SectionInfo sectionInfo : sections) {
            offset = sectionInfo.write(buffer, offset);
        }

        return offset;
    }
}
