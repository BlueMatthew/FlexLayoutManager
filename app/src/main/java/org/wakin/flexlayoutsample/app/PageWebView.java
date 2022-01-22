package org.wakin.flexlayoutsample.app;


import android.content.Context;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.VelocityTrackerCompat;
import androidx.core.view.ViewCompat;

import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.webkit.WebView;


public class PageWebView extends WebView implements NestedScrollingChild {

    private static final String TAG = PageWebView.class.getSimpleName();

    /**
     * fling速度阈值
     */
    public static final int FLING_THRESHOLD_VELOCITY = 500;

    private int mActivePointerId = -1;
    private int mTouchSlop;
    protected int mFlingThresholdVelocity;

    private int mMaximumVelocity;

    private int mLastTouchX;
    private int mLastTouchY;

    private final int[] mNestedOffsets = new int[2];
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

    private VelocityTracker mVelocityTracker;

    private NestedScrollingChildHelper mChildHelper;


    public PageWebView(Context context) {
        this(context, null);
    }

    public PageWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();

        float density = getResources().getDisplayMetrics().density;
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * density);

        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getPointerCount() > 1) {
            return true;
        }

        initVelocityTrackerIfNotExists();

        final int action = MotionEventCompat.getActionMasked(e);
        boolean eventAddedToVelocityTracker = false;

        boolean retValue = false;

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }

        //如果没有嵌套滚动的parent，就让super处理,保持webview自身对touch的处理
        if (action != MotionEvent.ACTION_DOWN && !hasNestedScrollingParent()) {
            Log.d(TAG, "onTouchEvent: does not have a nested scrolling parent, need not to handle touch event");
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mActivePointerId = -1;
                recycleVelocityTracker();
            }
            return super.onTouchEvent(e);
        }

        final MotionEvent vtev = MotionEvent.obtain(e);
        final MotionEvent copiedMotionEvent = MotionEvent.obtain(e);

        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mLastTouchX = (int) e.getX();
                mLastTouchY = (int) e.getY();
                mActivePointerId = e.getPointerId(0);

                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                retValue = super.onTouchEvent(e);

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int activePointerIndex = e.findPointerIndex(mActivePointerId);

                if (activePointerIndex < 0) {
                    return false;
                }

                final int x = (int) e.getX(activePointerIndex);
                final int y = (int) e.getY(activePointerIndex);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                //保持touch的位置为上次x，y，这样的话，如果外层view对webview进行了偏移，就可以保持上次的位置交给super去处理
                copiedMotionEvent.setLocation(mLastTouchX, mLastTouchY);

                mLastTouchX = x - mScrollOffset[0];
                mLastTouchY = y - mScrollOffset[1];

                scrollByInternal(dx, dy, vtev);
                //使用copiedMotionEvent
                retValue = super.onTouchEvent(copiedMotionEvent);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //使用copiedMotionEvent
                copiedMotionEvent.setLocation(mLastTouchX, mLastTouchY);

                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;

                //计算fling
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final float velocityX = -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mActivePointerId);
                final float velocityY = -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivePointerId);

                boolean isFling = Math.abs(velocityY) > mFlingThresholdVelocity;

                if (isFling) {
                    //通知外层view发生fling了
                    if (dispatchNestedPreFling(velocityX, velocityY)) {
                        dispatchNestedFling(velocityX, velocityY, false);
                    }
                }

                mActivePointerId = -1;
                recycleVelocityTracker();
                stopNestedScroll();

                //如果parent滚动了，就设置为取消，因为如果parent发生滚动，touch的y相对于WebView不会动，up时就会认为是一个点击事件了，所以需要设置为cancel
                if (Math.abs(mNestedOffsets[1]) > mTouchSlop) {
                    copiedMotionEvent.setAction(MotionEvent.ACTION_CANCEL);
                }

                retValue = super.onTouchEvent(copiedMotionEvent);
                break;
        }

        if (!eventAddedToVelocityTracker) {
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(vtev);
            }
        }

        vtev.recycle();
        copiedMotionEvent.recycle();
        return retValue;
    }

    void scrollByInternal(int dx, int dy, MotionEvent ev) {
        int oldY = getScrollY();
        int newScrollY = Math.max(0, oldY + dy);
        int dyConsumed = newScrollY - oldY;
        int dyUnconsumed = dy - dyConsumed;

        if (dispatchNestedScroll(0, dy, 0, dyUnconsumed, mScrollOffset)) {
            mLastTouchX -= mScrollOffset[0];
            mLastTouchY -= mScrollOffset[1];
            if (ev != null) {
                ev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
            }
            mNestedOffsets[0] += mScrollOffset[0];
            mNestedOffsets[1] += mScrollOffset[1];
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    // Nested Scroll implements
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}
