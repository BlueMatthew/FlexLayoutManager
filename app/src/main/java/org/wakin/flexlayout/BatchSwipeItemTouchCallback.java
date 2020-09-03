package org.wakin.flexlayout;

import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.List;

public class BatchSwipeItemTouchCallback extends ItemTouchHelper.Callback {

    static public interface RecyclerViewAdapter {

        boolean isItemPagable(int pos);
        int getCurrentPage();
        int getNumberOfPages();

        Object onPaginationStarted(int pos, int initialDirection);  // return BatchSwipeContext
        void onPaging(int pos, float offsetX, float offsetY, boolean scrollingOrAnimation, Object batchSwipeContext);
        void onPaginationFinished(int pos, int direction, Object batchSwipeContext);
        void onSwipeFinished(int pos, boolean cancelled, int direction, Object batchSwipeContext);
    }

    private boolean mPagable = false;
    private boolean mSwiped = false;
    private int mDirection = 0;
    private RecyclerViewAdapter mAdapter;
    private Object mBatchSwipeContext;
    private float mPrevDx;
    private float mPrevDy;

    public BatchSwipeItemTouchCallback(RecyclerViewAdapter adapter){
        mAdapter = adapter;
    }

    protected void updateBundledViews(RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, boolean setOrClearElevation) {
        /*
        if (null != mBundledViewHolders) {
            boolean hasNewElevation = false;
            float newElevation = 0f;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (setOrClearElevation) {
                    Object originalElevation = recyclerView.getTag(R.id.item_touch_helper_previous_elevation);
                    if (originalElevation == null) {
                        originalElevation = ViewCompat.getElevation(recyclerView);
                        newElevation = 1f + findMaxElevation(recyclerView, viewHolder.itemView);
                        hasNewElevation = true;
                        recyclerView.setTag(R.id.item_touch_helper_previous_elevation, originalElevation);
                    }
                } else {
                    final Object tag = recyclerView.getTag(R.id.item_touch_helper_previous_elevation);
                    if (tag != null && tag instanceof Float) {
                        newElevation = (Float) tag;
                        hasNewElevation = true;
                    }
                    recyclerView.setTag(R.id.item_touch_helper_previous_elevation, null);
                }
            }

            for (ViewHolder bundledViewHolder : mBundledViewHolders) {
                if (null != bundledViewHolder && null != bundledViewHolder.itemView) {
                    if (hasNewElevation) {
                        ViewCompat.setElevation(bundledViewHolder.itemView, newElevation);
                    }
                    // Log.d("ITH", String.format("Move BundledView: %d, dX=%f, dY=%f", bundledViewHolder.getAdapterPosition(), dX, dY));
                    bundledViewHolder.itemView.setTranslationX(dX);
                    bundledViewHolder.itemView.setTranslationY(dY);
                }
            }
        }

         */
    }

    private static float findMaxElevation(RecyclerView recyclerView, View itemView) {
        final int childCount = recyclerView.getChildCount();
        float max = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = recyclerView.getChildAt(i);
            if (child == itemView) {
                continue;
            }
            final float elevation = ViewCompat.getElevation(child);
            if (elevation > max) {
                max = elevation;
            }
        }
        return max;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue * 4;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
        // int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int dragFlags = 0;
        int swipeFlags = 0;
        int pageSize = mAdapter.getNumberOfPages();

        if (pageSize > 1 && mAdapter.isItemPagable(viewHolder.getAdapterPosition()))
        {
            // How to handle HORIZONTAL direction???
            int page = mAdapter.getCurrentPage();
            if (page > 0) swipeFlags |= ItemTouchHelper.RIGHT;
            if (page < (pageSize - 1)) swipeFlags |= ItemTouchHelper.LEFT;
        }
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false; // mPagable ?
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getBoundingBoxMargin () {
        return 0;
    }


    @Override
    public long getAnimationDuration (RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
        if (animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS || animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL) {
            return this.DEFAULT_SWIPE_ANIMATION_DURATION;
        }

        return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
    }

    @Override
    public void onSelectedChanged(RecyclerView recyclerView, ViewHolder viewHolder, int actionState, int initialDirection) {

        super.onSelectedChanged(recyclerView, viewHolder, actionState, initialDirection);

        Log.i("ITH", String.format("onSelectedChanged pos=%d, actionState=%d, initialDirection=%d",
                (null == viewHolder) ? -1 : viewHolder.getAdapterPosition(), actionState, initialDirection));

        if (ItemTouchHelper.ACTION_STATE_SWIPE == actionState && null != viewHolder) {
            mDirection = 0;
            mPagable = mAdapter.isItemPagable(viewHolder.getAdapterPosition());
            if (mPagable) {
                mPrevDx = 0f;
                mPrevDy = 0f;
                mSwiped = false;
                mBatchSwipeContext = mAdapter.onPaginationStarted(viewHolder.getAdapterPosition(), initialDirection);
            }
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        // Log.i("ITH", String.format("clearView %s", ""));

        if (mPagable) {
            // updateBundledViews(recyclerView, viewHolder, 0f, 0f, false);
            // If panition is finished successfully, there must be a direction which is not 0
            mAdapter.onSwipeFinished(viewHolder.getAdapterPosition(), mDirection == 0, mDirection, mBatchSwipeContext);
            mBatchSwipeContext = null;
        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public void onScroll(RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean scrollingOrAnimation) {
        super.onScroll(recyclerView, viewHolder, dX, dY, actionState, scrollingOrAnimation);

        Log.i("ITH", String.format("onScroll dx=%f, actionState=%d, scrollingOrAnimation=%d", dX, actionState, (scrollingOrAnimation ? 1 : 0)));

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (mPagable && !mSwiped) {
                if (mPrevDx != dX || mPrevDy != dY) {
                    // updateBundledViews(recyclerView, viewHolder, dX, dY, true);
                    mAdapter.onPaging(viewHolder.getAdapterPosition(), dX, dY, scrollingOrAnimation, mBatchSwipeContext);

                    mPrevDx = dX;
                    mPrevDy = dY;
                }
            }
        }
    }

    @Override
    public void onSwiped(RecyclerView recyclerView, ViewHolder viewHolder, int direction) {
        Log.i("ITH", String.format("onSwiped direction=%d", direction));
        if (mPagable)
        {
            mSwiped = true;
            mDirection = direction;
            mAdapter.onPaginationFinished(viewHolder.getAdapterPosition(), direction, mBatchSwipeContext);
            // updateBundledViews(recyclerView, viewHolder, 0f, 0f, false);
        }
    }

    // public void onChildDraw(Canvas c, RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        // ItemTouchUIUtilImpl.INSTANCE.onDraw(c, recyclerView, viewHolder.itemView, dX, dY, actionState, isCurrentlyActive);
    // }

}
