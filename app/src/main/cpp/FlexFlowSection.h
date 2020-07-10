//
// Created by Matthew on 2020-07-04.
//

#ifndef FLEXLAYOUTMANAGER_FLEXFLOWSECTION_H
#define FLEXLAYOUTMANAGER_FLEXFLOWSECTION_H

#include "Graphics.h"
#include "FlexNodes.h"
#include "FlexSection.h"

template<class TLayout>
class FlowSectionT : public SectionT<TLayout>
{
public:
    std::vector<FlexRow *> m_rows;

    FlowSectionT(TLayout *layout, int section, const Point &origin) : SectionT<TLayout>(layout, section, origin)
    {
    }

    virtual ~FlowSectionT()
    {
        for(std::vector<FlexRow *>::iterator it = m_rows.begin(); it != m_rows.end(); delete *it, ++it);
        m_rows.clear();
    }

    virtual void prepareLayout()
    {
        // SectionT<TLayout>::m_frame.size = isVertical() ? Size(SectionT<TLayout>::m_layout->getWidth(), 0) : Size(0, SectionT<TLayout>::m_layout->getHeight());
        m_rows.clear();
        int numberOfItems = SectionT<TLayout>::getNumberOfItems();
        if (numberOfItems > 0)
        {
            Insets sectionInset = SectionT<TLayout>::getInsets();
            bool vertical = SectionT<TLayout>::isVertical();

            SectionT<TLayout>::m_items.reserve(numberOfItems);

            // For FlowLayout, there is no column property but we still try to get the number of columns, and use it to estimate the number of rows
            int numberOfColumns = SectionT<TLayout>::getNumberOfColumns();
            m_rows.reserve(numberOfColumns > 0 ? (numberOfItems / numberOfColumns) : (numberOfItems >> 1));

            int minimumLineSpacing = SectionT<TLayout>::getMinimumLineSpacing();
            int minimumInteritemSpacing = SectionT<TLayout>::getMinimumInteritemSpacing();

            int maximalSizeOfRow = vertical ? (SectionT<TLayout>::m_frame.size.width - sectionInset.left - sectionInset.right) : (SectionT<TLayout>::m_frame.size.height - sectionInset.top - sectionInset.bottom);

            //
            FlexItem *sectionItem = NULL;
            FlexRow *row = NULL;

            Point originForRow(sectionInset.left, sectionInset.top);

            Size sizeOfItem;
            Point originOfItem;
            for (int item = 0; item < numberOfItems; item++)
            {
                SectionT<TLayout>::getItemSize(item, sizeOfItem);

                if (NULL != row)
                {
                    if (row->hasItem())
                    {
                        int availableSize = maximalSizeOfRow - (vertical ? row->getFrame().size.width : row->getFrame().size.height) - minimumInteritemSpacing;
                        if (availableSize < (vertical ? sizeOfItem.width : sizeOfItem.height))
                        {
                            // New Line
                            m_rows.push_back(row);
                            row = NULL;
                        }
                        else
                        {
                            originOfItem.x = (vertical ? row->getFrame().right() : row->getFrame().bottom()) + minimumInteritemSpacing;
                            originOfItem.y = row->getFrame().origin.y;
                        }
                    }
                }

                if (NULL == row)
                {
                    if (m_rows.size() > 0)
                    {
                        vertical ? originForRow.y += minimumLineSpacing + m_rows.back()->getFrame().size.height : originForRow.x += minimumLineSpacing + m_rows.back()->getFrame().size.width;
                    }

                    row = new FlexRow();
                    row->getFrame().origin = originForRow;

                    originOfItem = originForRow;
                }

                sectionItem = new FlexItem(item, originOfItem, sizeOfItem);

                SectionT<TLayout>::m_items.push_back(sectionItem);
                row->addItem(sectionItem, vertical);
            }

            if (NULL != row)
            {
                // Last row
                m_rows.push_back(row);

                vertical ? (SectionT<TLayout>::m_frame.size.height += row->getFrame().bottom() - m_rows[0]->getFrame().origin.y + sectionInset.top + sectionInset.bottom) : (SectionT<TLayout>::m_frame.size.width += row->getFrame().right() - m_rows[0]->getFrame().origin.x + sectionInset.left + sectionInset.right);
            }
        }

        // footer
        // SectionT<TLayout>::m_footer.m_frame.origin = isVertical ? PointMake(0, SectionT<TLayout>::m_frame.size.height) : PointMake(SectionT<TLayout>::m_frame.size.width, 0);
        // SectionT<TLayout>::m_footer.getFrame().size = [SectionT<TLayout>::m_layout getSizeForFooterInSection:(SectionT<TLayout>::m_section)];

        // isVertical ? (SectionT<TLayout>::m_frame.size.height += SectionT<TLayout>::m_footer.m_frame.size.height) : (SectionT<TLayout>::m_frame.size.width += SectionT<TLayout>::m_footer.getFrame().size.width);
    }

    virtual bool filterItems(std::vector<FlexItem *> &items, const Rect &rectInSection)
    {
        bool merged = false;

        FlexRowVerticalCompare vcomp;
        FlexRowHorizontalCompare hcomp;
        bool vertical = SectionT<TLayout>::isVertical();

        std::pair<std::vector<FlexRow *>::iterator, std::vector<FlexRow *>::iterator> range = vertical ? getVirticalRowsInRect(rectInSection) : getHorizontalRowsInRect(rectInSection);

        for (std::vector<FlexRow *>::iterator it = range.first; it != range.second; ++it)
        {
            if (vertical ? (*it)->filter(items, rectInSection, vcomp) : (*it)->filter(items, rectInSection, hcomp))
            {
                merged = true;
            }
        }

        return merged;
    }


    inline std::pair<std::vector<FlexRow *>::iterator, std::vector<FlexRow *>::iterator> getVirticalRowsInRect(const Rect& rect)
    {
        return std::equal_range(m_rows.begin(), m_rows.end(), std::pair<int, int>(rect.origin.y, rect.origin.y + rect.size.height), FlexRowVerticalCompare());
    }

    inline std::pair<std::vector<FlexRow *>::iterator, std::vector<FlexRow *>::iterator> getHorizontalRowsInRect(const Rect& rect)
    {
        return std::equal_range(m_rows.begin(), m_rows.end(), std::pair<int, int>(rect.origin.x, rect.origin.x + rect.size.width), FlexRowHorizontalCompare());
    }

};





#endif //FLEXLAYOUTMANAGER_FLEXFLOWSECTION_H
