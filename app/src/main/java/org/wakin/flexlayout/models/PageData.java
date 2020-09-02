package org.wakin.flexlayout.models;

import java.util.ArrayList;
import java.util.List;

public class PageData {

    protected int mPage;
    protected int mItemCount = 0;
    protected int mPageItemCount = 0;
    protected PageFixedData mFixedData = null;
    protected List<SectionData> mSections = new ArrayList<>();

    public PageData(PageFixedData fixedData) {
        mFixedData = fixedData;
    }

    public int getItemCount() {
        return mItemCount;
    }

    public int getPageItemCount() {
        return mPageItemCount;
    }

    public void addSection(SectionData sectionData) {
        mSections.add(sectionData);
        mItemCount += sectionData.getItemCount();
    }

    public void calcItemCount() {
        mItemCount = null == mFixedData ? 0 : mFixedData.getItemCount();
        mPageItemCount = 0;
        for (int idx = 0; idx < mSections.size(); idx++) {
            mPageItemCount += mSections.get(idx).getItemCount();
        }

        mItemCount += mPageItemCount;
    }

    public SectionData getPageSection(int sectionIndex) {
        return 0 <= sectionIndex && sectionIndex < mSections.size() ? mSections.get(sectionIndex) : null;
    }

    public SectionData getSection(int sectionIndex) {
        if (sectionIndex < mFixedData.getNumberOfSections()) {
            return mFixedData.getSection(sectionIndex);
        }

        sectionIndex -= mFixedData.getNumberOfSections();

        return 0 <= sectionIndex && sectionIndex < mSections.size() ? mSections.get(sectionIndex) : null;
    }

    public int getNumberOfSections() {
        int numberOfSections = null == mFixedData ? 0 : mFixedData.getNumberOfSections();
        numberOfSections += mSections.size();
        return numberOfSections;
    }

    public int getNumberOfPageSections() {
        return mSections.size();
    }
}
