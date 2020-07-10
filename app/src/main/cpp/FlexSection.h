//
// Created by Matthew on 2020-07-04.
//

#ifndef FLEXLAYOUTMANAGER_FLEXSECTION_H
#define FLEXLAYOUTMANAGER_FLEXSECTION_H

#include "Graphics.h"
#include "FlexNodes.h"

template<class TLayout>
class SectionT
{
public:
    int m_section;
    int m_position;
    TLayout *m_layout;

    Rect m_frame;


protected:
    struct {
        unsigned int sectionInvalidated : 1;    // The whole section is invalidated
        unsigned int itemsInvalidated : 1; // Some of items are invalidated
        unsigned int reserved : 4;
        unsigned int minimalInvalidatedItem : 26;   // If minimal invalidated item is greater than 2^26, just set sectionInvalidated to 1
    } m_invalidationContext;

public:
    std::vector<FlexItem *> m_items;

    SectionT(TLayout *layout, int section, const Point &origin) : m_section(section), m_layout(layout), m_frame(origin, Size())
    {
    }

    virtual ~SectionT()
    {
        m_layout = NULL;
        for(std::vector<FlexItem *>::iterator it = m_items.begin(); it != m_items.end(); delete *it, ++it);
        m_items.clear();
    }

    inline bool isVertical() const { m_layout->getOrientation() == 1; }
    inline Size getBounds() const { return m_layout->getBounds(); }
    inline int getNumberOfItems() const { return m_layout->numberOfItemsInSection(m_section); }
    inline int getNumberOfColumns() const { return m_layout->getNumberOfColumnsForSection(m_section); }
    inline Insets getInsets() const { return m_layout->getInsetForSection(m_section); }
    inline int getMinimumLineSpacing() const { return m_layout->getMinimumLineSpacingForSection(m_section); }
    inline int getMinimumInteritemSpacing() const { return m_layout->getMinimumInteritemSpacingForSection(m_section); }
    inline bool hasFixedItemSize(Size &size) const { return m_layout->hasFixedItemSize(m_section, size); }
    inline void getItemSize(int item, Size &size) const { m_layout->getItemSize(m_section, item, size); }

    inline int getPosition() const { return m_position; }
    inline const Rect getFrame() const { return m_frame; }
    inline Rect &getFrame() { return m_frame; }

    /*
    inline const Rect getItemsFrame() const
    {
        return isVertical() ? Rect(m_frame.origin.x, m_frame.origin.y, m_frame.size.width, m_frame.size.height) : Rect(m_frame.origin.x, m_frame.origin.y, m_frame.size.width, m_frame.size.height);
    }
     */

    virtual void prepareLayout() = 0;


    virtual bool filterItems(std::vector<FlexItem *> &items, const Rect &rectInSection) = 0;

    template<typename TCompare, typename TRCompare>
    bool getItemsInRect(std::vector<FlexItem *> &items, const Rect &rect)
    {
        bool merged = false;

        if (!m_frame.intersects(rect))
        {
            return merged;
        }

        Rect rectInSection(rect);

        rectInSection.offset(-m_frame.origin.x, -m_frame.origin.y);

        // Items
        // if (filterItems(items, rectInSection))
        {
            merged = true;
        }

        return merged;
    }

    void resetInvalidationContext()
    {
        unsigned int value = ~0;
        *((unsigned int *)&m_invalidationContext) = (value >> 6);
    }

    bool isSectionInvalidated() const { return m_invalidationContext.sectionInvalidated == 1; }
    bool isHeaderInvalidated() const { return m_invalidationContext.sectionInvalidated == 1; }
    bool hasInvalidatedItem() const { return m_invalidationContext.itemsInvalidated == 1; }
    bool isFooterInvalidated() const { return m_invalidationContext.sectionInvalidated == 1; }
    unsigned int getMinimalInvalidatedItem() const { return m_invalidationContext.minimalInvalidatedItem; }
    inline void invalidateSection()
    {
        m_invalidationContext.sectionInvalidated = 1;
    }

    void invalidateItem(int item)
    {
        m_invalidationContext.sectionInvalidated = 1;
        unsigned int maximum = ~0;
        maximum = (maximum >> 8);
        if (item > (int)maximum)
        {
            m_invalidationContext.sectionInvalidated = 1;
            m_invalidationContext.minimalInvalidatedItem = maximum;
        }
        else
        {
            m_invalidationContext.minimalInvalidatedItem = (unsigned int)item;
        }
    }

    void clearLayoutAttributes(int item = 0)
    {
        if (item >= m_items.size())
        {
            return;
        }

        std::vector<FlexItem *>::iterator it = m_items.begin() + item;
        for (; it != m_items.end(); ++it)
        {
            // (*it)->clearLayoutAttributes();
        }
    }
};

template<class TLayout>
struct SectionVerticalCompareT
{
    bool operator() (const SectionT<TLayout>* section, const Rect& rect) const
    {
        return section->getFrame().bottom < rect.top();
    }
    bool operator() (const Rect& rect, const SectionT<TLayout>* section) const
    {
        return rect.bottom() < section->getFrame().top();
    }
};

template<class TLayout>
struct SectionHorizontalCompareT
{
    bool operator() (const SectionT<TLayout>* section, const Rect& rect) const
    {
        return section->getFrame().right() < rect.left();
    }
    bool operator() (const Rect& rect, const SectionT<TLayout>* section) const
    {
        return rect.right() < section->getFrame().left();
    }
};



#endif //FLEXLAYOUTMANAGER_FLEXSECTION_H
