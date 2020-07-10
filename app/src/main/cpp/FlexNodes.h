//
// Created by Matthew on 2020-07-03.
//

#ifndef FLEXLAYOUTMANAGER_FLEXNODES_H
#define FLEXLAYOUTMANAGER_FLEXNODES_H

#include <vector>
#include <algorithm>
#include <utility>
#include <memory>
#include "Graphics.h"

class FlexSection;

class FlexSectionCallback
{
public:
    virtual ~FlexSectionCallback() {}

    virtual bool isVertical() const = 0;
    virtual Size getParentBounds() const = 0;
    virtual Insets getParentInsets() const = 0;

    virtual int getSection() const = 0;
    virtual int getPosition() const = 0;
    virtual int getNumberOfItems() const = 0;
    virtual int getNumberOfColumns() const = 0;
    virtual void getInsets(Insets &insets) const = 0;
    virtual int getMinimumLineSpacing() const = 0;
    virtual int getMinimumInteritemSpacing() const = 0;
    virtual bool hasFixedItemSize(Size &size) const = 0;
    virtual void getItemSize(int item, Size &size) const = 0;

    virtual void toParentRect(Rect &rect) const = 0;

};

class FlexItem {
private:

    FlexSection* m_section;
    int m_item;
    Rect m_frame;

public:
    typedef std::vector<FlexItem *>::iterator Iterator;
public:
    FlexItem() : m_item(0) { }
    FlexItem(int item) : m_item(item) { }
    FlexItem(int item, Point origin, Size size) : m_item(item), m_frame(origin, size) { }

    inline int getItem() const { return m_item; }
    int getPosition() const;
    inline Rect &getFrame() { return m_frame; }
    inline const Rect getFrame() const { return m_frame; }

    bool operator< (int rhs) const
    {
        return m_item < rhs;
    }

    bool operator> (int rhs) const
    {
        return m_item > rhs;
    }

};

struct FlexItemPositionCompare
{
    bool operator() ( const FlexItem* item, int itemToCompare) const
    {
        return *item < itemToCompare;
    }
    bool operator() ( int itemToCompare, const FlexItem* item ) const
    {
        return *item > itemToCompare;
    }
};

template<typename T>
struct FlexVerticalCompareT
{
    bool operator() ( const T* item, const Rect& rect) const
    {
        return item->getFrame().bottom() < rect.top();
    }
    bool operator() ( const Rect& rect, const T* item ) const
    {
        return rect.bottom() < item->getFrame().top();
    }
};

template<typename T>
struct FlexHorizontalCompareT
{
    bool operator() ( const T* item, const Rect& rect) const
    {
        return item->getFrame().right() < rect.left();
    }
    bool operator() ( const Rect& rect, const T* item ) const
    {
        return rect.right() < item->getFrame().left();
    }
};

template<typename T>
struct FlexSizeHorizontalCompareT
{
    bool operator() ( const T* lhs, const T* rhs) const
    {
        return lhs->getFrame().size.width < rhs->getFrame().size.width;
    }
};

template<typename T>
struct FlexSizeVerticalCompareT
{
    bool operator() ( const T* lhs, const T* rhs) const
    {
        return lhs->getFrame().size.height < rhs->getFrame().size.height;
    }
};

typedef FlexVerticalCompareT<FlexItem> FlexItemVerticalCompare;
typedef FlexHorizontalCompareT<FlexItem> FlexItemHorizontalCompare;

class FlexRow
{
private:
    std::vector<FlexItem *> m_items;

    Rect m_frame; // The origin is in the coordinate system of section, should convert to the coordinate system of UICollectionView

public:
    inline const Rect& getFrame() const { return m_frame; }
    inline Rect& getFrame() { return m_frame; }

    inline int getFirstItem() const { return m_items.empty() ? -1 : m_items.front()->getItem(); }
    inline int getLastItem() const { return m_items.empty() ? -1 : m_items.back()->getItem(); }

    inline bool hasItem() const { return !m_items.empty(); }
    inline void addItem(FlexItem *item, bool vertical)
    {
        m_items.push_back(item);
        if (vertical)
        {
            m_frame.size.width += item->getFrame().size.width;
            if (m_frame.size.height < item->getFrame().size.height) m_frame.size.height = item->getFrame().size.height;
        }
        else
        {
            m_frame.size.height += item->getFrame().size.height;
            if (m_frame.size.width < item->getFrame().size.width) m_frame.size.width = item->getFrame().size.width;
        }
    }

    template <typename TCompare>
    bool filter(std::vector<FlexItem *> &items, const Rect &rect,  const TCompare &compare) const
    {
        std::pair<FlexItem::Iterator, FlexItem::Iterator> range = std::equal_range(m_items.begin(), m_items.end(), rect, compare);

        bool matched = (range.first != range.second);
        if (matched)
        {
            items.insert(items.end(), range.first, range.second);
        }

        return matched;
    }

    template <typename TCompare, typename TRCompare>
    bool filter(std::vector<FlexItem *> &items, const Rect &rect,  const TCompare &compare1, TRCompare compare2) const
    {
        std::pair<FlexItem::Iterator, FlexItem::Iterator> range = std::equal_range(m_items.begin(), m_items.end(), rect, compare1);

        for (FlexItem::Iterator it = range.first; it != range.second; ++it)
        {
            if (compare2(*it, rect))
            {
                items.push_back(*it);
            }
        }

        return (range.first != range.second);
    }
};

struct FlexRowItemCompare
{
    bool operator() ( const FlexRow* row, int item) const
    {
        return row->getLastItem() < item;
    }
    bool operator() ( int item, const FlexRow* row ) const
    {
        return item < row->getFirstItem();
    }
};

typedef FlexVerticalCompareT<FlexRow> FlexRowVerticalCompare;
typedef FlexHorizontalCompareT<FlexRow> FlexRowHorizontalCompare;


class FlexColumn
{
private:
    std::vector<FlexItem *> m_items;

    Rect m_frame; // The origin is in the coordinate system of section, should convert to the coordinate system of CollectionView

public:
    FlexColumn() : m_frame() { }

    FlexColumn(int itemCapacity) { m_items.reserve(itemCapacity); }

    inline const Rect getFrame() const { return m_frame; }
    inline Rect& getFrame() { return m_frame; }
    inline int getFirstItem() const { return m_items.empty() ? -1 : m_items.front()->getItem(); }
    inline int getLastItem() const { return m_items.empty() ? -1 : m_items.back()->getItem(); }

    inline bool hasItem() const { return !m_items.empty(); }
    inline void addItem(FlexItem *item, bool vertical)
    {
        m_items.push_back(item);

        // vertical ? (m_size.height += item->m_size.height) : m_size.width += item->m_size.width;
        vertical ? (m_frame.size.height = item->getFrame().origin.y + item->getFrame().size.height - (*(m_items.begin()))->getFrame().origin.y) : (m_frame.size.width = item->getFrame().origin.x + item->getFrame().size.width - (*(m_items.begin()))->getFrame().origin.x);
    }


    template <typename TCompare>
    bool filter(std::vector<FlexItem *> &items, const Rect &rect,  const TCompare &compare) const
    {
        std::pair<FlexItem::Iterator, FlexItem::Iterator> range = std::equal_range(m_items.begin(), m_items.end(), rect, compare);

        bool matched = (range.first != range.second);
        if (matched)
        {
            items.insert(items.end(), range.first, range.second);
        }

        return matched;
    }

    template <typename TCompare, typename TRCompare>
    bool filter(std::vector<FlexItem *> &items, const Rect &rect,  const TCompare &compare1, TRCompare compare2) const
    {
        std::pair<FlexItem::Iterator, FlexItem::Iterator> range = std::equal_range(m_items.begin(), m_items.end(), rect, compare1);

        for (FlexItem::Iterator it = range.first; it != range.second; ++it)
        {
            if (compare2(*it, rect))
            {
                items.push_back(*it);
            }
        }

        return (range.first != range.second);
    }
    /*
    inline std::pair<std::vector<FlexItem *>::iterator, std::vector<FlexItem *>::iterator> getVirticalItemsInRect(const Rect& rect)
    {
        return std::equal_range(m_items.begin(), m_items.end(), std::pair<int, int>(rect.origin.y, rect.origin.y + rect.size.height), FlexItemVerticalCompare());
    }

    inline std::pair<std::vector<FlexItem *>::iterator, std::vector<FlexItem *>::iterator> getHorizontalItemsInRect(const Rect& rect)
    {
        return std::equal_range(m_items.begin(), m_items.end(), std::pair<int, int>(rect.origin.x, rect.origin.x + rect.size.width), FlexItemHorizontalCompare());
    }
     */
};

struct FlexColumnItemCompare
{
    bool operator() ( const FlexColumn* column, int item) const
    {
        return column->getLastItem() < item;
    }
    bool operator() ( int item, const FlexColumn* column ) const
    {
        return item < column->getFirstItem();
    }
};

typedef FlexSizeHorizontalCompareT<FlexColumn> FlexColumnSizeHorizontalCompare;
typedef FlexSizeVerticalCompareT<FlexColumn> FlexColumnSizeVerticalCompare;


#endif //FLEXLAYOUTMANAGER_FLEXNODES_H
