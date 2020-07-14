//
// Created by Matthew Shi on 2020-07-12.
//

#ifndef FLEXLAYOUTMANAGER_LAYOUTADAPTER_H
#define FLEXLAYOUTMANAGER_LAYOUTADAPTER_H

#include "FlexLayoutObjects.h"
//
// Adapter for org.wakin.flexlayout.LayoutManager.LayoutCallback
// And RecyclerView.LayoutManager
class LayoutAdapter {
private:
    JNIEnv* m_env;
    jobject m_layoutMnager;
    jobject m_callback;

    // Cache
    jobject             m_itemSize;

    mutable int         m_cachedSectionIndex;
    mutable int         m_cachedItemStart;
    mutable int         m_cachedItemCount;
    mutable int         m_cachedBufferSize;
    mutable int*        m_cachedBuffer;
    mutable jintArray   m_cachedJavaBuffer;

    // LayoutCallback
    jfieldID m_contentHeightFid;
    jmethodID m_orientationMid;
    jmethodID m_numberOfSectionsMid;
    jmethodID m_layoutModeMid;
    jmethodID m_numberOfItemsMid;
    jmethodID m_numberOfColumnsMid;
    jmethodID m_lineSpacingMid;
    jmethodID m_interitemSpacingMid;
    jmethodID m_insetsMid;
    jmethodID m_itemEnterStickyModeMid;
    jmethodID m_itemExitStickyModeMid;
    jmethodID m_hasFixItemSizeMid;
    jmethodID m_itemSizeMid;
    jmethodID m_fullSpanMid;
    jmethodID m_itemDataMid;

    // android/graphics/Point
    jclass      m_pointClass;
    jmethodID   m_pointConstructorMid;


    // org/wakin/flexlayout/LayoutManager/Size
    // jmethodID m_constructorOfSizeMid;
    jmethodID   m_sizeSetMid;
    jfieldID    m_sizeWidthFid;
    jfieldID    m_sizeHeightFid;

    // org/wakin/flexlayout/LayoutManager/Insets
    jfieldID    m_insetsLeftFid;
    jfieldID    m_insetsTopFid;
    jfieldID    m_insetsRightFid;
    jfieldID    m_insetsBottomFid;

    // java/util/List
    jmethodID   m_listAddMid;

    // org/wakin/flexlayout/LayoutManager/LayoutItem
    jclass      m_layoutItemClass;
    jmethodID   m_layoutItemConstuctMid;

    jmethodID m_layoutItemconstuctMid;
    jmethodID m_layoutItemSetOrgPtMid;

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

    LayoutAdapter(JNIEnv* env, jobject obj, jobject callback);
    ~LayoutAdapter();


    void addLayoutItem(jobject javaList, const LayoutItem &item) const;

public:
    void updateContentHeight(int height);
    void beginPreparingLayout(int cacheSize);
    void endPreparingLayout();

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

    void onItemEnterStickyMode(int section, int item, int position, Point point) const;
    void onItemExitStickyMode(int section, int item, int position) const;



};


#endif //FLEXLAYOUTMANAGER_LAYOUTADAPTER_H
