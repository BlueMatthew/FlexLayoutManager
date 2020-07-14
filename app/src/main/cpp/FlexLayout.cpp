//
// Created by Matthew on 2020-07-03.
//

#include <jni.h>
#include "LayoutAdapter.h"
#include "FlexLayout.h"

FlexLayout::FlexLayout(JNIEnv* env, jobject obj, jobject callback) : m_layoutAdapter(env, obj, callback), m_stackedStickyItems(true)
{
}

FlexLayout::~FlexLayout()
{
    clearSections();
    clearStickyItems();
}

void FlexLayout::prepareLayout(const Size &size, const Insets &padding)
{
    clearSections();

    int sectionCount = m_layoutAdapter.getNumberOfSections();
    if (sectionCount <= 0)
    {
        m_layoutAdapter.updateContentHeight(padding.top + padding.bottom);
        return;
    }

    m_layoutAdapter.beginPreparingLayout(128);

    Rect bounds(padding.left, padding.top, size.width - padding.right - padding.right, size.height - padding.top - padding.bottom);
    int positionBase = 0;

    Rect rectOfSection(padding.left, padding.top, bounds.width(), 0);
    for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
        int layoutMode = m_layoutAdapter.getLayoutModeForSection(sectionIndex);
        FlexSection *section = layoutMode == 1 ?
                static_cast<FlexSection *>(new FlexWaterfallSection(&m_layoutAdapter, sectionIndex, rectOfSection)) :
                static_cast<FlexSection *>(new FlexFlowSection(&m_layoutAdapter, sectionIndex, rectOfSection));

        section->setPositionBase(positionBase);
        section->prepareLayout(bounds);

        m_sections.push_back(section);

        positionBase += section->getItemCount();

        rectOfSection.origin.y += section->getFrame().height();
        // rectOfSection.size.height = 0;
    }

    int contentHeight = (int)rectOfSection.bottom() + padding.bottom;
    m_layoutAdapter.updateContentHeight(contentHeight);

    m_layoutAdapter.endPreparingLayout();

#ifndef NDK_DEBUG
    /*
    for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++)
    {
        FlexSection *section = m_sections[sectionIndex];

        int itemCount = section->getItemCount();
        Rect rectSection = section->getFrame();

        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++)
        {
            FlexItem *item = section->getItem(itemIndex);

            if (NULL == item)
            {
                LOGD("[%d, %d] is null", sectionIndex, itemIndex);
            }
            else
            {
                const Rect rect = item->getFrame();

                LOGD("[%d, %d]: section point=%d-%d point=%d-%d, size=%d-%d", sectionIndex, itemIndex, rectSection.left(), rectSection.top(), rect.left(), rect.top(), rect.width(), rect.height());
            }

        }
    }
     */

    LOGD("PERF nativPrepareLayout ends.");
#endif // NDK_DEBUG

}

void FlexLayout::updateItems(int action, int itemStart, int itemCount)
{

}

void FlexLayout::getItemsInRect(jobject javaList, const Size &size, const Insets &insets, const Size &contentSize, const Point &contentOffset) const
{
    bool vertical = m_layoutAdapter.isVertical();

    Rect rect(contentOffset.x, contentOffset.y, size.width, size.height);   // visibleRect

    std::pair<std::vector<FlexSection *>::const_iterator, std::vector<FlexSection *>::const_iterator> range = vertical ?
            std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.top(), rect.bottom()), FlexSectionVerticalCompare()) :
            std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.left(), rect.width()), FlexSectionHorizontalCompare());
    if (range.first == range.second)
    {
        // No Sections
        return;
    }

    std::vector<LayoutItem *> visibleItems;

    std::vector<const FlexItem *> flexItems;
    for (std::vector<FlexSection *>::const_iterator it = range.first; it != range.second; ++it)
    {
        (*it)->filterInRect(flexItems, rect);
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

            LayoutItem *item = new LayoutItem((*it)->getSection(), (*itItem)->getItem(), (*it)->getPositionBase() + (*itItem)->getItem(), frame);

            // m_layoutAdapter.addLayoutItem(javaList, item);
            visibleItems.push_back(item);
        }
        flexItems.clear();
    }

    if (!m_stickyItems.empty())
    {
        int maxSection = range.second - 1 - m_sections.begin();
        int minSection = range.first - m_sections.begin();

        // UIEdgeInsets contentInset = self.collectionView.contentInset;
        int totalStickyItemSize = 0; // When m_stackedStickyItems == YES

        Point origin;

        LayoutItemCompare comp;
        for (std::vector<LayoutItem *>::const_iterator it = m_stickyItems.begin(); it != m_stickyItems.end(); ++it)
        {
            if ((*it)->section > maxSection || (!m_stackedStickyItems && ((*it)->section < minSection)))
            {
                if ((*it)->data == 1)
                {
                    (*it)->clearStickyFlag();
                    notifyItemLeavingStickyMode((*it)->section, (*it)->item, (*it)->position);
                }
                continue;
            }

            FlexSection *section = m_sections[(*it)->section];
            const FlexItem *item = section->getItem((*it)->item);
            if (item == NULL)
            {
                continue;
            }

            (*it)->set(section->getPositionBase() + item->getItem(), item->getFrame());
            (*it)->frame.offset(section->getFrame().origin.x - contentOffset.x, section->getFrame().origin.y - contentOffset.y);
            (*it)->origin = (*it)->frame.origin;
            origin = (*it)->origin;

            // LayoutItem *item = makeLayoutItem((*it)->section, (*it)->item);
            (*it)->frame.offset(-contentOffset.x, -contentOffset.y);

            int stickyItemSize = (*it)->frame.height();
            if (m_stackedStickyItems)
            {
                origin.y = std::max(totalStickyItemSize + insets.top, origin.y);
            }
            else
            {
                Rect lastItemInSection = section->getItem(section->getItemCount() - 1)->getFrame();
                Rect frameItems(origin.x, origin.y, lastItemInSection.right(), lastItemInSection.bottom());
                frameItems.offset(section->getFrame().left(), section->getFrame().top());

                origin.y = std::min(
                        std::max(insets.top, (frameItems.origin.y - stickyItemSize)),
                        (frameItems.bottom() - stickyItemSize)
                );
            }
            (*it)->frame.origin = origin;

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            bool stickyMode = (*it)->data == 1 ? ((contentOffset.y + insets.top < (*it)->origin.y) ? false : true) : (((*it)->frame.origin.y > (*it)->origin.y) ? true : false);
            if (stickyMode != (*it)->isInSticky())
            {
                // Notify caller if changed
                (*it)->setInSticky(stickyMode);
                (stickyMode) ?
                    notifyItemEnterringStickyMode((*it)->section, (*it)->item, (*it)->position, (*it)->origin) :
                    notifyItemLeavingStickyMode((*it)->section, (*it)->item, (*it)->position);
            }

            if (stickyMode)
            {
                std::vector<LayoutItem *>::iterator itVisibleItem = std::lower_bound(visibleItems.begin(), visibleItems.end(), *it, comp);
                if (itVisibleItem == visibleItems.end() || *(*itVisibleItem) != *(*it))
                {
                    // Create new LayoutItem and put it into visibleItems
                    LayoutItem *layoutItem = new LayoutItem(*it);
                    // itVisibleItem == visibleItems.end() ? visibleItems.insert(itVisibleItem, layoutItem) : visibleItems.insert(itVisibleItem + 1, layoutItem);
                    visibleItems.insert(itVisibleItem, layoutItem);
                }
                else
                {
                    // Update in place
                    (*itVisibleItem)->set(*(*it));
                }

                // layoutAttributes.zIndex = 1024 + it->first;  //
                totalStickyItemSize += stickyItemSize;
            }
        }
    }

    for (std::vector<LayoutItem *>::iterator it = visibleItems.begin(); it != visibleItems.end(); ++it)
    {
        m_layoutAdapter.addLayoutItem(javaList, *(*it));
        // jobject javaLayoutItem = env->NewObject(layoutItemClass, constuctMid, (*it)->section, (*it)->item, (*it)->position, jboolean((*it)->data == 1 ? 1 : 0),
        //                                        (*it)->frame.left(), (*it)->frame.top(), (*it)->frame.right(), (*it)->frame.bottom());

        // jboolean ret = env->CallBooleanMethod(visibleItems, addMid, javaLayoutItem);
        delete (*it);
    }

    // for (std::vector<FlexSection *>::iterator it = visibleItems.begin(); it != visibleItems.end(); delete *it, ++it);
    visibleItems.clear();


    // PagingOffset
    /*
    if (m_pagingSection != NSNotFound && !CGPointEqualToPoint(m_pagingOffset, CGPointZero))
    {
        for (UICollectionViewLayoutAttributes *layoutAttributes in layoutAttributesArray)
        {
            if (layoutAttributes.indexPath.section >= m_pagingSection)
            {
                layoutAttributes.frame = CGRectOffset(layoutAttributes.frame, m_pagingOffset.x, m_pagingOffset.y);
            }
        }
        for (UICollectionViewLayoutAttributes *layoutAttributes in newLayoutAttributesArray)
        {
            if (layoutAttributes.indexPath.section >= m_pagingSection)
            {
                layoutAttributes.frame = CGRectOffset(layoutAttributes.frame, m_pagingOffset.x, m_pagingOffset.y);
            }
        }
    }

     */

}

jobject FlexLayout::calcContentOffsetForScroll(int position, int itemOffsetX, int itemOffsetY) const
{
    return NULL;
}

void FlexLayout::notifyItemEnterringStickyMode(int section, int item, int position, Point pt) const
{
    m_layoutAdapter.onItemEnterStickyMode(section, item, position, pt);
}

void FlexLayout::notifyItemLeavingStickyMode(int section, int item, int position) const
{
    m_layoutAdapter.onItemExitStickyMode(section, item, position);
}
