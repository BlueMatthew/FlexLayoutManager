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

using Point = nsflex::PointT<jint>;
using Size = nsflex::SizeT<jint>;
using Rect = nsflex::RectT<jint>;
using Insets = nsflex::InsetsT<jint>;

using FlexItem = nsflex::FlexItemT<jint, jint>;
using LayoutItem = RecyclerLayoutItemT<jint, jint>;

using StickySectionItem = RecyclerSectionItemT<jint, (char)(FlexItem::ITEM_TYPE_ITEM)>;
using StickyItemState = StickyItemStateT<jint>;
using StickyItem = StickyItemT<StickySectionItem, StickyItemState>;

struct SectionInfo
{
    jint section;    // For incremental update
    jint position;
    Insets padding;
    jint numberOfItems;
    jint numberOfColumns;
    jint layoutMode;
    jint lineSpacing;
    jint interitemSpacing;
    jint hasFixedItemSize;
    Size fixedItemSize;


    SectionInfo() : section(0), position(0), numberOfItems(0), numberOfColumns(0), layoutMode(0), lineSpacing(0), interitemSpacing(0), hasFixedItemSize(0)
    {
    }

    static int numberOfInts()
    {
        return 14;
    }

    inline int readFromBuffer(jint* buffer, int bufferLength)
    {
        if (bufferLength < numberOfInts())
        {
            return 0;
        }

        int offset = 0;
        section = buffer[offset++];
        position = buffer[offset++];
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
    jint orientation;
    Size size;
    Insets padding;
    // Size contentSize;
    Point contentOffset;

    jint page;
    jint numberOfPages;
    jint numberOfFixedSections;
    std::vector<jint> numberOfPageSections;
    jint numberOfPendingPages;
    std::vector<jint> pendingPages;

    LayoutInfo() : orientation(1)
    {
    }

    static int numberOfInts()
    {
        return 14; //1 + 2 + 4 + 2 + 2 + 1 + 1 + 1 + numberOfPages
    }

    inline int read(jint* buffer, int bufferLength)
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

        page = buffer[offset++];
        numberOfPages = buffer[offset++];
        numberOfFixedSections = buffer[offset++];
        numberOfPageSections.clear();
        if (numberOfPages > 0)
        {
            if (bufferLength - offset < numberOfPages)
            {
                return 0;
            }

            numberOfPageSections.reserve(numberOfPages);
            for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++)
            {
                jint val = buffer[offset++];
                numberOfPageSections.push_back(val);
            }
        }

        numberOfPendingPages = buffer[offset++];
        pendingPages.clear();
        if (numberOfPendingPages > 0)
        {
            if (bufferLength - offset < numberOfPendingPages)
            {
                return 0;
            }

            std::vector<jint> pages;
            pages.reserve(numberOfPendingPages);
            for (int pageIndex = 0; pageIndex < numberOfPendingPages; pageIndex++)
            {
                int val = buffer[offset++];
                pages.push_back(val);
            }

            pendingPages.reserve(numberOfPendingPages);
            for (std::vector<jint>::const_iterator it = pages.cbegin(); it != pages.cend(); ++it)
            {
                if (*it == page)
                {
                    pendingPages.push_back(page);
                    break;
                }
            }
            for (std::vector<jint>::const_iterator it = pages.cbegin(); it != pages.cend(); ++it)
            {
                if (*it != page)
                {
                    pendingPages.push_back(page);
                }
            }
        }

        return offset;
    }
};


struct LayoutAndSectionsInfo : public LayoutInfo
{
    jint numberOfSections;
    jint sectionStart;

    std::vector<SectionInfo> sections;

    LayoutAndSectionsInfo() : LayoutInfo(), numberOfSections(0), sectionStart(0)
    {

    }

    static int numberOfInts()
    {
        return LayoutInfo::numberOfInts() + 2;
    }

    inline int read(jint* buffer, int bufferLength)
    {
        int offset = 0;
        if (bufferLength < numberOfInts())
        {
            return 0;
        }

        int intsRead = LayoutInfo::read(buffer, bufferLength);
        if (intsRead == 0)
        {
            return 0;
        }
        offset += intsRead;

        if (bufferLength - offset < 2)
        {
            return offset;
        }

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

struct PageOffsetInfo
{
    jint page;
    Point offset;

    static int numberOfInts()
    {
        return 3;
    }

    inline int read(jint* buffer, int bufferLength)
    {
        if (bufferLength < numberOfInts())
        {
            return 0;
        }

        int offset = 0;
        page = buffer[offset++];
        this->offset.x = buffer[offset++];
        this->offset.y = buffer[offset++];

        return offset;
    }
};

struct DisplayInfo {
    jint orientation;
    Size size;
    Insets padding;
    Size contentSize;
    Point contentOffset;

    jint page;
    jint numberOfPages;
    jint numberOfFixedSections;
    std::vector<jint> numberOfPageSections;
    jint pagingOffset;
    jint numberOfPendingPages;
    std::vector<std::pair<jint, Point>> pendingPages;

    DisplayInfo() : orientation(1)
    {
    }

    static int numberOfInts()
    {
        return 14; //1 + 2 + 4 + 2 + 2 + 1 + 1 + 1 + numberOfPages
    }

    inline int read(jint* buffer, int bufferLength)
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
        contentSize.width = buffer[offset++];
        contentSize.height = buffer[offset++];
        contentOffset.x = buffer[offset++];
        contentOffset.y = buffer[offset++];

        page = buffer[offset++];
        numberOfPages = buffer[offset++];
        numberOfFixedSections = buffer[offset++];
        numberOfPageSections.clear();
        if (numberOfPages > 0)
        {
            if (bufferLength - offset < numberOfPages)
            {
                return 0;
            }

            numberOfPageSections.reserve(numberOfPages);
            for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++)
            {
                jint val = buffer[offset++];
                numberOfPageSections.push_back(val);
            }
        }

        pagingOffset = buffer[offset++];
        numberOfPendingPages = buffer[offset++];
        pendingPages.clear();
        if (numberOfPendingPages > 0)
        {
            if (bufferLength - offset < numberOfPendingPages)
            {
                return 0;
            }

            std::vector<std::pair<jint, Point>> pages;
            pages.reserve(numberOfPendingPages);
            pendingPages.reserve(numberOfPendingPages);

            Point contentOffset;
            for (int pageIndex = 0; pageIndex < numberOfPendingPages; pageIndex++)
            {
                jint val = buffer[offset++];
                contentOffset.x = buffer[offset++];
                contentOffset.y = buffer[offset++];
                pages.push_back(std::make_pair(val, contentOffset));
            }

            for (typename std::vector<std::pair<jint, Point>>::const_iterator it = pages.cbegin(); it != pages.cend(); ++it)
            {
                if (it->first == page)
                {
                    pendingPages.push_back(*it);
                    break;
                }
            }
            for (typename std::vector<std::pair<jint, Point>>::const_iterator it = pages.cbegin(); it != pages.cend(); ++it)
            {
                if (it->first != page)
                {
                    pendingPages.push_back(*it);
                }
            }
        }

        return offset;
    }
};

template<typename T>
inline void writeToBuffer(std::vector<jint> &buffer, const T &t)
{
}

template<>
inline void writeToBuffer(std::vector<jint> &buffer, const Rect &rect)
{
    buffer.push_back(rect.left());
    buffer.push_back(rect.top());
    buffer.push_back(rect.width());
    buffer.push_back(rect.height());
}

template<>
inline void writeToBuffer(std::vector<jint> &buffer, const LayoutItem &layoutItem)
{
    buffer.push_back(layoutItem.getPage());
    buffer.push_back(layoutItem.getSection());
    buffer.push_back(layoutItem.getItem());
    buffer.push_back(layoutItem.getPosition());
    writeToBuffer(buffer, layoutItem.getFrame());

    buffer.push_back(layoutItem.isInSticky() ? 1 : 0);
    buffer.push_back(layoutItem.isOriginChanged() ? 1 : 0);
}

template<>
inline void writeToBuffer(std::vector<jint> &buffer, const StickyItem &stickyItem)
{
    buffer.push_back(stickyItem.first.getSection());
    buffer.push_back(stickyItem.first.getItem());
    buffer.push_back(stickyItem.first.getPosition());
    buffer.push_back(stickyItem.second.isInSticky() ? 1 : 0);
    buffer.push_back(stickyItem.second.isOriginChanged() ? 1 : 0);
    writeToBuffer(buffer, stickyItem.second.getFrame());
}


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUTOBJECTS_H
