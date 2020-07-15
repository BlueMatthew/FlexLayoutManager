//
// Created by Matthew Shi on 2020-07-12.
//

#include <jni.h>
#include "LayoutAdapter.h"

#define USING_CACHE_ON_ITEMSIZE
#define CACHE_NUMBER_OF_ITEMS   1024

LayoutAdapter::LayoutAdapter(JNIEnv* env, jobject obj, jobject callback) : m_env(env), m_layoutInfo(NULL), m_cachedItemStart(0)
{
    m_layoutMnager = m_env->NewGlobalRef(obj);
    m_callback = m_env->NewGlobalRef(callback);

    jclass layoutManagerClass = m_env->GetObjectClass(obj);

    // Methods on LayoutManager
    m_setContentSizeMid = m_env->GetMethodID(layoutManagerClass, "setContentSize", "(II)V");
    m_orientationMid = m_env->GetMethodID(layoutManagerClass, "getOrientation", "()I");
    m_env->DeleteLocalRef(layoutManagerClass);

    jclass callbackClass = m_env->GetObjectClass(callback);
    // Methods on LayoutCallback
    m_layoutModeMid = m_env->GetMethodID(callbackClass, "getLayoutModeForSection", "(I)I");
    m_numberOfSectionsMid = m_env->GetMethodID(callbackClass, "getNumberOfSections", "()I");
    m_numberOfItemsMid = m_env->GetMethodID(callbackClass, "getNumberOfItemsInSection", "(I)I");
    m_numberOfColumnsMid = m_env->GetMethodID(callbackClass, "getNumberOfColumnsForSection", "(I)I");
    m_lineSpacingMid = m_env->GetMethodID(callbackClass, "getMinimumLineSpacingForSection", "(I)I");
    m_insetsMid = m_env->GetMethodID(callbackClass, "getInsetsForSection", "(I)Lorg/wakin/flexlayout/LayoutManager/Insets;");
    m_interitemSpacingMid = m_env->GetMethodID(callbackClass, "getMinimumInteritemSpacingForSection", "(I)I");
    m_itemEnterStickyModeMid = m_env->GetMethodID(callbackClass, "onItemEnterStickyMode", "(IIILandroid/graphics/Point;)V");
    m_itemExitStickyModeMid = m_env->GetMethodID(callbackClass, "onItemExitStickyMode", "(III)V");
    m_hasFixItemSizeMid = m_env->GetMethodID(callbackClass, "hasFixedItemSize", "(ILorg/wakin/flexlayout/LayoutManager/Size;)Z");
    m_itemSizeMid = m_env->GetMethodID(callbackClass, "getSizeForItem", "(IILorg/wakin/flexlayout/LayoutManager/Size;)V");
    m_fullSpanMid = m_env->GetMethodID(callbackClass, "isFullSpanAtItem", "(II)Z");
    m_itemDataMid = m_env->GetMethodID(callbackClass, "getInfoForItemsBatchly", "(III[I)I");
    m_env->DeleteLocalRef(callbackClass);

    // android/graphics/Point
    m_pointClass = findGlobalClass("android/graphics/Point");
    m_pointConstructorMid = env->GetMethodID(m_pointClass, "<init>", "(II)V");

    // org/wakin/flexlayout/LayoutManager/Size
    jclass sizeClass = m_env->FindClass("org/wakin/flexlayout/LayoutManager/Size");

    m_sizeSetMid = m_env->GetMethodID(sizeClass, "set", "(II)V");
    m_sizeWidthFid = m_env->GetFieldID(sizeClass, "width", "I");
    m_sizeHeightFid = m_env->GetFieldID(sizeClass, "height", "I");
    jmethodID sizeConstructorMid = env->GetMethodID(sizeClass, "<init>", "()V");

    m_itemSize = m_env->NewGlobalRef(m_env->NewObject(sizeClass, sizeConstructorMid));

    m_env->DeleteLocalRef(sizeClass);

    jclass insetsClass = m_env->FindClass("org/wakin/flexlayout/LayoutManager/Insets");
    m_insetsLeftFid = m_env->GetFieldID(insetsClass, "left", "I");
    m_insetsTopFid = m_env->GetFieldID(insetsClass, "top", "I");
    m_insetsRightFid = m_env->GetFieldID(insetsClass, "right", "I");
    m_insetsBottomFid = m_env->GetFieldID(insetsClass, "bottom", "I");
    m_env->DeleteLocalRef(insetsClass);

    // java/util/List
    jclass listClass = (jclass)env->FindClass("java/util/List");
    m_listAddMid = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    env->DeleteLocalRef(listClass);

    // org/wakin/flexlayout/LayoutManager/LayoutItem
    m_layoutItemClass = findGlobalClass("org/wakin/flexlayout/LayoutManager/LayoutItem");
    m_layoutItemConstuctMid = env->GetMethodID(m_layoutItemClass, "<init>", "(IIIZIIII)V");
    m_layoutItemSetOrgPtMid = env->GetMethodID(m_layoutItemClass, "setOriginalPoint", "(II)V");
}

LayoutAdapter::~LayoutAdapter()
{
    m_env->DeleteGlobalRef(m_layoutItemClass);
    m_env->DeleteGlobalRef(m_pointClass);

    if (NULL != m_cachedJavaBuffer)
    {
        m_env->DeleteGlobalRef(m_cachedJavaBuffer);
    }
    if (NULL != m_cachedBuffer)
    {
        delete[] m_cachedBuffer;
        m_cachedBuffer = NULL;
    }

    m_env->DeleteGlobalRef(m_itemSize);
    m_env->DeleteGlobalRef(m_callback);
    m_env->DeleteGlobalRef(m_layoutMnager);
}

void LayoutAdapter::updateContentSize(int width, int height)
{
    m_env->CallVoidMethod(m_layoutMnager, m_setContentSizeMid, width, height);
}

bool LayoutAdapter::isVertical() const
{
    if (NULL != m_layoutInfo)
    {
        return 1 == m_layoutInfo->orientation;
    }
    jint orientation = m_env->CallIntMethod(m_layoutMnager, m_orientationMid);
    return orientation == 1;
}

int LayoutAdapter::getNumberOfSections() const
{
    if (NULL != m_layoutInfo)
    {
        return m_layoutInfo->numberOfSections;
    }
    return m_env->CallIntMethod(m_callback, m_numberOfSectionsMid);
}

int LayoutAdapter::getNumberOfItemsInSection(int section) const
{
    if (NULL != m_layoutInfo)
    {
        return m_layoutInfo->sections[section].numberOfItems;
    }
    return m_env->CallIntMethod(m_callback, m_numberOfItemsMid, section);
}

Insets LayoutAdapter::getInsetForSection(int section) const
{
    if (NULL != m_layoutInfo)
    {
        return m_layoutInfo->sections[section].padding;
    }
    jobject javaInsets = m_env->CallObjectMethod(m_callback, m_insetsMid, section);

    Insets insets;
    insets.left = m_env->GetIntField(javaInsets, m_insetsLeftFid);
    insets.top = m_env->GetIntField(javaInsets, m_insetsTopFid);
    insets.right = m_env->GetIntField(javaInsets, m_insetsRightFid);
    insets.bottom = m_env->GetIntField(javaInsets, m_insetsBottomFid);

    m_env->DeleteLocalRef(javaInsets);

    return insets;
}

int LayoutAdapter::getMinimumLineSpacingForSection(int section) const
{
    if (NULL != m_layoutInfo)
    {
        return m_layoutInfo->sections[section].lineSpacing;
    }
    return m_env->CallIntMethod(m_callback, m_lineSpacingMid, section);
}

int LayoutAdapter::getMinimumInteritemSpacingForSection(int section) const
{
    if (NULL != m_layoutInfo)
    {
        return m_layoutInfo->sections[section].interitemSpacing;
    }
    return m_env->CallIntMethod(m_callback, m_interitemSpacingMid, section);
}

Size LayoutAdapter::getSizeForHeaderInSection(int section) const
{
    // No Header in RecyclerView
    return Size();
}

Size LayoutAdapter::getSizeForFooterInSection(int section) const
{
    // No Footer in RecyclerView
    return Size();
}

Size LayoutAdapter::getSizeForItem(int section, int item, bool *isFullSpan) const
{
#ifdef USING_CACHE_ON_ITEMSIZE
    if (section != m_cachedSectionIndex || item >= m_cachedItemStart + m_cachedItemCount)
    {
        // int getInfoForItemsBatchly(int section, int itemStart, int itemCount, int[] data);
        jint itemCountReceived = m_env->CallIntMethod(m_callback, m_itemDataMid, section, item, m_cachedBufferSize, m_cachedJavaBuffer);
        if (itemCountReceived > 0)
        {
            m_cachedSectionIndex = section;
            m_cachedItemStart = item;
            m_cachedItemCount = itemCountReceived;

            m_env->GetIntArrayRegion(m_cachedJavaBuffer, 0, itemCountReceived * 3, m_cachedBuffer);
        }
        else
        {
            m_cachedSectionIndex = -1;
            m_cachedItemStart = -1;
            m_cachedItemCount = 0;
        }
    }

    if (section == m_cachedSectionIndex && item < m_cachedItemStart + m_cachedItemCount)
    {
        int offset = 3 * (item - m_cachedItemStart);
        if (NULL != isFullSpan)
        {
            *isFullSpan = (m_cachedBuffer[offset + 2] == 1) ? true : false;
        }

        return Size(m_cachedBuffer[offset], m_cachedBuffer[offset + 1]);
    }

#endif // USING_CACHE_ON_ITEMSIZE

    m_env->CallVoidMethod(m_callback, m_itemSizeMid, section, item, m_itemSize);

    Size size(m_env->GetIntField(m_itemSize, m_sizeWidthFid),
                m_env->GetIntField(m_itemSize, m_sizeHeightFid));

    if (NULL != isFullSpan)
    {
        jboolean result = m_env->CallBooleanMethod(m_callback, m_fullSpanMid, section, item);
        *isFullSpan = (result == 1) ? true : false;
    }

    return size;
}

bool LayoutAdapter::hasFixedItemSize(int section, Size *fixedItemSize)
{
    if (NULL != m_layoutInfo)
    {
        if (m_layoutInfo->sections[section].hasFixedItemSize && NULL != fixedItemSize)
        {
            *fixedItemSize = m_layoutInfo->sections[section].fixedItemSize;
        }
        return m_layoutInfo->sections[section].hasFixedItemSize;
    }

    jboolean result = m_env->CallBooleanMethod(m_callback, m_hasFixItemSizeMid, section, m_itemSize);
    if (1 == result && NULL != fixedItemSize)
    {
        fixedItemSize->width = m_env->GetIntField(m_itemSize, m_sizeWidthFid);
        fixedItemSize->height = m_env->GetIntField(m_itemSize, m_sizeHeightFid);
    }
    return result == 1;
}

int LayoutAdapter::getNumberOfColumnsForSection(int section) const
{
    if (NULL != m_layoutInfo)
    {
        return m_layoutInfo->sections[section].numberOfColumns;
    }
    return m_env->CallIntMethod(m_callback, m_numberOfColumnsMid, section);
}

int LayoutAdapter::getLayoutModeForSection(int section) const
{
    if (NULL != m_layoutInfo)
    {
        return m_layoutInfo->sections[section].layoutMode;
    }
    return m_env->CallIntMethod(m_callback, m_layoutModeMid, section);
}

// int getPageSize();

void LayoutAdapter::onItemEnterStickyMode(int section, int item, int position, Point point) const
{
    jobject javaPoint = m_env->NewObject(m_pointClass, m_pointConstructorMid, point.x, point.y);
    m_env->CallVoidMethod(m_callback, m_itemEnterStickyModeMid, section, item, position, javaPoint);
    m_env->DeleteLocalRef(javaPoint);
}

void LayoutAdapter::onItemExitStickyMode(int section, int item, int position) const
{
    m_env->CallVoidMethod(m_callback, m_itemExitStickyModeMid, section, item, position);
}

void LayoutAdapter::beginPreparingLayout(const LayoutInfo *layoutInfo, int itemCacheSize)
{
    m_layoutInfo = layoutInfo;
    m_cachedSectionIndex = -1;
    m_cachedItemStart = -1;
    m_cachedItemCount = 0;
    m_cachedBufferSize = itemCacheSize;
    m_cachedBuffer = new int[m_cachedBufferSize * 3];
    m_cachedJavaBuffer = (jintArray)m_env->NewGlobalRef(m_env->NewIntArray(m_cachedBufferSize * 3));
}

void LayoutAdapter::endPreparingLayout()
{
    m_env->DeleteGlobalRef(m_cachedJavaBuffer);
    m_cachedJavaBuffer = NULL;
    delete[] m_cachedBuffer;
    m_cachedBuffer = NULL;

    m_cachedSectionIndex = -1;
    m_cachedItemStart = -1;
    m_cachedItemCount = 0;
    m_layoutInfo = NULL;
}

void LayoutAdapter::addLayoutItem(jobject javaList, const LayoutItem &item) const
{
    jobject javaLayoutItem = m_env->NewObject(m_layoutItemClass, m_layoutItemConstuctMid, item.section, item.item, item.position, jboolean(item.data == 1 ? 1 : 0),
                                              item.frame.left(), item.frame.top(), item.frame.right(), item.frame.bottom());
    jboolean ret = m_env->CallBooleanMethod(javaList, m_listAddMid, javaLayoutItem);
    m_env->DeleteLocalRef(javaLayoutItem);
}