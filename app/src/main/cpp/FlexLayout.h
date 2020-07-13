//
// Created by Matthew on 2020-07-03.
//

#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUT_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUT_H

#include <functional>
#include "Graphics.h"
#include "FlexItem.h"
#include "FlexRow.h"
#include "FlexColumn.h"
#include "FlexSection.h"
#include "FlexFlowSection.h"
#include "FlexWaterfallSection.h"
#include "LayoutCallbackAdapter.h"


class FlexSection : public nsflex::FlexSectionT<LayoutCallbackAdapter, int, int>
{
protected:
    typedef nsflex::FlexSectionT<LayoutCallbackAdapter, int, int> BaseSection;
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


typedef nsflex::FlexItemT<int, int> FlexItem;
typedef nsflex::FlexVerticalCompareT<FlexItem> FlexItemVerticalCompare;
typedef nsflex::FlexHorizontalCompareT<FlexItem> FlexItemHorizontalCompare;

typedef nsflex::FlexFlowSectionT<FlexSection> FlexFlowSection;
typedef nsflex::FlexWaterfallSectionT<FlexSection> FlexWaterfallSection;
typedef nsflex::FlexVerticalCompareT<FlexSection> FlexSectionVerticalCompare;
typedef nsflex::FlexHorizontalCompareT<FlexSection> FlexSectionHorizontalCompare;


struct LayoutItem
{
    int section;
    int item;
    int position;
    Rect frame;
    int data;

    LayoutItem(int s, int i, int pos, const Rect &f) : section(s), item(i), position(pos), frame(f), data(0) {}
    bool operator==(const LayoutItem &other)
    {
        if (this == &other) return true;
        return section == other.section && item == other.item;
    }

    bool operator==(const LayoutItem *other)
    {
        return *this == *other;
    }
};

struct LayoutItemCompare
{
    bool operator() ( const LayoutItem* lhs, const LayoutItem* rhs) const
    {
        return lhs->section < rhs->section || (lhs->section == rhs->section && lhs->item < rhs->item);
    }
};

class FlexLayout
{
protected:

    LayoutCallbackAdapter m_layoutCallback;
    std::vector<FlexSection *> m_sections;
    std::vector<LayoutItem *> m_stickyItems;

    bool m_stackedStickyItems;

    // bool m_vertical;

public:
    FlexLayout(JNIEnv* env, jobject obj, jobject callback);
    ~FlexLayout();


    void clearSections()
    {
        for (std::vector<FlexSection *>::iterator it = m_sections.begin(); it != m_sections.end(); delete *it, ++it);
        m_sections.clear();
    }

    void prepareLayout(const Size &size, const Insets &padding);
    // LayoutItem::data == 1, indicates that the item is sticky
    void getItemsInRect(std::vector<LayoutItem *> &items, const Size &size, const Insets &insets, const Size &contentSize, const Point &contentOffset) const;

    void notifyItemEnterringStickyMode(int section, int item, Point pt) const;
    void notifyItemLeavingStickyMode(int section, int item) const;

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
