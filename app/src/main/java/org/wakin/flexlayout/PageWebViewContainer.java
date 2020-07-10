package org.wakin.flexlayout;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

public class PageWebViewContainer extends LinearLayout implements PaginationView {

    public static interface ReusablePageWebViewStateListener {
        void onPageWebViewDetached(PageWebView pageWebView);
    }

    private PageWebView mWebView = null;

    public PageWebViewContainer(Context context) {
        this(context, null, 0);
    }
    public PageWebViewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public PageWebViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public PageWebViewContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void offsetView(float offsetX, float offsetY) {
        if (getTranslationX() != offsetX) setTranslationX(offsetX);
        if (getTranslationY() != offsetY) setTranslationY(offsetY);
    }

    @Override
    public void swap(RecyclerView recyclerView, int minPagbleItemPosition, boolean inSticky) {

    }

    @Override
    public void releaseReusableViews() {
        detachWebView();
    }

    @Override
    public void removeFromParentViewGroup() {
        // ViewGroup viewGroup = (ViewGroup) getParent();
        // viewGroup.removeView(this);
    }

    @Override
    public int getFirstVisibleItemPosition() {
        return RecyclerView.NO_POSITION;
    }

    @Override
    public int getFirstVisibleItemOffset() {
        return 0;
    }

    public void attachWebView(PageWebView pageWebView) {
        addView(pageWebView);
        mWebView = pageWebView;
    }

    public PageWebView detachWebView() {
        removeView(mWebView);
        PageWebView webView = mWebView;
        return webView;
    }
}
