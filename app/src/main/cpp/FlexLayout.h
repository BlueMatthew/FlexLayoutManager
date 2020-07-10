//
// Created by Matthew on 2020-07-03.
//

#ifndef FLEXLAYOUTMANAGER_FLEXLAYOUT_H
#define FLEXLAYOUTMANAGER_FLEXLAYOUT_H

#include "FlexNodes.h"
#include "FlexSection.h"

class FlexSectionCallbackImpl : FlexSectionCallback
{
public:
    ~FlexSectionCallbackImpl();

    bool isVertical() const;
    Size getParentBounds() const;
    Insets getParentInsets() const;

    int getSection() const;
    int getPosition() const;
    int getNumberOfItems() const;
    int getNumberOfColumns() const;
    void getInsets(Insets &insets) const;
    int getMinimumLineSpacing() const;
    int getMinimumInteritemSpacing() const;
    bool hasFixedItemSize(Size &size) const;
    void getItemSize(int item, Size &size) const;

    void toParentRect(Rect &rect) const;
};

class FlexLayout
{
private:

};


#endif //FLEXLAYOUTMANAGER_FLEXLAYOUT_H
