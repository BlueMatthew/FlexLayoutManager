//
// Created by Matthew on 2020-07-03.
//

#include "FlexLayout.h"

FlexSectionCallbackImpl::~FlexSectionCallbackImpl()
{

}

bool FlexSectionCallbackImpl::isVertical() const
{
    return true;
}

Size FlexSectionCallbackImpl::getParentBounds() const
{
    return Size();
}

Insets FlexSectionCallbackImpl::getParentInsets() const
{
    return Insets();
}

int FlexSectionCallbackImpl::getSection() const
{
    return 0;
}

int FlexSectionCallbackImpl::getPosition() const
{
    return 0;
}

int FlexSectionCallbackImpl::getNumberOfItems() const
{
    return 0;
}

int FlexSectionCallbackImpl::getNumberOfColumns() const
{
    return 0;
}

void FlexSectionCallbackImpl::getInsets(Insets &insets) const
{

}

int FlexSectionCallbackImpl::getMinimumLineSpacing() const
{
    return 0;
}

int FlexSectionCallbackImpl::getMinimumInteritemSpacing() const
{
    return 0;
}
bool FlexSectionCallbackImpl::hasFixedItemSize(Size &size) const
{
    return false;
}

void FlexSectionCallbackImpl::getItemSize(int item, Size &size) const
{

}

void FlexSectionCallbackImpl::toParentRect(Rect &rect) const
{

}