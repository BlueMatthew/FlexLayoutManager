//
// Created by Matthew Shi on 2020-07-12.
//

#include <jni.h>
#include "LayoutCallbackAdapter.h"

#define USING_CACHE_ON_ITEMSIZE
#define CACHE_NUMBER_OF_ITEMS   1024

LayoutCallbackAdapter::LayoutCallbackAdapter(JNIEnv* env, jobject obj, jobject callback) : m_env(env), m_cachedItemStart(0)
{
    m_layoutMnager = m_env->NewGlobalRef(obj);
    m_callback = m_env->NewGlobalRef(callback);

    m_layoutManagerClass = (jclass)(env->NewGlobalRef(env->GetObjectClass(obj)));
    m_callbackClass = (jclass)(env->NewGlobalRef(env->GetObjectClass(callback)));
    m_pointClass = (jclass)(env->NewGlobalRef(env->FindClass("android/graphics/Point")));

    m_constructorOfSizeMid = env->GetMethodID(m_pointClass, "<init>", "(II)V");
    // m_layoutMnagerClass = env->GetObjectClass(obj);
    // m_callbackClass = env->GetObjectClass(callback);

    jclass sizeClass = m_env->FindClass("org/wakin/flexlayout/LayoutManager/Size");

    m_sizeWidthFid = m_env->GetFieldID(sizeClass, "width", "I");
    m_sizeHeightFid = m_env->GetFieldID(sizeClass, "height", "I");

    jmethodID constructorMid = env->GetMethodID(sizeClass, "<init>", "()V");

    m_itemSize = m_env->NewGlobalRef(m_env->NewObject(sizeClass, constructorMid));

    m_env->DeleteLocalRef(sizeClass);

    m_itemSizeMid = m_env->GetMethodID(m_callbackClass, "getSizeForItem", "(IILorg/wakin/flexlayout/LayoutManager/Size;)V");
    m_fullSpanMid = m_env->GetMethodID(m_callbackClass, "isFullSpanAtItem", "(II)Z");
    m_itemDataMid = m_env->GetMethodID(m_callbackClass, "getInfoForItemsBatchly", "(III[I)I");
}

LayoutCallbackAdapter::~LayoutCallbackAdapter()
{
    m_env->DeleteGlobalRef(m_itemSize);

    m_env->DeleteGlobalRef(m_pointClass);
    m_env->DeleteGlobalRef(m_callbackClass);
    m_env->DeleteGlobalRef(m_layoutManagerClass);

    m_env->DeleteGlobalRef(m_callback);
    m_env->DeleteGlobalRef(m_layoutMnager);
}

void LayoutCallbackAdapter::updateContentHeight(int height)
{
    jfieldID fid = m_env->GetFieldID(m_layoutManagerClass, "mContentSizeHeight", "I");
    m_env->SetIntField(m_layoutMnager, fid, height);
}

bool LayoutCallbackAdapter::isVertical() const
{
    jmethodID mid = m_env->GetMethodID(m_layoutManagerClass, "getOrientation", "()I");
    jint orientation = m_env->CallIntMethod(m_layoutMnager, mid);
    return orientation == 1;
}

int LayoutCallbackAdapter::getNumberOfSections() const
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "getNumberOfSections", "()I");
    jint numberOfSections = m_env->CallIntMethod(m_callback, mid);
    return static_cast<int>(numberOfSections);
}

int LayoutCallbackAdapter::getNumberOfItemsInSection(int section) const
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "getNumberOfItemsInSection", "(I)I");
    jint numberOfItems = m_env->CallIntMethod(m_callback, mid, section);
    return numberOfItems;
}

Insets LayoutCallbackAdapter::getInsetForSection(int section) const
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "getInsetsForSection", "(I)Lorg/wakin/flexlayout/LayoutManager/Insets;");
    jobject javaInsets = m_env->CallObjectMethod(m_callback, mid, section);

    jclass insetsClass = m_env->GetObjectClass(javaInsets);
    jfieldID fid = m_env->GetFieldID(insetsClass, "left", "I");

    Insets insets;
    insets.left = m_env->GetIntField(javaInsets, fid);

    fid = m_env->GetFieldID(insetsClass, "top", "I");
    insets.top = m_env->GetIntField(javaInsets, fid);

    fid = m_env->GetFieldID(insetsClass, "right", "I");
    insets.right = m_env->GetIntField(javaInsets, fid);

    fid = m_env->GetFieldID(insetsClass, "bottom", "I");
    insets.bottom = m_env->GetIntField(javaInsets, fid);

    return insets;
}

int LayoutCallbackAdapter::getMinimumLineSpacingForSection(int section) const
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "getMinimumLineSpacingForSection", "(I)I");
    return m_env->CallIntMethod(m_callback, mid, section);
}

int LayoutCallbackAdapter::getMinimumInteritemSpacingForSection(int section) const
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "getMinimumInteritemSpacingForSection", "(I)I");
    return m_env->CallIntMethod(m_callback, mid, section);
}

Size LayoutCallbackAdapter::getSizeForHeaderInSection(int section) const
{
    return Size();
}

Size LayoutCallbackAdapter::getSizeForFooterInSection(int section) const
{
    return Size();
}

Size LayoutCallbackAdapter::getSizeForItem(int section, int item, bool *isFullSpan) const
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

bool LayoutCallbackAdapter::hasFixedItemSize(int section, Size *fixedItemSize)
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "hasFixedItemSize", "(ILorg/wakin/flexlayout/LayoutManager/Size;)Z");
    jboolean result = m_env->CallBooleanMethod(m_callback, mid, section, m_itemSize);
    if (1 == result && NULL != fixedItemSize)
    {
        fixedItemSize->width = m_env->GetIntField(m_itemSize, m_sizeWidthFid);
        fixedItemSize->height = m_env->GetIntField(m_itemSize, m_sizeHeightFid);
    }
    return result == 1;
}

int LayoutCallbackAdapter::getInfoForItemsBatchly(int section, int itemStart, int itemCount, std::pair<Size, int> &items)
{
    return 0;
}

int LayoutCallbackAdapter::getNumberOfColumnsForSection(int section) const
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "getNumberOfColumnsForSection", "(I)I");
    return m_env->CallIntMethod(m_callback, mid, section);
}

int LayoutCallbackAdapter::getLayoutModeForSection(int section) const
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "getLayoutModeForSection", "(I)I");
    return m_env->CallIntMethod(m_callback, mid, section);
}

// int getPageSize();

void LayoutCallbackAdapter::onItemEnterStickyMode(int section, int item, int position, Point point)
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "onItemEnterStickyMode", "(IIILandroid/graphics/Point;)V");
    jobject javaPoint = m_env->NewObject(m_pointClass, m_constructorOfSizeMid, point.x, point.y);
    m_env->CallVoidMethod(m_callback, mid, section, item, position, javaPoint);
    m_env->DeleteLocalRef(javaPoint);
}

void LayoutCallbackAdapter::onItemExitStickyMode(int section, int item, int position)
{
    jmethodID mid = m_env->GetMethodID(m_callbackClass, "onItemExitStickyMode", "(III)V");
    m_env->CallVoidMethod(m_callback, mid, section, item, position);
}

void LayoutCallbackAdapter::beginPreparingLayout(int cacheSize)
{
    m_cachedSectionIndex = -1;
    m_cachedItemStart = -1;
    m_cachedItemCount = 0;
    m_cachedBufferSize = cacheSize;
    m_cachedBuffer = new int[m_cachedBufferSize * 3];
    m_cachedJavaBuffer = (jintArray)m_env->NewGlobalRef(m_env->NewIntArray(m_cachedBufferSize * 3));
}

void LayoutCallbackAdapter::endPreparingLayout()
{
    m_env->DeleteGlobalRef(m_cachedJavaBuffer);
    m_cachedJavaBuffer = NULL;
    delete[] m_cachedBuffer;
    m_cachedBuffer = NULL;

    m_cachedSectionIndex = -1;
    m_cachedItemStart = -1;
    m_cachedItemCount = 0;
}