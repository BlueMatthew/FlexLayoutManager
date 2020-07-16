//
// Created by Matthew Shi on 2020-07-12.
//

#ifndef FLEXLAYOUTMANAGER_LAYOUTCALLBACKADAPTER_H
#define FLEXLAYOUTMANAGER_LAYOUTCALLBACKADAPTER_H

#include "FlexLayoutObjects.h"
//
// Adapter for org.wakin.flexlayout.LayoutManager.LayoutCallback
// And RecyclerView.LayoutManager
class LayoutCallbackAdapter {
private:
    JNIEnv* m_env;
    jobject m_layoutMnager;
    jobject m_callback;

    // Cache
    const LayoutInfo    *m_layoutInfo;

    // Cache
    mutable jobject             m_itemSize;

    mutable int         m_cachedSectionIndex;
    mutable int         m_cachedItemStart;
    mutable int         m_cachedItemCount;
    mutable int         m_cachedBufferSize;
    mutable int*        m_cachedBuffer;
    mutable jintArray   m_cachedJavaBuffer;

    // LayoutCallback
    static jmethodID m_setContentSizeMid;
    static jmethodID m_itemEnterStickyModeMid;
    static jmethodID m_itemExitStickyModeMid;
    static jmethodID m_itemSizeMid;
    static jmethodID m_fullSpanMid;
    static jmethodID m_itemDataMid;

    // android/graphics/Point
    // jclass      m_pointClass;
    static jmethodID   m_pointConstructorMid;


    // org/wakin/flexlayout/LayoutManager/Graphics/Size
    static jmethodID    m_sizeConstructorMid;
    static jmethodID    m_sizeSetMid;
    static jfieldID     m_sizeWidthFid;
    static jfieldID     m_sizeHeightFid;

protected:
    inline jclass getGlobalObjectClass(jobject obj)
    {
        jclass localClass = m_env->GetObjectClass(obj);
        jclass globalClass = (jclass)m_env->NewGlobalRef(localClass);
        m_env->DeleteLocalRef(localClass);
        return globalClass;
    }

    inline jclass findGlobalClass(const char *className)
    {
        jclass localClass = m_env->FindClass(className);
        jclass globalClass = (jclass)(m_env->NewGlobalRef(localClass));
        m_env->DeleteLocalRef(localClass);
        return globalClass;
    }

public:

    LayoutCallbackAdapter(JNIEnv* env, jobject obj, jobject callback);
    LayoutCallbackAdapter(JNIEnv* env, jobject obj, jobject callback, const LayoutInfo *layoutInfo);
    ~LayoutCallbackAdapter();


    static void initLayoutEnv(JNIEnv* env, jclass layoutManagerClass, jclass callbackClass);
    // static void addLayoutItem(jobject javaList, const LayoutItem &item);

public:
    void updateContentSize(int width, int height) const;

    bool isVertical() const;
    int getNumberOfSections() const;

    int getNumberOfItemsInSection(int section) const;
    int getNumberOfColumnsForSection(int section) const;
    Insets getInsetForSection(int section) const;
    int getMinimumLineSpacingForSection(int section) const;
    int getMinimumInteritemSpacingForSection(int section) const;
    Size getSizeForHeaderInSection(int section) const;
    Size getSizeForFooterInSection(int section) const;

    Size getSizeForItem(int section, int item, bool *isFullSpan) const;
    bool hasFixedItemSize(int section, Size *fixedItemSize);

    int getLayoutModeForSection(int section) const;


    // int getPageSize();

};


#endif //FLEXLAYOUTMANAGER_LAYOUTCALLBACKADAPTER_H
