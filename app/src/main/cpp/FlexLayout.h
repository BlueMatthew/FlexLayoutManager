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


#ifndef NDK_DEBUG
#include <android/log.h>

#define TAG "NDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif // NDK_DEBUG


#define INVALID_OFFSET INT_MIN

template<bool VERTICAL>
class SectionT : public nsflex::FlexSectionT<LayoutCallbackAdapter, int, int, VERTICAL>
{
protected:
    typedef nsflex::FlexSectionT<LayoutCallbackAdapter, int, int, VERTICAL> BaseSection;
    typedef typename BaseSection::LayoutType TLayout;
    typedef typename BaseSection::IntType TInt;
    typedef typename BaseSection::CoordinateType TCoordinate;

    int m_position;

public:
    SectionT(TInt section, const Rect& frame) : BaseSection(section, frame), m_position(0)
    {
    }

    int getPositionBase() const { return m_position; }
    void setPositionBase(int position) { m_position = position; }
};





// using FlexSection = SectionT<true>;
// typedef nsflex::FlexFlowSectionT<SectionT<true>, true> FlexFlowSection;
// typedef nsflex::FlexWaterfallSectionT<SectionT<true>, true> FlexWaterfallSection;
// typedef nsflex::FlexVerticalCompareT<SectionT<true>> FlexSectionVerticalCompare;
// typedef nsflex::FlexHorizontalCompareT<SectionT<true>> FlexSectionHorizontalCompare;
// typedef std::vector<SectionT<true> *>::const_iterator FlexSectionConstIterator;
// typedef std::pair<FlexSectionConstIterator, FlexSectionConstIterator> FlexSectionConstIteratorPair;

template<class T>
struct FlexSectionPositionCompare
{
    bool operator() ( const T* section, int position) const
    {
        return section->getPositionBase() + section->getItemCount() <= position;
    }
    bool operator() ( int position, const T* item ) const
    {
        return position < item->getPositionBase();
    }
};

template<bool VERTICAL>
class FlexLayout
{
public:

    using Section = SectionT<VERTICAL>;
    using FlowSection = typename nsflex::FlexFlowSectionT<Section, VERTICAL>;
    // using FlowSection = typename nsflex::FlexFlowSectionT<Section, VERTICAL>;
    using WaterfallSection = typename nsflex::FlexWaterfallSectionT<Section, VERTICAL>;
    using SectionCompare = nsflex::FlexCompareT<Section, VERTICAL>;
    using SectionConstIterator = typename std::vector<Section *>::const_iterator;
    using SectionConstIteratorPair = std::pair<SectionConstIterator, SectionConstIterator>;
    using SectionPositionCompare = FlexSectionPositionCompare<Section>;

protected:

    std::vector<Section *> m_sections;

public:
    FlexLayout()
    {

    }
    ~FlexLayout()
    {
        clearSections();
    }

    Size prepareLayout(const LayoutCallbackAdapter& layoutCallbackAdapter, const LayoutAndSectionsInfo &layoutAndSectionsInfo)
    {
        struct timeval startTime;
        gettimeofday(&startTime, NULL);

        clearSections();

        int sectionCount = layoutCallbackAdapter.getNumberOfSections();
        if (sectionCount <= 0)
        {
            layoutCallbackAdapter.updateContentSize(layoutAndSectionsInfo.size.width, layoutAndSectionsInfo.padding.top + layoutAndSectionsInfo.padding.bottom);
            return Size(layoutAndSectionsInfo.size.width, layoutAndSectionsInfo.padding.top + layoutAndSectionsInfo.padding.bottom);
        }

        Rect bounds(layoutAndSectionsInfo.padding.left, layoutAndSectionsInfo.padding.top, layoutAndSectionsInfo.size.width - layoutAndSectionsInfo.padding.right - layoutAndSectionsInfo.padding.right, layoutAndSectionsInfo.size.height - layoutAndSectionsInfo.padding.top - layoutAndSectionsInfo.padding.bottom);
        int positionBase = 0;

        Rect rectOfSection(layoutAndSectionsInfo.padding.left, layoutAndSectionsInfo.padding.top, bounds.width(), 0);
        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            int layoutMode = layoutCallbackAdapter.getLayoutModeForSection(sectionIndex);
            Section *section = layoutMode == 1 ?
                                   static_cast<Section *>(new WaterfallSection(sectionIndex, rectOfSection)) :
                                   static_cast<Section *>(new FlowSection(sectionIndex, rectOfSection));

            section->setPositionBase(positionBase);
            section->prepareLayout(&layoutCallbackAdapter, bounds);

            m_sections.push_back(section);

            positionBase += section->getItemCount();

            rectOfSection.origin.y += section->getFrame().height();
            // rectOfSection.size.height = 0;
        }

        int contentHeight = (int)rectOfSection.bottom() + layoutAndSectionsInfo.padding.bottom;
        layoutCallbackAdapter.updateContentSize(layoutAndSectionsInfo.size.width, contentHeight);

        struct timeval endTime;
        gettimeofday(&endTime, NULL);

        struct timeval t_result;
        timersub(&endTime, &startTime, &t_result);

        LOGI("PERF prepareLayout(C) elapsed time= %dns \n", (int)(t_result.tv_usec));

        return Size(layoutAndSectionsInfo.size.width, contentHeight);
    }
    void updateItems(int action, int itemStart, int itemCount)
    {

    }

    // LayoutItem::data == 1, indicates that the item is sticky
    void getItemsInRect(std::vector<LayoutItem> &items, std::vector<std::pair<StickyItem, Point>> &changingStickyItems, std::vector<StickyItem> &stickyItems, bool stackedStickyItems, const LayoutInfo &layoutInfo) const;

    int computerContentOffsetToMakePositionTopVisible(std::vector<StickyItem> &stickyItems, bool stackedStickyItems, const LayoutInfo &layoutInfo, int position, int positionOffset) const;

    bool getItem(int position, LayoutItem &layoutItem) const;


protected:

    inline void clearSections()
    {
        for (typename std::vector<Section *>::iterator it = m_sections.begin(); it != m_sections.end(); delete *it, ++it);
        m_sections.clear();
    }

    LayoutItem *makeLayoutItem(int sectionIndex, int itemIndex) const
    {
        if (sectionIndex < m_sections.size())
        {
            Section *section = m_sections[sectionIndex];

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

template<bool VERTICAL>
void FlexLayout<VERTICAL>::getItemsInRect(std::vector<LayoutItem> &items, std::vector<std::pair<StickyItem, Point>> &changingStickyItems, std::vector<StickyItem> &stickyItems, bool stackedStickyItems, const LayoutInfo &layoutInfo) const
{
#define INTERNAL_VERTICAL_LAYOUT

    Rect rect(layoutInfo.contentOffset.x, layoutInfo.contentOffset.y, layoutInfo.size.width, layoutInfo.size.height);   // visibleRect

#ifdef INTERNAL_VERTICAL_LAYOUT
    SectionConstIteratorPair range = std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.top(), rect.bottom()), SectionCompare());
#else
    FlexSectionConstIteratorPair range = std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.left(), rect.right()), FlexSectionHorizontalCompare());
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT

    if (range.first == range.second)
    {
        // No Sections
        return;
    }

    std::vector<const FlexItem *> flexItems;
    for (SectionConstIterator it = range.first; it != range.second; ++it)
    {
        (*it)->filterInRect(VERTICAL, flexItems, rect);
        if (flexItems.empty())
        {
            continue;
        }

        for (std::vector<const FlexItem *>::const_iterator itItem = flexItems.begin(); itItem != flexItems.end(); ++itItem)
        {
            if ((*itItem)->isPlaceHolder())
            {
                continue;
            }

            Rect frame = (*itItem)->getFrame();
            frame.offset((*it)->getFrame().left(), (*it)->getFrame().top());

            LayoutItem item((*it)->getSection(), (*itItem)->getItem(), (*it)->getPositionBase() + (*itItem)->getItem(), frame);

            items.push_back(item);
        }
        flexItems.clear();
    }

    if (!stickyItems.empty())
    {
        int maxSection = range.second - 1 - m_sections.begin();
        int minSection = range.first - m_sections.begin();

        int totalStickyItemSize = 0; // When m_stackedStickyItems == YES

        LayoutStickyItemCompare comp;
        Rect rect;
        Point origin;

        for (std::vector<StickyItem>::iterator it = stickyItems.begin(); it != stickyItems.end(); ++it)
        {
            if (it->section > maxSection || (!stackedStickyItems && (it->section < minSection)))
            {
                if (it->inSticky)
                {
                    it->inSticky = false;
                    // Pass the change info to caller
                    changingStickyItems.push_back(std::make_pair(*it, Point()));
                    // notifyItemLeavingStickyMode((*it)->section, (*it)->item, (*it)->position);
                }
                continue;
            }

            Section *section = m_sections[it->section];
            const FlexItem *item = section->getItem(it->item);
            if (item == NULL)
            {
                continue;
            }

            it->position = section->getPositionBase() + item->getItem();

            rect = item->getFrame();
            rect.offset(section->getFrame().origin.x - layoutInfo.contentOffset.x, section->getFrame().origin.y - layoutInfo.contentOffset.y);
            origin = rect.origin;

#ifdef INTERNAL_VERTICAL_LAYOUT
            int stickyItemSize = rect.height();
#else
            int stickyItemSize = rect.width();
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT

            if (stackedStickyItems)
            {
#ifdef INTERNAL_VERTICAL_LAYOUT
                rect.origin.y = std::max(totalStickyItemSize + layoutInfo.padding.top, origin.y);
#else
                rect.origin.x = std::max(totalStickyItemSize + insets.left, origin.x);
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT
            }
            else
            {
                Rect lastItemInSection = section->getItem(section->getItemCount() - 1)->getFrame();
                Rect frameItems(origin.x, origin.y, lastItemInSection.right(), lastItemInSection.bottom());
                frameItems.offset(section->getFrame().left(), section->getFrame().top());
#ifdef INTERNAL_VERTICAL_LAYOUT
                rect.origin.y = std::min(std::max(layoutInfo.padding.top, (frameItems.origin.y - stickyItemSize)),
                                         (frameItems.bottom() - stickyItemSize));
#else
                rect.origin.x = std::min(std::max(insets.left, (frameItems.origin.x - stickyItemSize)),
                        (frameItems.right() - stickyItemSize));
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT
            }

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
#ifdef INTERNAL_VERTICAL_LAYOUT
            bool stickyMode = it->inSticky ? ((layoutInfo.contentOffset.y + layoutInfo.padding.top < rect.origin.y) ? false : true) : ((rect.origin.y >= origin.y) ? true : false);
            bool originChanged = it->inSticky ? ((rect.origin.y >= layoutInfo.contentOffset.x + layoutInfo.padding.top) ? false : true) : ((rect.origin.y > origin.y) ? true : false);
            // bool stickyMode = (rect.origin.y >= origin.y);
#else
            bool stickyMode = it->inSticky ? ((contentOffset.x + insets.left < rect.origin.x) ? false : true) : ((rect.origin.x >= origin.x) ? true : false);
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT
            if (stickyMode != it->inSticky)
            {
                // Pass the change info to caller
                it->inSticky = stickyMode;
                changingStickyItems.push_back(std::make_pair(*it, origin));
            }

            if (stickyMode)
            {
                std::vector<LayoutItem>::iterator itVisibleItem = std::lower_bound(items.begin(), items.end(), *it, comp);
                if (itVisibleItem == items.end() || (itVisibleItem->section != it->section || itVisibleItem->item != it->item))
                {
                    // Create new LayoutItem and put it into visibleItems
                    LayoutItem *layoutItem = new LayoutItem(it->section, it->item, it->position, rect);
                    layoutItem->setInSticky(true);
                    layoutItem->setOriginChanged(true);
                    items.insert(itVisibleItem, layoutItem);
                }
                else
                {
                    // Update in place
                    itVisibleItem->frame = rect;
                    itVisibleItem->setInSticky(true);
                    itVisibleItem->setOriginChanged(true);
                }

                totalStickyItemSize += stickyItemSize;
            }
        }
    }

#undef INTERNAL_VERTICAL_LAYOUT
}

template<bool VERTICAL>
bool FlexLayout<VERTICAL>::getItem(int position, LayoutItem &layoutItem) const
{
    SectionConstIterator it = std::upper_bound(m_sections.begin(), m_sections.end(), position, SectionPositionCompare());
    if (it == m_sections.end())
    {
        return false;
    }

    int itemIndex = position - (*it)->getPositionBase();
    if (itemIndex >= (*it)->getItemCount())
    {
        return false;
    }

    // layoutItem = *it;
    Rect rect = (*it)->getItemFrameInView(itemIndex);
    layoutItem.reset((*it)->getSection(), itemIndex, position, rect, 0);

    return true;
}

template<bool VERTICAL>
int FlexLayout<VERTICAL>::computerContentOffsetToMakePositionTopVisible(std::vector<StickyItem> &stickyItems, bool stackedStickyItems, const LayoutInfo &layoutInfo, int position, int positionOffset) const
{
    SectionConstIterator itTargetSection = std::lower_bound(m_sections.begin(), m_sections.end(), position, SectionPositionCompare());
    if (itTargetSection == m_sections.end())
    {
        return INVALID_OFFSET;
    }

    FlexItem *targetItem = (*itTargetSection)->getItem(position - (*itTargetSection)->getPositionBase());
    if (NULL == targetItem)
    {
        return INVALID_OFFSET;
    }

#ifdef INTERNAL_VERTICAL_LAYOUT
    int newContentOffset = targetItem->getFrame().top() + positionOffset;
#else
    int newContentOffset = targetItem->getFrame().left() + positionOffset;
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT

    int totalStickyItemSize = 0; // When m_stackedStickyItems == true
    Rect rect;
    Point origin;   // old origin

    for (std::vector<StickyItem>::const_iterator it = stickyItems.begin(); it != stickyItems.end(); ++it)
    {
        if ((!stackedStickyItems) && (it->section < (*itTargetSection)->getSection()))
        {
            continue;
        }
        if (it->section > (*itTargetSection)->getSection() || (it->section == (*itTargetSection)->getSection() && it->item > targetItem->getItem()))
        {
            // If the sticky item is greater than target item
            break;
        }

        // int sectionIndex = it->section;
        const Section *section = m_sections[it->section];

        rect = section->getItemFrameInView(it->item);
        // TODO:There should be bug
        rect.offset(-layoutInfo.contentOffset.x, -layoutInfo.contentOffset.y);
        origin = rect.origin;

#ifdef INTERNAL_VERTICAL_LAYOUT
        int stickyItemSize = rect.height();
#else
        int stickyItemSize = rect.width();
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT

        if (stackedStickyItems)
        {
#ifdef INTERNAL_VERTICAL_LAYOUT
            rect.origin.y = std::max(totalStickyItemSize + layoutInfo.padding.top, rect.origin.y);
#else
            rect.origin.x = std::max(totalStickyItemSize + layoutInfo.padding.left, rect.origin.x);
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT
        }
        else
        {
#ifdef INTERNAL_VERTICAL_LAYOUT
            Rect frameItems = section->getItemsFrameInViewAfterItemVertically(it->item);
            rect.origin.y = std::min(
                    std::max(layoutInfo.padding.top, (frameItems.origin.y - stickyItemSize)),
                    (frameItems.bottom() - stickyItemSize)
            );
#else
            Rect frameItems = section->getItemsFrameInViewAfterItemHorizontally(it->item);
            rect.origin.x = std::min(
                    std::max(layoutInfo.padding.left, (frameItems.origin.x - stickyItemSize)),
                    (frameItems.right() - stickyItemSize)
            );
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT
        }

        // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
        // Otherwise, we check the top of sticky header
#ifdef INTERNAL_VERTICAL_LAYOUT
        bool stickyMode = it->inSticky ? ((layoutInfo.contentOffset.y + layoutInfo.padding.top < rect.origin.y) ? false : true) : ((rect.origin.y >= origin.y) ? true : false);
#else
        bool stickyMode = it->inSticky ? ((layoutInfo.contentOffset.x + layoutInfo.padding.left < rect.origin.x) ? false : true) : ((rect.origin.x >= origin.x) ? true : false);
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT
        if (stickyMode)
        {
            totalStickyItemSize = stackedStickyItems ? (totalStickyItemSize + stickyItemSize) : stickyItemSize;
        }

    }

    int contentOffset = 0;
    rect = targetItem->getFrame();
    rect.offset((*itTargetSection)->getFrame().origin.x, (*itTargetSection)->getFrame().origin.y);
#ifdef INTERNAL_VERTICAL_LAYOUT
    contentOffset = rect.top() - layoutInfo.padding.top - totalStickyItemSize + positionOffset;
#else
    contentOffset = rect.left() - layoutInfo.padding.left - totalStickyItemSize + positionOffset;
#endif // #ifdef INTERNAL_VERTICAL_LAYOUT

    if (1 == layoutInfo.orientation)
    {
        contentOffset = rect.top() - layoutInfo.padding.top - totalStickyItemSize + positionOffset;
    }
    else
    {
        contentOffset = rect.left() - layoutInfo.padding.left - totalStickyItemSize + positionOffset;
    }


    return contentOffset;
}



#endif //FLEXLAYOUTMANAGER_FLEXLAYOUT_H
