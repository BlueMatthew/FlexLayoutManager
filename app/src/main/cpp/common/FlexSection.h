//
//  Section.h
//  FlexLayout
//
//  Created by Matthew Shi on 2020/6/7.
//  Copyright © 2020 Matthew Shi. All rights reserved.
//

#ifndef Section_h
#define Section_h

#include <vector>
#include <map>
#include <algorithm>

#include "FlexItem.h"
#include "ContainerBase.h"

/*
// Required interfaces for TLayout!!!
class TLayoutImpl
{
public:

 TInt getNumberOfItemsInSection(TInt section) const;
 SizeT<TCoordinate> getSizeForItem(TInt section, TInt item, bool *isFullSpan) const;
 InsetsT<TCoordinate> getInsetForSection(TInt section) const;
 TCoordinate getMinimumLineSpacingForSection(TInt section) const;
 TCoordinate getMinimumInteritemSpacingForSection(TInt section) const;
 SizeT<TCoordinate> getSizeForHeaderInSection(TInt section) const;
 SizeT<TCoordinate> getSizeForFooterInSection(TInt section) const;
 TInt getNumberOfColumnsForSection(TInt section) const;
 bool hasFixedItemSize(TInt section, SizeT<TCoordinate> *fixedItemSize) const;

};
*/

namespace nsflex
{
    template<class TLayout, class TInt, class TCoordinate, bool VERTICAL>
    class FlexSectionT : public ContainerBaseT<TCoordinate, VERTICAL>
    {
    public:

        using TBase = ContainerBaseT<TCoordinate, VERTICAL>;
        using LayoutType = TLayout;
        using IntType = TInt;
        using CoordinateType = TCoordinate;
        using FlexItem = FlexItemT<TInt, TCoordinate>;
        using FlexItemIterator = typename std::vector<FlexItem *>::iterator;

        using Point = PointT<TCoordinate>;
        using Size = SizeT<TCoordinate>;
        using Rect = RectT<TCoordinate>;
        using Insets = InsetsT<TCoordinate>;

        using TBase::y;
        using TBase::top;
        using TBase::bottom;
        using TBase::leftBottom;
        using TBase::height;
        using TBase::width;

    protected:
        TInt m_section;
        Rect m_frame;   // The origin is in the coordinate system of section, should convert to the coordinate system of UICollectionView
        Rect m_itemsFrame;  // = (0, header.bottom) - (0, items.height)

    protected:
        union {
            struct {
                // If minimal invalidated item is greater than 2^24, just set m_sectionInvalidated to 1
                unsigned int m_sectionInvalidated : 1;    // The whole section is invalidated
                unsigned int m_headerInvalidated : 1;
                unsigned int m_itemsInvalidated : 1; // Some of items are invalidated
                unsigned int m_footerInvalidated : 1;
            };
            unsigned int m_invalidationContext;
        };
        TInt m_minimalInvalidatedItem;

#ifdef HAVING_HEADER_AND_FOOTER
        FlexItem m_header;
#endif // #ifdef HAVING_HEADER_AND_FOOTER
        std::vector<FlexItem *> m_items;    // The origin is from (sectionInsets.left, sectionInsets.top) and should to offset(section.left, section.top + header.height) to convert to view coordinate system
#ifdef HAVING_HEADER_AND_FOOTER
        FlexItem m_footer;
#endif // #ifdef HAVING_HEADER_AND_FOOTER

    public:
        FlexSectionT(TInt section, const Rect& frame) : ContainerBaseT<TCoordinate, VERTICAL>(), m_section(section), m_frame(frame), m_invalidationContext(0), m_minimalInvalidatedItem(std::numeric_limits<TInt>::max())
        {
#ifdef HAVING_HEADER_AND_FOOTER
            m_header.setHeader(true);
            m_footer.setFooter(true);
#endif // #ifdef HAVING_HEADER_AND_FOOTER
            
            width(m_itemsFrame, width(m_frame));
            invalidateSection();
        }

        virtual ~FlexSectionT()
        {
            clearItems();
        }

        FlexItem *getItem(TInt itemIndex) const
        {
            return (itemIndex < m_items.size()) ? m_items[itemIndex] : NULL;
        }

        inline void clearItems()
        {
            for(typename std::vector<FlexItem *>::iterator it = m_items.begin(); it != m_items.end(); delete *it, ++it);
            m_items.clear();
        }

        inline TInt getSection() const { return m_section; }
        inline void setSection(TInt section) { m_section = section; }
        inline Rect getFrame() const { return m_frame; }
        inline Rect &getFrame() { return m_frame; }
        inline TInt getItemCount() const { return m_items.size(); }

        virtual void reloadSection()
        {
            invalidateSection();
        }
        
        virtual void insertItem(TInt itemIndex)
        {
            FlexItem *item = new FlexItem(itemIndex);
            m_items.insert(m_items.begin() + itemIndex, item);
            invalidateItem(itemIndex);
        }
        
        virtual void reloadItem(TInt itemIndex)
        {
            invalidateItem(itemIndex);
        }
        
        virtual void deleteItem(TInt itemIndex)
        {
            typename std::vector<FlexItem *>::iterator it = m_items.begin() + itemIndex;
            delete (*it);
            m_items.erase(it);
            invalidateItem(itemIndex);
        }
        
        inline Rect getItemFrameInView(TInt item) const
        {
#ifndef NDEBUG
            assert(item < m_items.size());
#endif // NDEBUG
            Rect rect(m_items[item]->getFrame());
            rect.offset(m_itemsFrame.left(), m_itemsFrame.top());
            return getFrameInView(rect);
        }
        
        inline Rect getItemFrameInView(const FlexItem *item) const
        {
            Rect rect(item->getFrame());
            if (item->isItem())
            {
                rect.offset(m_itemsFrame.left(), m_itemsFrame.top());
            }
            return getFrameInView(rect);
        }

#ifdef HAVING_HEADER_AND_FOOTER
        inline Rect getHeaderFrameInView() const
        {
            return getFrameInView(m_header.getFrame());
        }

        inline Rect getFooterFrameInView() const
        {
            return getFrameInView(m_footer.getFrame());
        }
#endif // #ifdef HAVING_HEADER_AND_FOOTER

        inline Rect getItemsFrame() const
        {
            return getFrameInView(m_itemsFrame);
        }
        
        inline Rect getItemsFrameInView() const
        {
            return getFrameInView(m_itemsFrame);
        }

        void prepareLayout(const TLayout *layout, const Size &size)
        {
            if (!existsInvalidatedPart())
            {
                return;
            }
            
// Clear the frame height and calculate it by layout
            height(m_frame, 0);
#ifdef HAVING_HEADER_AND_FOOTER
            
#endif // #ifdef HAVING_HEADER_AND_FOOTER
            
#ifdef HAVING_HEADER_AND_FOOTER
            height(m_itemsFrame, height(m_frame));
            // Header
            m_header.getFrame().size = getSizeForHeader(layout);

            // Initialize the section height with header height
            height(m_frame, height(m_header.getFrame()));
            top(m_itemsFrame, bottom(m_header.getFrame()));
#endif // #ifdef HAVING_HEADER_AND_FOOTER

            prepareItemsLayout(layout, size);
            height(m_frame, bottom(m_itemsFrame));

#ifdef HAVING_HEADER_AND_FOOTER
            // Footer
            m_footer.getFrame().origin = leftBottom(m_itemsFrame);
            m_footer.getFrame().size = getSizeForFooter(layout);

            height(m_frame, bottom(m_footer.getFrame()));
#endif // #ifdef HAVING_HEADER_AND_FOOTER
            
            resetInvalidationContext();
        }
        
    public:
        bool filterInRect(std::vector<const FlexItem *> &items, const Rect &rect) const
        {
            bool matched = false;

            Rect rectInSection = Rect::intersectRects(m_frame, rect);
            if (rectInSection.empty())
            {
                return matched;
            }

            // Convert to coodinate of section
            rectInSection.offset( -m_frame.origin.x, -m_frame.origin.y);

#ifdef HAVING_HEADER_AND_FOOTER
            // Header
            if (!m_header.getFrame().empty() && rectInSection.intersects(m_header.getFrame()))
            {
                items.push_back(&m_header);
                matched = true;
            }
#endif // #ifdef HAVING_HEADER_AND_FOOTER

            // Items
            if (filterItemsInRect(rectInSection, items))
            {
                matched = true;
            }

#ifdef HAVING_HEADER_AND_FOOTER
            // Footer
            if (!m_footer.getFrame().size.empty() && m_footer.getFrame().intersects(rectInSection))
            {
                items.push_back(&m_footer);
                matched = true;
            }
#endif // #ifdef HAVING_HEADER_AND_FOOTER

            return matched;
        }

    protected:
        void resetInvalidationContext()
        {
            m_invalidationContext = 0;
            m_minimalInvalidatedItem = std::numeric_limits<TInt>::max();
        }

        bool isSectionInvalidated() const { return m_sectionInvalidated == 1; }
#ifdef HAVING_HEADER_AND_FOOTER
        bool isHeaderInvalidated() const { return m_headerInvalidated == 1; }
        bool isFooterInvalidated() const { return m_footerInvalidated == 1; }
#endif // #ifdef HAVING_HEADER_AND_FOOTER
        bool hasInvalidatedItems() const { return m_itemsInvalidated == 1; }
        
        bool existsInvalidatedPart() const
        {
            return m_invalidationContext != 0;
        }

        TInt getMinimalInvalidatedItem() const { return m_sectionInvalidated ? 0 : std::min(m_minimalInvalidatedItem, (TInt)m_items.size()); }
        inline void invalidateSection()
        {
            m_sectionInvalidated = 1;
        }

#ifdef HAVING_HEADER_AND_FOOTER
        inline void invalidateHeader()
        {
            m_headerInvalidated = 1;
        }

        void invalidateFooter()
        {
            m_footerInvalidated = 1;
        }
#endif // #ifdef HAVING_HEADER_AND_FOOTER

        void invalidateItem(TInt item)
        {
            m_itemsInvalidated = 1;
            if (item < m_minimalInvalidatedItem)
            {
                m_minimalInvalidatedItem = item;
            }
        }

    protected:

        virtual void prepareItemsLayout(const TLayout *layout, const Size &size) = 0;
        virtual bool filterItemsInRect(const Rect &rectInSection, std::vector<const FlexItem *> &items) const = 0;

        inline Rect getFrameInView(const Rect& rect) const
        {
            Rect rectInView(rect);
            rectInView.offset(m_frame.left(), m_frame.top());
            return rectInView;
        }
        
        inline void prepareItems(TInt numberOfItems)
        {
            TInt orgNumberOfItems = m_items.size();
            if (numberOfItems > orgNumberOfItems)
            {
                m_items.reserve(numberOfItems);
                for (TInt itemIndex = orgNumberOfItems; itemIndex < numberOfItems; ++itemIndex)
                {
                    m_items.push_back(new FlexItem(itemIndex));
                }
            }
            else if (numberOfItems < orgNumberOfItems)
            {
                FlexItemIterator itStart = m_items.begin() + numberOfItems;
                FlexItemIterator it = itStart;
                for (; it < m_items.end(); ++it)
                {
                    delete (*it);
                }
                m_items.erase(itStart, m_items.end());
            }
        }

        // Layout Adapter Functions Begin
        inline TInt getNumberOfItems(const TLayout *layout) const
        {
            return layout->getNumberOfItemsInSection(m_section);
        }

        inline Size getSizeForItem(const TLayout *layout, TInt item, bool *isFullSpan) const
        {
            return layout->getSizeForItem(m_section, item, isFullSpan);
        }

        inline Insets getInsets(const TLayout *layout) const
        {
            return layout->getInsetForSection(m_section);
        }

        inline TCoordinate getMinimumLineSpacing(const TLayout *layout) const
        {
            return layout->getMinimumLineSpacingForSection(m_section);
        }

        inline TCoordinate getMinimumInteritemSpacing(const TLayout *layout) const
        {
            return layout->getMinimumInteritemSpacingForSection(m_section);
        }

#ifdef HAVING_HEADER_AND_FOOTER
        inline Size getSizeForHeader(const TLayout *layout) const
        {
            return layout->getSizeForHeaderInSection(m_section);
        }

        inline Size getSizeForFooter(const TLayout *layout) const
        {
            return layout->getSizeForFooterInSection(m_section);
        }
#endif // #ifdef HAVING_HEADER_AND_FOOTER

        inline TInt getNumberOfColumns(const TLayout *layout) const
        {
            return layout->getNumberOfColumnsForSection(m_section);
        }

        inline bool isFullSpanAtItem(const TLayout *layout, TInt item) const
        {
            return layout->isFullSpanAtItem(m_section, item);
        }

        inline bool hasFixedSize(const TLayout *layout, TInt section, Size *fixedSize) const
        {
            return layout->hasFixedSize(m_section, fixedSize);
        }
        // Layout Adapter Functions End

    };

} // namespace nsflex
    
#endif /* Section_h */
