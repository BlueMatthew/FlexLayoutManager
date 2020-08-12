//
// Created by Matthew on 2020/8/10.
//

#include <limits>
#include <vector>
#include "common/FlexLayout.h"

#ifndef FLEXLAYOUTMANAGER_RECYCLERLAYOUT_H
#define FLEXLAYOUTMANAGER_RECYCLERLAYOUT_H



template<class TLayout, class TInt, class TCoordinate, bool VERTICAL>
class SectionT : public nsflex::FlexSectionT<TLayout, TInt, TCoordinate, VERTICAL>
{
public:
    using BaseSection = nsflex::FlexSectionT<TLayout, TInt, TCoordinate, VERTICAL>;
    using IntType = TInt;
    using CoordinateType = TCoordinate;
    using Rect = typename BaseSection::Rect;
    using FlexItem = typename BaseSection ::FlexItem;

    int m_position;

public:
    SectionT(TInt section, const Rect& frame) : BaseSection(section, frame), m_position(0)
    {
    }

    int getPositionBase() const { return m_position; }
    void setPositionBase(int position) { m_position = position; }

    inline const Rect getItemsFrameInViewAfterItem(TInt itemIndex) const
    {
        FlexItem *item = BaseSection::m_items[itemIndex];
        FlexItem *itemLast = BaseSection::m_items.back();

        Rect rect(item->getFrame().left() + BaseSection::m_itemsFrame.left(), item->getFrame().bottom() + BaseSection::m_itemsFrame.top(), (*itemLast).getFrame().width(), (*itemLast).getFrame().bottom() - item->getFrame().bottom());
        return BaseSection::getFrameInView(rect);
        return rect;
    }
};


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


template<class TInt, char type>
class RecyclerSectionItemT : public SectionItemT<TInt, type>
{
public:
    using TBase = SectionItemT<TInt, type>;

protected:
    TInt m_position;

public:
    RecyclerSectionItemT(TInt section, TInt item) : TBase(section, item), m_position(0)
    {
    }

public:
    TInt getPosition() const
    {
        return m_position;
    }

    void setPosition(TInt position)
    {
        m_position = position;
    }



};

template<class TInt, class TCoordinate>
class RecyclerLayoutItemT : public LayoutItemT<TInt, TCoordinate>
{
public:
    using TBase = LayoutItemT<TInt, TCoordinate>;
    using Rect = nsflex::RectT<TCoordinate>;
    using FlexItem = typename TBase::FlexItem;
    using TBase::getSection;
    using TBase::getType;
    using TBase::getItem;

protected:
    TInt m_position;

public:
    RecyclerLayoutItemT() : TBase(), m_position(0) {}
    RecyclerLayoutItemT(TInt s, TInt i) : TBase(s, i), m_position(0) {}
    RecyclerLayoutItemT(TInt s, TInt i,const Rect &f) : TBase(s, i, f), m_position(0) {}
    RecyclerLayoutItemT(TInt s, TInt i, TInt position, const Rect &f) : TBase(s, i, f), m_position(position) {}
    RecyclerLayoutItemT(TInt section, const FlexItem &src) : TBase(section, src), m_position(0) {}
    RecyclerLayoutItemT(const RecyclerLayoutItemT &src) : TBase(src), m_position(src.m_position) {}
    RecyclerLayoutItemT(const RecyclerLayoutItemT *src) : TBase(src), m_position(src->m_position) {}

    template <class TStickySectionItem>
    inline static RecyclerLayoutItemT makeLayoutItem(const TStickySectionItem &stickySectionItem, const Rect &rect)
    {
        return RecyclerLayoutItemT(stickySectionItem.getSection(), stickySectionItem.getItem(), stickySectionItem.getPosition(), rect);
    }

    RecyclerLayoutItemT& operator=(const RecyclerLayoutItemT &other)
    {
        (TBase &)(*this) = (TBase &)other;
        m_position = other.m_position;

        return *this;
    }
    TInt getPosition() const
    {
        return m_position;
    }

    void setPosition(TInt position)
    {
        m_position = position;
    }

    void reset(TInt section, TInt item, TInt position, const Rect &frame, bool inSticky, bool originChanged)
    {
        TBase::reset(section, item, frame, inSticky, originChanged);
        m_position = position;
    }

    template<class TSectionItem>
    bool operator<(const TSectionItem &other) const
    {
        // !!! Here MUST use FlexItem::getItem() to compare
        return (getSection() < other.getSection()) ||
               (getSection() == other.getSection() && getType() < other.getType()) ||
               (getSection() == other.getSection() && getType() == other.getType() && getItem() < other.getItem());
    }

    template<class TSectionItem>
    bool operator<(const TSectionItem *other) const
    {
        return *this < *other;
    }
};

template <class TLayoutItem>
inline TLayoutItem makeLayoutItem(const RecyclerSectionItemT<int, (char)(TLayoutItem::FlexItem::ITEM_TYPE_ITEM)> &stickySectionItem, const typename TLayoutItem::Rect &rect)
{
    return TLayoutItem(stickySectionItem.getSection(), stickySectionItem.getItem(), rect, stickySectionItem.getPosition());
}

template<class TLayoutCallbackAdapter, class TSectionBase, bool VERTICAL>
class RecyclerLayoutT : public FlexLayoutT<TLayoutCallbackAdapter, TSectionBase, VERTICAL>
{
public:
    using TBase = FlexLayoutT<TLayoutCallbackAdapter, TSectionBase, VERTICAL>;
    using TInt = typename TBase::TInt;
    using TCoordinate = typename TBase::TCoordinate;

    using Point = typename TBase::Point;
    using Size = typename TBase::Size;
    using Rect = typename TBase::Rect;
    using Insets = typename TBase::Insets;

    using FlexItem = typename TBase::FlexItem;
    using Section = typename TBase::Section;
    using SectionIterator = typename TBase::SectionIterator;
    using SectionConstIterator = typename TBase::SectionConstIterator;
    using SectionPositionCompare = FlexSectionPositionCompare<TSectionBase>;

    using StickySectionItem = RecyclerSectionItemT<int, (char)(FlexItem::ITEM_TYPE_ITEM)>;
    using StickyItemState = StickyItemStateT<TCoordinate>;

    template<class TStickyItem>
    using StickyItemList = std::vector<std::pair<TStickyItem, StickyItemState>>;

    using TBase::x;
    using TBase::y;
    using TBase::left;
    using TBase::top;
    using TBase::right;
    using TBase::bottom;

    using TBase::offset;
    using TBase::height;
    using TBase::makeRect;

public:
    static const TCoordinate INVALID_OFFSET = std::numeric_limits<TCoordinate>::min();

public:

    void prepareLayout(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding)
    {
        TBase::prepareLayout(layoutCallbackAdapter, boundSize, padding);

        TInt positionBase = 0;
        for (SectionIterator it = TBase::m_sections.begin(); it != TBase::m_sections.end(); ++it)
        {
            (*it)->setPositionBase(positionBase);
            positionBase += (*it)->getItemCount();
        }
    }

    virtual void adjustFrameForStickyItem(Rect &rect, Point &origin, TInt sectionIndex, TInt itemIndex, bool stackedStickyItems, const Point &contentOffset, const Insets &padding, TCoordinate totalStickyItemSize) const
    {
        Section *section = TBase::m_sections[sectionIndex];
        // offset(rect, left(section->getFrame()) - x(contentOffset), top(section->getFrame()) - y(contentOffset));
        offset(rect, -x(contentOffset), -y(contentOffset));
        origin = rect.origin;

        if (stackedStickyItems)
        {
            top(rect, std::max(totalStickyItemSize, y(origin)));
        }
        else
        {
            Rect lastItemInSection = section->getItem(section->getItemCount() - 1)->getFrame();
            Rect frameItems = makeRect(x(origin), y(origin), right(lastItemInSection), bottom(lastItemInSection));
            frameItems.offset(section->getFrame().left(), section->getFrame().top());
            frameItems = section->getItemsFrameInViewAfterItem(itemIndex);
            top(rect, std::min(std::max(0, (top(frameItems) - y(contentOffset) - height(rect))),
                               (bottom(frameItems) - y(contentOffset) - height(rect))));

            /*
            Rect lastItemInSection = section->getItem(section->getItemCount() - 1)->getFrame();
            // Rect frameItems = makeRect(x(origin), y(origin), right(lastItemInSection), bottom(lastItemInSection));
            Rect frameItems = makeRect(left(rect), bottom(rect), right(lastItemInSection), bottom(lastItemInSection));
            frameItems.offset(section->getFrame().left(), section->getFrame().top());
            top(rect, std::min(std::max(0, (top(frameItems) - height(rect))),
                               (bottom(frameItems) - height(rect))));
                               */
        }
    }

    // LayoutItem::data == 1, indicates that the item is sticky
    template <class TLayoutItem, class TStickyItem>
    void getItemsInRect(std::vector<TLayoutItem> &items, StickyItemList<TStickyItem> &changingStickyItems, StickyItemList<TStickyItem> &stickyItems, bool stackedStickyItems, const Rect &rect, const Size &size,  const Size &contentSize, const Insets &padding, const Point &contentOffset) const
    {
        TBase::getItemsInRect(items, changingStickyItems, stickyItems, stackedStickyItems, rect, size, contentSize, padding, contentOffset);

        for (typename std::vector<TLayoutItem>::iterator it = items.begin(); it != items.end(); ++it)
        {
            Section *section = TBase::m_sections[it->getSection()];
            offset(it->getFrame(), left(padding), top(padding));
            it->setPosition(section->getPositionBase() + it->getItem());
        }
    }

    template <class TStickyItem>
    void updateStickyItemPosition(StickyItemList<TStickyItem> &stickyItems)
    {
        for (typename StickyItemList<TStickyItem>::iterator it = stickyItems.begin(); it != stickyItems.end(); ++it)
        {
            Section *section = TBase::m_sections[it->first.getSection()];
            it->first.setPosition(section->getPositionBase() + it->first.getItem());
        }
    }

    template <class TStickyItem>
    TCoordinate computerContentOffsetToMakePositionTopVisible(StickyItemList<TStickyItem> &stickyItems, bool stackedStickyItems, const Size &size,  const Size &contentSize, const Insets &padding, const Point &contentOffset, TInt position, TCoordinate positionOffset) const
    {
        SectionConstIterator itTargetSection = std::lower_bound(TBase::m_sections.begin(), TBase::m_sections.end(), position, SectionPositionCompare());
        if (itTargetSection == TBase::m_sections.end())
        {
            return INVALID_OFFSET;
        }

        FlexItem *targetItem = (*itTargetSection)->getItem(position - (*itTargetSection)->getPositionBase());
        if (NULL == targetItem)
        {
            return INVALID_OFFSET;
        }

        TCoordinate newContentOffset = top(targetItem->getFrame()) + positionOffset;

        TCoordinate totalStickyItemSize = 0; // When m_stackedStickyItems == true
        Rect rect;
        Point origin;   // old origin

        // for (std::vector<StickyItem>::const_iterator it = stickyItems.begin(); it != stickyItems.end(); ++it)
        for (typename StickyItemList<TStickyItem>::iterator it = stickyItems.begin(); it != stickyItems.end(); ++it)
        {
            if ((!stackedStickyItems) && (it->first.getSection() < (*itTargetSection)->getSection()))
            {
                continue;
            }
            if (it->first.getSection() > (*itTargetSection)->getSection() || (it->first.getSection() == (*itTargetSection)->getSection() && it->first.getItem() > targetItem->getItem()))
            {
                // If the sticky item is greater than target item
                break;
            }

            // int sectionIndex = it->section;
            const TSectionBase *section = TBase::m_sections[it->first.getSection()];

            rect = section->getItemFrameInView(it->first.getItem());
            TCoordinate stickyItemSize = height(rect);

            adjustFrameForStickyItem(rect, origin, it->first.getSection(),  it->first.getItem(), stackedStickyItems, contentOffset, padding, totalStickyItemSize);

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            bool stickyMode = top(rect) >= y(origin);
            if (stickyMode)
            {
                totalStickyItemSize = stackedStickyItems ? (totalStickyItemSize + stickyItemSize) : stickyItemSize;
            }
        }

        TCoordinate val = 0;
        rect = targetItem->getFrame();
        offset(rect, left((*itTargetSection)->getFrame()), top((*itTargetSection)->getFrame()));
        val = top(rect) - totalStickyItemSize + positionOffset;

        return val;
    }

    template <class TLayoutItem>
    bool getItem(int position, TLayoutItem &layoutItem) const
    {
        // SectionConstIterator it = std::upper_bound(TBase::m_sections.begin(), TBase::m_sections.end(), position, SectionPositionCompare());
        SectionConstIterator it = std::lower_bound(TBase::m_sections.begin(), TBase::m_sections.end(), position, SectionPositionCompare());
        if (it == TBase::m_sections.end())
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
        layoutItem.reset((*it)->getSection(), itemIndex, position, rect, false, false);
        // layoutItem.setPosition(position);

        return true;
    }

protected:


};



#endif //FLEXLAYOUTMANAGER_RECYCLERLAYOUT_H
