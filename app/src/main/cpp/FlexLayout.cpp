//
// Created by Matthew on 2020-07-03.
//

#include <jni.h>
#include "LayoutCallbackAdapter.h"
#include "FlexLayout.h"

FlexLayout::FlexLayout() : m_stackedStickyItems(true)
{
}

FlexLayout::~FlexLayout()
{
    clearSections();
    clearStickyItems();
}

Size FlexLayout::prepareLayout(const LayoutCallbackAdapter& layoutCallbackAdapter, const LayoutInfo *layoutInfo)
{
    clearSections();

    int sectionCount = layoutCallbackAdapter.getNumberOfSections();
    if (sectionCount <= 0)
    {
        layoutCallbackAdapter.updateContentSize(layoutInfo->size.width, layoutInfo->padding.top + layoutInfo->padding.bottom);
        return Size(layoutInfo->size.width, layoutInfo->padding.top + layoutInfo->padding.bottom);
    }

    Rect bounds(layoutInfo->padding.left, layoutInfo->padding.top, layoutInfo->size.width - layoutInfo->padding.right - layoutInfo->padding.right, layoutInfo->size.height - layoutInfo->padding.top - layoutInfo->padding.bottom);
    int positionBase = 0;

    Rect rectOfSection(layoutInfo->padding.left, layoutInfo->padding.top, bounds.width(), 0);
    for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
        int layoutMode = layoutCallbackAdapter.getLayoutModeForSection(sectionIndex);
        FlexSection *section = layoutMode == 1 ?
                static_cast<FlexSection *>(new FlexWaterfallSection(sectionIndex, rectOfSection)) :
                static_cast<FlexSection *>(new FlexFlowSection(sectionIndex, rectOfSection));

        section->setPositionBase(positionBase);
        section->prepareLayout(&layoutCallbackAdapter, bounds);

        m_sections.push_back(section);

        positionBase += section->getItemCount();

        rectOfSection.origin.y += section->getFrame().height();
        // rectOfSection.size.height = 0;
    }

    int contentHeight = (int)rectOfSection.bottom() + layoutInfo->padding.bottom;
    layoutCallbackAdapter.updateContentSize(layoutInfo->size.width, contentHeight);

    return Size(layoutInfo->size.width, contentHeight);
}

void FlexLayout::updateItems(int action, int itemStart, int itemCount)
{

}

void FlexLayout::getItemsInRect(std::vector<LayoutItem> &items, std::vector<std::pair<StickyItem, Point>> &changingStickyItems, bool vertical, const Size &size, const Insets &insets, const Size &contentSize, const Point &contentOffset) const
{
    // bool vertical = m_layoutAdapter.isVertical();

    Rect rect(contentOffset.x, contentOffset.y, size.width, size.height);   // visibleRect

    std::pair<std::vector<FlexSection *>::const_iterator, std::vector<FlexSection *>::const_iterator> range = vertical ?
            std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.top(), rect.bottom()), FlexSectionVerticalCompare()) :
            std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.left(), rect.width()), FlexSectionHorizontalCompare());
    if (range.first == range.second)
    {
        // No Sections
        return;
    }

    // std::vector<LayoutItem *> visibleItems;

    std::vector<const FlexItem *> flexItems;
    for (std::vector<FlexSection *>::const_iterator it = range.first; it != range.second; ++it)
    {
        (*it)->filterInRect(vertical, flexItems, rect);
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

            // m_layoutAdapter.addLayoutItem(javaList, item);
            items.push_back(item);
        }
        flexItems.clear();
    }

    if (!m_stickyItems.empty())
    {
        int maxSection = range.second - 1 - m_sections.begin();
        int minSection = range.first - m_sections.begin();

        // UIEdgeInsets contentInset = self.collectionView.contentInset;
        int totalStickyItemSize = 0; // When m_stackedStickyItems == YES

        LayoutStickyItemCompare comp;
        Rect rect;
        Point origin;

        for (std::vector<StickyItem>::iterator it = m_stickyItems.begin(); it != m_stickyItems.end(); ++it)
        {
            if (it->section > maxSection || (!m_stackedStickyItems && (it->section < minSection)))
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

            FlexSection *section = m_sections[it->section];
            const FlexItem *item = section->getItem(it->item);
            if (item == NULL)
            {
                continue;
            }

            it->position = section->getPositionBase() + item->getItem();

            rect = item->getFrame();
            rect.offset(section->getFrame().origin.x - contentOffset.x, section->getFrame().origin.y - contentOffset.y);
            origin = rect.origin;

            int stickyItemSize = rect.height();

            if (m_stackedStickyItems)
            {
                rect.origin.y = std::max(totalStickyItemSize + insets.top, origin.y);
            }
            else
            {
                Rect lastItemInSection = section->getItem(section->getItemCount() - 1)->getFrame();
                Rect frameItems(origin.x, origin.y, lastItemInSection.right(), lastItemInSection.bottom());
                frameItems.offset(section->getFrame().left(), section->getFrame().top());

                rect.origin.y = std::min(
                        std::max(insets.top, (frameItems.origin.y - stickyItemSize)),
                        (frameItems.bottom() - stickyItemSize)
                );
            }

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            bool stickyMode = it->inSticky ? ((contentOffset.y + insets.top < rect.origin.y) ? false : true) : ((rect.origin.y > origin.y) ? true : false);
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
                    items.insert(itVisibleItem, layoutItem);
                }
                else
                {
                    // Update in place
                    itVisibleItem->frame = rect;
                    itVisibleItem->setInSticky(true);
                }

                // layoutAttributes.zIndex = 1024 + it->first;  //
                totalStickyItemSize += stickyItemSize;
            }
        }
    }
}

jobject FlexLayout::calcContentOffsetForScroll(int position, int itemOffsetX, int itemOffsetY) const
{
    return NULL;
}
