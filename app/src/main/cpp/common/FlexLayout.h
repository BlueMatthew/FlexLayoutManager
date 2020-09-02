//
// Created by Matthew on 2020-07-03.
//

#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUT_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUT_H

#include <algorithm>
#include <cmath>

#include "FlexSection.h"
#include "FlexFlowSection.h"
#include "FlexWaterfallSection.h"

enum FlexLayoutMode
{
    FlexLayoutModeFlow = 0,
    FlexLayoutModeWaterfall = 1,
};

template<class TInt, char type>
class SectionItemT
{
protected:
    TInt m_section;
    TInt m_item;
    
public:
    SectionItemT(TInt section, TInt item) : m_section(section), m_item(item)
    {
    }
    
    TInt getSection() const { return m_section; }
    TInt getItem() const { return m_item; }
    char getType() const { return type; }
    
    bool operator==(const SectionItemT &other) const
    {
        return (m_section == other.getSection() && m_item == other.getItem());
    }
    bool operator!=(const SectionItemT &other) const
    {
        return !(*this == other);
    }
    
    bool operator<(const SectionItemT &other) const
    {
        return m_section < other.getSection() || (m_section == other.getSection() && m_item < other.getItem());
    }
    
    bool operator>(const SectionItemT &other) const
    {
        return m_section > other.getSection() || (m_section == other.getSection() && m_item > other.getItem());
    }
    
};

template<class TCoordinate>
class StickyItemStateT
{
public:
    using Rect = nsflex::RectT<TCoordinate>;

protected:
    bool m_inSticky;
    bool m_originChanged;
    Rect m_itemsFrame;
    Rect m_frame;
    
public:
    StickyItemStateT() : m_inSticky(false), m_originChanged(false)
    {
    }
    
    StickyItemStateT(bool inSticky) : m_inSticky(inSticky), m_originChanged(false)
    {
    }
    
    inline bool isInSticky() const
    {
        return m_inSticky;
    }
    
    inline void setInSticky(bool inSticky = true)
    {
        m_inSticky = inSticky;
    }
    
    inline bool isOriginChanged() const
    {
        return m_originChanged;
    }
    
    inline void setOriginChanged(bool originChanged = true)
    {
        m_originChanged = originChanged;
    }
    
    inline Rect getFrame() const
    {
        return m_frame;
    }
    
    inline void setFrame(const Rect &frame)
    {
        m_frame = frame;
    }
    
    inline Rect getItemsFrame() const
    {
        return m_itemsFrame;
    }
    
    inline void setItemsFrame(const Rect &frame)
    {
        m_itemsFrame = frame;
    }
    
};

template<class TSectionItem, class TStickyItemState>
using StickyItemT = std::pair<TSectionItem, TStickyItemState>;

template<class TSectionItem, class TStickyItemState>
inline bool operator<(const StickyItemT<TSectionItem, TStickyItemState> &lhs, const StickyItemT<TSectionItem, TStickyItemState> &rhs)
{
    return lhs.first < rhs.first;
}

template<class TSectionItem, class TStickyItemState>
struct StickyItemAndSectionItemCompareT
{
    inline bool operator() (const StickyItemT<TSectionItem, TStickyItemState> &lhs, const TSectionItem &rhs) const
    {
        return lhs.first < rhs;
        
    }
    inline bool operator() (const TSectionItem &lhs, const StickyItemT<TSectionItem, TStickyItemState> &rhs) const
    {
        return lhs < rhs.first;
    }

    inline bool operator() (const StickyItemT<TSectionItem, TStickyItemState> &lhs, const StickyItemT<TSectionItem, TStickyItemState> &rhs) const
    {
        return lhs.first < rhs.first;
    }
};

template<class TInt, class TCoordinate>
class LayoutItemT : public nsflex::FlexItemT<TInt, TCoordinate>
{
public:
    using FlexItem = nsflex::FlexItemT<TInt, TCoordinate>;
    // using HeaderSectionItem = HeaderSectionItemT<TInt, TCoordinate>;
    using Rect = nsflex::RectT<TCoordinate>;
    using FlexItem::getItem;
    using FlexItem::getType;

protected:
    TInt m_page;
    TInt m_section;
    bool m_inSticky;
    bool m_originChanged;

public:
    LayoutItemT() : FlexItem(), m_page(0), m_section(0), m_inSticky(false), m_originChanged(false) {}
    // LayoutItemT(TInt s, TInt i) : FlexItem(i), m_section(s), m_inSticky(false), m_originChanged(false) {}
    LayoutItemT(TInt page, TInt s, TInt i, const Rect &f) : FlexItem(i, f), m_page(page), m_section(s), m_inSticky(false), m_originChanged(false) {}
    LayoutItemT(TInt page, TInt section, const FlexItem &src) : FlexItem(src), m_page(page), m_section(section), m_inSticky(false), m_originChanged(false) {}
    LayoutItemT(const LayoutItemT &src) : FlexItem((const FlexItem &)src), m_page(src.m_page), m_section(src.m_section), m_inSticky(src.m_inSticky), m_originChanged(src.m_originChanged) {}
    LayoutItemT(const LayoutItemT *src) : LayoutItemT(*src) {}

    template<class TSection>
    LayoutItemT(TInt page, const TSection &section, const FlexItem &src) : FlexItem(src), m_page(page), m_section(section.getSection()), m_inSticky(false), m_originChanged(false) {}


    template <class TStickySectionItem>
    inline static LayoutItemT makeLayoutItem(const TStickySectionItem &stickySectionItem, const Rect &rect)
    {
        LayoutItemT layoutItem(0, stickySectionItem.getSection(), stickySectionItem.getItem(), rect);
#ifdef HAVING_HEADER_AND_FOOTER
        layoutItem.setHeader(true);
#endif // #ifdef HAVING_HEADER_AND_FOOTER
        return layoutItem;
    }

    LayoutItemT& operator=(const LayoutItemT &other)
    {
        if (this == &other) return *this;

        m_page = other.m_page;
        m_section = other.m_section;
        FlexItem::m_item = other.m_item;
        // this->position = position;
        FlexItem::m_frame = other.m_frame;
        FlexItem ::m_flags = other.m_flags;
        m_inSticky = other.m_inSticky;
        m_originChanged = other.m_originChanged;

        return *this;
    }

    bool operator==(const LayoutItemT &other) const
    {
        if (this == &other) return true;
        return m_section == other.getSection() && getItem() == other.getItem();
    }

    bool operator==(const LayoutItemT *other) const
    {
        return *this == *other;
    }

    bool operator!=(const LayoutItemT &other) const
    {
        return !(*this == other);
    }

    bool operator!=(const LayoutItemT *other) const
    {
        return !(*this == *other);
    }

    bool operator<(const LayoutItemT &other) const
    {
        return (m_section < other.getSection()) || ((m_section == other.getSection()) && (getItem() < other.getItem()));
    }

    template<class TSectionItem>
    bool operator<(const TSectionItem &other) const
    {
        return (m_section < other.getSection()) ||
               (m_section == other.getSection() && getType() < other.getType()) ||
               (m_section == other.getSection() && getType() == other.getType() && getItem() < other.getItem());
    }

    template<class TSectionItem>
    bool equalsSectionItem(const TSectionItem &other) const
    {
        return (m_section == other.getSection()) &&
               (getType() == other.getType()) &&
               (getItem() == other.getItem());
    }

    template<class TSectionItem>
    bool equalsSectionItem(const TSectionItem *other) const
    {
        return equalsSectionItem(*other);
    }

    inline TInt getPage() const
    {
        return m_page;
    }

    inline TInt getSection() const
    {
        return m_section;
    }

    inline void setInSticky(bool inSticky)
    {
        m_inSticky = inSticky;
    }

    inline bool isInSticky() const
    {
        return m_inSticky;
    }

    inline void setOriginChanged(bool originChanged)
    {
        m_originChanged = originChanged;
    }

    inline bool isOriginChanged() const
    {
        return m_originChanged;
    }

    void reset(TInt page, TInt section, TInt item, const Rect &frame, bool inSticky, bool originChanged)
    {
        m_page = page;
        m_section = section;
        FlexItem::m_item = item;
        FlexItem::m_frame = frame;
        m_inSticky = inSticky;
        m_originChanged = originChanged;
    }

};

template <class TLayoutItem, class TStickySectionItem>
inline TLayoutItem makeLayoutItem(typename TLayoutItem::TInt page, const TStickySectionItem &stickySectionItem, const typename TLayoutItem::Rect &rect)
{
    TLayoutItem layoutItem(page, stickySectionItem.getSection(), stickySectionItem.getItem(), rect);
#ifdef HAVING_HEADER_AND_FOOTER
    layoutItem.setHeader(true);
#endif // #ifdef HAVING_HEADER_AND_FOOTER
    return layoutItem;
}

template<class TLayoutItem, class TStickySectionItem>
struct LayoutStickyItemCompareT
{
    bool operator() ( const TLayoutItem &lhs, const TStickySectionItem &rhs) const
    {
        return lhs < rhs;
    }

    bool operator() ( const TLayoutItem *lhs, const TStickySectionItem *rhs) const
    {
        return *lhs < rhs;
    }
};

template<class TLayoutCallbackAdapter, class TSectionBase, bool VERTICAL>
class FlexLayoutT : public nsflex::ContainerBaseT<typename TSectionBase::CoordinateType, VERTICAL>
{
public:
    using TInt = typename TSectionBase::IntType;
    using TCoordinate = typename TSectionBase::CoordinateType;
    using TBase = nsflex::ContainerBaseT<typename TSectionBase::CoordinateType, VERTICAL>;
    
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

    FlexLayoutT() : m_page(0)
    {
    }

    FlexLayoutT(TInt page) : m_page(page)
    {
    }

    ~FlexLayoutT()
    {
        clearSections();
    }

    void insertItem(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex, TInt itemIndex)
    {
        if (sectionIndex < 0 || sectionIndex >= m_sections.size())
        {
            return;
        }
        
        SectionIterator it = m_sections.begin() + sectionIndex;  // Previous Section
        (*it)->insertItem(itemIndex);
    }
    
    void insertSection(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex)
    {
        int mode = layoutCallbackAdapter.getLayoutModeForSection(sectionIndex);
        
        Rect frame;
        width(frame, width(boundSize));
        // Get the leftBottom(topRight for horizontal direction) of the previous section
        if (sectionIndex >= 1)
        {
            SectionConstIterator it = m_sections.cbegin() + (sectionIndex - 1);  // Previous Section
            top(frame, bottom((*it)->getFrame()));
        }
        
        Section *section = (mode == FlexLayoutModeFlow) ? ((Section *)(new FlowSection(sectionIndex, frame))) : ((Section *)(new WaterfallSection(sectionIndex, frame)));
        
        m_sections.insert(m_sections.begin() + sectionIndex, section);
    }
    
    void deleteItem(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex, TInt itemIndex)
    {
        if (sectionIndex < 0 || sectionIndex >= m_sections.size())
        {
            return;
        }
        SectionIterator it = m_sections.begin() + sectionIndex;
        (*it)->deleteItem(itemIndex);
    }
    
    // Parameter "relayout" indicates wheather we should relayout the following sections immidiately
    // If we have multiple updates, it is better to layout after the last update.
    void deleteSection(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex)
    {
        if (sectionIndex < 0 || sectionIndex >= m_sections.size())
        {
            return;
        }
        typename std::vector<Section *>::iterator it = m_sections.begin() + sectionIndex;
        delete *it;
        m_sections.erase(it);
    }
    
    void reloadItem(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex, TInt itemIndex)
    {
        if (sectionIndex < 0 || sectionIndex >= m_sections.size())
        {
            return;
        }
        SectionIterator it = m_sections.begin() + sectionIndex;
        (*it)->reloadItem(itemIndex);
    }
    
    // Parameter "relayout" indicates wheather we should relayout the following sections immidiately
    // If we have multiple updates, it is better to layout after the last update.
    void reloadSection(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt sectionIndex)
    {
        if (sectionIndex < 0 || sectionIndex >= m_sections.size())
        {
            return;
        }
        SectionIterator it = m_sections.begin() + sectionIndex;
        (*it)->reloadSection();
    }

    void prepareLayout(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding)
    {
        // Clear all sections, maybe optimize later
        clearSections();

        TInt numberOfPages = layoutCallbackAdapter.getNumberOfPages();
        // TInt sectionCount = layoutCallbackAdapter.getNumberOfSections();
        TInt numberOfFixedSections = layoutCallbackAdapter.getNumberOfFixedSections();
        // std::vector<int> fixedSections;
        std::vector<int> pageSections;
        layoutCallbackAdapter.getPageSections(m_page, pageSections);

        if (pageSections.empty())
        {
            // Set contentSize to bound size
            m_contentSize = boundSize;;
            return;
        }
        
        // Initialize width and set height to 0, layout will calculate new height
        Rect rectOfSection = makeRect(0, 0, width(boundSize), 0);
        for (std::vector<int>::const_iterator it = pageSections.cbegin(); it != pageSections.cend(); ++it)
        {
            int layoutMode = layoutCallbackAdapter.getLayoutModeForSection(*it);
            Section *section = layoutMode == FlexLayoutModeWaterfall ?
                static_cast<Section *>(new WaterfallSection(*it, rectOfSection)) :
                static_cast<Section *>(new FlowSection(*it, rectOfSection));

            section->prepareLayout(&layoutCallbackAdapter, boundSize);
            
            m_sections.push_back(section);

            offsetY(rectOfSection, height(section->getFrame()));
        }
        
        m_contentSize = makeSize(width(boundSize), bottom(rectOfSection));
    }
    
    void prepareLayoutIncrementally(const TLayoutCallbackAdapter& layoutCallbackAdapter, const Size &boundSize, const Insets &padding, TInt minInvalidatedSection)
    {
        TInt sectionCount = layoutCallbackAdapter.getNumberOfSections();
        if (sectionCount <= 0)
        {
            // Set contentSize to bound size
            m_contentSize = boundSize;;
            return;
        }
        
        if (minInvalidatedSection >= sectionCount)
        {
            return;
        }
        
        // Initialize width and set height to 0, layout will calculate new height
        
        typename std::vector<Section *>::iterator it = m_sections.begin() + minInvalidatedSection;
        Rect rectOfSection = (*it)->getFrame();
        height(rectOfSection, 0);
        TInt sectionIndex = minInvalidatedSection;
        for (; it != m_sections.end(); ++it, ++sectionIndex)
        {
            top((*it)->getFrame(), bottom(rectOfSection));
            (*it)->setSection(sectionIndex);
            (*it)->prepareLayout(&layoutCallbackAdapter, boundSize);
            offsetY(rectOfSection, height((*it)->getFrame()));
        }
        
        m_contentSize = makeSize(width(boundSize), bottom(rectOfSection));
    }

    inline Size getContentSize() const
    {
        return m_contentSize;
    }

    void updateItems(TInt action, TInt itemStart, TInt itemCount)
    {
    }

    virtual bool adjustFrameForStickyItem(Rect &rect, Point &origin, TInt sectionIndex, TInt itemIndex, bool stackedStickyItems, const Point &contentOffset, const Insets &padding, TCoordinate totalStickyItemSize) const
    {
        origin = rect.origin;

        if (stackedStickyItems)
        {
            // top(rect, std::max(totalStickyItemSize + top(layoutInfo.padding), y(origin)));

            top(rect, std::max(y(contentOffset) + totalStickyItemSize - top(padding), y(origin)));
        }
        else
        {
            Rect frameItems = m_sections[sectionIndex]->getItemsFrameInView();
            top(rect, std::min(std::max(top(padding) + y(contentOffset), (top(frameItems) - height(rect))),
                               (bottom(frameItems) - height(rect))));
        }

        return top(rect) >= y(origin) && top(rect) == y(contentOffset) + totalStickyItemSize;
        // return top(rect) > y(origin);
        // stickyMode = (layoutAttributes.frame.origin.y >= oldOrigin.y) && (layoutAttributes.frame.origin.y == contentOffset.y + totalHeaderHeight) ? YES : NO;
        //
    }

    template <class TLayoutItem, class TStickyItem>
    void getStickyItems(std::vector<TLayoutItem> &items, StickyItemList<TStickyItem> &changingStickyItems, StickyItemList<TStickyItem> &stickyItems, bool stackedStickyItems, const SectionConstIteratorPair &range, const Point &contentOffset, const Insets &padding) const
    {
        TInt maxSection = range.second - 1 - m_sections.begin();
        TInt minSection = range.first - m_sections.begin();

        TCoordinate totalStickyItemSize = 0; // When m_stackedStickyItems == YES

        using LayoutStickyItemCompare = LayoutStickyItemCompareT<TLayoutItem, TStickyItem>;
        LayoutStickyItemCompare comp;
        Rect rect;
        Point origin;

        for (typename StickyItemList<TStickyItem>::iterator it = stickyItems.begin(); it != stickyItems.end(); ++it)
        {
            if (it->first.getSection() > maxSection || (!stackedStickyItems && (it->first.getSection() < minSection)))
            {
                if (it->second.isInSticky())
                {
                    it->second.setInSticky(false);
                    // Pass the change info to caller
                    changingStickyItems.push_back(std::make_pair(it->first, it->second));
                    // notifyItemLeavingStickyMode((*it)->section, (*it)->item, (*it)->position);
                }
                continue;
            }

            Section *section = m_sections[it->first.getSection()];
#ifdef HAVING_HEADER_AND_FOOTER
            rect = section->getHeaderFrameInView();
#else
            rect = section->getItemFrameInView(it->first.getItem());
#endif // #ifdef HAVING_HEADER_AND_FOOTER
            if (rect.size.empty())
            {
                continue;
            }

            TCoordinate stickyItemSize = height(rect);

            bool stickyMode = adjustFrameForStickyItem(rect, origin, it->first.getSection(), it->first.getItem(), stackedStickyItems, contentOffset, padding, totalStickyItemSize);
            /*
            if (stackedStickyItems)
            {
                top(rect, std::max(y(contentOffset) + totalStickyItemSize - top(padding), y(origin)));
            }
            else
            {
                Rect frameItems = m_sections[it->first.getSection()]->getItemsFrameInView();
                top(rect, std::min(std::max(top(padding) + y(contentOffset), (top(frameItems) - height(rect))),
                                   (bottom(frameItems) - height(rect))));
            }
             */

            // If original mode is sticky, we check contentOffset and if contentOffset.y is less than origin.y, it is exiting sticky mode
            // Otherwise, we check the top of sticky header
            // stickyMode = (layoutAttributes.frame.origin.y >= oldOrigin.y) && (layoutAttributes.frame.origin.y == contentOffset.y + totalHeaderHeight) ? YES : NO;
            // bool stickyMode = top(rect) >= y(origin) && ;
            // bool stickyMode = (rect.origin.y >= origin.y);
            bool originChanged = top(rect) > y(origin);
            if (stickyMode != it->second.isInSticky())
            {
                // Pass the change info to caller
                it->second.setInSticky(stickyMode);
                changingStickyItems.push_back(std::make_pair(it->first, it->second));
            }

            if (stickyMode)
            {
                typename std::vector<TLayoutItem>::iterator itVisibleItem = std::lower_bound(items.begin(), items.end(), it->first, comp);
                if (itVisibleItem != items.end() && itVisibleItem->equalsSectionItem(it->first))
                {
                    // Update in place
                    if (originChanged)
                    {
                        itVisibleItem->getFrame() = rect;
                    }
                    itVisibleItem->setInSticky(true);
                    itVisibleItem->setOriginChanged(originChanged);
                }
                else
                {
                    // Create new LayoutItem and put it into visibleItems
                    TLayoutItem layoutItem = TLayoutItem::makeLayoutItem(it->first, rect);
                    layoutItem.setHeader(true);
                    layoutItem.setInSticky(true);
                    layoutItem.setOriginChanged(originChanged);
                    items.insert(itVisibleItem, layoutItem);
                }

                totalStickyItemSize += stickyItemSize;
            }
        }

    }

    // LayoutItem::data == 1, indicates that the item is sticky
    template <class TLayoutItem, class TStickyItem>
    void getItemsInRect(std::vector<TLayoutItem> &items, StickyItemList<TStickyItem> &changingStickyItems, StickyItemList<TStickyItem> &stickyItems, bool stackedStickyItems, const Rect &rect, const Size &size,  const Size &contentSize, const Insets &padding, const Point &contentOffset, TCoordinate pagingOffset, TInt fixedSection, TInt currentPage) const
    {
        if (fixedSection >= m_sections.size())
        {
            return;
        }
        SectionConstIterator itStart = m_sections.cbegin();
        if (m_page != currentPage) itStart += fixedSection;
        SectionConstIteratorPair range = std::equal_range(itStart, m_sections.cend(), std::pair<TCoordinate, TCoordinate>(top(rect), bottom(rect)), SectionCompare());
        
        if (range.first == range.second)
        {
            // No Sections
            // If there is no matched section in rect, there must not be sticky items
            return;
        }
        
        std::vector<const FlexItem *> flexItems;
        for (SectionConstIterator it = range.first; it != range.second; ++it)
        {
            (*it)->filterInRect(flexItems, rect);
            if (flexItems.empty())
            {
                continue;
            }
            
            for (ItemConstIterator itItem = flexItems.begin(); itItem != flexItems.end(); ++itItem)
            {
                // TLayoutItem layoutItem(m_page, (*it)->getSection(), *(*itItem));
                TLayoutItem layoutItem(m_page, *(*it), *(*itItem));
                layoutItem.getFrame() = (*it)->getItemFrameInView(*itItem);
                if (m_page != currentPage || (*it)->getSection() >= fixedSection)
                {
                    offsetX(layoutItem.getFrame(), pagingOffset);
                }

                items.push_back(layoutItem);
            }
            
            flexItems.clear();
        }

        if (currentPage == m_page && !stickyItems.empty())
        {
            getStickyItems(items, changingStickyItems, stickyItems, stackedStickyItems, range, contentOffset, padding);
        }
    }

    bool getItemFrame(TInt sectionIndex, TInt itemIndex, Rect &frame) const
    {
        if (sectionIndex >= m_sections.size())
        {
            return false;
        }
        Section *section = m_sections[sectionIndex];
        if (itemIndex >= section->getItemCount())
        {
            return false;
        }
        
        // layoutItem = *it;
        frame = section->getItemFrameInView(itemIndex);
        return true;
    }

    bool getHeaderFrame(TInt sectionIndex, Rect &frame) const
    {
        if (sectionIndex >= m_sections.size())
        {
            return false;
        }
        
        // layoutItem = *it;
        frame = m_sections[sectionIndex]->getHeaderFrameInView();
        return true;
    }
    
    bool getFooterFrame(TInt sectionIndex, Rect &frame) const
    {
        if (sectionIndex >= m_sections.size())
        {
            return false;
        }
        
        // layoutItem = *it;
        frame = m_sections[sectionIndex]->getFooterFrameInView();
        return true;
    }

    
protected:

    template<class TLayoutItem>
    void makeLayoutItem(const Section &section, const FlexItem &item, TLayoutItem &layoutItem)
    {
        // TLayoutItem layoutItem(section.getSection(), item);
        // layoutItem.getFrame() = section.getItemFrameInView(item);
        // return layoutItem;
    }


    inline void clearSections()
    {
        for (typename std::vector<Section *>::iterator it = m_sections.begin(); it != m_sections.end(); delete *it, ++it);
        m_sections.clear();
    }

    /*
    LayoutItem *makeLayoutItem(TInt sectionIndex, TInt itemIndex) const
    {
        if (sectionIndex < m_sections.size())
        {
            Section *section = m_sections[sectionIndex];

            FlexItem *item = section->getItem(itemIndex);

            if (NULL != item)
            {
                Rect rect = item->getFrame();
                rect.offset(section->getFrame().left(), section->getFrame().top());

                return new LayoutItem(sectionIndex, itemIndex, rect);
            }
        }
        return NULL;
    }

     */

#ifndef NDEBUG
    std::string printDebugInfo(std::string prefix) const
    {
        std::ostringstream str;


        int idx = 1;
        for (typename std::vector<Section *>::const_iterator it = m_sections.begin(); it != m_sections.end(); ++it)
        {
            str << prefix << "Section " << idx << "[" << (*it)->getFrame().left() << "," << (*it)->getFrame().top() << "-" << (*it)->getFrame().width() << "," << (*it)->getFrame().height() << "]\r\n";

            idx++;
        }

        return str.str();
    }
#endif

};



#endif //FLEXLAYOUTMANAGER_FLEXLAYOUT_H
