//
// Created by Matthew on 2020-07-03.
//

#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUT_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUT_H

#include <algorithm>
#include "FlexLayoutObjects.h"

#include "FlexSection.h"
#include "FlexFlowSection.h"
#include "FlexWaterfallSection.h"
#include "LayoutAdapter.h"


#ifndef NDK_DEBUG
#include <android/log.h>

#define TAG "NDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif // NDK_DEBUG


class FlexSection : public nsflex::FlexSectionT<LayoutAdapter, int, int>
{
protected:
    typedef nsflex::FlexSectionT<LayoutAdapter, int, int> BaseSection;
    typedef typename BaseSection::LayoutType TLayout;
    typedef typename BaseSection::IntType TInt;
    typedef typename BaseSection::CoordinateType TCoordinate;

    int m_position;

public:
    FlexSection(TLayout *layout, TInt section, const Rect& frame) : BaseSection(layout, section, frame), m_position(0)
    {
    }

    int getPositionBase() const { return m_position; }
    void setPositionBase(int position) { m_position = position; }


};

typedef nsflex::FlexFlowSectionT<FlexSection> FlexFlowSection;
typedef nsflex::FlexWaterfallSectionT<FlexSection> FlexWaterfallSection;
typedef nsflex::FlexVerticalCompareT<FlexSection> FlexSectionVerticalCompare;
typedef nsflex::FlexHorizontalCompareT<FlexSection> FlexSectionHorizontalCompare;


class FlexLayout
{
protected:

    LayoutAdapter m_layoutAdapter;
    std::vector<FlexSection *> m_sections;
    std::vector<LayoutItem *> m_stickyItems;

    bool m_stackedStickyItems;

public:
    FlexLayout(JNIEnv* env, jobject obj, jobject callback);
    ~FlexLayout();

    void clearSections()
    {
        for (std::vector<FlexSection *>::iterator it = m_sections.begin(); it != m_sections.end(); delete *it, ++it);
        m_sections.clear();
    }

    void addStickyItem(int section, int item)
    {
        LayoutItem *layoutItem = new LayoutItem(section, item);
        m_stickyItems.push_back(layoutItem);
        if (m_stickyItems.size() > 1)
        {
            std::sort(m_stickyItems.begin(), m_stickyItems.end());
        }
    }
    void clearStickyItems()
    {
        for (std::vector<LayoutItem *>::iterator it = m_stickyItems.begin(); it != m_stickyItems.end(); delete *it, ++it);
        m_stickyItems.clear();
    }

    void setStackedStickyItems(bool stackedStickyItems)
    {
        m_stackedStickyItems = stackedStickyItems;
    }

    bool isStackedStickyItems() const
    {
        return m_stackedStickyItems;
    }

    void prepareLayout(const Size &size, const Insets &padding);
    void updateItems(int action, int itemStart, int itemCount);

    // LayoutItem::data == 1, indicates that the item is sticky
    void getItemsInRect(jobject javaList, const Size &size, const Insets &insets, const Size &contentSize, const Point &contentOffset) const;

    jobject calcContentOffsetForScroll(int position, int itemOffsetX, int itemOffsetY) const;

    void notifyItemEnterringStickyMode(int section, int item, int position, Point pt) const;
    void notifyItemLeavingStickyMode(int section, int item, int position) const;


protected:

    LayoutItem *makeLayoutItem(int sectionIndex, int itemIndex) const
    {
        if (sectionIndex < m_sections.size())
        {
            FlexSection *section = m_sections[sectionIndex];

            FlexItem *item = section->getItem(itemIndex);

            if (NULL != item)
            {
                Rect rect = item->getFrame();
                rect.offset(section->getFrame().left(), section->getFrame().top());

                return new LayoutItem(sectionIndex, itemIndex, section->getPositionBase() + itemIndex, rect);
            }
        }
        return NULL;
    }

};


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUT_H
