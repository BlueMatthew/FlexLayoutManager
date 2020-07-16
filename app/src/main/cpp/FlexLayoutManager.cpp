#include <jni.h>
#include <string>
#include <vector>
#include "FlexLayout.h"
#include "LayoutCallbackAdapter.h"

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_initLayoutEnv(
        JNIEnv* env,
        jclass layoutManagerClass,
        jclass callbackClass) {
    LayoutCallbackAdapter::initLayoutEnv(env, layoutManagerClass, callbackClass);
}

// protected native long createLayout();
extern "C" JNIEXPORT long JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_createLayout(
        JNIEnv* env,
        jobject javaThis,
        jobject layoutCallback) {

    FlexLayout *pLayout = new FlexLayout();

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
        jobject layoutCallback,
        jintArray layoutAndSectionsInfo) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL == pLayout || NULL == layoutAndSectionsInfo)
    {
        return;
    }

    jsize arrayLength = env->GetArrayLength(layoutAndSectionsInfo);
    if (0 >= arrayLength)
    {
        return;
    }
    std::vector<int> buffer;
    buffer.resize(arrayLength);
    env->GetIntArrayRegion(layoutAndSectionsInfo, 0, arrayLength, &(buffer[0]));
    LayoutAndSectionsInfo localLayoutAndSectionsInfo;
    localLayoutAndSectionsInfo.readFromBuffer(&(buffer[1]), arrayLength - 1);

    LayoutCallbackAdapter layoutCallbackAdapter(env, javaThis, layoutCallback, &localLayoutAndSectionsInfo);

    pLayout->prepareLayout(layoutCallbackAdapter, &localLayoutAndSectionsInfo);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_filterItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jintArray layoutInfo) {

    // void getItemsInRect(std::vector<LayoutItem *> items, const Rect &bounds, const Rect &rect, const Point &contentOffset);


    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL == pLayout || NULL == layoutInfo)
    {
        return NULL;
    }

    jsize arrayLength = env->GetArrayLength(layoutInfo);
    if (0 >= arrayLength)
    {
        return NULL;
    }
    std::vector<int> buffer;
    buffer.resize(arrayLength);
    env->GetIntArrayRegion(layoutInfo, 0, arrayLength, &(buffer[0]));
    LayoutInfo localLayoutInfo;
    localLayoutInfo.readFromBuffer(&(buffer[1]), arrayLength - 1);

    std::vector<LayoutItem> items;
    std::vector<std::pair<StickyItem, Point>> changingStickyItems;

    pLayout->getItemsInRect(items, changingStickyItems, localLayoutInfo.orientation == 1, localLayoutInfo.size, localLayoutInfo.padding, localLayoutInfo.contentOffset);

    buffer.clear();
    buffer.reserve(items.size() * 8 + changingStickyItems.size() * 6 + 2);
    buffer.push_back(items.size());
    for (std::vector<LayoutItem>::const_iterator it = items.begin(); it != items.end(); ++it)
    {
        buffer.push_back(it->section);
        buffer.push_back(it->item);
        buffer.push_back(it->position);
        buffer.push_back(it->frame.origin.x);
        buffer.push_back(it->frame.origin.y);
        buffer.push_back(it->frame.size.width);
        buffer.push_back(it->frame.size.height);
        buffer.push_back(it->data);
    }

    buffer.push_back(changingStickyItems.size());

    for (std::vector<std::pair<StickyItem, Point>>::const_iterator it = changingStickyItems.begin(); it != changingStickyItems.end(); ++it)
    {
        buffer.push_back(it->first.section);
        buffer.push_back(it->first.item);
        buffer.push_back(it->first.position);
        buffer.push_back(it->first.inSticky ? 1 : 0);
        buffer.push_back(it->second.x);
        buffer.push_back(it->second.y);
    }

    jintArray result = env->NewIntArray(buffer.size());
    env->SetIntArrayRegion(result, 0, buffer.size(), &(buffer[0]));
    return result;

}

extern "C" JNIEXPORT jobject JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_calcVerticalContentOffsetForScroll(
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