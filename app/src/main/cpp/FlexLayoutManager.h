//
// Created by Matthew Shi on 2020-07-19.
//

#include "RecyclerLayout.h"

#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUTMANAGER_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUTMANAGER_H

class FlexLayoutManager
{
public:
    static int VERTICAL;
    static int INVALID_OFFSET;
    inline static bool isVertical(int orientation)
    {
        return VERTICAL == orientation;
    }

    using FlexItem = nsflex::FlexItemT<int, int>;
    using StickySectionItem = RecyclerSectionItemT<int, (char)(FlexItem::ITEM_TYPE_ITEM)>;
    using StickyItemState = StickyItemStateT<int>;

    using StickyItem = StickyItemT<StickySectionItem, StickyItemState>;
    using StickyItemList = std::vector<StickyItem>;
    using StickyItemAndSectionItemCompare = StickyItemAndSectionItemCompareT<StickySectionItem, StickyItemState>;

    template <bool VERTICAL>
    using Section = SectionT<LayoutCallbackAdapter, int, int, VERTICAL>;
    using VerticalLayout = RecyclerLayoutT<LayoutCallbackAdapter, Section<true>, true>;
    using HorizontalLayout = RecyclerLayoutT<LayoutCallbackAdapter, Section<false>, false>;

    using VerticalLayoutIterator = typename std::vector<VerticalLayout *>::iterator;
    using HorizontalLayoutIterator = typename std::vector<HorizontalLayout *>::iterator;
    using VerticalLayoutConstIterator = typename std::vector<VerticalLayout *>::const_iterator;
    using HorizontalLayoutConstIterator = typename std::vector<HorizontalLayout *>::const_iterator;

protected:
    int m_orientation;
    std::vector<VerticalLayout *> m_verticalLayout;
    std::vector<HorizontalLayout *> m_horizontalLayout;

    // As layout will be recreated once orientation is changed. But sticky items info won't.
    // We put sticky items info in the LayoutManager to avoid being destroyed.
    // Another better solution is to move sticky hendling out of Layout and put it into LayoutManager.
    // Can do it later
    mutable StickyItemList m_stickyItems; // Section Index -> Sticy Status(YES/NO)  // The state should be updated
    bool m_stackedStickyItems;

public:
    FlexLayoutManager(JNIEnv* env, jobject layoutManager);
    ~FlexLayoutManager();

    static void initLayoutEnv(JNIEnv* env, jclass layoutManagerClass);

    inline void addStickyItem(int section, int item)
    {
        StickySectionItem sectionItem(section, item);
        StickyItemList::iterator it = std::lower_bound(m_stickyItems.begin(), m_stickyItems.end(), sectionItem, StickyItemAndSectionItemCompare());
        if (it == m_stickyItems.end() || it->first != sectionItem)
        {
            m_stickyItems.insert(it, std::make_pair(sectionItem, StickyItemState()));
        }
    }

    inline void clearStickyItems()
    {
        m_stickyItems.clear();
    }

    inline void setStackedStickyItems(bool stackedStickyItems)
    {
        m_stackedStickyItems = stackedStickyItems;
    }

    inline bool isStackedStickyItems() const
    {
        return m_stackedStickyItems;
    }

    inline bool isVertical() const
    {
        return VERTICAL == m_orientation;
    }

    Size prepareLayout(const LayoutCallbackAdapter& layoutCallbackAdapter, int pageStart, int pageCount, const LayoutAndSectionsInfo &layoutAndSectionsInfo);
    void updateItems(int action, int itemStart, int itemCount);
    void getItemsInRect(std::vector<LayoutItem> &items, StickyItemList &changingStickyItems, const DisplayInfo &displayInfo) const;
    int computerContentOffsetToMakePositionTopVisible(const LayoutInfo &layoutInfo, int position, int positionOffset) const;
    bool getItem(int page, int position, LayoutItem &layoutItem) const;


protected:
    inline void cleanVerticalLayouts()
    {
        if (!m_verticalLayout.empty())
        {
            for (VerticalLayoutIterator it = m_verticalLayout.begin(); it != m_verticalLayout.end(); delete *it, ++it);
            m_verticalLayout.clear();
        }
    }

    inline void cleanHorizontalLayouts()
    {
        if (!m_horizontalLayout.empty())
        {
            for (HorizontalLayoutIterator it = m_horizontalLayout.begin(); it != m_horizontalLayout.end(); delete *it, ++it);
            m_horizontalLayout.clear();
        }
    }

    inline void buildVerticalLayouts(int numberOfPages)
    {
        if (numberOfPages > m_verticalLayout.size())
        {
            for (int page = m_verticalLayout.size(); page < numberOfPages; ++page)
            {
                m_verticalLayout.push_back(new VerticalLayout(page));
            }
        }
        else if (numberOfPages < m_horizontalLayout.size())
        {
            VerticalLayoutIterator itStart = m_verticalLayout.begin() + numberOfPages;
            for (VerticalLayoutIterator it = itStart; it != m_verticalLayout.end(); delete *it, ++it);
            m_verticalLayout.erase(itStart, m_verticalLayout.end());
        }
    }

    inline void buildHorizontalLayouts(int numberOfPages)
    {
        if (numberOfPages > m_horizontalLayout.size())
        {
            for (int page = m_horizontalLayout.size(); page < numberOfPages; ++page)
            {
                m_horizontalLayout.push_back(new HorizontalLayout(page));
            }
        }
        else if (numberOfPages < m_horizontalLayout.size())
        {
            HorizontalLayoutIterator itStart = m_horizontalLayout.begin() + numberOfPages;
            for (HorizontalLayoutIterator it = itStart; it != m_horizontalLayout.end(); delete *it, ++it);
            m_horizontalLayout.erase(itStart, m_horizontalLayout.end());
        }
    }
};


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUTMANAGER_H
