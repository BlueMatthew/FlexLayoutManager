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

protected:
    int m_orientation;
    VerticalLayout *m_verticalLayout;
    HorizontalLayout *m_horizontalLayout;

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

    Size prepareLayout(const LayoutCallbackAdapter& layoutCallbackAdapter, const LayoutAndSectionsInfo &layoutAndSectionsInfo);
    void updateItems(int action, int itemStart, int itemCount);
    void getItemsInRect(std::vector<LayoutItem> &items, StickyItemList &changingStickyItems, const LayoutInfo &layoutInfo) const;
    int computerContentOffsetToMakePositionTopVisible(const LayoutInfo &layoutInfo, int position, int positionOffset) const;
    bool getItem(int position, LayoutItem &layoutItem) const;

};


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUTMANAGER_H
