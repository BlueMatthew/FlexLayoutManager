package org.wakin.flexlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
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
import androidx.recyclerview.widget.ItemTouchUIUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewPager extends RecyclerView.ItemDecoration
        implements RecyclerView.OnChildAttachStateChangeListener {

    /**
     * Up direction, used for swipe & drag control.
     */
    public static final int VERTICAL = RecyclerView.VERTICAL;
    public static final int HORIZONTAL = RecyclerView.HORIZONTAL;

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
    public static final int ACTION_STATE_SWIPE = 1;
    /**
     * A View is currently being dragged.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ACTION_STATE_DRAG = 2;
    /**
     * Animation type for views which are swiped successfully.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ANIMATION_TYPE_SWIPE_SUCCESS = 1 << 1;
    /**
     * Animation type for views which are not completely swiped thus will animate back to their
     * original position.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ANIMATION_TYPE_SWIPE_CANCEL = 1 << 2;
    /**
     * Animation type for views that were dragged and now will animate to their final position.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ANIMATION_TYPE_DRAG = 1 << 3;
    private static final String TAG = "ItemTouchHelper";
    private static final boolean DEBUG = true;
    private static final int ACTIVE_POINTER_ID_NONE = -1;
    static final int DIRECTION_FLAG_COUNT = 8;
    private static final int ACTION_MODE_IDLE_MASK = (1 << DIRECTION_FLAG_COUNT) - 1;
    static final int ACTION_MODE_SWIPE_MASK = ACTION_MODE_IDLE_MASK << DIRECTION_FLAG_COUNT;
    static final int ACTION_MODE_DRAG_MASK = ACTION_MODE_SWIPE_MASK << DIRECTION_FLAG_COUNT;
    /**
     * The unit we are using to track velocity
     */
    private static final int PIXELS_PER_SECOND = 1000;
    /**
     * Views, whose state should be cleared after they are detached from RecyclerView.
     * This is necessary after swipe dismissing an item. We wait until animator finishes its job
     * to clean these views.
     */
    final List<View> mPendingCleanup = new ArrayList<>();
    /**
     * Re-use array to calculate dx dy for a ViewHolder
     */
    private final float[] mTmpPosition = new float[2];
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
    private float mSwipeEscapeVelocity;
    /**
     * Set when ItemTouchHelper is assigned to a RecyclerView.
     */
    private float mMaxSwipeVelocity;
    /**
     * The diff between the last event and initial touch.
     */
    private float mDx;
    private float mDy;
    /**
     * The coordinates of the selected view at the time it is selected. We record these values
     * when action starts so that we can consistently position it even if LayoutManager moves the
     * View.
     */
    private float mSelectedStartX;
    private float mSelectedStartY;
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
     * {@link Callback#getAbsoluteMovementFlags(RecyclerView, ViewHolder)} for the current
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
    List<RecoverAnimation> mRecoverAnimations = new ArrayList<>();
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
    //re-used list for selecting a swap target
    private List<ViewHolder> mSwapTargets;
    //re used for for sorting swap targets
    private List<Integer> mDistances;
    /**
     * If drag & drop is supported, we use child drawing order to bring them to front.
     */
    private RecyclerView.ChildDrawingOrderCallback mChildDrawingOrderCallback = null;
    /**
     * This keeps a reference to the child dragged by the user. Even after user stops dragging,
     * until view reaches its final position (end of recover animation), we keep a reference so
     * that it can be drawn above other children.
     */
    private View mOverdrawChild = null;
    /**
     * We cache the position of the overdraw child to avoid recalculating it each time child
     * position callback is called. This value is invalidated whenever a child is attached or
     * detached.
     */
    private int mOverdrawChildPosition = -1;
    /**
     * Used to detect long press.
     */
    private GestureDetectorCompat mGestureDetector;
    /**
     * Callback for when long press occurs.
     */
    private ItemTouchHelperGestureListener mItemTouchHelperGestureListener;
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
                /*
                if (mSelected == null) {
                    final RecoverAnimation animation = findAnimation(event);
                    if (animation != null) {
                        mInitialTouchX -= animation.mX;
                        mInitialTouchY -= animation.mY;
                        endRecoverAnimation(animation.mViewHolder, true);
                        if (mPendingCleanup.remove(animation.mViewHolder.itemView)) {
                            mCallback.clearView(mRecyclerView, animation.mViewHolder);
                        }
                        select(animation.mViewHolder, animation.mActionState);
                        updateDxDy(event, mSelectedFlags, 0);
                    }
                }
                 */
            } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                mActivePointerId = ACTIVE_POINTER_ID_NONE;
                select(null, ACTION_STATE_IDLE);
            } else if (mActivePointerId != ACTIVE_POINTER_ID_NONE) {
                // in a non scroll orientation, if distance change is above threshold, we
                // can select the item
                final int index = event.findPointerIndex(mActivePointerId);
                if (DEBUG) {
                    Log.d(TAG, "pointer index " + index);
                }
                if (index >= 0) {
                    checkSelectForSwipe(action, event, index);
                }
            }
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
            }
            return true;
            // return mSelected != null;
        }
        @Override
        public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            if (DEBUG) {
                Log.d(TAG,
                        "on touch: x:" + mInitialTouchX + ",y:" + mInitialTouchY + ", :" + event);
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
                checkSelectForSwipe(action, event, activePointerIndex);
            }
            /*
            ViewHolder viewHolder = mSelected;
            if (viewHolder == null) {
                return;
            }
             */
            switch (action) {
                case MotionEvent.ACTION_MOVE: {
                    // Find the index of the active pointer and fetch its position
                    Log.i("Flex", "TouchMove: rawX=" + event.getRawX() + " rawY=" + event.getRawY());


                    if (activePointerIndex >= 0) {
                        Log.i("Flex", "TouchMove: active x=" + event.getX(activePointerIndex) + " y=" + event.getY(activePointerIndex));
                        updateDxDy(event, mSelectedFlags, activePointerIndex);
                        // moveIfNecessary(viewHolder);
                        mRecyclerView.removeCallbacks(mScrollRunnable);
                        mScrollRunnable.run();
                        mRecyclerView.invalidate();
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
     * Temporary rect instance that is used when we need to lookup Item decorations.
     */
    private Rect mTmpRect;
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
            mSwipeEscapeVelocity = resources
                    .getDimension(R.dimen.item_touch_helper_swipe_escape_velocity);
            mMaxSwipeVelocity = resources
                    .getDimension(R.dimen.item_touch_helper_swipe_escape_max_velocity);
            setupCallbacks();
        }
    }
    private void setupCallbacks() {
        ViewConfiguration vc = ViewConfiguration.get(mRecyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mRecyclerView.addItemDecoration(this);
        mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);
        mRecyclerView.addOnChildAttachStateChangeListener(this);
        startGestureDetection();
    }
    private void destroyCallbacks() {
        if (null != mState) {
            mState.remove(mResIdPaging);
        }
        mState = null;
        mRecyclerView.removeItemDecoration(this);
        mRecyclerView.removeOnItemTouchListener(mOnItemTouchListener);
        mRecyclerView.removeOnChildAttachStateChangeListener(this);
        // clean all attached
        final int recoverAnimSize = mRecoverAnimations.size();
        for (int i = recoverAnimSize - 1; i >= 0; i--) {
            final RecoverAnimation recoverAnimation = mRecoverAnimations.get(0);
            mCallback.clearView(mRecyclerView, recoverAnimation.mViewHolder);
        }
        mRecoverAnimations.clear();
        mOverdrawChild = null;
        mOverdrawChildPosition = -1;
        releaseVelocityTracker();
        stopGestureDetection();
    }
    private void startGestureDetection() {
        mItemTouchHelperGestureListener = new ItemTouchHelperGestureListener();
        mGestureDetector = new GestureDetectorCompat(mRecyclerView.getContext(),
                mItemTouchHelperGestureListener);
    }
    private void stopGestureDetection() {
        if (mItemTouchHelperGestureListener != null) {
            mItemTouchHelperGestureListener.doNotReactToLongPress();
            mItemTouchHelperGestureListener = null;
        }
        if (mGestureDetector != null) {
            mGestureDetector = null;
        }
    }
    private void getSelectedDxDy(float[] outPosition) {
        outPosition[0] = mDx;
        outPosition[1] = mDy;
        /*
        if ((mSelectedFlags & (LEFT | RIGHT)) != 0) {
            outPosition[0] = mSelectedStartX + mDx - mSelected.itemView.getLeft();
        } else {
            outPosition[0] = mSelected.itemView.getTranslationX();
        }
        if ((mSelectedFlags & (UP | DOWN)) != 0) {
            outPosition[1] = mSelectedStartY + mDy - mSelected.itemView.getTop();
        } else {
            outPosition[1] = mSelected.itemView.getTranslationY();
        }

         */
    }
    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (null == mState) {
            mState = state;
        }
        float dx = 0, dy = 0;
        // if (mSelected != null) {
            getSelectedDxDy(mTmpPosition);
            dx = mTmpPosition[0];
            dy = mTmpPosition[1];
        // }
        mCallback.onDrawOver(c, parent, null,
                mRecoverAnimations, mActionState, dx, dy);
    }
    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (null == mState) {
            mState = state;
        }
        // we don't know if RV changed something so we should invalidate this index.
        mOverdrawChildPosition = -1;
        float dx = 0, dy = 0;
        // if (mSelected != null) {
            getSelectedDxDy(mTmpPosition);
            dx = mTmpPosition[0];
            dy = mTmpPosition[1];
        // }
        mCallback.onDraw(c, parent, null,
                mRecoverAnimations, mActionState, dx, dy);
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
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (mRecyclerView != null && mRecyclerView.isAttachedToWindow()
                        && !anim.mOverridden
                        && anim.mViewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    final RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
                    // if animator is running or we have other active recover animations, we try
                    // not to call onSwiped because DefaultItemAnimator is not good at merging
                    // animations. Instead, we wait and batch.
                    if ((animator == null || !animator.isRunning(null))
                            && !hasRunningRecoverAnim()) {
                        mCallback.onSwiped(mRecyclerView, anim.mViewHolder, swipeDir);
                        if (!anim.mIsPendingCleanup) {
                            removeChildDrawingOrderCallbackIfNecessary(null != anim.mViewHolder ? anim.mViewHolder.itemView : null);
                            mCallback.clearView(mRecyclerView, anim.mViewHolder);
                        }
                    } else {
                        mRecyclerView.post(this);
                    }
                }
            }
        });
    }
    private boolean hasRunningRecoverAnim() {
        final int size = mRecoverAnimations.size();
        for (int i = 0; i < size; i++) {
            if (!mRecoverAnimations.get(i).mEnded) {
                return true;
            }
        }
        return false;
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
    private List<ViewHolder> findSwapTargets(ViewHolder viewHolder) {
        if (mSwapTargets == null) {
            mSwapTargets = new ArrayList<>();
            mDistances = new ArrayList<>();
        } else {
            mSwapTargets.clear();
            mDistances.clear();
        }
        final int margin = mCallback.getBoundingBoxMargin();
        final int left = Math.round(mSelectedStartX + mDx) - margin;
        final int top = Math.round(mSelectedStartY + mDy) - margin;
        final int right = left + viewHolder.itemView.getWidth() + 2 * margin;
        final int bottom = top + viewHolder.itemView.getHeight() + 2 * margin;
        final int centerX = (left + right) / 2;
        final int centerY = (top + bottom) / 2;
        final RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
        final int childCount = lm.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View other = lm.getChildAt(i);
            if (other == viewHolder.itemView) {
                continue; //myself!
            }
            if (other.getBottom() < top || other.getTop() > bottom
                    || other.getRight() < left || other.getLeft() > right) {
                continue;
            }
            final ViewHolder otherVh = mRecyclerView.getChildViewHolder(other);
            if (mCallback.canDropOver(mRecyclerView, null, otherVh)) {
                // find the index to add
                final int dx = Math.abs(centerX - (other.getLeft() + other.getRight()) / 2);
                final int dy = Math.abs(centerY - (other.getTop() + other.getBottom()) / 2);
                final int dist = dx * dx + dy * dy;
                int pos = 0;
                final int cnt = mSwapTargets.size();
                for (int j = 0; j < cnt; j++) {
                    if (dist > mDistances.get(j)) {
                        pos++;
                    } else {
                        break;
                    }
                }
                mSwapTargets.add(pos, otherVh);
                mDistances.add(pos, dist);
            }
        }
        return mSwapTargets;
    }
    /**
     * Checks if we should swap w/ another view holder.
     */
    private void moveIfNecessary(ViewHolder viewHolder) {
        if (mRecyclerView.isLayoutRequested()) {
            return;
        }
        if (mActionState != ACTION_STATE_DRAG) {
            return;
        }
        final float threshold = mCallback.getMoveThreshold(viewHolder);
        final int x = (int) (mSelectedStartX + mDx);
        final int y = (int) (mSelectedStartY + mDy);
        if (Math.abs(y - viewHolder.itemView.getTop()) < viewHolder.itemView.getHeight() * threshold
                && Math.abs(x - viewHolder.itemView.getLeft())
                < viewHolder.itemView.getWidth() * threshold) {
            return;
        }
        List<ViewHolder> swapTargets = findSwapTargets(viewHolder);
        if (swapTargets.size() == 0) {
            return;
        }
        // may swap.
        ViewHolder target = mCallback.chooseDropTarget(viewHolder, swapTargets, x, y);
        if (target == null) {
            mSwapTargets.clear();
            mDistances.clear();
            return;
        }
        final int toPosition = target.getAdapterPosition();
        final int fromPosition = viewHolder.getAdapterPosition();
        if (mCallback.onMove(mRecyclerView, viewHolder, target)) {
            // keep target visible
            mCallback.onMoved(mRecyclerView, viewHolder, fromPosition,
                    target, toPosition, x, y);
        }
    }
    @Override
    public void onChildViewAttachedToWindow(@NonNull View view) {
    }
    @Override
    public void onChildViewDetachedFromWindow(@NonNull View view) {
        removeChildDrawingOrderCallbackIfNecessary(view);
        final ViewHolder holder = mRecyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }
        endRecoverAnimation(holder, false); // this may push it into pending cleanup list.
        if (mPendingCleanup.remove(holder.itemView)) {
            mCallback.clearView(mRecyclerView, holder);
        }
    }
    /**
     * Returns the animation type or 0 if cannot be found.
     */
    private void endRecoverAnimation(ViewHolder viewHolder, boolean override) {
        final int recoverAnimSize = mRecoverAnimations.size();
        for (int i = recoverAnimSize - 1; i >= 0; i--) {
            final RecoverAnimation anim = mRecoverAnimations.get(i);
            if (anim.mViewHolder == viewHolder) {
                anim.mOverridden |= override;
                if (!anim.mEnded) {
                    anim.cancel();
                }
                mRecoverAnimations.remove(i);
                return;
            }
        }
    }
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.setEmpty();
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
    private ViewHolder findSwipedView(MotionEvent motionEvent) {
        final RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
        if (mActivePointerId == ACTIVE_POINTER_ID_NONE) {
            return null;
        }
        final int pointerIndex = motionEvent.findPointerIndex(mActivePointerId);
        final float dx = motionEvent.getX(pointerIndex) - mInitialTouchX;
        final float dy = motionEvent.getY(pointerIndex) - mInitialTouchY;
        final float absDx = Math.abs(dx);
        final float absDy = Math.abs(dy);
        if (absDx < mSlop && absDy < mSlop) {
            return null;
        }
        if (absDx > absDy && lm.canScrollHorizontally()) {
            return null;
        } else if (absDy > absDx && lm.canScrollVertically()) {
            return null;
        }
        View child = findChildView(motionEvent);
        if (child == null) {
            return null;
        }
        return mRecyclerView.getChildViewHolder(child);
    }
    /**
     * Checks whether we should select a View for swiping.
     */
    private void checkSelectForSwipe(int action, MotionEvent motionEvent, int pointerIndex) {
        return;
        /*
        if (mSelected != null || action != MotionEvent.ACTION_MOVE
                || mActionState == ACTION_STATE_DRAG || !mCallback.isItemViewSwipeEnabled()) {
            return;
        }
        if (mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
            return;
        }
        final ViewHolder vh = findSwipedView(motionEvent);
        if (vh == null) {
            return;
        }
        final int movementFlags = mCallback.getAbsoluteMovementFlags(mRecyclerView, vh);
        final int swipeFlags = (movementFlags & ACTION_MODE_SWIPE_MASK)
                >> (DIRECTION_FLAG_COUNT * ACTION_STATE_SWIPE);
        if (swipeFlags == 0) {
            return;
        }
        // mDx and mDy are only set in allowed directions. We use custom x/y here instead of
        // updateDxDy to avoid swiping if user moves more in the other direction
        final float x = motionEvent.getX(pointerIndex);
        final float y = motionEvent.getY(pointerIndex);
        // Calculate the distance moved
        final float dx = x - mInitialTouchX;
        final float dy = y - mInitialTouchY;
        // swipe target is chose w/o applying flags so it does not really check if swiping in that
        // direction is allowed. This why here, we use mDx mDy to check slope value again.
        final float absDx = Math.abs(dx);
        final float absDy = Math.abs(dy);
        if (absDx < mSlop && absDy < mSlop) {
            return;
        }
        mInitialSwipeDir = 0;
        if (absDx > absDy) {
            if (dx < 0 && (swipeFlags & LEFT) == 0) {
                return;
            }
            if (dx > 0 && (swipeFlags & RIGHT) == 0) {
                return;
            }
            if (dx < 0) {
                mInitialSwipeDir |= LEFT;
            } else if (dx > 0) {
                mInitialSwipeDir |= RIGHT;
            }
        } else {
            if (dy < 0 && (swipeFlags & UP) == 0) {
                return;
            }
            if (dy > 0 && (swipeFlags & DOWN) == 0) {
                return;
            }
            if (dy < 0) {
                mInitialSwipeDir |= UP;
            } else if (dy > 0) {
                mInitialSwipeDir |= DOWN;
            }
        }

        mDx = mDy = 0f;
        mActivePointerId = motionEvent.getPointerId(0);
        select(vh, ACTION_STATE_SWIPE);

         */
    }
    private View findChildView(MotionEvent event) {
        return null;
        /*
        // first check elevated views, if none, then call RV
        final float x = event.getX();
        final float y = event.getY();
        if (mSelected != null) {
            final View selectedView = mSelected.itemView;
            if (hitTest(selectedView, x, y, mSelectedStartX + mDx, mSelectedStartY + mDy)) {
                return selectedView;
            }
        }
        for (int i = mRecoverAnimations.size() - 1; i >= 0; i--) {
            final RecoverAnimation anim = mRecoverAnimations.get(i);
            final View view = anim.mViewHolder.itemView;
            if (hitTest(view, x, y, anim.mX, anim.mY)) {
                return view;
            }
        }
        return mRecyclerView.findChildViewUnder(x, y);

         */
    }

    public void startDrag(@NonNull ViewHolder viewHolder) {
        if (!mCallback.hasDragFlag(mRecyclerView, viewHolder)) {
            Log.e(TAG, "Start drag has been called but dragging is not enabled");
            return;
        }
        if (viewHolder.itemView.getParent() != mRecyclerView) {
            Log.e(TAG, "Start drag has been called with a view holder which is not a child of "
                    + "the RecyclerView which is controlled by this ItemTouchHelper.");
            return;
        }
        obtainVelocityTracker();
        mDx = mDy = 0f;
        select(viewHolder, ACTION_STATE_DRAG);
    }

    public void startSwipe(@NonNull ViewHolder viewHolder) {
        if (!mCallback.hasSwipeFlag(mRecyclerView, viewHolder)) {
            Log.e(TAG, "Start swipe has been called but swiping is not enabled");
            return;
        }
        if (viewHolder.itemView.getParent() != mRecyclerView) {
            Log.e(TAG, "Start swipe has been called with a view holder which is not a child of "
                    + "the RecyclerView controlled by this ItemTouchHelper.");
            return;
        }
        obtainVelocityTracker();
        mDx = mDy = 0f;
        select(viewHolder, ACTION_STATE_SWIPE);
    }
    private RecoverAnimation findAnimation(MotionEvent event) {
        if (mRecoverAnimations.isEmpty()) {
            return null;
        }
        View target = findChildView(event);
        for (int i = mRecoverAnimations.size() - 1; i >= 0; i--) {
            final RecoverAnimation anim = mRecoverAnimations.get(i);
            if (anim.mViewHolder.itemView == target) {
                return anim;
            }
        }
        return null;
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

        if (null != mCallback) {
            float dx = 0, dy = 0;
            // if (mSelected != null) {
                getSelectedDxDy(mTmpPosition);
                dx = mTmpPosition[0];
                dy = mTmpPosition[1];
            // }

            mCallback.onScroll(mRecyclerView, null, dx, dy, mActionState, true);
        }
    }
    private int swipeIfNecessary(ViewHolder viewHolder) {
        if (mActionState == ACTION_STATE_DRAG) {
            return 0;
        }
        final int originalMovementFlags = mCallback.getMovementFlags(mRecyclerView, viewHolder);
        final int absoluteMovementFlags = mCallback.convertToAbsoluteDirection(
                originalMovementFlags,
                ViewCompat.getLayoutDirection(mRecyclerView));
        final int flags = (absoluteMovementFlags
                & ACTION_MODE_SWIPE_MASK) >> (ACTION_STATE_SWIPE * DIRECTION_FLAG_COUNT);
        if (flags == 0) {
            return 0;
        }
        final int originalFlags = (originalMovementFlags
                & ACTION_MODE_SWIPE_MASK) >> (ACTION_STATE_SWIPE * DIRECTION_FLAG_COUNT);
        int swipeDir;
        if (Math.abs(mDx) > Math.abs(mDy)) {
            if ((swipeDir = checkHorizontalSwipe(viewHolder, flags)) > 0) {
                // if swipe dir is not in original flags, it should be the relative direction
                if ((originalFlags & swipeDir) == 0) {
                    // convert to relative
                    return Callback.convertToRelativeDirection(swipeDir,
                            ViewCompat.getLayoutDirection(mRecyclerView));
                }
                return swipeDir;
            }
            if ((swipeDir = checkVerticalSwipe(viewHolder, flags)) > 0) {
                return swipeDir;
            }
        } else {
            if ((swipeDir = checkVerticalSwipe(viewHolder, flags)) > 0) {
                return swipeDir;
            }
            if ((swipeDir = checkHorizontalSwipe(viewHolder, flags)) > 0) {
                // if swipe dir is not in original flags, it should be the relative direction
                if ((originalFlags & swipeDir) == 0) {
                    // convert to relative
                    return Callback.convertToRelativeDirection(swipeDir,
                            ViewCompat.getLayoutDirection(mRecyclerView));
                }
                return swipeDir;
            }
        }
        return 0;
    }
    private int checkHorizontalSwipe(ViewHolder viewHolder, int flags) {
        if ((flags & (LEFT | RIGHT)) != 0) {
            final int dirFlag = mDx > 0 ? RIGHT : LEFT;
            if (mVelocityTracker != null && mActivePointerId > -1) {
                mVelocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND,
                        mCallback.getSwipeVelocityThreshold(mMaxSwipeVelocity));
                final float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
                final float yVelocity = mVelocityTracker.getYVelocity(mActivePointerId);
                final int velDirFlag = xVelocity > 0f ? RIGHT : LEFT;
                final float absXVelocity = Math.abs(xVelocity);
                if ((velDirFlag & flags) != 0 && dirFlag == velDirFlag
                        && absXVelocity >= mCallback.getSwipeEscapeVelocity(mSwipeEscapeVelocity)
                        && absXVelocity > Math.abs(yVelocity)) {
                    return velDirFlag;
                }
            }
            final float threshold = mRecyclerView.getWidth() * mCallback
                    .getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(mDx) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }
    private int checkVerticalSwipe(ViewHolder viewHolder, int flags) {
        if ((flags & (UP | DOWN)) != 0) {
            final int dirFlag = mDy > 0 ? DOWN : UP;
            if (mVelocityTracker != null && mActivePointerId > -1) {
                mVelocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND,
                        mCallback.getSwipeVelocityThreshold(mMaxSwipeVelocity));
                final float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
                final float yVelocity = mVelocityTracker.getYVelocity(mActivePointerId);
                final int velDirFlag = yVelocity > 0f ? DOWN : UP;
                final float absYVelocity = Math.abs(yVelocity);
                if ((velDirFlag & flags) != 0 && velDirFlag == dirFlag
                        && absYVelocity >= mCallback.getSwipeEscapeVelocity(mSwipeEscapeVelocity)
                        && absYVelocity > Math.abs(xVelocity)) {
                    return velDirFlag;
                }
            }
            final float threshold = mRecyclerView.getHeight() * mCallback
                    .getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(mDy) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }
    private void addChildDrawingOrderCallback() {
        if (Build.VERSION.SDK_INT >= 21) {
            return; // we use elevation on Lollipop
        }
        if (mChildDrawingOrderCallback == null) {
            mChildDrawingOrderCallback = new RecyclerView.ChildDrawingOrderCallback() {
                @Override
                public int onGetChildDrawingOrder(int childCount, int i) {
                    if (mOverdrawChild == null) {
                        return i;
                    }
                    int childPosition = mOverdrawChildPosition;
                    if (childPosition == -1) {
                        childPosition = mRecyclerView.indexOfChild(mOverdrawChild);
                        mOverdrawChildPosition = childPosition;
                    }
                    if (i == childCount - 1) {
                        return childPosition;
                    }
                    return i < childPosition ? i : i + 1;
                }
            };
        }
        mRecyclerView.setChildDrawingOrderCallback(mChildDrawingOrderCallback);
    }
    private void removeChildDrawingOrderCallbackIfNecessary(View view) {
        if (view == mOverdrawChild) {
            mOverdrawChild = null;
            // only remove if we've added
            if (mChildDrawingOrderCallback != null) {
                mRecyclerView.setChildDrawingOrderCallback(null);
            }
        }
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

        @SuppressWarnings("WeakerAccess")
        @NonNull
        public static ItemTouchUIUtil getDefaultUIUtil() {
            return ItemTouchUIUtilImpl.INSTANCE;
        }
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

        public static int makeMovementFlags(int dragFlags, int swipeFlags) {
            return makeFlag(ACTION_STATE_IDLE, swipeFlags | dragFlags)
                    | makeFlag(ACTION_STATE_SWIPE, swipeFlags)
                    | makeFlag(ACTION_STATE_DRAG, dragFlags);
        }

        @SuppressWarnings("WeakerAccess")
        public static int makeFlag(int actionState, int directions) {
            return directions << (actionState * DIRECTION_FLAG_COUNT);
        }

        public abstract int getMovementFlags(@NonNull RecyclerView recyclerView,
                                             @NonNull ViewHolder viewHolder);

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
        final int getAbsoluteMovementFlags(RecyclerView recyclerView,
                                           ViewHolder viewHolder) {
            final int flags = getMovementFlags(recyclerView, viewHolder);
            return convertToAbsoluteDirection(flags, ViewCompat.getLayoutDirection(recyclerView));
        }
        boolean hasDragFlag(RecyclerView recyclerView, ViewHolder viewHolder) {
            final int flags = getAbsoluteMovementFlags(recyclerView, viewHolder);
            return (flags & ACTION_MODE_DRAG_MASK) != 0;
        }
        boolean hasSwipeFlag(RecyclerView recyclerView,
                             ViewHolder viewHolder) {
            final int flags = getAbsoluteMovementFlags(recyclerView, viewHolder);
            return (flags & ACTION_MODE_SWIPE_MASK) != 0;
        }

        @SuppressWarnings("WeakerAccess")
        public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull ViewHolder current,
                                   @NonNull ViewHolder target) {
            return true;
        }

        public abstract boolean onMove(@NonNull RecyclerView recyclerView,
                                       @NonNull ViewHolder viewHolder, @NonNull ViewHolder target);

        public boolean isLongPressDragEnabled() {
            return true;
        }
        /**
         * Returns whether ItemTouchHelper should start a swipe operation if a pointer is swiped
         * over the View.
         * <p>
         * Default value returns true but you may want to disable this if you want to start
         * swiping on a custom view touch using {@link #startSwipe(ViewHolder)}.
         *
         * @return True if ItemTouchHelper should start swiping an item when user swipes a pointer
         * over the View, false otherwise. Default value is <code>true</code>.
         * @see #startSwipe(ViewHolder)
         */
        public boolean isItemViewSwipeEnabled() {
            return true;
        }
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
         * Default value is .5f, which means, to swipe a View, user must move the View at least
         * half of RecyclerView's width or height, depending on the swipe direction.
         *
         * @param viewHolder The ViewHolder that is being dragged.
         * @return A float value that denotes the fraction of the View size. Default value
         * is .5f .
         */
        @SuppressWarnings("WeakerAccess")
        public float getSwipeThreshold(@NonNull ViewHolder viewHolder) {
            return .5f;
        }
        /**
         * Returns the fraction that the user should move the View to be considered as it is
         * dragged. After a view is moved this amount, ItemTouchHelper starts checking for Views
         * below it for a possible drop.
         *
         * @param viewHolder The ViewHolder that is being dragged.
         * @return A float value that denotes the fraction of the View size. Default value is
         * .5f .
         */
        @SuppressWarnings("WeakerAccess")
        public float getMoveThreshold(@NonNull ViewHolder viewHolder) {
            return .5f;
        }

        @SuppressWarnings("WeakerAccess")
        public float getSwipeEscapeVelocity(float defaultValue) {
            return defaultValue;
        }

        @SuppressWarnings("WeakerAccess")
        public float getSwipeVelocityThreshold(float defaultValue) {
            return defaultValue;
        }

        @SuppressWarnings("WeakerAccess")
        public ViewHolder chooseDropTarget(@NonNull ViewHolder selected,
                                           @NonNull List<ViewHolder> dropTargets, int curX, int curY) {
            int right = curX + selected.itemView.getWidth();
            int bottom = curY + selected.itemView.getHeight();
            ViewHolder winner = null;
            int winnerScore = -1;
            final int dx = curX - selected.itemView.getLeft();
            final int dy = curY - selected.itemView.getTop();
            final int targetsSize = dropTargets.size();
            for (int i = 0; i < targetsSize; i++) {
                final ViewHolder target = dropTargets.get(i);
                if (dx > 0) {
                    int diff = target.itemView.getRight() - right;
                    if (diff < 0 && target.itemView.getRight() > selected.itemView.getRight()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dx < 0) {
                    int diff = target.itemView.getLeft() - curX;
                    if (diff > 0 && target.itemView.getLeft() < selected.itemView.getLeft()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dy < 0) {
                    int diff = target.itemView.getTop() - curY;
                    if (diff > 0 && target.itemView.getTop() < selected.itemView.getTop()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dy > 0) {
                    int diff = target.itemView.getBottom() - bottom;
                    if (diff < 0 && target.itemView.getBottom() > selected.itemView.getBottom()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
            }
            return winner;
        }

        public abstract void onSwiped(RecyclerView recyclerView, @NonNull ViewHolder viewHolder, int direction);

        public void onSelectedChanged(@NonNull RecyclerView recyclerView, @Nullable ViewHolder viewHolder, int actionState, int initialDirection) {
            if (viewHolder != null) {
                ItemTouchUIUtilImpl.INSTANCE.onSelected(viewHolder.itemView);
            }
        }
        private int getMaxDragScroll(RecyclerView recyclerView) {
            if (mCachedMaxScrollSpeed == -1) {
                mCachedMaxScrollSpeed = recyclerView.getResources().getDimensionPixelSize(
                        R.dimen.item_touch_helper_max_drag_scroll_per_frame);
            }
            return mCachedMaxScrollSpeed;
        }

        public void onScroll(@NonNull RecyclerView recyclerView,
                             @NonNull ViewHolder viewHolder, float dX, float dY, int actionState, boolean scrollingOrAnimation) {
        }

        public void onMoved(@NonNull final RecyclerView recyclerView,
                            @NonNull final ViewHolder viewHolder, int fromPos, @NonNull final ViewHolder target,
                            int toPos, int x, int y) {
            final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof ViewDropHandler) {
                ((ViewDropHandler) layoutManager).prepareForDrop(viewHolder.itemView,
                        target.itemView, x, y);
                return;
            }
            // if layout manager cannot handle it, do some guesswork
            if (layoutManager.canScrollHorizontally()) {
                final int minLeft = layoutManager.getDecoratedLeft(target.itemView);
                if (minLeft <= recyclerView.getPaddingLeft()) {
                    recyclerView.scrollToPosition(toPos);
                }
                final int maxRight = layoutManager.getDecoratedRight(target.itemView);
                if (maxRight >= recyclerView.getWidth() - recyclerView.getPaddingRight()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }
            if (layoutManager.canScrollVertically()) {
                final int minTop = layoutManager.getDecoratedTop(target.itemView);
                if (minTop <= recyclerView.getPaddingTop()) {
                    recyclerView.scrollToPosition(toPos);
                }
                final int maxBottom = layoutManager.getDecoratedBottom(target.itemView);
                if (maxBottom >= recyclerView.getHeight() - recyclerView.getPaddingBottom()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }
        }

        void onDraw(Canvas c, RecyclerView parent, ViewHolder selected,
                    List<RecyclerViewPager.RecoverAnimation> recoverAnimationList,
                    int actionState, float dX, float dY) {
            final int recoverAnimSize = recoverAnimationList.size();
            for (int i = 0; i < recoverAnimSize; i++) {
                final RecyclerViewPager.RecoverAnimation anim = recoverAnimationList.get(i);
                anim.update();
                final int count = c.save();
                onChildDraw(c, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState,
                        false);
                c.restoreToCount(count);

                onScroll(parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState, false);
            }
            if (selected != null) {

                final int count = c.save();
                onChildDraw(c, parent, selected, dX, dY, actionState, true);
                c.restoreToCount(count);

                // It means user is dragging the itemview if selected != null
                // The itemView will be moved by touch event but not here
                // So we don't need to call onScroll
            }
        }
        void onDrawOver(Canvas c, RecyclerView parent, ViewHolder selected,
                        List<RecyclerViewPager.RecoverAnimation> recoverAnimationList,
                        int actionState, float dX, float dY) {
            final int recoverAnimSize = recoverAnimationList.size();
            for (int i = 0; i < recoverAnimSize; i++) {
                final RecyclerViewPager.RecoverAnimation anim = recoverAnimationList.get(i);
                final int count = c.save();
                onChildDrawOver(c, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState,
                        false);
                c.restoreToCount(count);
            }
            if (selected != null) {
                final int count = c.save();
                onChildDrawOver(c, parent, selected, dX, dY, actionState, true);
                c.restoreToCount(count);
            }
            boolean hasRunningAnimation = false;
            for (int i = recoverAnimSize - 1; i >= 0; i--) {
                final RecoverAnimation anim = recoverAnimationList.get(i);
                if (anim.mEnded && !anim.mIsPendingCleanup) {
                    recoverAnimationList.remove(i);
                } else if (!anim.mEnded) {
                    hasRunningAnimation = true;
                }
            }
            if (hasRunningAnimation) {
                parent.invalidate();
            }

            // if animator is running or we have other active recover animations, we try
            // not to call onSwiped because DefaultItemAnimator is not good at merging
            // animations. Instead, we wait and batch.

        }

        public void clearView(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
            ItemTouchUIUtilImpl.INSTANCE.clearView(viewHolder.itemView);
        }

        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            ItemTouchUIUtilImpl.INSTANCE.onDraw(c, recyclerView, viewHolder.itemView, dX, dY,
                    actionState, isCurrentlyActive);
        }

        public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
            ItemTouchUIUtilImpl.INSTANCE.onDrawOver(c, recyclerView, viewHolder.itemView, dX, dY,
                    actionState, isCurrentlyActive);
        }

        @SuppressWarnings("WeakerAccess")
        public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType,
                                         float animateDx, float animateDy) {
            final RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return animationType == ANIMATION_TYPE_DRAG ? DEFAULT_DRAG_ANIMATION_DURATION
                        : DEFAULT_SWIPE_ANIMATION_DURATION;
            } else {
                return animationType == ANIMATION_TYPE_DRAG ? itemAnimator.getMoveDuration()
                        : itemAnimator.getRemoveDuration();
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

    private class ItemTouchHelperGestureListener extends GestureDetector.SimpleOnGestureListener {

        private boolean mShouldReactToLongPress = true;
        ItemTouchHelperGestureListener() {
        }
        /**
         * Call to prevent executing code in response to
         * {@link ItemTouchHelperGestureListener#onLongPress(MotionEvent)} being called.
         */
        void doNotReactToLongPress() {
            mShouldReactToLongPress = false;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            if (!mShouldReactToLongPress) {
                return;
            }
            View child = findChildView(e);
            if (child != null) {
                ViewHolder vh = mRecyclerView.getChildViewHolder(child);
                if (vh != null) {
                    if (!mCallback.hasDragFlag(mRecyclerView, vh)) {
                        return;
                    }
                    int pointerId = e.getPointerId(0);
                    // Long press is deferred.
                    // Check w/ active pointer id to avoid selecting after motion
                    // event is canceled.
                    if (pointerId == mActivePointerId) {
                        final int index = e.findPointerIndex(mActivePointerId);
                        final float x = e.getX(index);
                        final float y = e.getY(index);
                        mInitialTouchX = x;
                        mInitialTouchY = y;
                        mInitialSwipeDir = 0;
                        mDx = mDy = 0f;
                        if (DEBUG) {
                            Log.d(TAG,
                                    "onlong press: x:" + mInitialTouchX + ",y:" + mInitialTouchY);
                        }
                        if (mCallback.isLongPressDragEnabled()) {
                            select(vh, ACTION_STATE_DRAG);
                        }
                    }
                }
            }
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }
    }
    private static class RecoverAnimation implements Animator.AnimatorListener {
        final float mStartDx;
        final float mStartDy;
        final float mTargetX;
        final float mTargetY;
        final ViewHolder mViewHolder;
        final int mActionState;
        private final ValueAnimator mValueAnimator;
        final int mAnimationType;
        boolean mIsPendingCleanup;
        float mX;
        float mY;
        // if user starts touching a recovering view, we put it into interaction mode again,
        // instantly.
        boolean mOverridden = false;
        boolean mEnded = false;
        private float mFraction;
        RecoverAnimation(ViewHolder viewHolder, int animationType,
                         int actionState, float startDx, float startDy, float targetX, float targetY) {
            mActionState = actionState;
            mAnimationType = animationType;
            mViewHolder = viewHolder;
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
            mValueAnimator.setTarget(viewHolder.itemView);
            mValueAnimator.addListener(this);
            setFraction(0f);
        }
        public void setDuration(long duration) {
            mValueAnimator.setDuration(duration);
        }
        public void start() {
            mViewHolder.setIsRecyclable(false);
            mValueAnimator.start();
        }
        public void cancel() {
            mValueAnimator.cancel();
        }
        public void setFraction(float fraction) {
            mFraction = fraction;
        }
        /**
         * We run updates on onDraw method but use the fraction from animator callback.
         * This way, we can sync translate x/y values w/ the animators to avoid one-off frames.
         */
        public void update() {
            if (mStartDx == mTargetX) {
                mX = mViewHolder.itemView.getTranslationX();
            } else {
                mX = mStartDx + mFraction * (mTargetX - mStartDx);
            }
            if (mStartDy == mTargetY) {
                mY = mViewHolder.itemView.getTranslationY();
            } else {
                mY = mStartDy + mFraction * (mTargetY - mStartDy);
            }
        }
        @Override
        public void onAnimationStart(Animator animation) {
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            if (!mEnded) {
                mViewHolder.setIsRecyclable(true);
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