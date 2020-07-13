#include <jni.h>
#include <string>
#include <vector>
#include "FlexLayout.h"

extern "C" JNIEXPORT jstring JNICALL
Java_org_wakin_flexlayoutmanager_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

// protected native long createLayout();
extern "C" JNIEXPORT long JNICALL
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_createLayout(
        JNIEnv* env,
        jobject javaThis,
        jobject layoutCallback) {

    FlexLayout *pLayout = new FlexLayout(env, javaThis, layoutCallback);

    return reinterpret_cast<long>(pLayout);
}

// protected native long createLayout();
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
Java_org_wakin_flexlayout_LayoutManager_FlexLayoutManager_prepareLayout(
        JNIEnv* env,
        jobject javaThis,
        jlong layout,
        jint width, jint height, jint paddingLeft, jint paddingTop, jint paddingRight, jint paddingBottom) {

    FlexLayout *pLayout = reinterpret_cast<FlexLayout *>(layout);
    if (NULL != pLayout)
    {
        pLayout->prepareLayout(Size(width, height), Insets(paddingLeft, paddingTop, paddingRight, paddingBottom));
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
        pLayout->getItemsInRect(layoutItems, size, insets, contentSize, contentOffset);

        jclass listClass = env->GetObjectClass(items);

        jclass layoutItemClass = env->FindClass("org/wakin/flexlayout/LayoutManager/LayoutItem");
        jclass rectClass = env->FindClass("android/graphics/Rect");

        jmethodID constuctMid = env->GetMethodID(layoutItemClass, "<init>", "(IIIZIIII)V");

        jmethodID addMid = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");

        for (std::vector<LayoutItem *>::iterator it = layoutItems.begin(); it != layoutItems.end(); ++it)
        {
            jobject javaLayoutItem = env->NewObject(layoutItemClass, constuctMid, (*it)->section, (*it)->item, (*it)->position, jboolean((*it)->data == 1 ? 1 : 0),
                                                    (*it)->frame.left(), (*it)->frame.top(), (*it)->frame.right(), (*it)->frame.bottom());


            jboolean ret = env->CallBooleanMethod(items, addMid, javaLayoutItem);

            delete (*it);

        }

        layoutItems.clear();
    }
}