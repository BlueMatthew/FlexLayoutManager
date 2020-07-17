//
// Created by Matthew Shi on 2020-07-12.
//

#include <jni.h>
#include "LayoutCallbackAdapter.h"

#define USING_CACHE_ON_ITEMSIZE
#define CACHE_NUMBER_OF_ITEMS   1024

#define JAVA_CLASS_POINT "android/graphics/Point"
#define JAVA_CLASS_SIZE "org/wakin/flexlayout/LayoutManager/Graphics/Size"


jmethodID LayoutCallbackAdapter::m_setContentSizeMid = NULL;
jmethodID LayoutCallbackAdapter::m_itemEnterStickyModeMid = NULL;
jmethodID LayoutCallbackAdapter::m_itemExitStickyModeMid = NULL;
jmethodID LayoutCallbackAdapter::m_itemSizeMid = NULL;
jmethodID LayoutCallbackAdapter::m_fullSpanMid = NULL;
jmethodID LayoutCallbackAdapter::m_itemDataMid = NULL;

jmethodID LayoutCallbackAdapter::m_sizeConstructorMid = NULL;
jmethodID LayoutCallbackAdapter::m_sizeSetMid = NULL;
jfieldID  LayoutCallbackAdapter::m_sizeWidthFid = NULL;
jfieldID  LayoutCallbackAdapter::m_sizeHeightFid = NULL;

jmethodID LayoutCallbackAdapter::m_pointConstructorMid = NULL;

void LayoutCallbackAdapter::initLayoutEnv(JNIEnv* env, jclass layoutManagerClass, jclass callbackClass)
{
    // Methods on LayoutManager
    m_setContentSizeMid = env->GetMethodID(layoutManagerClass, "setContentSize", "(II)V");
    // env->DeleteLocalRef(layoutManagerClass);

    // Methods on LayoutCallback
    m_itemEnterStickyModeMid = env->GetMethodID(callbackClass, "onItemEnterStickyMode", "(IIILandroid/graphics/Point;)V");
    m_itemExitStickyModeMid = env->GetMethodID(callbackClass, "onItemExitStickyMode", "(III)V");
    m_itemSizeMid = env->GetMethodID(callbackClass, "getSizeForItem", "(IILorg/wakin/flexlayout/LayoutManager/Graphics/Size;)V");
    m_fullSpanMid = env->GetMethodID(callbackClass, "isFullSpanAtItem", "(II)Z");
    m_itemDataMid = env->GetMethodID(callbackClass, "getInfoForItemsBatchly", "(III[I)I");

    // android/graphics/Point
    jclass pointClass = env->FindClass("android/graphics/Point");
    m_pointConstructorMid = env->GetMethodID(pointClass, "<init>", "(II)V");
    env->DeleteLocalRef(pointClass);

    // org/wakin/flexlayout/LayoutManager/Size
    jclass sizeClass = env->FindClass(JAVA_CLASS_SIZE);

    m_sizeSetMid = env->GetMethodID(sizeClass, "set", "(II)V");
    m_sizeWidthFid = env->GetFieldID(sizeClass, "width", "I");
    m_sizeHeightFid = env->GetFieldID(sizeClass, "height", "I");
    m_sizeConstructorMid = env->GetMethodID(sizeClass, "<init>", "()V");

    env->DeleteLocalRef(sizeClass);
}

jintArray LayoutCallbackAdapter::makeIntArray(JNIEnv* env, const std::vector<jint> &buffer)
{
    jintArray result = env->NewIntArray(buffer.size());
    env->SetIntArrayRegion(result, 0, buffer.size(), &(buffer[0]));
    return result;
}

LayoutCallbackAdapter::LayoutCallbackAdapter(JNIEnv* env, jobject obj, jobject callback, const LayoutAndSectionsInfo *layoutAndSectionsInfo) : m_env(env), m_layoutAndSectionsInfo(layoutAndSectionsInfo), m_cachedItemStart(0)
{
    m_layoutMnager = obj;
    m_callback = callback;

    // org/wakin/flexlayout/LayoutManager/Size
    jclass sizeClass = m_env->FindClass(JAVA_CLASS_SIZE);
    jmethodID sizeConstructorMid = env->GetMethodID(sizeClass, "<init>", "()V");

    jobject localSize = m_env->NewObject(sizeClass, sizeConstructorMid);
    m_itemSize = m_env->NewGlobalRef(localSize);

    m_env->DeleteLocalRef(localSize);
    m_env->DeleteLocalRef(sizeClass);

    m_cachedSectionIndex = -1;
    m_cachedItemStart = -1;
    m_cachedItemCount = 0;
    m_cachedBufferSize = 128;
    m_cachedBuffer = NULL;
    m_cachedJavaBuffer = NULL;
}

LayoutCallbackAdapter::~LayoutCallbackAdapter()
{
    if (NULL != m_cachedJavaBuffer)
    {
        m_env->DeleteGlobalRef(m_cachedJavaBuffer);
        m_cachedJavaBuffer = NULL;
    }
    if (NULL != m_cachedBuffer)
    {
        delete[] m_cachedBuffer;
        m_cachedBuffer = NULL;
    }

    if (NULL != m_itemSize)
    {
        m_env->DeleteLocalRef(m_itemSize);
        m_itemSize = NULL;
    }
}

void LayoutCallbackAdapter::updateContentSize(int width, int height) const
{
    m_env->CallVoidMethod(m_layoutMnager, m_setContentSizeMid, width, height);
}

bool LayoutCallbackAdapter::isVertical() const
{
    // ASSERT(NULL != m_layoutInfo)
    return 1 == m_layoutAndSectionsInfo->orientation;
}

int LayoutCallbackAdapter::getNumberOfSections() const
{
    // ASSERT(NULL != m_layoutInfo)
    return m_layoutAndSectionsInfo->numberOfSections;
}

int LayoutCallbackAdapter::getLayoutModeForSection(int section) const
{
    // ASSERT(NULL != m_layoutInfo)
    return m_layoutAndSectionsInfo->sections[section].layoutMode;
}

int LayoutCallbackAdapter::getNumberOfItemsInSection(int section) const
{
    return m_layoutAndSectionsInfo->sections[section].numberOfItems;
}

int LayoutCallbackAdapter::getNumberOfColumnsForSection(int section) const
{
    // ASSERT(NULL != m_layoutInfo)
    return m_layoutAndSectionsInfo->sections[section].numberOfColumns;
}


Insets LayoutCallbackAdapter::getInsetForSection(int section) const
{
    // ASSERT(NULL != m_layoutInfo)
    return m_layoutAndSectionsInfo->sections[section].padding;
}

int LayoutCallbackAdapter::getMinimumLineSpacingForSection(int section) const
{
    // ASSERT(NULL != m_layoutInfo)
    return m_layoutAndSectionsInfo->sections[section].lineSpacing;
}

int LayoutCallbackAdapter::getMinimumInteritemSpacingForSection(int section) const
{
    // ASSERT(NULL != m_layoutInfo)
    return m_layoutAndSectionsInfo->sections[section].interitemSpacing;
}

Size LayoutCallbackAdapter::getSizeForHeaderInSection(int section) const
{
    // No Header in RecyclerView
    return Size();
}

Size LayoutCallbackAdapter::getSizeForFooterInSection(int section) const
{
    // No Footer in RecyclerView
    return Size();
}

Size LayoutCallbackAdapter::getSizeForItem(int section, int item, bool *isFullSpan) const
{
#ifdef USING_CACHE_ON_ITEMSIZE
    if (section != m_cachedSectionIndex || item >= m_cachedItemStart + m_cachedItemCount)
    {
        if (NULL == m_cachedBuffer)
        {
            m_cachedBuffer = new int[m_cachedBufferSize * 3];
        }
        if (NULL == m_cachedJavaBuffer)
        {
            jintArray localIntArray = m_env->NewIntArray(m_cachedBufferSize * 3);
            m_cachedJavaBuffer = (jintArray)m_env->NewGlobalRef(localIntArray);
            m_env->DeleteLocalRef(localIntArray);
        }

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

    if (NULL == m_itemSize)
    {
        jclass sizeClass = m_env->FindClass(JAVA_CLASS_SIZE);
        m_itemSize = m_env->NewObject(sizeClass, m_sizeConstructorMid);
        m_env->DeleteLocalRef(sizeClass);
    }

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
    // ASSERT(NULL != m_layoutInfo)
    if (m_layoutAndSectionsInfo->sections[section].hasFixedItemSize && NULL != fixedItemSize)
    {
        *fixedItemSize = m_layoutAndSectionsInfo->sections[section].fixedItemSize;
    }
    return m_layoutAndSectionsInfo->sections[section].hasFixedItemSize;
}

// int getPageSize();
