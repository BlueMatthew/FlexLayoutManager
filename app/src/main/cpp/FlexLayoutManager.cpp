#include <jni.h>
#include <string>
#include <vector>
#include "FlexLayout.h"

// protected native long createLayout();
extern "C" JNIEXPORT long JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_createLayout(
        JNIEnv* env,
        jobject javaThis,
        jobject layoutCallback) {

    FlexLayout *pLayout = new FlexLayout(env, javaThis, layoutCallback);

    return reinterpret_cast<long>(pLayout);
}

// protected native long releaseLayout();
extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_releaseLayout(
        JNIEnv* env,
        jobject javaThis,
        jlong layout) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL != pLayout)
    {
        delete pLayout;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_addStickyItem(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jint section, jint item) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL != pLayout)
    {
        pLayout->addStickyItem(section, item);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_clearStickyItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL != pLayout)
    {
        pLayout->clearStickyItems();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_setStackedStickyItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jboolean stackedStickyItems) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL != pLayout)
    {
        pLayout->setStackedStickyItems(stackedStickyItems);
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_isStackedStickyItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout) {

    jboolean result = 1;
    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL != pLayout)
    {
        result = pLayout->isStackedStickyItems() ? 1 : 0;
    }

    return result;
}

// 0: isVertical
// 1:

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_prepareLayout(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jintArray layoutInfo) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL != pLayout)
    {
        int arrayLength = env->GetArrayLength(layoutInfo);
        std::vector<int> buffer;
        buffer.resize(arrayLength);
        env->GetIntArrayRegion(layoutInfo, 0, arrayLength, &(buffer[0]));
        LayoutInfo localLayoutInfo;
        localLayoutInfo.readFromBuffer(&(buffer[1]));
        pLayout->prepareLayout(&localLayoutInfo);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_filterItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jobject items, jint width, jint height, jint paddingLeft, jint paddingTop, jint paddingRight, jint paddingBottom, jint contentWidth, jint contentHeight, jint contentOffsetX, jint contentOffsetY) {

    // void getItemsInRect(std::vector<LayoutItem *> items, const Rect &bounds, const Rect &rect, const Point &contentOffset);

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);

    if (NULL != pLayout)
    {
        std::vector<LayoutItem *> layoutItems;
        Insets insets(paddingLeft, paddingTop, paddingRight, paddingBottom);
        Size size(width, height);
        Size contentSize(contentWidth, contentHeight);
        Point contentOffset(contentOffsetX, contentOffsetY);
        pLayout->getItemsInRect(items, size, insets, contentSize, contentOffset);
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_calcContentOffsetForScroll(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jint position, jint itemOffsetX, jint itemOffsetY) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);

    if (NULL != pLayout)
    {
        return pLayout->calcContentOffsetForScroll(position, itemOffsetX, itemOffsetY);
    }

    return NULL;
}