//
// Created by Matthew Shi on 2020-07-14.
//

#include "common/Graphics.h"
#include "common/FlexItem.h"
#include "common/FlexRow.h"
#include "common/FlexColumn.h"
#include "common/FlexLayout.h"
#include "RecyclerLayout.h"


#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H

using Point = nsflex::PointT<int>;
using Size = nsflex::SizeT<int>;
using Rect = nsflex::RectT<int>;
using Insets = nsflex::InsetsT<int>;

using FlexItem = nsflex::FlexItemT<int, int>;
using LayoutItem = RecyclerLayoutItemT<int, int>;

using StickySectionItem = RecyclerSectionItemT<int, (char)(FlexItem::ITEM_TYPE_ITEM)>;
using StickyItemState = StickyItemStateT<int>;
using StickyItem = StickyItemT<StickySectionItem, StickyItemState>;

struct SectionInfo
{
    int section;    // For incremental update
    int layoutMode;
    Insets padding;
    int numberOfItems;
    int numberOfColumns;
    int lineSpacing;
    int interitemSpacing;
    int hasFixedItemSize;
    Size fixedItemSize;

    SectionInfo() : section(0), layoutMode(0), numberOfItems(0), numberOfColumns(0), lineSpacing(0), interitemSpacing(0), hasFixedItemSize(0)
    {
    }

    static int numberOfInts()
    {
        return 13;
    }

    inline int readFromBuffer(int* buffer, int bufferLength)
    {
        if (bufferLength < numberOfInts())
        {
            return 0;
        }

        int offset = 0;
        section = buffer[offset++];
        layoutMode = buffer[offset++];

        padding.left = buffer[offset++];
        padding.top = buffer[offset++];
        padding.right = buffer[offset++];
        padding.bottom = buffer[offset++];

        numberOfItems = buffer[offset++];
        numberOfColumns = buffer[offset++];
        lineSpacing = buffer[offset++];
        interitemSpacing = buffer[offset++];
        hasFixedItemSize = buffer[offset++];
        fixedItemSize.width = buffer[offset++];
        fixedItemSize.height = buffer[offset++];

        return offset;
    }

};


struct LayoutInfo {
    int orientation;
    Size size;
    Insets padding;
    Size contentSize;
    Point contentOffset;

    LayoutInfo() : orientation(1)
    {
    }

    static int numberOfInts()
    {
        return 9; //1 + 2 + 4 + 2
    }

    inline int readFromBuffer(int* buffer, int bufferLength)
    {
        if (bufferLength < numberOfInts())
        {
            return 0;
        }

        int offset = 0;
        orientation = buffer[offset++];
        size.width = buffer[offset++];
        size.height = buffer[offset++];
        padding.left = buffer[offset++];
        padding.top = buffer[offset++];
        padding.right = buffer[offset++];
        padding.bottom = buffer[offset++];
        contentOffset.x = buffer[offset++];
        contentOffset.y = buffer[offset++];

        return offset;
    }
};


struct LayoutAndSectionsInfo : public LayoutInfo
{
    int numberOfSections;
    int sectionStart;

    std::vector<SectionInfo> sections;

    LayoutAndSectionsInfo() : LayoutInfo(), numberOfSections(0), sectionStart(0)
    {

    }

    static int numberOfInts()
    {
        return LayoutInfo::numberOfInts() + 2;
    }

    inline int readFromBuffer(int* buffer, int bufferLength)
    {
        int offset = 0;
        if (bufferLength < numberOfInts())
        {
            return 0;
        }

        int intsRead = LayoutInfo::readFromBuffer(buffer, bufferLength);
        if (intsRead == 0)
        {
            return 0;
        }
        offset += intsRead;

        numberOfSections = buffer[offset++];
        sectionStart = buffer[offset++];
        if (numberOfSections <= 0)
        {
            sections.clear();
            return offset;
        }

        sections.resize(numberOfSections);
        for (std::vector<SectionInfo>::iterator it = sections.begin(); it != sections.end(); ++it)
        {
            intsRead = (*it).readFromBuffer(buffer + offset, bufferLength - offset);
            if (0 == intsRead)
            {
                break;
            }
            offset += intsRead;
        }

        return offset;
    }

};

template<typename T>
inline void writeToBuffer(std::vector<int> &buffer, const T &t)
{
}

template<>
inline void writeToBuffer(std::vector<int> &buffer, const Rect &rect)
{
    buffer.push_back(rect.left());
    buffer.push_back(rect.top());
    buffer.push_back(rect.width());
    buffer.push_back(rect.height());
}

template<>
inline void writeToBuffer(std::vector<int> &buffer, const LayoutItem &layoutItem)
{
    buffer.push_back(layoutItem.getSection());
    buffer.push_back(layoutItem.getItem());
    buffer.push_back(layoutItem.getPosition());
    writeToBuffer(buffer, layoutItem.getFrame());

    buffer.push_back(layoutItem.isInSticky() ? 1 : 0);
    buffer.push_back(layoutItem.isOriginChanged() ? 1 : 0);
}

template<>
inline void writeToBuffer(std::vector<int> &buffer, const StickyItem &stickyItem)
{
    buffer.push_back(stickyItem.first.getSection());
    buffer.push_back(stickyItem.first.getItem());
    buffer.push_back(stickyItem.first.getPosition());
    buffer.push_back(stickyItem.second.isInSticky() ? 1 : 0);
    buffer.push_back(stickyItem.second.isOriginChanged() ? 1 : 0);
    buffer.push_back(stickyItem.second.getFrame().origin.x);
    buffer.push_back(stickyItem.second.getFrame().origin.y);

}


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H
