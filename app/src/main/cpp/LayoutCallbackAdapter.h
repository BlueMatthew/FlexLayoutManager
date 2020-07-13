//
// Created by Matthew Shi on 2020-07-12.
//

#ifndef FLEXLAYOUTMANAGER_LAYOUTCALLBACKADAPTER_H
#define FLEXLAYOUTMANAGER_LAYOUTCALLBACKADAPTER_H

#include "Graphics.h"

typedef nsflex::PointT<int> Point;
typedef nsflex::SizeT<int> Size;
typedef nsflex::RectT<int> Rect;
typedef nsflex::InsetsT<int> Insets;

//
// Adapter for org.wakin.flexlayout.LayoutManager.LayoutCallback
// And RecyclerView.LayoutManager
class LayoutCallbackAdapter {
private:
    JNIEnv* m_env;
    jobject m_layoutMnager;
    jobject m_callback;

    jclass m_callbackClass;
    jclass m_layoutManagerClass;
    jclass m_pointClass;
    jmethodID m_itemSizeMid;
    jmethodID m_fullSpanMid;
    jmethodID m_itemDataMid;
    jmethodID m_constructorOfSizeMid;

    jobject m_itemSize;
    jfieldID m_sizeWidthFid;
    jfieldID m_sizeHeightFid;

    mutable int         m_cachedSectionIndex;
    mutable int         m_cachedItemStart;
    mutable int         m_cachedItemCount;
    mutable int         m_cachedBufferSize;
    mutable int*        m_cachedBuffer;
    mutable jintArray   m_cachedJavaBuffer;

    // jmethodID m_getSctionCount;
public:

    LayoutCallbackAdapter(JNIEnv* env, jobject obj, jobject callback);
    ~LayoutCallbackAdapter();

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

    int getInfoForItemsBatchly(int section, int itemStart, int itemCount, std::pair<Size, int> &items);

    int getLayoutModeForSection(int section) const;


    // int getPageSize();

    void onItemEnterStickyMode(int section, int item, int position, Point point);
    void onItemExitStickyMode(int section, int item, int position);

};


#endif //FLEXLAYOUTMANAGER_LAYOUTCALLBACKADAPTER_H
