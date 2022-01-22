package org.wakin.flexlayoutsample.app;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.wakin.flexlayoutsample.R;

import java.util.List;

public class RecyclerViewPager {

    /**
     * Up direction, used for swipe & drag control.
     */
    public static final int HORIZONTAL = 2 ^ RecyclerView.HORIZONTAL;
    public static final int VERTICAL = 2 ^ RecyclerView.VERTICAL;

    /**
     * Up direction, used for swipe & drag control.
     */
    public static final int UP = 1;
    /**
     * Down direction, used for swipe & drag control.
     */
    public static final int DOWN = 1 << 1;
    /**
     * Left direction, used for swipe & drag control.
     */
    public static final int LEFT = 1 << 2;
    /**
     * Right direction, used for swipe & drag control.
     */
    public static final int RIGHT = 1 << 3;
    // If you change these relative direction values, update Callback#convertToAbsoluteDirection,
    // Callback#convertToRelativeDirection.
    /**
     * Horizontal start direction. Resolved to LEFT or RIGHT depending on RecyclerView's layout
     * direction. Used for swipe & drag control.
     */
    public static final int START = LEFT << 2;
    /**
     * Horizontal end direction. Resolved to LEFT or RIGHT depending on RecyclerView's layout
     * direction. Used for swipe & drag control.
     */
    public static final int END = RIGHT << 2;
    /**
     * ItemTouchHelper is in idle state. At this state, either there is no related motion event by
     * the user or latest motion events have not yet triggered a swipe or drag.
     */
    public static final int ACTION_STATE_IDLE = 0;
    /**
     * A View is currently being swiped.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ACTION_STATE_PAGING = 1;
    /**
     * Animation type for views which are swiped successfully.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ANIMATION_TYPE_PAGING_SUCCESS = 1 << 1;
    /**
     * Animation type for views which are not completely swiped thus will animate back to their
     * original position.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ANIMATION_TYPE_PAGING_CANCEL = 1 << 2;

    private static final String TAG = "RecyclerViewPager";
    private static final boolean DEBUG = false;
    private static final int ACTIVE_POINTER_ID_NONE = -1;
    private static final int DIRECTION_FLAG_COUNT = 8;
    private static final int ACTION_MODE_IDLE_MASK = (1 << DIRECTION_FLAG_COUNT) - 1;
    private static final int ACTION_MODE_SWIPE_MASK = ACTION_MODE_IDLE_MASK << DIRECTION_FLAG_COUNT;
    /**
     * The unit we are using to track velocity
     */
    private static final int PIXELS_PER_SECOND = 1000;
    /**
     * The reference coordinates for the action start. For drag & drop, this is the time long
     * press is completed vs for swipe, this is the initial touch point.
     */
    private float mInitialTouchX;
    private float mInitialTouchY;
    private int mInitialSwipeDir;   // Only for swipe???
    /**
     * Set when ItemTouchHelper is assigned to a RecyclerView.
     */
    private float mPagerEscapeVelocity;
    /**
     * Set when ItemTouchHelper is assigned to a RecyclerView.
     */
    private float mMaxPagerVelocity;
    /**
     * The diff between the last event and initial touch.
     */
    private float mDx;
    private float mDy;
    private float mOutDx;
    private float mOutDy;

    private boolean mInPagableArea = false;
    private boolean mInPaging = false;

    /**
     * The pointer we are tracking.
     */
    private int mActivePointerId = ACTIVE_POINTER_ID_NONE;
    /**
     * Developer callback which controls the behavior of ItemTouchHelper.
     */
    @NonNull
    private Callback mCallback;
    /**
     * Current mode.
     */
    private int mActionState = ACTION_STATE_IDLE;
    /**
     * The direction flags obtained from unmasking
     * {@link Callback#getAbsoluteMovementFlags(RecyclerView)} for the current
     * action state.
     */
    private int mSelectedFlags;
    /**
     * When a View is dragged or swiped and needs to go back to where it was, we create a Recover
     * Animation and animate it to its location using this custom Animator, instead of using
     * framework Animators.
     * Using framework animators has the side effect of clashing with ItemAnimator, creating
     * jumpy UIs.
     */
    RecoverAnimation mRecoverAnimation = null;
    private int mSlop;
    private RecyclerView mRecyclerView;

    private RecyclerView.State mState;
    private int mResIdPaging;
    /**
     * When user drags a view to the edge, we start scrolling the LayoutManager as long as View
     * is partially out of bounds.
     */
    private final Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (scrollIfNecessary()) {
                // moveIfNecessary();

                mRecyclerView.removeCallbacks(mScrollRunnable);
                ViewCompat.postOnAnimation(mRecyclerView, this);
            }
        }
    };
    /**
     * Used for detecting fling swipe
     */
    private VelocityTracker mVelocityTracker;
    //re used for for sorting swap targets
    private List<Integer> mDistances;
    /**
     * Used to detect drag and scroll.
     */
    private GestureDetectorCompat mGestureDetector;
    /**
     * Callback for when long press occurs.
     */
    private PagerGestureListener mPagerGestureListener;
    private final OnItemTouchListener mOnItemTouchListener = new OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView,
                                             @NonNull MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            if (DEBUG) {
                Log.d(TAG, "intercept: x:" + event.getX() + ",y:" + event.getY() + ", " + event);
            }
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mActivePointerId = event.getPointerId(0);
                mInitialTouchX = event.getX();
                mInitialTouchY = event.getY();
                obtainVelocityTracker();

                if (mCallback.shouldStartPager(event.getX(), event.getY())) {
                    mActionState = ACTION_STATE_PAGING;
                    int actionStateMask = (1 << (DIRECTION_FLAG_COUNT + DIRECTION_FLAG_COUNT * mActionState)) - 1;
                    mSelectedFlags =
                            (mCallback.getAbsoluteMovementFlags(mRecyclerView) & actionStateMask)
                                    >> (mActionState * DIRECTION_FLAG_COUNT);

                    mInPagableArea = true;
                }
            } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                mActivePointerId = ACTIVE_POINTER_ID_NONE;
                select(null, ACTION_STATE_IDLE);
                mInPagableArea = false;
            } else if (mActivePointerId != ACTIVE_POINTER_ID_NONE) {
                // in a non scroll orientation, if distance change is above threshold, we
                // can select the item
                final int index = event.findPointerIndex(mActivePointerId);
                if (DEBUG) {
                    Log.d(TAG, "pointer index " + index);
                }
                if (index >= 0) {
                    // checkSelectForSwipe(action, event, index);
                }
            }
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
            }

            return mInPagableArea && mInPaging;
            // return mSelected != null;
        }
        @Override
        public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            if (DEBUG) {
                Log.d(TAG, "on touch: x:" + mInitialTouchX + ",y:" + mInitialTouchY + ", :" + event);
            }
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
            }
            if (mActivePointerId == ACTIVE_POINTER_ID_NONE) {
                return;
            }
            final int action = event.getActionMasked();
            final int activePointerIndex = event.findPointerIndex(mActivePointerId);
            if (activePointerIndex >= 0) {
                // checkSelectForSwipe(action, event, activePointerIndex);
            }

            switch (action) {
                case MotionEvent.ACTION_MOVE: {
                    // Find the index of the active pointer and fetch its position
                    if (activePointerIndex >= 0) {
                        // updateDxDy(event, mSelectedFlags, activePointerIndex);
                        // moveIfNecessary(viewHolder);
                        // mRecyclerView.removeCallbacks(mScrollRunnable);
                        // mScrollRunnable.run();
                        // mRecyclerView.invalidate();

                    }
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                    if (mVelocityTracker != null) {
                        mVelocityTracker.clear();
                    }
                    // fall through
                case MotionEvent.ACTION_UP:
                    select(null, ACTION_STATE_IDLE);

                    if (mInPagableArea && mInPaging) {
                        startAnimation();
                    }
                    mInPaging = false;
                    mInPagableArea = false;

                    mActivePointerId = ACTIVE_POINTER_ID_NONE;
                    break;
                case MotionEvent.ACTION_POINTER_UP: {
                    final int pointerIndex = event.getActionIndex();
                    final int pointerId = event.getPointerId(pointerIndex);
                    if (pointerId == mActivePointerId) {
                        // This was our active pointer going up. Choose a new
                        // active pointer and adjust accordingly.
                        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        mActivePointerId = event.getPointerId(newPointerIndex);
                        updateDxDy(event, mSelectedFlags, pointerIndex);
                    }
                    break;
                }
            }
        }
        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (!disallowIntercept) {
                return;
            }
            select(null, ACTION_STATE_IDLE);
        }
    };

    /**
     * When user started to drag scroll. Reset when we don't scroll
     */
    private long mDragScrollStartTimeInMs;
    /**
     * Creates an ItemTouchHelper that will work with the given Callback.
     * <p>
     * You can attach ItemTouchHelper to a RecyclerView via
     * {@link #attachToRecyclerView(RecyclerView)}. Upon attaching, it will add an item decoration,
     * an onItemTouchListener and a Child attach / detach listener to the RecyclerView.
     *
     * @param callback The Callback which controls the behavior of this touch helper.
     */
    public RecyclerViewPager(@NonNull Callback callback, int resIdPaging) {
        mCallback = callback;
        mResIdPaging = resIdPaging;
    }
    private static boolean hitTest(View child, float x, float y, float left, float top) {
        return x >= left
                && x <= left + child.getWidth()
                && y >= top
                && y <= top + child.getHeight();
    }

    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return; // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks();
        }
        mRecyclerView = recyclerView;
        if (recyclerView != null) {
            final Resources resources = recyclerView.getResources();
            mPagerEscapeVelocity = resources
                    .getDimension(R.dimen.item_touch_helper_swipe_escape_velocity);
            mMaxPagerVelocity = resources
                    .getDimension(R.dimen.item_touch_helper_swipe_escape_max_velocity);
            setupCallbacks();
        }
    }
    private void setupCallbacks() {
        ViewConfiguration vc = ViewConfiguration.get(mRecyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);
        startGestureDetection();
    }
    private void destroyCallbacks() {
        if (null != mState) {
            mState.remove(mResIdPaging);
        }
        mState = null;
        mRecyclerView.removeOnItemTouchListener(mOnItemTouchListener);
        mRecoverAnimation = null;
        releaseVelocityTracker();
        stopGestureDetection();
    }
    private void startGestureDetection() {
        mPagerGestureListener = new PagerGestureListener();
        mGestureDetector = new GestureDetectorCompat(mRecyclerView.getContext(),
                mPagerGestureListener);
        mGestureDetector.setIsLongpressEnabled(false);
    }
    private void stopGestureDetection() {
        if (mPagerGestureListener != null) {
            mPagerGestureListener = null;
        }
        if (mGestureDetector != null) {
            mGestureDetector = null;
        }
    }

    private void startAnimation() {
        // find where we should animate to
        final float targetTranslateX, targetTranslateY;
        final int prevActionState = ACTION_STATE_PAGING;
        int animationType = ANIMATION_TYPE_PAGING_SUCCESS;
        int pagerDir = pagerIfNecessary();

        int page = mCallback.getPage();
        if (page == 0) {
            switch (pagerDir) {
                case RIGHT:
                case END:
                case DOWN:
                    pagerDir = 0;
                    break;
            }
        } else if (page == mCallback.getNumberOfPages() - 1) {
            switch (pagerDir) {
                case LEFT:
                case START:
                case UP:
                    pagerDir = 0;
                    break;
            }
        }

        switch (pagerDir) {
            case LEFT:
            case RIGHT:
            case START:
            case END:
                targetTranslateY = 0;
                targetTranslateX = Math.signum(mDx) * mRecyclerView.getWidth();
                break;
            case UP:
            case DOWN:
                targetTranslateX = 0;
                targetTranslateY = Math.signum(mDy) * mRecyclerView.getHeight();
                break;
            default:
                targetTranslateX = 0;
                targetTranslateY = 0;
        }


        if (pagerDir > 0) {
            animationType = ANIMATION_TYPE_PAGING_SUCCESS;
        } else {
            animationType = ANIMATION_TYPE_PAGING_CANCEL;
        }

        final int pagerDirection = pagerDir;
        /// getSelectedDxDy(mTmpPosition);
        final float currentTranslateX = mOutDx;
        final float currentTranslateY = mOutDy;
        final RecoverAnimation rv = new RecoverAnimation(animationType,
                prevActionState, currentTranslateX, currentTranslateY,
                targetTranslateX, targetTranslateY) {

            @Override
            public void onUpdate(float x, float y) {
                mCallback.onPaging(x, y, false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCallback.onPagerFinished(this.mAnimationType == ANIMATION_TYPE_PAGING_CANCEL, pagerDirection, 0, 0);
                if (this.mOverridden) {
                    return;
                }
                /*
                if (pagerDir <= 0) {
                    // this is a drag or failed swipe. recover immediately
                    // mCallback.clearView(mRecyclerView, prevSelected);
                    // full cleanup will happen on onDrawOver
                } else {
                    // wait until remove animation is complete.
                    // mPendingCleanup.add(prevSelected.itemView);
                    // mIsPendingCleanup = true;
                    if (pagerDir > 0) {
                        // Animation might be ended by other animators during a layout.
                        // We defer callback to avoid editing adapter during a layout.
                        // postDispatchSwipe(this, pagerDir);
                    }
                }

                 */

            }
        };
        final long duration = mCallback.getAnimationDuration(mRecyclerView, animationType,
                targetTranslateX - currentTranslateX, targetTranslateY - currentTranslateY);
        rv.setDuration(duration);
        mRecoverAnimation = rv;
        rv.start();
    }

    /**
     * Starts dragging or swiping the given View. Call with null if you want to clear it.
     *
     * @param selected    The ViewHolder to drag or swipe. Can be null if you want to cancel the
     *                    current action, but may not be null if actionState is ACTION_STATE_DRAG.
     * @param actionState The type of action
     */
    private void select(@Nullable ViewHolder selected, int actionState) {
        return ;
        /*
        if (selected == mSelected && actionState == mActionState) {
            return;
        }
        mDragScrollStartTimeInMs = Long.MIN_VALUE;
        final int prevActionState = mActionState;
        // prevent duplicate animations
        endRecoverAnimation(selected, true);
        mActionState = actionState;
        if (actionState == ACTION_STATE_DRAG) {
            if (selected == null) {
                throw new IllegalArgumentException("Must pass a ViewHolder when dragging");
            }
            // we remove after animation is complete. this means we only elevate the last drag
            // child but that should perform good enough as it is very hard to start dragging a
            // new child before the previous one settles.
            mOverdrawChild = selected.itemView;
            addChildDrawingOrderCallback();
        }
        int actionStateMask = (1 << (DIRECTION_FLAG_COUNT + DIRECTION_FLAG_COUNT * actionState))
                - 1;
        boolean preventLayout = false;
        if (mSelected != null) {
            final ViewHolder prevSelected = mSelected;
            if (prevSelected.itemView.getParent() != null) {
                final int swipeDir = prevActionState == ACTION_STATE_DRAG ? 0
                        : swipeIfNecessary(prevSelected);
                releaseVelocityTracker();
                mInitialSwipeDir = swipeDir;
                // find where we should animate to
                final float targetTranslateX, targetTranslateY;
                int animationType;
                switch (swipeDir) {
                    case LEFT:
                    case RIGHT:
                    case START:
                    case END:
                        targetTranslateY = 0;
                        targetTranslateX = Math.signum(mDx) * mRecyclerView.getWidth();
                        break;
                    case UP:
                    case DOWN:
                        targetTranslateX = 0;
                        targetTranslateY = Math.signum(mDy) * mRecyclerView.getHeight();
                        break;
                    default:
                        targetTranslateX = 0;
                        targetTranslateY = 0;
                }
                if (prevActionState == ACTION_STATE_DRAG) {
                    animationType = ANIMATION_TYPE_DRAG;
                } else if (swipeDir > 0) {
                    animationType = ANIMATION_TYPE_SWIPE_SUCCESS;
                } else {
                    animationType = ANIMATION_TYPE_SWIPE_CANCEL;
                }
                getSelectedDxDy(mTmpPosition);
                final float currentTranslateX = mTmpPosition[0];
                final float currentTranslateY = mTmpPosition[1];
                final RecoverAnimation rv = new RecoverAnimation(prevSelected, animationType,
                        prevActionState, currentTranslateX, currentTranslateY,
                        targetTranslateX, targetTranslateY) {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (this.mOverridden) {
                            return;
                        }
                        if (swipeDir <= 0) {
                            // this is a drag or failed swipe. recover immediately
                            mCallback.clearView(mRecyclerView, prevSelected);
                            // full cleanup will happen on onDrawOver
                        } else {
                            // wait until remove animation is complete.
                            // mPendingCleanup.add(prevSelected.itemView);
                            // mIsPendingCleanup = true;
                            if (swipeDir > 0) {
                                // Animation might be ended by other animators during a layout.
                                // We defer callback to avoid editing adapter during a layout.
                                postDispatchSwipe(this, swipeDir);
                            }
                        }
                        // removed from the list after it is drawn for the last time
                        if (mOverdrawChild == prevSelected.itemView) {
                            removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView);
                        }
                    }
                };
                final long duration = mCallback.getAnimationDuration(mRecyclerView, animationType,
                        targetTranslateX - currentTranslateX, targetTranslateY - currentTranslateY);
                rv.setDuration(duration);
                mRecoverAnimations.add(rv);
                rv.start();
                preventLayout = true;
            } else {
                removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView);
                mCallback.clearView(mRecyclerView, prevSelected);
            }
            mSelected = null;
        }
        if (selected != null) {
            mSelectedFlags =
                    (mCallback.getAbsoluteMovementFlags(mRecyclerView, selected) & actionStateMask)
                            >> (mActionState * DIRECTION_FLAG_COUNT);
            mSelectedStartX = selected.itemView.getLeft();
            mSelectedStartY = selected.itemView.getTop();
            mSelected = selected;
            if (actionState == ACTION_STATE_DRAG) {
                mSelected.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        }
        final ViewParent rvParent = mRecyclerView.getParent();
        if (rvParent != null) {
            rvParent.requestDisallowInterceptTouchEvent(mSelected != null);
        }
        if (!preventLayout) {
            mRecyclerView.getLayoutManager().requestSimpleAnimationsInNextLayout();
        }
        mCallback.onSelectedChanged(mRecyclerView, mSelected, mActionState, mInitialSwipeDir);
        mRecyclerView.invalidate();

         */
    }
    private void postDispatchSwipe(final RecoverAnimation anim, final int swipeDir) {
        // wait until animations are complete.

    }
    private boolean hasRunningRecoverAnim() {
        return !(mRecoverAnimation == null || mRecoverAnimation.mEnded);
    }
    /**
     * If user drags the view to the edge, trigger a scroll if necessary.
     */
    private boolean scrollIfNecessary() {
        return true;
        /*
        if (mSelected == null) {
            mDragScrollStartTimeInMs = Long.MIN_VALUE;
            return false;
        }
        final long now = System.currentTimeMillis();
        final long scrollDuration = mDragScrollStartTimeInMs
                == Long.MIN_VALUE ? 0 : now - mDragScrollStartTimeInMs;
        RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
        if (mTmpRect == null) {
            mTmpRect = new Rect();
        }
        int scrollX = 0;
        int scrollY = 0;
        lm.calculateItemDecorationsForChild(mSelected.itemView, mTmpRect);
        if (lm.canScrollHorizontally()) {
            int curX = (int) (mSelectedStartX + mDx);
            final int leftDiff = curX - mTmpRect.left - mRecyclerView.getPaddingLeft();
            if (mDx < 0 && leftDiff < 0) {
                scrollX = leftDiff;
            } else if (mDx > 0) {
                final int rightDiff =
                        curX + mSelected.itemView.getWidth() + mTmpRect.right
                                - (mRecyclerView.getWidth() - mRecyclerView.getPaddingRight());
                if (rightDiff > 0) {
                    scrollX = rightDiff;
                }
            }
        }
        if (lm.canScrollVertically()) {
            int curY = (int) (mSelectedStartY + mDy);
            final int topDiff = curY - mTmpRect.top - mRecyclerView.getPaddingTop();
            if (mDy < 0 && topDiff < 0) {
                scrollY = topDiff;
            } else if (mDy > 0) {
                final int bottomDiff = curY + mSelected.itemView.getHeight() + mTmpRect.bottom
                        - (mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom());
                if (bottomDiff > 0) {
                    scrollY = bottomDiff;
                }
            }
        }
        if (scrollX != 0) {
            scrollX = mCallback.interpolateOutOfBoundsScroll(mRecyclerView,
                    mSelected.itemView.getWidth(), scrollX,
                    mRecyclerView.getWidth(), scrollDuration);
        }
        if (scrollY != 0) {
            scrollY = mCallback.interpolateOutOfBoundsScroll(mRecyclerView,
                    mSelected.itemView.getHeight(), scrollY,
                    mRecyclerView.getHeight(), scrollDuration);
        }
        if (scrollX != 0 || scrollY != 0) {
            if (mDragScrollStartTimeInMs == Long.MIN_VALUE) {
                mDragScrollStartTimeInMs = now;
            }
            mRecyclerView.scrollBy(scrollX, scrollY);
            return true;
        }
        mDragScrollStartTimeInMs = Long.MIN_VALUE;
        return false;

         */
    }

    /**
     * Returns the animation type or 0 if cannot be found.
     */
    private void endRecoverAnimation(ViewHolder viewHolder, boolean override) {
        if (mRecoverAnimation != null) {
            mRecoverAnimation.mOverridden |= override;
            if (!mRecoverAnimation.mEnded) {
                mRecoverAnimation.cancel();
            }
            mRecoverAnimation = null;
        }
    }

    private void obtainVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
        mVelocityTracker = VelocityTracker.obtain();
    }
    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void updateDxDy(MotionEvent ev, int directionFlags, int pointerIndex) {
        final float x = ev.getX(pointerIndex);
        final float y = ev.getY(pointerIndex);
        // Calculate the distance moved
        mDx = x - mInitialTouchX;
        mDy = y - mInitialTouchY;

        if ((directionFlags & LEFT) == 0) {
            mDx = Math.max(0, mDx);
        }
        if ((directionFlags & RIGHT) == 0) {
            mDx = Math.min(0, mDx);
        }
        if ((directionFlags & UP) == 0) {
            mDy = Math.max(0, mDy);
        }
        if ((directionFlags & DOWN) == 0) {
            mDy = Math.min(0, mDy);
        }

        if ((directionFlags & (LEFT | RIGHT)) != 0) {
            mOutDx = mDx;
        } else {
            mOutDx = 0;
        }
        if ((directionFlags & (UP | DOWN)) != 0) {
            mOutDy = mDy;
        } else {
            mOutDy = 0;
        }
    }
    private int pagerIfNecessary() {
        final int originalMovementFlags = mCallback.getMovementFlags(mRecyclerView);
        final int absoluteMovementFlags = mCallback.convertToAbsoluteDirection(
                originalMovementFlags,
                ViewCompat.getLayoutDirection(mRecyclerView));
        final int flags = (absoluteMovementFlags
                & ACTION_MODE_SWIPE_MASK) >> (ACTION_STATE_PAGING * DIRECTION_FLAG_COUNT);
        if (flags == 0) {
            return 0;
        }

        final int originalFlags = (originalMovementFlags
                & ACTION_MODE_SWIPE_MASK) >> (ACTION_STATE_PAGING * DIRECTION_FLAG_COUNT);
        int pagerDir;
        if (Math.abs(mDx) > Math.abs(mDy)) {
            if ((pagerDir = checkHorizontalPager(flags)) > 0) {
                // if swipe dir is not in original flags, it should be the relative direction
                if ((originalFlags & pagerDir) == 0) {
                    // convert to relative
                    return Callback.convertToRelativeDirection(pagerDir,
                            ViewCompat.getLayoutDirection(mRecyclerView));
                }
                return pagerDir;
            }
            if ((pagerDir = checkVerticalPager(flags)) > 0) {
                return pagerDir;
            }
        } else {
            if ((pagerDir = checkVerticalPager(flags)) > 0) {
                return pagerDir;
            }
            if ((pagerDir = checkHorizontalPager(flags)) > 0) {
                // if swipe dir is not in original flags, it should be the relative direction
                if ((originalFlags & pagerDir) == 0) {
                    // convert to relative
                    return Callback.convertToRelativeDirection(pagerDir,
                            ViewCompat.getLayoutDirection(mRecyclerView));
                }
                return pagerDir;
            }
        }
        return 0;
    }
    private int checkHorizontalPager(int flags) {
        if ((flags & (LEFT | RIGHT)) != 0) {
            final int dirFlag = mDx > 0 ? RIGHT : LEFT;
            if (mVelocityTracker != null && mActivePointerId > -1) {
                mVelocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND,
                        mCallback.getPagerVelocityThreshold(mMaxPagerVelocity));
                final float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
                final float yVelocity = mVelocityTracker.getYVelocity(mActivePointerId);
                final int velDirFlag = xVelocity > 0f ? RIGHT : LEFT;
                final float absXVelocity = Math.abs(xVelocity);
                if ((velDirFlag & flags) != 0 && dirFlag == velDirFlag
                        && absXVelocity >= mCallback.getPagerEscapeVelocity(mPagerEscapeVelocity)
                        && absXVelocity > Math.abs(yVelocity)) {
                    return velDirFlag;
                }
            }
            final float threshold = mRecyclerView.getWidth() * mCallback.getPagerThreshold();
            if ((flags & dirFlag) != 0 && Math.abs(mDx) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }
    private int checkVerticalPager(int flags) {
        if ((flags & (UP | DOWN)) != 0) {
            final int dirFlag = mDy > 0 ? DOWN : UP;
            if (mVelocityTracker != null && mActivePointerId > -1) {
                mVelocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND,
                        mCallback.getPagerVelocityThreshold(mMaxPagerVelocity));
                final float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
                final float yVelocity = mVelocityTracker.getYVelocity(mActivePointerId);
                final int velDirFlag = yVelocity > 0f ? DOWN : UP;
                final float absYVelocity = Math.abs(yVelocity);
                if ((velDirFlag & flags) != 0 && velDirFlag == dirFlag
                        && absYVelocity >= mCallback.getPagerEscapeVelocity(mPagerEscapeVelocity)
                        && absYVelocity > Math.abs(xVelocity)) {
                    return velDirFlag;
                }
            }
            final float threshold = mRecyclerView.getHeight() * mCallback.getPagerThreshold();
            if ((flags & dirFlag) != 0 && Math.abs(mDy) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }


    /**
     * An interface which can be implemented by LayoutManager for better integration with
     * {@link RecyclerViewPager}.
     */
    public interface ViewDropHandler {
        void prepareForDrop(@NonNull View view, @NonNull View target, int x, int y);
    }

    @SuppressWarnings("UnusedParameters")
    public abstract static class Callback {
        @SuppressWarnings("WeakerAccess")
        public static final int DEFAULT_DRAG_ANIMATION_DURATION = 200;
        @SuppressWarnings("WeakerAccess")
        public static final int DEFAULT_SWIPE_ANIMATION_DURATION = 250;
        static final int RELATIVE_DIR_FLAGS = START | END
                | ((START | END) << DIRECTION_FLAG_COUNT)
                | ((START | END) << (2 * DIRECTION_FLAG_COUNT));
        private static final int ABS_HORIZONTAL_DIR_FLAGS = LEFT | RIGHT
                | ((LEFT | RIGHT) << DIRECTION_FLAG_COUNT)
                | ((LEFT | RIGHT) << (2 * DIRECTION_FLAG_COUNT));
        private static final Interpolator sDragScrollInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                return t * t * t * t * t;
            }
        };
        private static final Interpolator sDragViewScrollCapInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                t -= 1.0f;
                return t * t * t * t * t + 1.0f;
            }
        };
        /**
         * Drag scroll speed keeps accelerating until this many milliseconds before being capped.
         */
        private static final long DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS = 2000;
        private int mCachedMaxScrollSpeed = -1;


        /**
         * Replaces a movement direction with its relative version by taking layout direction into
         * account.
         *
         * @param flags           The flag value that include any number of movement flags.
         * @param layoutDirection The layout direction of the View. Can be obtained from
         *                        {@link ViewCompat#getLayoutDirection(View)}.
         * @return Updated flags which uses relative flags ({@link #START}, {@link #END}) instead
         * of {@link #LEFT}, {@link #RIGHT}.
         * @see #convertToAbsoluteDirection(int, int)
         */
        @SuppressWarnings("WeakerAccess")
        public static int convertToRelativeDirection(int flags, int layoutDirection) {
            int masked = flags & ABS_HORIZONTAL_DIR_FLAGS;
            if (masked == 0) {
                return flags; // does not have any abs flags, good.
            }
            flags &= ~masked; //remove left / right.
            if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                // no change. just OR with 2 bits shifted mask and return
                flags |= masked << 2; // START is 2 bits after LEFT, END is 2 bits after RIGHT.
                return flags;
            } else {
                // add RIGHT flag as START
                flags |= ((masked << 1) & ~ABS_HORIZONTAL_DIR_FLAGS);
                // first clean RIGHT bit then add LEFT flag as END
                flags |= ((masked << 1) & ABS_HORIZONTAL_DIR_FLAGS) << 2;
            }
            return flags;
        }

        @SuppressWarnings("WeakerAccess")
        public int convertToAbsoluteDirection(int flags, int layoutDirection) {
            int masked = flags & RELATIVE_DIR_FLAGS;
            if (masked == 0) {
                return flags; // does not have any relative flags, good.
            }
            flags &= ~masked; //remove start / end
            if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                // no change. just OR with 2 bits shifted mask and return
                flags |= masked >> 2; // START is 2 bits after LEFT, END is 2 bits after RIGHT.
                return flags;
            } else {
                // add START flag as RIGHT
                flags |= ((masked >> 1) & ~RELATIVE_DIR_FLAGS);
                // first clean start bit then add END flag as LEFT
                flags |= ((masked >> 1) & RELATIVE_DIR_FLAGS) >> 2;
            }
            return flags;
        }

        final int getAbsoluteMovementFlags(RecyclerView recyclerView) {
            final int flags = getMovementFlags(recyclerView);
            return convertToAbsoluteDirection(flags, ViewCompat.getLayoutDirection(recyclerView));
        }

        public static int makeMovementFlags(int dragFlags, int swipeFlags) {
            return makeFlag(ACTION_STATE_IDLE, swipeFlags | dragFlags)
                    | makeFlag(ACTION_STATE_PAGING, swipeFlags);
        }

        @SuppressWarnings("WeakerAccess")
        public static int makeFlag(int actionState, int directions) {
            return directions << (actionState * DIRECTION_FLAG_COUNT);
        }

        public abstract int getMovementFlags(@NonNull RecyclerView recyclerView);


        public abstract int getPage();
        public abstract int getNumberOfPages();

        public abstract boolean shouldStartPager(float x, float y);
        public abstract void beforePaging(float x, float y, float dx, float dy, int orientation);
        public abstract void onPaging(float dx, float dy, boolean draggingOrFlying);
        public abstract void onPagerFinished(boolean recovered, int direction, float offsetX, float offsetY);

        /**
         * When finding views under a dragged view, by default, ItemTouchHelper searches for views
         * that overlap with the dragged View. By overriding this method, you can extend or shrink
         * the search box.
         *
         * @return The extra margin to be added to the hit box of the dragged View.
         */
        @SuppressWarnings("WeakerAccess")
        public int getBoundingBoxMargin() {
            return 0;
        }
        /**
         * Returns the fraction that the user should move the View to be considered as swiped.
         * The fraction is calculated with respect to RecyclerView's bounds.
         * <p>
         * Default value is .5f, which means, to paging, user must move with the distance at least
         * half of RecyclerView's width or height, depending on the swipe direction.
         *
         * @return A float value that denotes the fraction of the View size. Default value
         * is .5f .
         */
        @SuppressWarnings("WeakerAccess")
        public float getPagerThreshold() {
            return .5f;
        }


        @SuppressWarnings("WeakerAccess")
        public float getPagerEscapeVelocity(float defaultValue) {
            return defaultValue;
        }

        @SuppressWarnings("WeakerAccess")
        public float getPagerVelocityThreshold(float defaultValue) {
            return defaultValue;
        }



        private int getMaxDragScroll(RecyclerView recyclerView) {
            if (mCachedMaxScrollSpeed == -1) {
                mCachedMaxScrollSpeed = recyclerView.getResources().getDimensionPixelSize(
                        R.dimen.item_touch_helper_max_drag_scroll_per_frame);
            }
            return mCachedMaxScrollSpeed;
        }



        @SuppressWarnings("WeakerAccess")
        public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType,
                                         float animateDx, float animateDy) {
            final RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return DEFAULT_SWIPE_ANIMATION_DURATION;
            } else {
                return itemAnimator.getRemoveDuration();
            }
        }

        @SuppressWarnings("WeakerAccess")
        public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView,
                                                int viewSize, int viewSizeOutOfBounds,
                                                int totalSize, long msSinceStartScroll) {
            final int maxScroll = getMaxDragScroll(recyclerView);
            final int absOutOfBounds = Math.abs(viewSizeOutOfBounds);
            final int direction = (int) Math.signum(viewSizeOutOfBounds);
            // might be negative if other direction
            float outOfBoundsRatio = Math.min(1f, 1f * absOutOfBounds / viewSize);
            final int cappedScroll = (int) (direction * maxScroll
                    * sDragViewScrollCapInterpolator.getInterpolation(outOfBoundsRatio));
            final float timeRatio;
            if (msSinceStartScroll > DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS) {
                timeRatio = 1f;
            } else {
                timeRatio = (float) msSinceStartScroll / DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS;
            }
            final int value = (int) (cappedScroll * sDragScrollInterpolator
                    .getInterpolation(timeRatio));
            if (value == 0) {
                return viewSizeOutOfBounds > 0 ? 1 : -1;
            }
            return value;
        }
    }

    private class PagerGestureListener implements GestureDetector.OnGestureListener {

        PagerGestureListener() {
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mInPagableArea) {
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (mInPaging == false) {
                    float absX = Math.abs(dx);
                    float absY = Math.abs(dy);
                    if (absX > absY) {
                        mInPaging = true;
                    } else {
                        // Cancel this dragging
                        mInPagableArea = false;
                    }
                }

                if (mInPaging) {
                    final int activePointerIndex = e2.findPointerIndex(mActivePointerId);
                    if (activePointerIndex >= 0) {
                        // checkSelectForSwipe(action, event, activePointerIndex);
                    }

                    if (activePointerIndex >= 0) {
                        updateDxDy(e2, mSelectedFlags, activePointerIndex);
                    }

                    mCallback.onPaging(mOutDx, mOutDy, true);
                }

            }
            return mInPagableArea && mInPaging;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }
    }

    private abstract static class RecoverAnimation implements Animator.AnimatorListener {
        final float mStartDx;
        final float mStartDy;
        final float mTargetX;
        final float mTargetY;
        final int mActionState;
        private final ValueAnimator mValueAnimator;
        final int mAnimationType;
        // boolean mIsPendingCleanup;
        float mX;
        float mY;
        // if user starts touching a recovering view, we put it into interaction mode again,
        // instantly.
        boolean mOverridden = false;
        boolean mEnded = false;
        private float mFraction;
        RecoverAnimation( int animationType,
                         int actionState, float startDx, float startDy, float targetX, float targetY) {
            mActionState = actionState;
            mAnimationType = animationType;

            mStartDx = startDx;
            mStartDy = startDy;
            mTargetX = targetX;
            mTargetY = targetY;
            mValueAnimator = ValueAnimator.ofFloat(0f, 1f);
            mValueAnimator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            setFraction(animation.getAnimatedFraction());
                        }
                    });
            // mValueAnimator.setTarget(viewHolder.itemView);
            mValueAnimator.addListener(this);
            setFraction(0f);
        }
        public void setDuration(long duration) {
            mValueAnimator.setDuration(duration);
        }
        public void start() {
            // mViewHolder.setIsRecyclable(false);
            mValueAnimator.start();
        }
        public void cancel() {
            mValueAnimator.cancel();
        }
        public void setFraction(float fraction) {
            mFraction = fraction;
            if (DEBUG) {
                Log.i(TAG, "setFraction: " + fraction);
            }
            update();
            onUpdate(mX, mY);
        }
        /**
         * We run updates on onDraw method but use the fraction from animator callback.
         * This way, we can sync translate x/y values w/ the animators to avoid one-off frames.
         */
        public void update() {
            if (mStartDx == mTargetX) {
                // mX = mViewHolder.itemView.getTranslationX();
            } else {
                mX = mStartDx + mFraction * (mTargetX - mStartDx);
            }
            if (mStartDy == mTargetY) {
                // mY = mViewHolder.itemView.getTranslationY();
            } else {
                mY = mStartDy + mFraction * (mTargetY - mStartDy);
            }
        }

        public abstract void onUpdate(float x, float y);

        @Override
        public void onAnimationStart(Animator animation) {
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            if (!mEnded) {
                // mViewHolder.setIsRecyclable(true);
            }
            mEnded = true;
        }
        @Override
        public void onAnimationCancel(Animator animation) {
            setFraction(1f); //make sure we recover the view's state.
        }
        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
}