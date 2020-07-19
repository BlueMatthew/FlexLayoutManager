//
// Created by Matthew Shi on 2020-07-19.
//

#include "FlexLayout.h"

#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUTMANAGER_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUTMANAGER_H


class FlexLayoutManager
{
public:
    static int VERTICAL/* = 1*/;
    inline static bool isVertical(int orientation)
    {
        return VERTICAL == orientation;
    }

protected:
    int m_orientation;
    FlexLayout<true> *m_verticalLayout;
    FlexLayout<false> *m_horizontalLayout;

    // As layout will be recreated once orientation is changed. But sticky items info won't.
    // We put sticky items info in the LayoutManager to avoid being destroyed.
    // Another better solution is to move sticky hendling out of Layout and put it into LayoutManager.
    // Can do it later
    mutable std::vector<StickyItem> m_stickyItems;  // The state should be updated
    bool m_stackedStickyItems;

public:
    FlexLayoutManager(JNIEnv* env, jobject layoutManager);
    ~FlexLayoutManager();

    static void initLayoutEnv(JNIEnv* env, jclass layoutManagerClass);

    inline void addStickyItem(int section, int item)
    {
        m_stickyItems.push_back(StickyItem(section, item));
        if (m_stickyItems.size() > 1)
        {
            std::sort(m_stickyItems.begin(), m_stickyItems.end());
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

    // LayoutItem::data == 1, indicates that the item is sticky
    void getItemsInRect(std::vector<LayoutItem> &items, std::vector<std::pair<StickyItem, Point>> &changingStickyItems, const LayoutInfo &layoutInfo) const;

    int computerContentOffsetToMakePositionTopVisible(const LayoutInfo &layoutInfo, int position, int positionOffset) const;

    bool getItem(int position, LayoutItem &layoutItem) const;

};


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUTMANAGER_H
