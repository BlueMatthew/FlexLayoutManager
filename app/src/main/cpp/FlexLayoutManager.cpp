//
// Created by Matthew Shi on 2020-07-19.
//
#include <jni.h>

#ifndef NDEBUG
#include <android/log.h>

#define TAG "NDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif // NDK_DEBUG

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

FlexLayoutManager::FlexLayoutManager(JNIEnv* env, jobject layoutManager) : m_orientation(VERTICAL)
{
    jclass layoutManagerClass = env->GetObjectClass(layoutManager);
    jmethodID mid = env->GetMethodID(layoutManagerClass, "getOrientation", "()I");
    m_orientation = env->CallIntMethod(layoutManager, mid);
    env->DeleteLocalRef(layoutManagerClass);
}

FlexLayoutManager::~FlexLayoutManager()
{
    cleanVerticalLayouts();
    cleanHorizontalLayouts();
}

Size FlexLayoutManager::prepareLayout(const LayoutCallbackAdapter& layoutCallbackAdapter, int pageStart, int pageCount, const LayoutAndSectionsInfo &layoutAndSectionsInfo)
{
    m_orientation = layoutAndSectionsInfo.orientation;
    Size contentSize;
    Size boundSize(layoutAndSectionsInfo.size.width - (layoutAndSectionsInfo.padding.left + layoutAndSectionsInfo.padding.right), layoutAndSectionsInfo.size.height - (layoutAndSectionsInfo.padding.top + layoutAndSectionsInfo.padding.bottom));
    if (isVertical())
    {
        cleanHorizontalLayouts();
        buildVerticalLayouts(layoutAndSectionsInfo.numberOfPages);

        VerticalLayoutIterator it = m_verticalLayout.begin() + pageStart;
        for (int idx = 0; idx < pageCount; ++idx, ++it)
        {
            LOGI("FLEX prepareLayout: page=%d", idx);
            (*it)->prepareLayout(layoutCallbackAdapter, boundSize, layoutAndSectionsInfo.padding);
            (*it)->updateStickyItemPosition(m_stickyItems);
            if ((pageStart + idx) == layoutAndSectionsInfo.page)
            {
                contentSize = (*it)->getContentSize();
                layoutCallbackAdapter.updateContentSize(contentSize.width, contentSize.height);
            }
            LOGI("FLEX prepareLayout end: page=%d", idx);
        }
    }
    else
    {
        cleanVerticalLayouts();
        buildHorizontalLayouts(layoutAndSectionsInfo.numberOfPages);

        HorizontalLayoutIterator it = m_horizontalLayout.begin() + pageStart;
        for (int idx = 0; idx < pageCount; ++idx, ++it)
        {
            (*it)->prepareLayout(layoutCallbackAdapter, boundSize, layoutAndSectionsInfo.padding);
            (*it)->updateStickyItemPosition(m_stickyItems);
            if ((pageStart + idx) == layoutAndSectionsInfo.page)
            {
                contentSize = (*it)->getContentSize();
                layoutCallbackAdapter.updateContentSize(contentSize.width, contentSize.height);
            }
        }
    }

    return contentSize;
}

void FlexLayoutManager::updateItems(int action, int itemStart, int itemCount)
{
    /*
    isVertical() ?
        m_verticalLayout->updateItems(action, itemStart, itemCount) :
        m_horizontalLayout->updateItems(action, itemStart, itemCount);
        */
}

void FlexLayoutManager::getItemsInRect(std::vector<LayoutItem> &items, StickyItemList &changingStickyItems, const DisplayInfo &displayInfo) const
{
    Rect rect(displayInfo.contentOffset.x, displayInfo.contentOffset.y, displayInfo.size.width - displayInfo.padding.hsize(), displayInfo.size.height - displayInfo.padding.vsize());   // visibleRect

    if (isVertical())
    {
        VerticalLayoutConstIterator itBegin = m_verticalLayout.cbegin();
        Point pagingOffset(displayInfo.pagingOffset, 0);

        VerticalLayout *layout = *(itBegin + displayInfo.page);
        layout->getItemsInRect(items, changingStickyItems, m_stickyItems, m_stackedStickyItems, rect, displayInfo.size, displayInfo.contentSize,
                               displayInfo.padding, displayInfo.contentOffset, pagingOffset, displayInfo.numberOfFixedSections, displayInfo.page);

        if (displayInfo.numberOfPendingPages > 1)
        {
            int sizeOfFixedSections = 0;
            if (displayInfo.numberOfFixedSections > 0)
            {
                Rect frame;
                bool found = layout->getSectionFrame(displayInfo.numberOfFixedSections - 1, frame);
                if (found)
                {
                    sizeOfFixedSections = frame.bottom();
                }
            }
            int maxSizeOfFixedItems = 0;
            for (std::vector<LayoutItem>::const_iterator it = items.cbegin(); it != items.cend(); ++it)
            {
                if (it->getSection() >= displayInfo.numberOfFixedSections)
                {
                    break;
                }
                if (maxSizeOfFixedItems < it->getFrame().bottom())
                {
                    // maxBottomOfFixedItems = it->getFrame().bottom();
                    maxSizeOfFixedItems = it->getFrame().bottom()/* - displayInfo.contentOffset.y*/;
                }
            }

            int maxBottomOfFixedItems = maxSizeOfFixedItems + displayInfo.contentOffset.y + displayInfo.padding.top;
            // pagingOffset.y = maxFixedItemsSize;
            rect.size.height -= maxSizeOfFixedItems;

            for (std::vector<std::pair<int, Point>>::const_iterator itPage = displayInfo.pendingPages.cbegin(); itPage != displayInfo.pendingPages.cend(); ++itPage)
            {
                if (itPage->first == displayInfo.page) continue;
                Point contentOffset = displayInfo.contentOffset;
                if (itPage->second.x == INVALID_OFFSET || itPage->second.y == INVALID_OFFSET)
                {
                    contentOffset.y = sizeOfFixedSections - maxSizeOfFixedItems;
                    rect.origin = displayInfo.contentOffset;
                }
                else
                {
                    rect.origin = itPage->second;
                    rect.origin.y += maxSizeOfFixedItems;
                    contentOffset = itPage->second;
                }

                pagingOffset.x = displayInfo.pagingOffset + (itPage->first - displayInfo.page) * (displayInfo.size.width);

                // LOGI("TouchMove: getItems page=%d, offset=%d width=%d", itPage->first, pagingOffset, displayInfo.size.width);
                layout = *(itBegin + itPage->first);
                std::vector<LayoutItem> pageItems;
                layout->getItemsInRect(pageItems, changingStickyItems, m_stickyItems, m_stackedStickyItems, rect, displayInfo.size, displayInfo.contentSize,
                                       displayInfo.padding, contentOffset, pagingOffset, displayInfo.numberOfFixedSections, displayInfo.page);

                if (pageItems.empty()) continue;
                if (items.empty())
                {
                    items.swap(pageItems);
                }
                else
                {
                    std::vector<LayoutItem>::const_iterator itItem = std::lower_bound(items.cbegin(), items.cend(), pageItems.front());
                    items.insert(itItem, pageItems.cbegin(), pageItems.cend());
                }
            }
        }
    }
    else
    {
        HorizontalLayout *layout = m_horizontalLayout[displayInfo.page];
        // layout->getItemsInRect(items, changingStickyItems, m_stickyItems, m_stackedStickyItems, rect, displayInfo.size, displayInfo.contentSize, displayInfo.padding, displayInfo.contentOffset);
    }
}

bool FlexLayoutManager::getItem(int page, int position, LayoutItem &layoutItem) const
{
    if (isVertical())
    {
        VerticalLayout *layout = m_verticalLayout[page];
        return layout->getItem(position, layoutItem);
    }
    else
    {
        HorizontalLayout *layout = m_horizontalLayout[page];
        return layout->getItem(position, layoutItem);
    }
 }

int FlexLayoutManager::computerContentOffsetToMakePositionTopVisible(const LayoutInfo &layoutInfo, int position, int positionOffset) const
{
    if (isVertical())
    {
        VerticalLayout *layout = m_verticalLayout[layoutInfo.page];
        return layout->computerContentOffsetToMakePositionTopVisible(m_stickyItems, m_stackedStickyItems, layoutInfo.size, layoutInfo.padding, layoutInfo.contentOffset, position, positionOffset);
    }
    else
    {
        HorizontalLayout *layout = m_horizontalLayout[layoutInfo.page];
        return layout->computerContentOffsetToMakePositionTopVisible(m_stickyItems, m_stackedStickyItems, layoutInfo.size, layoutInfo.padding, layoutInfo.contentOffset, position, positionOffset);
    }
}

// JNI Functions
extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_initLayoutEnv(
        JNIEnv* env,
        jclass layoutManagerClass,
        jclass callbackClass) {

    FlexLayoutManager::initLayoutEnv(env, layoutManagerClass);
    LayoutCallbackAdapter::initLayoutEnv(env, layoutManagerClass, callbackClass);
}

// protected native long createLayout();
extern "C" JNIEXPORT long JNICALL
Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_createLayout(
        JNIEnv* env,
        jobject javaThis,
        jobject layoutCallback) {

    FlexLayoutManager *layoutManager = new FlexLayoutManager(env, javaThis);

    return reinterpret_cast<long>(layoutManager);
}

// protected native long releaseLayout();
extern "C" JNIEXPORT void JNICALL
Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_releaseLayout(
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
Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_addStickyItem(
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
Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_clearStickyItems(
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
Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_setStackedStickyItems(
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
        Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_isStackedStickyItems(
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
Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_prepareLayout(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jobject layoutCallback,
        jint pageStart, jint pageCount, jintArray layoutAndSectionsInfo) {

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
    localLayoutAndSectionsInfo.read(&(buffer[0]), arrayLength);

    LayoutCallbackAdapter layoutCallbackAdapter(env, javaThis, layoutCallback, &localLayoutAndSectionsInfo);

    Size contentSize = layoutManager->prepareLayout(layoutCallbackAdapter, pageStart, pageCount, localLayoutAndSectionsInfo);

}

extern "C" JNIEXPORT jintArray JNICALL
        Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_filterItems(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jintArray layoutInfo) {

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
    DisplayInfo localDisplayInfo;
    localDisplayInfo.read(&(buffer[0]), arrayLength);

    std::vector<LayoutItem> items;
    FlexLayoutManager::StickyItemList changingStickyItems;

    layoutManager->getItemsInRect(items, changingStickyItems, localDisplayInfo);

    buffer.clear();
    buffer.reserve(items.size() * 10 + changingStickyItems.size() * 9 + 2);
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
        Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_getItemRect(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jint page, jint position) {

    FlexLayoutManager *layoutManager = reinterpret_cast<FlexLayoutManager *>(layout);
    if (NULL == layoutManager)
    {
        return NULL;
    }

    LayoutItem layoutItem;
    bool found = layoutManager->getItem(page, position, layoutItem);
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
        Java_org_wakin_flexlayout_layoutmanager_FlexLayoutManager_computerContentOffsetToMakePositionTopVisible(
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
    localLayoutInfo.read(&(buffer[0]), arrayLength);

    return layoutManager->computerContentOffsetToMakePositionTopVisible(localLayoutInfo, position, positionOffset);
}