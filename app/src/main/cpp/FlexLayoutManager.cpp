//
// Created by Matthew Shi on 2020-07-19.
//
#include <jni.h>
#include <vector>
#include "common/FlexLayout.h"
#include "LayoutCallbackAdapter.h"
#include "FlexLayoutManager.h"

int FlexLayoutManager::VERTICAL = 1;
int FlexLayoutManager::INVALID_OFFSET = std::numeric_limits<int>::min();
void FlexLayoutManager::initLayoutEnv(JNIEnv* env, jclass layoutManagerClass)
{
    jfieldID fid = env->GetStaticFieldID(layoutManagerClass, "VERTICAL", "I");
    if (NULL != fid)
    {
        // Sync the value of VERTICAL from java class
        VERTICAL = env->GetStaticIntField(layoutManagerClass, fid);
    }

    fid = env->GetStaticFieldID(layoutManagerClass, "INVALID_OFFSET", "I");
    if (NULL != fid)
    {
        // Sync the value of INVALID_OFFSET from java class
        INVALID_OFFSET = env->GetStaticIntField(layoutManagerClass, fid);
    }
}

FlexLayoutManager::FlexLayoutManager(JNIEnv* env, jobject layoutManager) : m_orientation(VERTICAL), m_verticalLayout(NULL), m_horizontalLayout(NULL)
{
    jclass layoutManagerClass = env->GetObjectClass(layoutManager);
    jmethodID mid = env->GetMethodID(layoutManagerClass, "getOrientation", "()I");
    m_orientation = env->CallIntMethod(layoutManager, mid);
    env->DeleteLocalRef(layoutManagerClass);
}

FlexLayoutManager::~FlexLayoutManager()
{
    if (NULL != m_verticalLayout)
    {
        delete m_verticalLayout;
        m_verticalLayout = NULL;
    }
    if (NULL != m_horizontalLayout)
    {
        delete m_horizontalLayout;
        m_horizontalLayout = NULL;
    }
}

Size FlexLayoutManager::prepareLayout(const LayoutCallbackAdapter& layoutCallbackAdapter, const LayoutAndSectionsInfo &layoutAndSectionsInfo)
{
    m_orientation = layoutAndSectionsInfo.orientation;
    Size contentSize;
    Size boundSize(layoutAndSectionsInfo.size.width - (layoutAndSectionsInfo.padding.left + layoutAndSectionsInfo.padding.right), layoutAndSectionsInfo.size.height - (layoutAndSectionsInfo.padding.top + layoutAndSectionsInfo.padding.bottom));
    if (isVertical())
    {
        if (NULL != m_horizontalLayout)
        {
            delete m_horizontalLayout;
            m_horizontalLayout = NULL;
        }
        if (NULL == m_verticalLayout)
        {
            m_verticalLayout = new VerticalLayout();
        }


        m_verticalLayout->prepareLayout(layoutCallbackAdapter, boundSize, layoutAndSectionsInfo.padding);
        m_verticalLayout->updateStickyItemPosition(m_stickyItems);
        contentSize = m_verticalLayout->getContentSize();
    }
    else
    {
        if (NULL != m_verticalLayout)
        {
            delete m_verticalLayout;
            m_verticalLayout = NULL;
        }
        if (NULL == m_horizontalLayout)
        {
            m_horizontalLayout = new HorizontalLayout();
        }

        m_horizontalLayout->prepareLayout(layoutCallbackAdapter, boundSize, layoutAndSectionsInfo.padding);
        m_horizontalLayout->updateStickyItemPosition(m_stickyItems);
        contentSize = m_horizontalLayout->getContentSize();
    }

    layoutCallbackAdapter.updateContentSize(contentSize.width, contentSize.height);
    return contentSize;
}

void FlexLayoutManager::updateItems(int action, int itemStart, int itemCount)
{
    isVertical() ?
        m_verticalLayout->updateItems(action, itemStart, itemCount) :
        m_horizontalLayout->updateItems(action, itemStart, itemCount);
}

void FlexLayoutManager::getItemsInRect(std::vector<LayoutItem> &items, StickyItemList &changingStickyItems, const LayoutInfo &layoutInfo) const
{
    Rect rect(layoutInfo.contentOffset.x, layoutInfo.contentOffset.y, layoutInfo.size.width, layoutInfo.size.height);   // visibleRect

    isVertical() ?
        m_verticalLayout->getItemsInRect(items, changingStickyItems, m_stickyItems, m_stackedStickyItems, rect, layoutInfo.size, layoutInfo.contentSize, layoutInfo.padding, layoutInfo.contentOffset) :
        m_horizontalLayout->getItemsInRect(items, changingStickyItems, m_stickyItems, m_stackedStickyItems, rect, layoutInfo.size, layoutInfo.contentSize, layoutInfo.padding, layoutInfo.contentOffset);
}

bool FlexLayoutManager::getItem(int position, LayoutItem &layoutItem) const
{
    return isVertical() ?
        m_verticalLayout->getItem(position, layoutItem) :
        m_horizontalLayout->getItem(position, layoutItem);
}

int FlexLayoutManager::computerContentOffsetToMakePositionTopVisible(const LayoutInfo &layoutInfo, int position, int positionOffset) const
{
    return isVertical() ?
        m_verticalLayout->computerContentOffsetToMakePositionTopVisible(m_stickyItems, m_stackedStickyItems, layoutInfo.size, layoutInfo.contentSize, layoutInfo.padding, layoutInfo.contentOffset, position, positionOffset) :
        m_horizontalLayout->computerContentOffsetToMakePositionTopVisible(m_stickyItems, m_stackedStickyItems, layoutInfo.size, layoutInfo.contentSize, layoutInfo.padding, layoutInfo.contentOffset, position, positionOffset);

}

// JNI Functions
extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_initLayoutEnv(
        JNIEnv* env,
        jclass layoutManagerClass,
        jclass callbackClass) {

    FlexLayoutManager::initLayoutEnv(env, layoutManagerClass);
    LayoutCallbackAdapter::initLayoutEnv(env, layoutManagerClass, callbackClass);
}

// protected native long createLayout();
extern "C" JNIEXPORT long JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_createLayout(
        JNIEnv* env,
        jobject javaThis,
        jobject layoutCallback) {

    FlexLayoutManager *layoutManager = new FlexLayoutManager(env, javaThis);

    return reinterpret_cast<long>(layoutManager);
}

// protected native long releaseLayout();
extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_releaseLayout(
        JNIEnv* env,
        jobject javaThis,
        jlong layout) {

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL != layoutManager)
    {
        delete layoutManager;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_addStickyItem(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jint section, jint item) {

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL != layoutManager)
    {
        layoutManager->addStickyItem(section, item);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_clearStickyItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout) {

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL != layoutManager)
    {
        layoutManager->clearStickyItems();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_setStackedStickyItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jboolean stackedStickyItems) {

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL != layoutManager)
    {
        layoutManager->setStackedStickyItems(stackedStickyItems);
    }
}

extern "C" JNIEXPORT jboolean JNICALL
        Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_isStackedStickyItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout) {

    jboolean result = 1;
    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL != layoutManager)
    {
        result = layoutManager->isStackedStickyItems() ? 1 : 0;
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

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL == layoutManager || NULL == layoutAndSectionsInfo)
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

    Size contentSize = layoutManager->prepareLayout(layoutCallbackAdapter, localLayoutAndSectionsInfo);

}

extern "C" JNIEXPORT jintArray JNICALL
        Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_filterItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jintArray layoutInfo) {

    // void getItemsInRect(std::vector<LayoutItem *> items, const Rect &bounds, const Rect &rect, const Point &contentOffset);


    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL == layoutManager || NULL == layoutInfo)
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
    FlexLayoutManager::StickyItemList changingStickyItems;

    layoutManager->getItemsInRect(items, changingStickyItems, localLayoutInfo);

    buffer.clear();
    buffer.reserve(items.size() * 9 + changingStickyItems.size() * 7 + 2);
    buffer.push_back(items.size());
    for (typename std::vector<LayoutItem>::const_iterator it = items.cbegin(); it != items.cend(); ++it)
    {
        writeToBuffer(buffer, *it);
    }

    buffer.push_back(changingStickyItems.size());

    for (FlexLayoutManager::StickyItemList::const_iterator it = changingStickyItems.cbegin(); it != changingStickyItems.cend(); ++it)
    {
        writeToBuffer(buffer, *it);
    }

    return LayoutCallbackAdapter::makeIntArray(env, buffer);
}

// int array: left, top, right, bottom
extern "C" JNIEXPORT jintArray JNICALL
        Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_getItemRect(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jint position) {

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL == layoutManager)
    {
        return NULL;
    }

    LayoutItem layoutItem;
    bool found = layoutManager->getItem(position, layoutItem);
    if (found)
    {
        // return
        std::vector<jint> buffer;
        buffer.reserve(4);
        writeToBuffer(buffer, layoutItem.getFrame());

        return LayoutCallbackAdapter::makeIntArray(env, buffer);
    }

    return NULL;
}

extern "C" JNIEXPORT jint JNICALL
        Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_computerContentOffsetToMakePositionTopVisible(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jintArray layoutInfo,
        jint position, int positionOffset) {

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL == layoutManager || NULL == layoutInfo)
    {
        return FlexLayoutManager::INVALID_OFFSET;
    }

    jsize arrayLength = env->GetArrayLength(layoutInfo);
    if (0 >= arrayLength)
    {
        return FlexLayoutManager::INVALID_OFFSET;
    }

    std::vector<int> buffer;
    buffer.resize(arrayLength);
    env->GetIntArrayRegion(layoutInfo, 0, arrayLength, &(buffer[0]));
    LayoutInfo localLayoutInfo;
    localLayoutInfo.readFromBuffer(&(buffer[1]), arrayLength - 1);

    return layoutManager->computerContentOffsetToMakePositionTopVisible(localLayoutInfo, position, positionOffset);
}