//
// Created by Matthew on 2020-07-04.
//

#ifndef FLEXLAYOUTMANAGER_FLEXWATERFALLSECTION_H
#define FLEXLAYOUTMANAGER_FLEXWATERFALLSECTION_H

#include "Graphics.h"
#include "FlexNodes.h"
#include "FlexSection.h"

template<class TLayout>
class WaterfallSectionT : public SectionT<TLayout>
{
public:
    std::vector<FlexColumn *> m_columns;

    WaterfallSectionT(TLayout *layout, int section, Point origin) : SectionT<TLayout>(layout, section, origin)
    {
    }

    ~WaterfallSectionT()
    {
        for(std::vector<FlexColumn *>::iterator it = m_columns.begin(); it != m_columns.end(); delete *it, ++it);
        m_columns.clear();
    }

    void prepareLayout()
    {
        SectionT<TLayout>::m_frame.size.height = 0;

        bool vertical = SectionT<TLayout>::isVertical();
        // Size boundSize = SectionT<TLayout>::getBounds();

        // vertical ? (SectionT<TLayout>::m_frame.size.width = boundSize.width) : (SectionT<TLayout>::m_frame.size.height = boundSize.height);

        // header
        // SectionT<TLayout>::m_header.m_frame.size = [SectionT<TLayout>::m_layout getSizeForHeaderInSection:SectionT<TLayout>::m_section];
        // IS_CV_VERTICAL(SectionT<TLayout>::m_layout) ? (SectionT<TLayout>::m_frame.size.height += SectionT<TLayout>::m_header.m_frame.size.height) : (SectionT<TLayout>::m_frame.size.width += SectionT<TLayout>::m_header.m_frame.size.width);

        int numberOfItems = SectionT<TLayout>::getNumberOfItems();
        if (numberOfItems > 0)
        {
            Insets sectionInset = SectionT<TLayout>::getInsets();

            SectionT<TLayout>::m_items.reserve(numberOfItems);

            int minimumLineSpacing = SectionT<TLayout>::getMinimumLineSpacing();
            int minimumInteritemSpacing = SectionT<TLayout>::getMinimumInteritemSpacing();

            int numberOfColumns = SectionT<TLayout>::getNumberOfColumns();

            m_columns.reserve(numberOfColumns);
            int sizeOfColumn = vertical ? (SectionT<TLayout>::m_frame.size.width - sectionInset.left - sectionInset.right) : (SectionT<TLayout>::m_frame.size.height - sectionInset.top - sectionInset.bottom);
            if (numberOfColumns > 1)
            {
                sizeOfColumn = (sizeOfColumn - (numberOfColumns - 1) * minimumInteritemSpacing) / numberOfColumns;
            }

            Point columnOrigin(sectionInset.left, sectionInset.top);
            Size columnSize = vertical ? Size(sizeOfColumn, 0) : Size(0, sizeOfColumn);
            int itemCapacity = (int)(numberOfItems / numberOfColumns + 0.5);

            for (int column = 0; column < numberOfColumns; column++)
            {
                FlexColumn *sectionColumn = new FlexColumn(itemCapacity);
                sectionColumn->getFrame().origin = columnOrigin;
                sectionColumn->getFrame().size = columnSize;
                m_columns.push_back(sectionColumn);

                vertical ? (columnOrigin.x += sizeOfColumn + minimumInteritemSpacing) : (columnOrigin.y += sizeOfColumn + minimumInteritemSpacing);
            }

            std::vector<FlexColumn *>::iterator columnItOfMinimalSize = m_columns.begin();
            int itemIndexInColumn = 0;
            // SectionItem *sectionItem = new SectionItem[numberOfItems];

            // 比较器实例，因为成本很小，所以两个实例全部创建，避免后续频繁分配小对象
            FlexColumnSizeVerticalCompare vComp;
            FlexColumnSizeHorizontalCompare hComp;

            Size sizeOfItem;
            for (int item = 0; item < numberOfItems; item++)
            {
                // Column with lowest height
                columnItOfMinimalSize = vertical ? min_element(m_columns.begin(), m_columns.end(), vComp) : min_element(m_columns.begin(), m_columns.end(), hComp);

                // If it is not first column, add interitem spacing
                // itemIndexInColumn = (*columnItOfMinimalSize)->em;

                Point originOfItem = vertical ? Point((*columnItOfMinimalSize)->getFrame().origin.x, (*columnItOfMinimalSize)->getFrame().bottom() + ((*columnItOfMinimalSize)->hasItem() ? minimumLineSpacing : 0)) : Point((*columnItOfMinimalSize)->getFrame().right() + ((*columnItOfMinimalSize)->hasItem() ? minimumLineSpacing : 0), (*columnItOfMinimalSize)->getFrame().origin.y);

                SectionT<TLayout>::getItemSize(item, sizeOfItem);
                FlexItem *sectionItem = new FlexItem(item, originOfItem, sizeOfItem);

                SectionT<TLayout>::m_items.push_back(sectionItem);
                // Add into the column with lowest hight
                (*columnItOfMinimalSize)->addItem(sectionItem, vertical);
            }

            // Find the column with highest height
            std::vector<FlexColumn *>::iterator columnItOfMaximalSize = vertical ? max_element(m_columns.begin(), m_columns.end(), vComp) : max_element(m_columns.begin(), m_columns.end(), hComp);

            vertical ? SectionT<TLayout>::m_frame.size.height += (*columnItOfMaximalSize)->getFrame().size.height + sectionInset.top + sectionInset.bottom : SectionT<TLayout>::m_frame.size.width += (*columnItOfMaximalSize)->getFrame().size.width + sectionInset.left + sectionInset.right;
        }
    }

    virtual bool filterItems(std::vector<FlexItem *> &items, const Rect &rectInSection)
    // bool getLayoutAttributesForItemsInRect(NSMutableArray<CollectionViewLayoutAttributes *> *layoutAttributes, const Rect &rectInSection)
    {
        bool vertical = SectionT<TLayout>::isVertical();

        std::pair<std::vector<FlexRow *>::iterator, std::vector<FlexRow *>::iterator> range = vertical ? getVirticalRowsInRect(rectInSection) : getHorizontalRowsInRect(rectInSection);

        if (range.first == range.second)
        {
            return false;
        }

        FlexItemVerticalCompare vcomp;
        FlexItemHorizontalCompare hcomp;
        bool matched = false;
        for (std::vector<FlexRow *>::iterator it = range.first; it != range.second; ++it)
        {
            if (vertical ? (*it)->filter(items, rectInSection, hcomp) : (*it)->filter(items, rectInSection, vcomp))
            {
                matched = true;
            }
        }

        return matched;
    }

    inline std::pair<std::vector<FlexRow *>::iterator, std::vector<FlexRow *>::iterator> getVirticalRowsInRect(const Rect& rect)
    {
        return std::equal_range(m_columns.begin(), m_columns.end(), std::pair<int, int>(rect.origin.y, rect.origin.y + rect.size.height), FlexItemVerticalCompare());
    }

    inline std::pair<std::vector<FlexRow *>::iterator, std::vector<FlexRow *>::iterator> getHorizontalRowsInRect(const Rect& rect)
    {
        return std::equal_range(m_columns.begin(), m_columns.end(), std::pair<int, int>(rect.origin.x, rect.origin.x + rect.size.width), FlexItemHorizontalCompare());
    }


};

#endif //FLEXLAYOUTMANAGER_FLEXWATERFALLSECTION_H
