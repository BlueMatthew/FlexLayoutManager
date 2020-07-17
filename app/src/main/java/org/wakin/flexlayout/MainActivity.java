package org.wakin.flexlayout;


import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsoluteLayout;

import org.wakin.flexlayout.LayoutManager.Graphics.Insets;
import org.wakin.flexlayout.LayoutManager.LayoutCallback;
import org.wakin.flexlayout.LayoutManager.FlexLayoutManager;
import org.wakin.flexlayout.LayoutManager.SectionPosition;
import org.wakin.flexlayout.LayoutManager.Graphics.Size;

import java.util.List;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements LayoutCallback {

    private static String TAG = "Flex";
    private static boolean DEBUG = true;
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

    private HashMap<Integer, Bundle> mRecyclerViewStates = new HashMap<>();

    private PaginationView mLeftRecyclerView = null;
    private PaginationView mRightRecyclerView = null;

    private boolean mIsCatBarStickyMode = false;
    private Point mStickyItemPoint = new Point();

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

        // Create ItemTouchCallback
        mCallback = new BatchSwipeItemTouchCallback(mAdapter);
        //Create ItemtouchHelper
        mTouchHelper = new ItemTouchHelper(mCallback);
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

    private void setFlexLayoutManager() {

        mRecyclerView.setBackgroundColor(Color.LTGRAY);
        FlexLayoutManager layoutManager = new FlexLayoutManager(this, FlexLayoutManager.VERTICAL, false, this);

        for (SectionPosition sp : mDataSource.getStickyItems()) {
            layoutManager.addStickyItem(sp.section, sp.item);
        }

        // layoutManager.setStackedStickyItems(false);
        // layoutManager.setLayoutCallback(this);

        mRecyclerView.setLayoutManager(layoutManager);
    }

    protected PaginationView buildRecyclerViewForPagination(int catIndex, final int position, int viewId, boolean leftOrRight, final boolean inSticky) {

        final PageRecyclerView recyclerView = new PageRecyclerView(mRecyclerView.getContext());
        recyclerView.setRecycledViewPool(mRecyclerView.getRecycledViewPool());

        recyclerView.setId(viewId);
        // recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = MainActivityHelper.createLayoutManagerForPagination(mRecyclerView);
        recyclerView.setLayoutManager(layoutManager);

        final int minPagablePos = mDataSource.getMinPagablePosition();
        if (RecyclerView.NO_POSITION == minPagablePos) {
            // ASSERT false
            return null;
        }

        RecyclerView.LayoutManager mainLayoutManager = mRecyclerView.getLayoutManager();

        int posCat = minPagablePos - 1;
        FlexRecyclerView.SectionViewHolder viewHolderCat = (FlexRecyclerView.SectionViewHolder) mRecyclerView.findViewHolderForAdapterPosition(posCat);
        View viewCCat = (null != viewHolderCat) ? viewHolderCat.itemView : null;

        int width = mRecyclerView.getWidth();

        int top = 0;
        if (null != viewCCat) {
            // top += (int)(mainLayoutManager.getDecoratedTop(view) + mainLayoutManager.getDecoratedMeasuredHeight(view));
            top = (int)(viewCCat.getY() + mainLayoutManager.getDecoratedMeasuredHeight(viewCCat));
        }
        int height = mRecyclerView.getHeight() - top;
        // int top = (int)(mRecyclerView.getY() + view.getY() + view.getHeight());
        int left = catIndex > mDataSource.getPage() ? (mRecyclerView.getLeft() + width) : (mRecyclerView.getLeft() - width);

        AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(width, height, left, top);
        recyclerView.setLayoutParams(lp);

        ViewGroup viewGroup = (ViewGroup)mRecyclerView.getParent();
        viewGroup.addView(recyclerView);

        PaginationAdapter adapter = new PaginationAdapter(false, catIndex, 0);

        // Restore scroll offset
        final int page = catIndex;
        final int minPagableItemPosition = mDataSource.getMinPagablePosition();

        PositionAndOffset po = null;
        if (inSticky) {
            po = getStoredPositionAndOffset(page, false);
            // Log.i("ITH", String.format("postOnAnimationDelayed runnable page=%d", page));
        }

        if (null == po) {
            po = new PositionAndOffset(minPagableItemPosition, 0);
            // Log.i("ITH", String.format("scrollToPositionWithOffset minPagableItemPosition=%d offset=0, page=%d", minPagableItemPosition, page));
        }

        MainActivityHelper.scrollToPositionWithOffset(recyclerView, po.position, po.offset);

        recyclerView.setAdapter(adapter);
        // recyclerView.requestLayout();

        final PositionAndOffset finalPo = po;
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                adjustRecyclerViewForPagination(recyclerView, page, finalPo);
            }
        });

        return recyclerView;
    }

    protected void adjustRecyclerViewForPagination(final RecyclerView recyclerView, final int page, final PositionAndOffset po) {
        if (recyclerView.isInLayout()) {
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    adjustRecyclerViewForPagination(recyclerView, page, po);
                }
            });
            return;
        }

        int firstItemPos = MainActivityHelper.findFirstVisibleItemPosition(recyclerView);
        int lastItemPos = MainActivityHelper.findLastVisibleItemPosition(recyclerView);

        int startItemPos = Math.max(firstItemPos, po.position);
        int visibleHeight = po.offset;
        for (; startItemPos <= lastItemPos; startItemPos++) {
            ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(startItemPos);
            visibleHeight += recyclerView.getLayoutManager().getDecoratedMeasuredHeight(viewHolder.itemView);
        }

        if (visibleHeight < recyclerView.getHeight()) {

            // final SimpleItemAnimator animator = ((SimpleItemAnimator)recyclerView.getItemAnimator());
            // recyclerView.setItemAnimator(null);

            // RecyclerView.Adapter adapter = recyclerView.getAdapter();
            // if (adapter instanceof PaginationAdapter) {
            //    ((PaginationAdapter)adapter).setPaddingBottom((recyclerView.getHeight() - visibleHeight), true);
            // }
            // MainActivityHelper.scrollToPositionWithOffset(recyclerView, po.position, po.offset);
            // recyclerView.setItemAnimator(animator);

            recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), (recyclerView.getHeight() - visibleHeight));
            recyclerView.setClipToPadding(false);
            MainActivityHelper.scrollToPositionWithOffset(recyclerView, po.position, po.offset);
            Log.i("ITH", String.format("scrollToPositionWithOffset pos=%d offset=0, page=%d, mPaddingBottom=%d", po.position, page, (recyclerView.getHeight() - visibleHeight)));
        }
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

    protected PageWebView buildWebViewForPagination(RecyclerView recyclerView, int catIndex) {

        RecyclerView.LayoutParams lp = buildLayoutParamsForRecyclerView(recyclerView);

        final PageWebView webView = new PageWebView(mRecyclerView.getContext());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                return false;
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setId(R.id.web_view);
        webView.setLayoutParams(lp);

        return webView;
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

        private int mMainAdapter = 0;
        private int mPaddingBottom = 0;

        public PaginationAdapter() {
            mMainAdapter = 1;
            // setCatIndex(0);
        }

        public PaginationAdapter(boolean mainAdapter, int catIndex, int baseGroupIndex) {
            mMainAdapter = mainAdapter ? 1 : 0;
            // setCatIndex(catIndex);
        }

        public int getPaddingBottom() {
            return mPaddingBottom;
        }

        public void setPaddingBottom(int paddingBottom) {
            setPaddingBottom(paddingBottom, false);
        }

        public void setPaddingBottom(final int paddingBottom, boolean notifyChanged) {
            /*
            if (paddingBottom == mPaddingBottom) {
                return;
            }

            final int orgPaddingBottom = mPaddingBottom;
            final int itemCount = getItemCount();

            if (notifyChanged) {
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mPaddingBottom = paddingBottom;
                        if (orgPaddingBottom == 0) {
                            notifyItemInserted(itemCount);
                        } else if (paddingBottom == 0) {
                            notifyItemRemoved(itemCount - 1);
                        }
                    }
                });
            }

            */
        }


        @Override
        public FlexRecyclerView.SectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return mDataSource == null ? null : mDataSource.createViewHolder(parent, viewType);
        }

        protected boolean swapViewHolder(ViewHolder viewHolder, ViewHolder viewHolderForSwap) {
            if (null == viewHolder || null == viewHolderForSwap || null == viewHolder.itemView || null == viewHolderForSwap.itemView) {
                return false;
            }

            /*
            View viewContainer = viewHolder.itemView.findViewById(R.id.itemContainerView);
            View viewContainerForSwap = viewHolderForSwap.itemView.findViewById(R.id.itemContainerView);

            if (null == viewContainer || null == viewContainerForSwap) {
                return false;
            }

            ViewGroup viewParent = (ViewGroup) viewContainer.getParent();
            ViewGroup viewParentForSwap = (ViewGroup)viewContainerForSwap.getParent();

            viewParent.removeView(viewContainer);
            viewParentForSwap.removeView(viewContainerForSwap);

            viewParent.addView(viewContainerForSwap);
            viewParentForSwap.addView(viewContainer);
            */

            return true;
        }

        @Override
        public void onBindViewHolder(FlexRecyclerView.SectionViewHolder holder, int position, List<Object> payloads) {

            if (null == payloads || 0 == payloads.size()) {
                onBindViewHolder(holder, position);
                return;
            }

            boolean handled = false;
            Object payload = payloads.get(0);

            if (payload instanceof RecyclerView) {

                RecyclerView recyclerView = (RecyclerView)payload;
                long itemId = holder.getItemId();
                if (itemId != -1) {
                    ViewHolder viewHolderForSwap = recyclerView.findViewHolderForAdapterPosition(holder.getAdapterPosition());
                    if (swapViewHolder(holder, viewHolderForSwap)) {
                        handled = true;
                    }
                }
            }

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
        public int getPageSize() {
            return mDataSource == null ? 1 : mDataSource.getPageSize();
        }

        @Override
        public boolean isItemPagable(int pos) {
            // int minPagablePos = calcMinPagablePosition();
            // return minPagablePos == RecyclerView.NO_POSITION ? false : (pos >= minPagablePos);
            return false;
        }

        protected void tryToBuildPaginationRecyclerView(boolean leftOrRight, int page, int pos, boolean inSticky) {

            if (page > 0 && null == mLeftRecyclerView && !leftOrRight) {
                int newPage = mDataSource.getPage() - 1;
                Log.i("ITH", String.format("buildRecyclerViewForCat:%d =>left", newPage));
                if (false /*newPage == PAGE_INDEX_OF_WEBVIEW*/) {
                    // mLeftRecyclerView = buildWebViewContainerForPagination(newPage, pos, R.id.left_page_view, true, inSticky);
                    mLeftRecyclerView = buildRecyclerViewForPagination(newPage, pos, R.id.left_page_view, true, inSticky);
                } else {
                    mLeftRecyclerView = buildRecyclerViewForPagination(newPage, pos, R.id.left_page_view, true, inSticky);
                }
            }

            if (page < (getPageSize() - 1) && null == mRightRecyclerView && leftOrRight) {
                int newPage = mDataSource.getPage() + 1;
                Log.i("ITH", String.format("buildRecyclerViewForCat:%d =>right", newPage));
                if (false /*newPage == PAGE_INDEX_OF_WEBVIEW*/) {
                    // mRightRecyclerView = buildWebViewContainerForPagination(newPage, pos, R.id.right_page_view, false, inSticky);
                    mRightRecyclerView = buildRecyclerViewForPagination(newPage, pos, R.id.right_page_view, false, inSticky);
                } else {
                    mRightRecyclerView = buildRecyclerViewForPagination(newPage, pos, R.id.right_page_view, false, inSticky);
                }
            }
        }

        @Override
        public Object onPaginationStarted(int pos, int initialDirection) {
            Log.i("ITH", "onPaginationStarted");

            int page = mDataSource.getPage();

            tryToBuildPaginationRecyclerView((initialDirection & ItemTouchHelper.LEFT) != 0, page, pos, mIsCatBarStickyMode);

            return null;
        }

        @Override
        public void onPaging(int pos, float offsetX, float offsetY, boolean scrollingOrAnimation, Object batchSwipeContext) {

            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof FlexLayoutManager) {
                FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;
            }

            float absX = java.lang.Math.abs(offsetX);
            if (true/*absX < 50 || absX > 1070*/) {
                // Log.i("ITH", String.format("onPaging offsetX=%f offsetY=%f scrollingOrAnimation=%d", offsetX, offsetY, scrollingOrAnimation ? 1 : 0));
            }
            if (offsetY != 0) {
                Log.i("ITH", String.format("onPaging offsetX=%f offsetY=%f scrollingOrAnimation=%d", offsetX, offsetY, scrollingOrAnimation ? 1 : 0));
            }

            final boolean inSticky = mIsCatBarStickyMode;
            final int page = mDataSource.getPage();

            if (scrollingOrAnimation && offsetX != 0f) {
                // During scrolling, user may change direction and need to create new pageview for new direction
                tryToBuildPaginationRecyclerView(offsetX < 0f, page, pos, inSticky);
            }

            if (null != mLeftRecyclerView) {
                mLeftRecyclerView.offsetView(offsetX, offsetY);
            }
            if (null != mRightRecyclerView) {
                mRightRecyclerView.offsetView(offsetX, offsetY);
            }
        }

        public void onPaginationFinished(int pos, int direction, Object batchSwipeContext) {
            final boolean inSticky = mIsCatBarStickyMode;

            Log.i("ITH", String.format("onPaginationFinished, inSticky=%d", (inSticky ? 1 : 0)));

            final int page = mDataSource.getPage();
            int minPagablePos = mDataSource.getMinPagablePosition();
            int orgItemCount = getItemCount();
            // ASSERT minPagablePos == RecyclerView.NO_POSITION ???

            int posCat = minPagablePos - 1;

            int firstPosition = MainActivityHelper.findFirstVisibleItemPosition(mRecyclerView);
            firstPosition = MainActivityHelper.findFirstVisibleItemPosition(mRecyclerView);
            int lastPosition = MainActivityHelper.findLastVisibleItemPosition(mRecyclerView);

            FlexRecyclerView.SectionViewHolder viewHolder = (FlexRecyclerView.SectionViewHolder) mRecyclerView.findViewHolderForAdapterPosition(firstPosition);
            if (null == viewHolder) {
                return;
            }
            View view = viewHolder.itemView;

            FlexRecyclerView.SectionViewHolder viewHolderCat = (FlexRecyclerView.SectionViewHolder) mRecyclerView.findViewHolderForAdapterPosition(posCat);
            // Need any validation here???
            View viewCat = viewHolderCat.itemView;

            int top = view.getTop();
            int height = view.getHeight();
            int heightCat = viewCat.getHeight();
            int bottomCat = (int)(viewCat.getY() + viewCat.getHeight());
            int currentOffset = top;
            int currentPosition = firstPosition;

            int firstPagableAndVisiblePos = firstPosition;

            // Save Current RecyclerViewState first
            Bundle bundle = new Bundle();
            bundle.putInt("position", currentPosition);
            bundle.putInt("offset", currentOffset);
            if (!inSticky) {
                // NOT STICKY
                bundle.putInt("pagablePosition", firstPosition - minPagablePos);
                bundle.putInt("pagableOffset", 0);
                firstPagableAndVisiblePos = posCat + 1;
            } else {
                // IN-STICKY
                // Find first visible position under the Cat
                int bottom = (int)(view.getY() + view.getHeight());

                while (bottom <= bottomCat && firstPagableAndVisiblePos <= lastPosition) {
                    firstPagableAndVisiblePos ++;

                    viewHolder = (FlexRecyclerView.SectionViewHolder)mRecyclerView.findViewHolderForAdapterPosition(firstPagableAndVisiblePos);
                    top = (int)viewHolder.itemView.getY();
                    height = viewHolder.itemView.getHeight();
                    bottom = top + height;
                }
                bundle.putInt("pagablePosition", firstPagableAndVisiblePos);
                bundle.putInt("pagableOffset", (bottom - bottomCat) - height);
            }

            Log.i("ITH", "SaveState for Page:" + Integer.toString(page) + " " + bundle.toString());
            mRecyclerViewStates.put(new Integer(page), bundle);

            // Switch Pages
            final PaginationView paginationView;
            if (direction == ItemTouchHelper.LEFT) {
                mDataSource.setPage(mDataSource.getPage() + 1);
                // MainActivity.this.mCatIndex += 1;

                paginationView = mRightRecyclerView;

            } else if (direction == ItemTouchHelper.RIGHT) {
                mDataSource.setPage(mDataSource.getPage() - 1);
                // MainActivity.this.mCatIndex -= 1;
                // setCatIndex(MainActivity.this.mCatIndex);
                paginationView = mLeftRecyclerView;
            } else {
                paginationView = null;
            }

            int pageFirstPosition = RecyclerView.NO_POSITION;
            int visibleItemCount = 0;
            int paddingBottom = 0;
            boolean clipToPadding = true;
            if (null != paginationView) {

                pageFirstPosition = MainActivityHelper.findFirstVisibleItemPosition((RecyclerView) paginationView);
                paddingBottom = ((RecyclerView) paginationView).getPaddingBottom();
                clipToPadding = ((RecyclerView) paginationView).getClipToPadding();
                if (RecyclerView.NO_POSITION != pageFirstPosition) {
                    visibleItemCount = MainActivityHelper.findLastVisibleItemPosition((RecyclerView) paginationView) - pageFirstPosition + 1;
                }

                paginationView.releaseReusableViews();
            }


            final SimpleItemAnimator animator = ((SimpleItemAnimator)mRecyclerView.getItemAnimator());
            mRecyclerView.setItemAnimator(null);

            mRecyclerView.setLayoutFrozen(true);
            // if (mPaddingBottom != mRecyclerView.getPaddingBottom()) {
            // setPaddingBottom(paddingBottom);
            mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(), mRecyclerView.getPaddingRight(), paddingBottom);
            mRecyclerView.setClipToPadding(clipToPadding);
            // }

            if (inSticky) {
                PositionAndOffset po = getStoredPositionAndOffset(getCurrentPage(), true);
                if (null == po) {
                    // make the first pagable item first-visible
                    po = new PositionAndOffset(mDataSource.getMinPagablePosition(), (int)(viewCat.getY() + mRecyclerView.getLayoutManager().getDecoratedMeasuredHeight(viewCat)));
                }

                MainActivityHelper.scrollToPositionWithOffset(mRecyclerView, po.position, po.offset);
                Log.i("ITH RVA", String.format("scrollToPositionWithOffset position=%d offset=%d, page=%d", po.position, po.offset, getCurrentPage()));

            }

            if (RecyclerView.NO_POSITION == pageFirstPosition) {
                notifyItemRemoved(pos); // ???
            } else {
                int newItemCount = getItemCount();
                // Log.i("ITH RVA", String.format("newItemCount=%d: orgItemCount=%d", newItemCount, orgItemCount));

                if (orgItemCount < newItemCount) {
                    notifyItemRangeChanged(pageFirstPosition, Math.min(visibleItemCount, orgItemCount - pageFirstPosition), paginationView);
                    int updatedItemsCount = Math.min(visibleItemCount, orgItemCount - pageFirstPosition);
                    // Log.i("ITH RVA", String.format("notifyItemRangeChanged: From:%d Count:%d", pageFirstPosition, updatedItemsCount));
                    notifyItemRangeInserted(orgItemCount, newItemCount - orgItemCount);
                    // Log.i("ITH RVA", String.format("notifyItemRangeInserted: From:%d Count:%d", orgItemCount, newItemCount - orgItemCount));
                } else if (orgItemCount > newItemCount) {
                    notifyItemRangeChanged(pageFirstPosition, Math.min(visibleItemCount, newItemCount - pageFirstPosition), paginationView);
                    int updatedItemsCount = Math.min(visibleItemCount, newItemCount - pageFirstPosition);
                    // Log.i("ITH RVA", String.format("notifyItemRangeChanged: From:%d Count:%d", pageFirstPosition, updatedItemsCount));
                    notifyItemRangeRemoved(newItemCount, orgItemCount - newItemCount);
                    // Log.i("ITH RVA", String.format("notifyItemRangeInserted: From:%d Count:%d", newItemCount, orgItemCount - newItemCount));
                }
                else {
                    notifyItemRangeChanged(pageFirstPosition, visibleItemCount, paginationView);
                    // Log.i("ITH RVA", String.format("notifyItemRangeChanged: From:%d Count:%d", pageFirstPosition, visibleItemCount));
                }

            }

            notifyItemChanged(posCat, PRELOAD_FLAG);

            // AbsoluteLayout layout = (AbsoluteLayout)mRecyclerView.getParent();
            mRecyclerView.setLayoutFrozen(false);

            final int position = pos;
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {

                    // animator.setChangeDuration(changeDuration);
                    // removePaginationRecyclerView(position, true);
                    mRecyclerView.setItemAnimator(animator);

                }
            }, 100);

            // mRecyclerView.setLayoutFrozen(false);
            mRecyclerViewStates.remove(new Integer(page));
        }

        private void removePaginationRecyclerView(int pos, boolean alreadyAsync) {

            ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(pos);
            /*
            if (null != viewHolder && null != viewHolder.itemView) {
                float translationX = viewHolder.itemView.getTranslationX();
                // Log.i("ITH RVA", String.format("reset ItemView: pos=%d, translationX=%f", pos, translationX));
                viewHolder.itemView.setTranslationX(0f);
                viewHolder.itemView.setTranslationY(0f);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    final Object tag = viewHolder.itemView.getTag(R.id.item_touch_helper_previous_elevation);
                    if (tag != null && tag instanceof Float) {
                        ViewCompat.setElevation(viewHolder.itemView, (Float) tag);
                    }
                    viewHolder.itemView.setTag(R.id.item_touch_helper_previous_elevation, null);
                }
            }

            List<ViewHolder> viewHolders = mCallback.getBundledViewHolders();
            for (ViewHolder viewHolder1 : viewHolders) {
                if (null != viewHolder1.itemView) {
                    float translationX = viewHolder.itemView.getTranslationX();
                    Log.i("ITH RVA", String.format("reset ItemView: pos=%d, translationX=%f", viewHolder1.getAdapterPosition(), translationX));
                    viewHolder1.itemView.setTranslationX(0f);
                    viewHolder1.itemView.setTranslationY(0f);
                }
            }
            */

            if (alreadyAsync) {

                if (null != mLeftRecyclerView) {
                    final PaginationView leftRecyclerView = mLeftRecyclerView;
                    mLeftRecyclerView = null;
                    Log.i("ITH", "Left removed");
                    leftRecyclerView.removeFromParentViewGroup();
                }

                if (null != mRightRecyclerView) {
                    final PaginationView rightRecyclerView = mRightRecyclerView;
                    mRightRecyclerView = null;
                    Log.i("ITH", "Right removed");
                    rightRecyclerView.removeFromParentViewGroup();
                }

            } else {
                // Remove temporary recyclerviews for pagination
                if (null != mLeftRecyclerView) {
                    final PaginationView leftRecyclerView = mLeftRecyclerView;
                    mLeftRecyclerView = null;
                    mRecyclerView.postDelayed(new Runnable() {
                        public void run() {
                            Log.i("ITH", "Left removed");
                            leftRecyclerView.removeFromParentViewGroup();
                        }
                    }, 100);
                }
                if (null != mRightRecyclerView) {
                    final PaginationView rightRecyclerView = mRightRecyclerView;
                    mRightRecyclerView = null;
                    mRecyclerView.postDelayed(new Runnable() {
                        public void run() {
                            Log.i("ITH", "Right removed");
                            rightRecyclerView.removeFromParentViewGroup();
                        }
                    }, 100);
                }
            }
        }

        public void onSwipeFinished(int pos, boolean cancelled, int direction, Object batchSwipeContext) {
            // Log.i("ITH", "onSwipeFinished");

            removePaginationRecyclerView(pos, false);
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
        return mDataSource == null ? 0 : mDataSource.getSection(section).itemCount;
    }

    @Override
    public Insets getInsetsForSection(int section) {
        return mDataSource == null ? Insets.NONE : mDataSource.getSection(section).insets;
    }

    @Override
    public int getMinimumInteritemSpacingForSection(int section) {
        return mDataSource.getSection(section).interitemSpacing;
    }

    @Override
    public int getMinimumLineSpacingForSection(int section) {
        return mDataSource.getSection(section).lineSpacing;
    }

    @Override
    public boolean hasFixedItemSize(int section, Size size) {
        return false;
    }

    @Override
    public void getSizeForItem(int section, int item, Size size) {
        mDataSource.getItemSize(section, item, size);
    }

    @Override
    public boolean isFullSpanAtItem(int section, int item) {
        return mDataSource.isItemFullSpan(section, item);
    }

    @Override
    public int getNumberOfColumnsForSection(int section) {
        return mDataSource.getSection(section).columns;
    }

    @Override
    public int getInfoForItemsBatchly(int section, int itemStart, int itemCount, int[] data) {
        return mDataSource.getInfoForItemsBatchly(section, itemStart, itemCount, data);
    }

    @Override
    public void onItemEnterStickyMode(int section, int item, int position, Point point) {
        if (section == mDataSource.getMinPagableSection() && item == 0) {
            mIsCatBarStickyMode = true;
            mStickyItemPoint.set(point.x, point.y);
        }

        Log.d("Flex", "Item: [" + section + "," + item + "] enter into sticky mode");
    }

    @Override
    public void onItemExitStickyMode(int section, int item, int position) {
        if (section == mDataSource.getMinPagableSection() && item == 0) {
            mIsCatBarStickyMode = false;
        }
        Log.d("Flex", "Item: [" + section + "," + item + "] exit from sticky mode");
    }
}
