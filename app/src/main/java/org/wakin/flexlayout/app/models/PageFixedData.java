package org.wakin.flexlayout.app.models;

import java.util.ArrayList;
import java.util.List;

public class PageFixedData {
    private int mItemCount = 0;
    private List<SectionData> mSections = new ArrayList<>();

    public void calcItemCount() {
        mItemCount = 0;
        for (int idx = 0; idx < mSections.size(); idx++) {
            mItemCount += mSections.get(idx).getItemCount();
        }
    }

    public void addSection(SectionData sectionData) {
        mSections.add(sectionData);
        mItemCount += sectionData.getItemCount();
    }

    public int getNumberOfSections() {
        return mSections.size();
    }

    public int getItemCount() {
        return mItemCount;
    }

    public SectionData getSection(int sectionIndex) {
        return 0 <= sectionIndex && sectionIndex < mSections.size() ? mSections.get(sectionIndex) : null;
    }
}
