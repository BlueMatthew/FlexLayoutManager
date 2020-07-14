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
    Point origin;

    LayoutItem() : section(0), item(0), position(0), data(0) {}
    LayoutItem(int s, int i) : section(s), item(i), position(0), data(0) {}
    LayoutItem(int s, int i, int pos, const Rect &f) : section(s), item(i), position(pos), frame(f), data(0), origin(f.origin) {}
    LayoutItem(const LayoutItem &src) : section(src.section), item(src.item), position(src.position), frame(src.frame), data(src.data), origin(src.origin) {}
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
        this->origin = frame.origin;
    }

    void set(int position, const Rect &frame)
    {
        this->position = position;
        this->frame = frame;
        this->origin = frame.origin;
    }

    void set(const LayoutItem &other)
    {
        position = other.position;
        frame = other.frame;
        origin = other.origin;
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


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H
