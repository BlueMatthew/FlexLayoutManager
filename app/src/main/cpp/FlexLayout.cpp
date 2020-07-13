//
// Created by Matthew on 2020-07-03.
//

#include <jni.h>
#include "LayoutCallbackAdapter.h"
#include "FlexLayout.h"

#ifndef NDK_DEBUG
#include <android/log.h>

#define TAG "NDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif // NDK_DEBUG

FlexLayout::FlexLayout(JNIEnv* env, jobject obj, jobject callback) : m_layoutCallback(env, obj, callback)
{
}

FlexLayout::~FlexLayout()
{
    clearSections();
}

void FlexLayout::prepareLayout(const Size &size, const Insets &padding)
{
    clearSections();



    int sectionCount = m_layoutCallback.getNumberOfSections();
    if (sectionCount <= 0)
    {
        m_layoutCallback.updateContentHeight(padding.top + padding.bottom);
        return;
    }

    m_layoutCallback.beginPreparingLayout(128);

    Rect bounds(padding.left, padding.top, size.width - padding.right - padding.right, size.height - padding.top - padding.bottom);
    int positionBase = 0;

    Rect rectOfSection(padding.left, padding.top, bounds.width(), 0);
    for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
        int layoutMode = m_layoutCallback.getLayoutModeForSection(sectionIndex);
        FlexSection *section = layoutMode == 1 ?
                static_cast<FlexSection *>(new FlexWaterfallSection(&m_layoutCallback, sectionIndex, rectOfSection)) :
                static_cast<FlexSection *>(new FlexFlowSection(&m_layoutCallback, sectionIndex, rectOfSection));

        section->setPositionBase(positionBase);
        section->prepareLayout(bounds);

        m_sections.push_back(section);

        positionBase += section->getItemCount();

        rectOfSection.origin.y += section->getFrame().height();
        // rectOfSection.size.height = 0;
    }

    int contentHeight = (int)rectOfSection.bottom() + padding.bottom;
    m_layoutCallback.updateContentHeight(contentHeight);

    m_layoutCallback.endPreparingLayout();

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

void FlexLayout::getItemsInRect(std::vector<LayoutItem *> &items, const Size &size, const Insets &insets, const Size &contentSize, const Point &contentOffset) const
{
    bool vertical = m_layoutCallback.isVertical();

    Rect rect(contentOffset.x, contentOffset.y, size.width, size.height);   // visibleRect

    std::pair<std::vector<FlexSection *>::const_iterator, std::vector<FlexSection *>::const_iterator> range = vertical ?
            std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.top(), rect.bottom()), FlexSectionVerticalCompare()) :
            std::equal_range(m_sections.begin(), m_sections.end(), std::pair<int, int>(rect.left(), rect.width()), FlexSectionHorizontalCompare());
    if (range.first == range.second)
    {
        // No Sections
        return;
    }

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


            if (NULL == *itItem)
            {
                LOGD("[%d, %d] is null", (*it)->getSection(), (*itItem)->getItem());
            }
            else
            {
                const Rect rect1 = (*itItem)->getFrame();

                LOGD("[%d, %d]: section point=%d-%d point=%d-%d, size=%d-%d", (*it)->getSection(), (*itItem)->getItem(), (*it)->getFrame().left(), (*it)->getFrame().top(), rect1.left(), rect1.top(), rect1.width(), rect1.height());
            }

            Rect frame = (*itItem)->getFrame();
            frame.offset((*it)->getFrame().left(), (*it)->getFrame().top());

            LayoutItem *item = new LayoutItem((*it)->getSection(), (*itItem)->getItem(), (*it)->getPositionBase() + (*itItem)->getItem(), frame);
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

        Point origin;
        Point oldOrigin;

        LayoutItemCompare comp;
        for (std::vector<LayoutItem *>::const_iterator it = m_stickyItems.begin(); it != m_stickyItems.end(); ++it)
        {
            if ((*it)->section > maxSection || (!m_stackedStickyItems && ((*it)->section < minSection)))
            {
                if ((*it)->data == 1)
                {
                    (*it)->data = 0;
                    notifyItemLeavingStickyMode((*it)->section, (*it)->item);
                    // [self exitStickyModeAt:it->first];
                }
                continue;
            }

            LayoutItem *item = NULL;
            std::vector<LayoutItem *>::iterator itVisibleItem = std::lower_bound(items.begin(), items.end(), *it, comp);

            // std::map<int, FlexItem *>::const_iterator itHeaderLayoutAttributes = headerLayoutAttributesMap.find(it->first);
            if (itVisibleItem == items.end() || (*itVisibleItem) != (*it))
            {
                item = makeLayoutItem((*it)->section, (*it)->item);
                itVisibleItem == items.end() ? items.insert(itVisibleItem, item) : items.insert(itVisibleItem + 1, item);
            }
            else
            {
                item = *itVisibleItem;
            }
            item->data = 1;

            origin = item->frame.origin;
            oldOrigin = origin;

            // LayoutItem *item = makeLayoutItem((*it)->section, (*it)->item);
            item->frame.offset(-contentOffset.x, -contentOffset.y);

            int stickyItemSize = item->frame.height();
            if (m_stackedStickyItems)
            {
                origin.y = std::max(contentOffset.y + totalStickyItemSize + insets.top, origin.y);
            }
            else
            {
                FlexSection *section = m_sections[item->section];
                Rect lastItemInSection = section->getItem(section->getItemCount() - 1)->getFrame();
                Rect frameItems(origin.x, origin.y, lastItemInSection.right(), lastItemInSection.bottom());
                frameItems.offset(section->getFrame().left(), section->getFrame().top());

                origin.y = std::min(
                        std::max(contentOffset.y + insets.top, (frameItems.origin.y - stickyItemSize)),
                        (frameItems.bottom() - stickyItemSize)
                );
            }
            item->frame.origin = origin;

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            int stickyMode = (*it)->data == 1 ? ((contentOffset.y + insets.top < oldOrigin.y) ? 0 : 1) : ((item->frame.origin.y > oldOrigin.y) ? 1 : 0);

            if (stickyMode != (*it)->data)
            {
                // Notify caller if changed
                (*it)->data = stickyMode;
                (stickyMode == 1) ? notifyItemEnterringStickyMode((*it)->section, (*it)->item, oldOrigin) : notifyItemEnterringStickyMode((*it)->section, (*it)->item, oldOrigin);
            }

            if (stickyMode)
            {
                // layoutAttributes.zIndex = 1024 + it->first;  //
                totalStickyItemSize += stickyItemSize;
            }

        }
    }

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

void FlexLayout::notifyItemEnterringStickyMode(int section, int item, Point pt) const
{

}

void FlexLayout::notifyItemLeavingStickyMode(int section, int item) const
{

}
