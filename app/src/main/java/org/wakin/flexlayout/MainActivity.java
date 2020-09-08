package org.wakin.flexlayout;


import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.view.View;
import android.view.ViewGroup;

import org.wakin.flexlayout.LayoutManager.Graphics.Insets;
import org.wakin.flexlayout.LayoutManager.LayoutCallback;
import org.wakin.flexlayout.LayoutManager.FlexLayoutManager;
import org.wakin.flexlayout.LayoutManager.SectionPosition;
import org.wakin.flexlayout.LayoutManager.Graphics.Size;

import java.util.List;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements LayoutCallback {

    private final static String TAG = "Flex";
    private final static boolean DEBUG = true;
    private static Object PRELOAD_FLAG = new Object();
    private static Object PRELOAD_FLAG_SWAP = new Object();

    private static class PositionAndOffset {
        public int position;
        public int offset;

        public PositionAndOffset(int position, int offset) {
            this.position = position;
            this.offset = offset;
        }
    }

    private boolean mStackedStickyHeaders = true;
    private FlexRecyclerView mRecyclerView;
    private PaginationAdapter mAdapter;
    BatchSwipeItemTouchCallback mCallback;
    ItemTouchHelper mTouchHelper;

    private int mPendingPageStart = 0;
    private int mPendingPageCount = 1;

    private HashMap<Integer, Bundle> mRecyclerViewStates = new HashMap<>();

    private boolean mIsCatBarStickyMode = false;
    private Rect mNavFrame = new Rect();
    private Rect mStickyItemFrame = new Rect();
    private int mStickyItemSize = 0;

    private MainActivityDataSource mDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View container = findViewById(R.id.recycler_view_container);
        container.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                int width = right - left;
                int height = bottom - top;
                int oldWidth = oldRight - oldLeft;
                int oldHeight = oldBottom - oldTop;

                if (width > 0 && height > 0) {

                    if (mDataSource == null) {
                        int boundWidth = width - mRecyclerView.getPaddingLeft() - mRecyclerView.getPaddingRight();
                        int boundHeight = height - mRecyclerView.getPaddingTop() - mRecyclerView.getPaddingBottom();

                        mDataSource = new MainActivityDataSource(mRecyclerView.getContext(), width, height);

                        Log.d(TAG, "onLyaoutChange width=" + width);
                        // mRecyclerView.requestLayout();

                        mAdapter.notifyDataSetChanged();
                        // mRecyclerView.swapAdapter(mAdapter, true);

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // mRecyclerView.smoothScrollToPosition(100);
                                // mRecyclerView.scrollToPosition(100);


                                // FlexLayoutManager layoutManager = (FlexLayoutManager) mRecyclerView.getLayoutManager();
                                // layoutManager.scrollToPositionWithOffset(100, 60);
                                // layoutManager.scrollToPositionWithOffset(100, -20);

                                // layoutManager.smoothScrollToPosition(100, 60);

                            }
                        }, 3000);

                    }
                }
            }
        });


        Log.i("ITH", String.format("onCreate TID=%d", Thread.currentThread().getId()));

        mRecyclerView = findViewById(R.id.recycler_view);
        mAdapter = new PaginationAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // mRecyclerView.setHasFixedSize(true);
        // mRecyclerView.getRecycledViewPool().setMaxRecycledViews(VIEW_TYPE_ITEM, 5);
        Insets padding = MainActivityDataSource.RECYCLERVIEW_PADDING;
        mRecyclerView.setPadding(padding.left, padding.top, padding.right, padding.bottom);

        mRecyclerView.setItemAnimator(null);
        // Create ItemTouchCallback
        mCallback = new BatchSwipeItemTouchCallback(mAdapter);
        //Create ItemtouchHelper
        mTouchHelper = new ItemTouchHelper(mCallback, R.id.pagingFlag);
        mTouchHelper.attachToRecyclerView(mRecyclerView);

        setFlexLayoutManager();
        // setLinearLayoutManager();

        mRecyclerView.addOnScrollListener (new RecyclerView.OnScrollListener() {
            int scrollState = RecyclerView.SCROLL_STATE_IDLE;
            public void onScrollStateChanged (RecyclerView recyclerView, int newState) {
                scrollState = newState;
            }
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                if (recyclerView.getPaddingBottom() > 0) {
                    int lastPos = MainActivityHelper.findLastVisibleItemPosition(mRecyclerView);
                    if (lastPos == mAdapter.getItemCount() - 1) {
                        ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(lastPos);
                        boolean resetPaddingBottom = true;
                        if (null != viewHolder && null != viewHolder.itemView) {
                            int height = mRecyclerView.getLayoutManager().getDecoratedMeasuredHeight(viewHolder.itemView);

                            int y = (int)(viewHolder.itemView.getY() + height);

                            Log.i("ITH", String.format("onScrolled: scrollState=%d, y=%d, height=%d", scrollState, y, mRecyclerView.getHeight()));

                            if (y < mRecyclerView.getHeight()) {
                                resetPaddingBottom = false;
                            }
                        }
                        if (resetPaddingBottom) {
                            mRecyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), 0);
                            mRecyclerView.setClipToPadding(false);
                        }
                    }

                    int vOffset = recyclerView.computeVerticalScrollOffset();
                    int range = recyclerView.computeVerticalScrollRange();
                }
            }
        });

    }

    @Override
    public void onDestroy() {
        mCallback = null;
        mTouchHelper.attachToRecyclerView(null);
        mTouchHelper = null;

        mRecyclerView.clearOnScrollListeners();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        int targetOrientation = (MainActivityDataSource.ORIENTATION == FlexLayoutManager.VERTICAL) ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        if(getRequestedOrientation() != targetOrientation) {
            setRequestedOrientation(targetOrientation);
        }

        super.onResume();
    }

    private void setFlexLayoutManager() {

        mRecyclerView.setBackgroundColor(Color.LTGRAY);
        FlexLayoutManager layoutManager = new FlexLayoutManager(this, MainActivityDataSource.ORIENTATION, false, this);

        for (SectionPosition sp : mDataSource.getStickyItems()) {
            layoutManager.addStickyItem(sp.section, sp.item);
        }

        layoutManager.setStackedStickyItems(true);
        // layoutManager.setLayoutCallback(this);

        mRecyclerView.setLayoutManager(layoutManager);
    }

    protected RecyclerView.LayoutParams buildLayoutParamsForRecyclerView(RecyclerView recyclerView) {

        int minPagablePos = mDataSource.getMinPagablePosition();
        if (RecyclerView.NO_POSITION == minPagablePos) {
            // ASSERT false
            return null;
        }

        // RecyclerView Height - Nav Height - Cat Height
        int height = recyclerView.getHeight();
        int posCat = minPagablePos - 1;
        FlexRecyclerView.SectionViewHolder viewHolderCat = (FlexRecyclerView.SectionViewHolder) mRecyclerView.findViewHolderForAdapterPosition(posCat);
        if (null == viewHolderCat || null == viewHolderCat.itemView) {
            height -= 105; // Workaround
        } else {
            height -= mRecyclerView.getLayoutManager().getDecoratedMeasuredHeight(viewHolderCat.itemView);
        }

        FlexRecyclerView.SectionViewHolder viewHolderNav = (FlexRecyclerView.SectionViewHolder) mRecyclerView.findViewHolderForAdapterPosition(0);
        if (null != viewHolderNav && null != viewHolderNav.itemView) {
            height -= mRecyclerView.getLayoutManager().getDecoratedMeasuredHeight(viewHolderNav.itemView);
        }

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(recyclerView.getWidth(), height);

        return lp;
    }

    protected PositionAndOffset getStoredPositionAndOffset(int page, boolean mainPage) {
        return getStoredPositionAndOffset(new Integer(page), mainPage);
    }

    protected PositionAndOffset getStoredPositionAndOffset(Integer page, boolean mainPage) {
        PositionAndOffset po = null;
        Bundle bundle = null;

        if (mRecyclerViewStates.containsKey(page)) {
            bundle = mRecyclerViewStates.get(page);
        }
        if (null != bundle) {
            po = new PositionAndOffset(bundle.getInt(mainPage ? "position" : "pagablePosition"), bundle.getInt(mainPage ? "offset" : "pagableOffset"));
        }

        return po;
    }

    class PaginationAdapter extends RecyclerView.Adapter<FlexRecyclerView.SectionViewHolder> implements BatchSwipeItemTouchCallback.RecyclerViewAdapter {


        public PaginationAdapter() {

        }

        public PaginationAdapter(boolean mainAdapter, int catIndex, int baseGroupIndex) {
            // setCatIndex(catIndex);
        }

        @Override
        public FlexRecyclerView.SectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return mDataSource == null ? null : mDataSource.createViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(FlexRecyclerView.SectionViewHolder holder, int position, List<Object> payloads) {

            if (null == payloads || 0 == payloads.size()) {
                onBindViewHolder(holder, position);
                return;
            }

            boolean handled = false;
            // Object payload = payloads.get(0);

            if (!handled) {
                onBindViewHolder(holder, position);
                return;
            }
        }

        @Override
        public void onBindViewHolder(FlexRecyclerView.SectionViewHolder holder, int position) {
            int viewType = holder.getItemViewType();
            // Log.d("PERF", String.format("onBindViewHolder, Main=%d Page=%d, position=%d", mMainAdapter, getCatIndex(), position));

            SectionPosition sectionPosition = mDataSource.toSectionPosition(position);
            holder.setSectionAndItem(sectionPosition.section, sectionPosition.item);

            mDataSource.bindCellView(position, holder);
        }

        @Override
        public int getItemCount() {
            return mDataSource == null ? 0 : mDataSource.getItemCount();
        }

        @Override
        public long getItemId (int position) {
            /*
            SectionPosition sectionPosition = calcSectionPosition(position);
            if (SECTION_INDEX_ITEM == sectionPosition.section) {
                return position;
            }
             */
            return RecyclerView.NO_ID;
        }

        @Override
        public int getItemViewType(int position) {
            return mDataSource.getViewType(position);
        }

        @Override
        public void onViewAttachedToWindow(FlexRecyclerView.SectionViewHolder holder) {
            super.onViewAttachedToWindow(holder);
        }

        @Override
        public int getCurrentPage() {
            return mDataSource == null ? 0 : mDataSource.getPage();
        }

        @Override
        public int getNumberOfPages() {
            return mDataSource == null ? 1 : mDataSource.getNumberOfPages();
        }

        @Override
        public boolean isItemPagable(int pos) {
            int minPagablePos = mDataSource.getMinPagablePosition();
            return minPagablePos == RecyclerView.NO_POSITION ? false : (pos >= minPagablePos);
        }

        @Override
        public Object onPaginationStarted(int pos, int initialDirection) {
            Log.i("ITH", "onPaginationStarted");

            int page = mDataSource.getPage();

            // tryToBuildPaginationRecyclerView((initialDirection & ItemTouchHelper.LEFT) != 0, page, pos, mIsCatBarStickyMode);

            return null;
        }

        @Override
        public void onPaging(int pos, float offsetX, float offsetY, boolean scrollingOrAnimation, Object batchSwipeContext) {

            float absX = java.lang.Math.abs(offsetX);
            if (true/*absX < 50 || absX > 1070*/) {
                Log.i("ITH", String.format("TouchMove: offsetX=%f offsetY=%f scrollingOrAnimation=%d", offsetX, offsetY, scrollingOrAnimation ? 1 : 0));
                // Log.i("ITH", String.format("onPaging offsetX=%f offsetY=%f scrollingOrAnimation=%d", offsetX, offsetY, scrollingOrAnimation ? 1 : 0));
            }
            if (offsetY != 0) {
                // Log.i("ITH", String.format("onPaging offsetX=%f offsetY=%f scrollingOrAnimation=%d", offsetX, offsetY, scrollingOrAnimation ? 1 : 0));
            }

            final boolean inSticky = mIsCatBarStickyMode;
            final int page = mDataSource.getPage();

            int pageStart = mPendingPageStart;
            int pageEnd = mPendingPageStart + mPendingPageCount - 1;
            int oldPageEnd = pageEnd;

            if (scrollingOrAnimation && offsetX != 0f) {
                // During scrolling, user may change direction and need to show new pageview in another direction
                int pageDelta = offsetX > 0 ? (int)Math.ceil(offsetX / mRecyclerView.getWidth()) : (int)Math.floor(offsetX / mRecyclerView.getWidth());
                pageStart = Math.min(pageStart, page + pageDelta);
                pageEnd = Math.max(pageEnd, page + pageDelta);
            }

            boolean layoutNeeded = false;
            if (pageStart != mPendingPageStart || oldPageEnd != pageEnd) {
                for (int pageIndex = pageStart; pageIndex <= pageEnd; ++pageIndex) {
                    if (pageIndex < mPendingPageStart || pageIndex > oldPageEnd) {
                        // Invalidate layout of pageIndex
                    }
                }
                mPendingPageStart = pageStart;
                mPendingPageCount = pageEnd - pageStart + 1;

                layoutNeeded = true;
            }

            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof FlexLayoutManager) {
                FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;
                if (layoutNeeded) {
                    // flexLayoutManager.
                }
                // flexLayoutManager.setPagingOffset((int)offsetX, (int)offsetY);
                // mRecyclerView.invalidate();
                // mRecyclerView.getAdapter().
                // mRecyclerView.scrollBy((int)offsetX, (int)offsetY);

                Bundle bundle = new Bundle();
                bundle.putInt("offsetX", (int)offsetX);
                bundle.putInt("offsetY", (int)offsetY);
                mRecyclerView.performAccessibilityAction(1, bundle);
            }
        }

        public void onPaginationFinished(int pos, int direction, Object batchSwipeContext) {
            final boolean inSticky = mIsCatBarStickyMode;

            Log.i("ITH", String.format("onPaginationFinished, inSticky=%d", (inSticky ? 1 : 0)));

            final int page = mDataSource.getPage();
            // Switch Pages
            int newPage = page;
            if (direction == ItemTouchHelper.LEFT) {
                ++newPage;
            } else if (direction == ItemTouchHelper.RIGHT) {
                --newPage;
            }

            Point newContentOffset = FlexLayoutManager.INVALID_CONTENT_OFFSET;
            if (mIsCatBarStickyMode) {
                // Save Current RecyclerViewState first
                RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
                if (layoutManager instanceof FlexLayoutManager) {
                    FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;
                    Point contentOffset = flexLayoutManager.getContentOffset();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("contentOffset", contentOffset);

                    Log.i("ITH", "SaveState for Page:" + Integer.toString(page) + " " + bundle.toString());
                    mRecyclerViewStates.put(new Integer(page), bundle);
                }

                newContentOffset = getContentOffsetForPage(newPage);
            }

            mDataSource.setPage(newPage);

            mPendingPageStart = mDataSource.getPage();
            mPendingPageCount = 1;

            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof FlexLayoutManager) {
                FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;
                if (FlexLayoutManager.INVALID_CONTENT_OFFSET != newContentOffset) {
                    // mRecyclerView.scrollTo(newContentOffset.x, newContentOffset.y);
                    // mRecyclerView.scrollP
                    // flexLayoutManager.scrollToPositionWithOffset(0, newContentOffset.y);
                    flexLayoutManager.setContentOffset(newContentOffset);
                }
                flexLayoutManager.clearPagingOffset();
            }

            // notifyItemChanged(posCat, PRELOAD_FLAG);
        }

        public void onSwipeFinished(int pos, boolean cancelled, int direction, Object batchSwipeContext) {
            // Log.i("ITH", "onSwipeFinished");

            // removePaginationRecyclerView(pos, false);
        }
    }

    /// interface LayoutCallback
    @Override
    public int getLayoutModeForSection(int section) {
        return mDataSource != null && mDataSource.isSectionWaterfallMode(section) ? FlexLayoutManager.WATERFLAALAYOUT : FlexLayoutManager.FLOWLAYOUT;
    }

    @Override
    public int getNumberOfSections() {
        return mDataSource == null ? 0 : mDataSource.getSectionCount();
    }

    @Override
    public int getNumberOfItemsInSection(int section) {
        return mDataSource == null ? 0 : mDataSource.getSection(section).getItemCount();
    }

    @Override
    public Insets getInsetsForSection(int section) {
        return mDataSource == null ? Insets.NONE : mDataSource.getSection(section).getInsets();
    }

    @Override
    public int getMinimumInteritemSpacingForSection(int section) {
        return mDataSource.getSection(section).getInteritemSpacing();
    }

    @Override
    public int getMinimumLineSpacingForSection(int section) {
        return mDataSource.getSection(section).getLineSpacing();
    }

    @Override
    public boolean hasFixedItemSize(int section, Size size) {
        return false;
    }

    @Override
    public void getSizeForItem(int section, int item, Size size) {
        // Log.i("", "FLEX prepareLayout section=" + section + " item=" + item);
        mDataSource.getItemSize(section, item, size);
    }

    @Override
    public boolean isFullSpanAtItem(int section, int item) {
        return mDataSource.isItemFullSpan(section, item);
    }

    @Override
    public int getNumberOfColumnsForSection(int section) {
        return mDataSource.getSection(section).getColumns();
    }

    @Override
    public int getInfoForItemsBatchly(int section, int itemStart, int itemCount, int[] data) {
        // Log.i("FLEX", "FLEX prepareLayout batch section=" + section + " itemStart=" + itemStart + " itemCount=" + itemCount);
        return mDataSource.getInfoForItemsBatchly(section, itemStart, itemCount, data);
    }

    @Override
    public int getPage() {
        return mDataSource.getPage();
    }

    @Override
    public int getNumberOfPages() {
        return mDataSource.getNumberOfPages();
    }

    @Override
    public int getNumberOfFixedSections() {
        return mDataSource.getNumberOfFixedSections();
    }

    @Override
    public int getNumberOfSectionsForPage(int page) {
        return mDataSource.getNumberOfSections(page);
    }

    @Override
    public int[] getPendingPages() {
        return null;
    }

    @Override
    public Point getContentOffsetForPage(int page) {
        if (mIsCatBarStickyMode && page != mDataSource.getPage()) {

            Integer pageInteger = new Integer(page);
            if (mRecyclerViewStates.containsKey(pageInteger)) {
                Bundle bundle = mRecyclerViewStates.get(pageInteger);
                return bundle.getParcelable("contentOffset");
            } else {

                RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
                if (layoutManager instanceof FlexLayoutManager) {
                    FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;

                    View view = mRecyclerView.getChildAt(mDataSource.getMinPagablePosition() - 1);

                    Point contentOffset = flexLayoutManager.getContentOffset();
                    return new Point(0, mStickyItemFrame.top - mNavFrame.height() );
                }

            }
        } else {
            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof FlexLayoutManager) {
                FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;
                return flexLayoutManager.getContentOffset();
            }
        }

        return FlexLayoutManager.INVALID_CONTENT_OFFSET;
    }

    @Override
    public void onItemEnterStickyMode(int section, int item, int position, Rect frame) {
        if (section == MainActivityDataSource.SECTION_INDEX_NAVBAR && item == 0) {
            mNavFrame.set(frame);
        } else if (section == (mDataSource.getMinPagableSection() - 1) && item == 0) {
            mIsCatBarStickyMode = true;
            mStickyItemFrame.set(frame);

            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof FlexLayoutManager) {
                FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;

                View view = mRecyclerView.getChildAt(mDataSource.getMinPagablePosition() - 1);

                Point contentOffset = flexLayoutManager.getContentOffset();
                mStickyItemSize = mStickyItemFrame.bottom - contentOffset.y;
            }


        }

        Log.d("Flex", "Item: [" + section + "," + item + "] x into sticky mode, frame=" + frame.toShortString());
    }

    @Override
    public void onItemExitStickyMode(int section, int item, int position) {
        if (section == mDataSource.getMinPagableSection() && item == 0) {
            mIsCatBarStickyMode = false;
            mRecyclerViewStates.clear();
        }
        Log.d("Flex", "Item: [" + section + "," + item + "] exit from sticky mode");
    }
}
