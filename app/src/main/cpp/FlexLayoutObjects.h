//
// Created by Matthew Shi on 2020-07-14.
//

#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H


#include "Graphics.h"

#include "Graphics.h"
#include "FlexItem.h"
#include "FlexRow.h"
#include "FlexColumn.h"


typedef nsflex::PointT<int> Point;
typedef nsflex::SizeT<int> Size;
typedef nsflex::RectT<int> Rect;
typedef nsflex::InsetsT<int> Insets;

typedef nsflex::FlexItemT<int, int> FlexItem;
typedef nsflex::FlexVerticalCompareT<FlexItem> FlexItemVerticalCompare;
typedef nsflex::FlexHorizontalCompareT<FlexItem> FlexItemHorizontalCompare;


struct LayoutItem
{
    int section;
    int item;
    int position;
    Rect frame;
    int data;
    // Point origin;

    LayoutItem() : section(0), item(0), position(0), data(0) {}
    LayoutItem(int s, int i) : section(s), item(i), position(0), data(0) {}
    LayoutItem(int s, int i, int pos, const Rect &f) : section(s), item(i), position(pos), frame(f), data(0) {}
    LayoutItem(const LayoutItem &src) : section(src.section), item(src.item), position(src.position), frame(src.frame), data(src.data) {}
    LayoutItem(const LayoutItem *src) : LayoutItem(*src) {}


    bool operator==(const LayoutItem &other)
    {
        if (this == &other) return true;
        return section == other.section && item == other.item;
    }

    bool operator==(const LayoutItem *other)
    {
        return *this == *other;
    }

    bool operator!=(const LayoutItem &other)
    {
        return !(*this == other);
    }

    bool operator!=(const LayoutItem *other)
    {
        return !(*this == *other);
    }

    void reset(int section, int item, int position, const Rect &frame, int data)
    {
        this->section = section;
        this->item = item;
        this->position = position;
        this->frame = frame;
        this->data = data;
        // this->origin = frame.origin;
    }

    void set(int position, const Rect &frame)
    {
        this->position = position;
        this->frame = frame;
        // this->origin = frame.origin;
    }

    void set(const LayoutItem &other)
    {
        position = other.position;
        frame = other.frame;
        // origin = other.origin;
        data = other.data;
    }

    void clearStickyFlag()
    {
        data &= ~1;
    }

    void setInSticky(bool inSticky)
    {
        inSticky ? (data |= 1) : (data &= (~1));
    }

    bool isInSticky() const
    {
        return (data & 1) == 1;
    }
};

struct LayoutItemCompare
{
    bool operator() ( const LayoutItem* lhs, const LayoutItem* rhs) const
    {
        return lhs->section < rhs->section || (lhs->section == rhs->section && lhs->item < rhs->item);
    }
};


struct StickyItem
{
    int section;
    int item;
    int position;
    bool inSticky;

    StickyItem(int s, int i) : section(s), item(i), position(0), inSticky(false)
    {
    }

    StickyItem(const StickyItem& src) : section(src.section), item(src.item), position(src.position), inSticky(src.inSticky)
    {
    }

    bool operator==(const StickyItem &other)
    {
        if (this == &other) return true;
        return section == other.section && item == other.item;
    }

    bool operator!=(const StickyItem &other)
    {
        return !(*this == other);
    }

    bool operator<(const StickyItem &other)
    {
        return (section < other.section) || ((section == other.section) && item == other.item);
    }

};

struct LayoutStickyItemCompare
{
    bool operator() ( const LayoutItem &lhs, const StickyItem &rhs) const
    {
        return lhs.section < rhs.section || (lhs.section == rhs.section && lhs.item < rhs.item);
    }
};


struct SectionInfo
{
    int section;    // For increasemental update
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


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H
