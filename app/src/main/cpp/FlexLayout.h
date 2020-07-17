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
#include "LayoutCallbackAdapter.h"


#ifdef NDK_DEBUG
#include <android/log.h>

#define TAG "NDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif // NDK_DEBUG


#define INVALID_OFFSET INT_MIN

class FlexSection : public nsflex::FlexSectionT<LayoutCallbackAdapter, int, int>
{
protected:
    typedef nsflex::FlexSectionT<LayoutCallbackAdapter, int, int> BaseSection;
    typedef typename BaseSection::LayoutType TLayout;
    typedef typename BaseSection::IntType TInt;
    typedef typename BaseSection::CoordinateType TCoordinate;

    int m_position;

public:
    FlexSection(TInt section, const Rect& frame) : BaseSection(section, frame), m_position(0)
    {
    }

    int getPositionBase() const { return m_position; }
    void setPositionBase(int position) { m_position = position; }


};

typedef nsflex::FlexFlowSectionT<FlexSection> FlexFlowSection;
typedef nsflex::FlexWaterfallSectionT<FlexSection> FlexWaterfallSection;
typedef nsflex::FlexVerticalCompareT<FlexSection> FlexSectionVerticalCompare;
typedef nsflex::FlexHorizontalCompareT<FlexSection> FlexSectionHorizontalCompare;
typedef std::vector<FlexSection *>::const_iterator FlexSectionConstIterator;
typedef std::pair<FlexSectionConstIterator, FlexSectionConstIterator> FlexSectionConstIteratorPair;


struct FlexSectionPositionCompare
{
    bool operator() ( const FlexSection* section, int position) const
    {
        return section->getPositionBase() + section->getItemCount() <= position;
    }
    bool operator() ( int position, const FlexSection* item ) const
    {
        return position < item->getPositionBase();
    }
};


class FlexLayout
{
protected:

    std::vector<FlexSection *> m_sections;
    mutable std::vector<StickyItem> m_stickyItems;

    bool m_stackedStickyItems;

public:
    FlexLayout();
    ~FlexLayout();

    void addStickyItem(int section, int item)
    {
        m_stickyItems.push_back(StickyItem(section, item));
        if (m_stickyItems.size() > 1)
        {
            std::sort(m_stickyItems.begin(), m_stickyItems.end());
        }
    }

    void clearStickyItems()
    {
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

    Size prepareLayout(const LayoutCallbackAdapter& layoutCallbackAdapter, const LayoutAndSectionsInfo &layoutAndSectionsInfo);
    void updateItems(int action, int itemStart, int itemCount);

    // LayoutItem::data == 1, indicates that the item is sticky
    void getItemsInRect(std::vector<LayoutItem> &items, std::vector<std::pair<StickyItem, Point>> &changingStickyItems, bool vertical, const Size &size, const Insets &insets, const Point &contentOffset) const;

    int computerContentOffsetToMakePositionTopVisible(const LayoutInfo &layoutInfo, int position, int positionOffset) const;

    bool getItem(int position, LayoutItem &layoutItem) const;


protected:

    inline void clearSections()
    {
        for (std::vector<FlexSection *>::iterator it = m_sections.begin(); it != m_sections.end(); delete *it, ++it);
        m_sections.clear();
    }

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
