//
// Created by Matthew on 2020-07-03.
//

#ifndef FLEXLAYOUTMANAGER_FLEXPAGELAYOUT_H
#define FLEXLAYOUTMANAGER_FLEXPAGELAYOUT_H

#include "FlexLayout.h"

template<class TLayoutCallbackAdapter, class TSectionBase, bool VERTICAL>
class FlexPageLayoutT : public FlexLayoutT<TLayoutCallbackAdapter, TSectionBase, VERTICAL>
{
public:
    using TInt = typename TSectionBase::IntType;
    using TCoordinate = typename TSectionBase::CoordinateType;
    using TBase = FlexLayoutT<TLayoutCallbackAdapter, TSectionBase, VERTICAL>;

    // using StickyItem = StickyItemT<TInt, TCoordinate>;
    using LayoutItem = LayoutItemT<TInt, TCoordinate>;

    using Point = typename TBase ::Point;
    using Size = typename TBase::Size;
    using Rect = typename TBase::Rect;
    using Insets = typename TBase::Insets;
    
    using FlexItem = nsflex::FlexItemT<TInt, TCoordinate>;
    using Section = TSectionBase;
    using FlowSection = typename nsflex::FlexFlowSectionT<Section, VERTICAL>;
    using WaterfallSection = typename nsflex::FlexWaterfallSectionT<Section, VERTICAL>;
    using SectionCompare = nsflex::FlexCompareT<Section, VERTICAL>;
    using SectionIterator = typename std::vector<Section *>::iterator;
    using SectionConstIterator = typename std::vector<Section *>::const_iterator;
    using SectionConstIteratorPair = std::pair<SectionConstIterator, SectionConstIterator>;
    // using SectionPositionCompare = FlexSectionPositionCompare<Section>;
    using ItemConstIterator = typename std::vector<const FlexItem *>::const_iterator;

    using StickyItemState = StickyItemStateT<TCoordinate>;
    template<class TStickySectionItem>
    using StickyItemList = std::vector<std::pair<TStickySectionItem, StickyItemState>>;

    using TBase::y;
    using TBase::top;
    using TBase::bottom;

    using TBase::offsetX;
    using TBase::offsetY;

    using TBase::height;
    using TBase::width;

    using TBase::makeSize;
    using TBase::makeRect;

protected:

    TInt m_page;
    std::vector<Section *> m_sections;
    Size m_contentSize;

public:

    FlexPageLayoutT() : TBase(), m_page(0)
    {
    }

    FlexPageLayoutT(TInt page) : TBase(), m_page(page)
    {
    }

    ~FlexPageLayoutT()
    {
    }

    void insertItem(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex, TInt itemIndex)
    {
        TBase::insertItem(layoutCallbackAdapter, boundSize, padding, sectionIndex, itemIndex);
    }
    
    void insertSection(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex)
    {
        TBase::insertSection(layoutCallbackAdapter, boundSize, padding, sectionIndex);
    }
    
    void deleteItem(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex, TInt itemIndex)
    {
        TBase::deleteItem(layoutCallbackAdapter, boundSize, padding, sectionIndex, itemIndex);
    }
    
    void deleteSection(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex)
    {
        TBase::deleteSection(layoutCallbackAdapter, boundSize, padding, sectionIndex);
    }
    
    void reloadItem(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex, TInt itemIndex)
    {
        TBase::reloadItem(layoutCallbackAdapter, boundSize, padding, sectionIndex, itemIndex);
    }
    
    void reloadSection(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex)
    {
        TBase::reloadSection(layoutCallbackAdapter, boundSize, padding, sectionIndex);
    }

    void prepareLayout(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding)
    {

    }
    
    void prepareLayoutIncrementally(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt minInvalidatedSection)
    {

    }

    inline Size getContentSize() const
    {
        return m_contentSize;
    }

    inline Size getContentSize(TInt page) const
    {
        // return m_contentSize;
    }

    void updateItems(TInt action, TInt itemStart, TInt itemCount)
    {
    }


    // LayoutItem::data == 1, indicates that the item is sticky
    template <class TLayoutItem, class TStickyItem>
    void getItemsInRect(std::vector<TLayoutItem> &items, StickyItemList<TStickyItem> &changingStickyItems, StickyItemList<TStickyItem> &stickyItems, bool stackedStickyItems, const Rect &rect, const Size &size,  const Size &contentSize, const Insets &padding, const Point &contentOffset, const Point & pagingOffset, TInt fixedSection, TInt currentPage) const
    {
    }


    
protected:


};



#endif //FLEXLAYOUTMANAGER_FLEXPAGELAYOUT_H
