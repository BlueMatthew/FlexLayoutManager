package org.wakin.flexlayout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.wakin.flexlayout.layoutmanager.FlexLayoutManager;
import org.wakin.flexlayout.layoutmanager.graphics.Insets;
import org.wakin.flexlayout.layoutmanager.SectionPosition;
import org.wakin.flexlayout.layoutmanager.graphics.Size;
import org.wakin.flexlayout.cells.ItemView;
import org.wakin.flexlayout.models.PageData;
import org.wakin.flexlayout.models.PageFixedData;
import org.wakin.flexlayout.models.SectionData;
import org.wakin.flexlayout.models.CellData;

import java.util.ArrayList;
import java.util.List;

public class MainActivityDataSource {

    private static boolean DEBUG = true;
    private static boolean USING_INFLATE = false; // For perf comparison
    public final static int ORIENTATION = FlexLayoutManager.VERTICAL;

    public int ITEM_COLUMNS = 2;
    public boolean ITEM_LAYOUT_MODE_WATERFALL = true;

    // public static Insets RECYCLERVIEW_PADDING = Insets.of(20, 20, 20, 20);
    public static Insets RECYCLERVIEW_PADDING = Insets.NONE;

    public String ITEM_TEXT_ITEM =               "Page:%d SI:%d-%d";
    public String ITEM_TEXT_ITEM2 =              "Page:%d SI2:%d-%d";
    public String ITEM_URL_ITEM =                "http://api.wakin.org/images/items/%lu_%03lu.jpg";
    public String URL_FOR_TEST   =               "https://www.jianshu.com/p/4f9591291365";

    public static int SECTION_INDEX_NAVBAR =            0;
    public static int SECTION_INDEX_ENTRY  =            1;
    public static int SECTION_INDEX_TEST1  =            2;
    public static int SECTION_INDEX_TEST2  =            3;
    public static int SECTION_INDEX_CATBAR =            4;
    public static int SECTION_INDEX_ITEM =              5;
    public static int SECTION_INDEX_ITEM2 =             6;

    public static int SECTION_INDEX_ITEM1_INNER =       0;
    public static int SECTION_INDEX_ITEM2_INNER =       1;

    public static int SECTION_INDEX_OF_WEBVIEW = 3;

    public static int SECTION_INDEX_WITH_FULL_SPAN = SECTION_INDEX_ITEM;
    // public static int SECTION_INDEX_WITH_PAGINATION = SECTION_INDEX_ITEM;
    public static int ITEM_INDEX_WITH_FULL_SPAN = 500;

    public static int VIEW_TYPE_NAV = 1;
    public static int VIEW_TYPE_ENTRY = 2;
    public static int VIEW_TYPE_CAT = 3;
    public static int VIEW_TYPE_FIXED_ITEM = 4;
    public static int VIEW_TYPE_ITEM = 5;
    public static int VIEW_TYPE_ITEM2 = 6;
    public static int VIEW_TYPE_ITEM_HALF_SIZE = 7; // 2 columns
    public static int VIEW_TYPE_FULL_SPAN = 8;


    public int[] VIEW_TYPES_OF_SECTION = {VIEW_TYPE_NAV, VIEW_TYPE_ENTRY, VIEW_TYPE_CAT, ITEM_COLUMNS == 1 ? VIEW_TYPE_ITEM : VIEW_TYPE_ITEM_HALF_SIZE, VIEW_TYPE_ITEM2};   // Navbar Product Entry Cat Product Brand
    public int VIEW_TYPE_WEBVIEW = 7;
    public int VIEW_TYPE_PADDINGBOTTOM = 99;
    // Navbar Product Entry Cat
    public int[] NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART = {1, 1, 1, 1, 1};
    // Product Brand
    public int[][] NUM_OF_ITEMS_IN_SECTION_FOR_PAGES = {
            {20, 2},
            {9, 0},
            {20, 0},
            {},
            {2, 0},
            {15, 0},
            {1, 0},
            {11, 0}};   // Navbar Product Entry Cat Product Brand
    public int PAGE_INDEX_OF_WEBVIEW = 16;

    public int[] STICKY_SECTIONS = {0, 2};

    public int[] ITEM_HEIGHT_IN_SECTION = {100, 90, 90, 90, 50, 160, 120};
    public int[] ITEM_HEIGHT_ITEM2 = {120};
    public int ITEM_HEIGHT_FULL_SPAN = 60;

    protected List<SectionData> mSections = new ArrayList<>();
    public PageFixedData mFixedData = new PageFixedData();
    public List<PageData> mPages = new ArrayList<>();    // PageData
    public int mBoundWidth = 0;
    public int mBoundHeight = 0;

    private int mPage;

    private Context mContext;

    public MainActivityDataSource(Context context, int boundWidth, int boundHeight) {
        mContext = context;

        mBoundWidth = boundWidth;
        mBoundHeight = boundHeight;

        long debugStartTime = 0;
        long debugEndTime = 0;
        long startTime=System.nanoTime();
        if (DEBUG) {
            debugStartTime = System.nanoTime();
        }

        initializeDataSource();

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "initializeDataSource takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
        }
    }

    public int getPage() {
        return mPage;
    }

    public void setPage(int page) {
        mPage = page;
    }

    public int getNumberOfPages() {
        return mPages.size();
    }

    public int getNumberOfFixedSections() { return mFixedData.getNumberOfSections(); }

    public int getNumberOfSections(int page) {
        return mPages.get(page).getNumberOfPageSections();
    }

    public int getNumberOfSections() {
        return mSections.size();
    }

    public int getItemCount() {
        return getTotalItemCount();
        // return mPages.get(mPage).getItemCount();
    }

    public int getTotalItemCount() {
        int numberOfItems = 0;
        if (null != mFixedData) {
            numberOfItems += mFixedData.getItemCount();
        }
        for (int page = 0; page < mPages.size(); ++page) {
            numberOfItems += mPages.get(page).getPageItemCount();
        }
        return numberOfItems;
    }

    public SectionPosition toSectionPosition(int position) {
        int section = 0;
        int item = position;
        for (; section < mSections.size(); ++section) {
            int itemCount = mSections.get(section).getItemCount();
            if (position >= itemCount) {
                position -= itemCount;
                continue;
            }
            break;
        }

        return new SectionPosition(section, position);
    }

    public int getMinPagablePosition() {

        int pos = 0;
        for (int idx = 0; idx < NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length; idx++) {
            pos += NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART[idx];
        }

        return pos;
    }

    public int getMinPagableSection() {
        return NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length;
    }

    /*
    public SectionData getSection(int sectionIndex) {
        return mPages[mPage].sections[sectionIndex];
    }
     */

    public void getItemSize(int section, int item, Size size) {
        SectionData sectionData = mSections.get(section);
        CellData cellData = sectionData.getItem(item);
        size.set(cellData.width, cellData.height);
    }

    public void setCellWidth(CellData cellData, int width) {
        if (ORIENTATION == FlexLayoutManager.VERTICAL) {
            cellData.width = width;
        } else {
            cellData.height = width;
        }
    }

    public void setCellHeight(CellData cellData, int height) {
        if (ORIENTATION == FlexLayoutManager.VERTICAL) {
            cellData.height = height;
        } else {
            cellData.width = height;
        }
    }

    public boolean isItemFullSpan(int section, int item) {
        SectionData sectionData = mSections.get(section);
        CellData cellData = sectionData.getItem(item);
        return cellData.fullSpan;
    }

    protected SectionData getSection(int section) {
        return section >= 0 && section < mSections.size() ? mSections.get(section) : null;
    }

    public int getInfoForItemsBatchly(int section, int itemStart, int itemCount, int[] data) {
        if (null == data) {
            return 0;
        }

        SectionData sectionData = getSection(section);
        int length = data.length;
        int itemCountInBuffer = (int)Math.floor(length / 3);
        if (itemCountInBuffer < 1) {
            return 0;
        }
        itemCount = Math.min(itemCount, itemCountInBuffer);
        itemCount = Math.min(itemCount, sectionData.getItemCount() - itemStart);

        int offset = 0;
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            CellData cellData = sectionData.getItem(itemStart + itemIndex);

            data[offset] = cellData.width;
            data[offset + 1] = cellData.height;
            data[offset + 2] = cellData.fullSpan ? 1 : 0;
            offset += 3;
        }

        return itemCount;
    }

    // mDataSource.getItemSize(section, item, size);

    public int getViewType(int position) {
        CellData cellData = getCellData(position);
        return cellData.viewType;
    }

    public static List<SectionPosition> getStickyItems() {
        List<SectionPosition> items = new ArrayList<>();

        items.add(new SectionPosition(SECTION_INDEX_NAVBAR, 0));
        items.add(new SectionPosition(SECTION_INDEX_CATBAR, 0));

        return items;
    }

    int getSectionCount() {
        return mSections.size();
    }

    boolean isSectionWaterfallMode(int section) {
        return mSections.get(section).isWaterfallMode();
    }

    CellData getCellData(int position) {
        int index = 0;
        for (; index < mSections.size(); index++) {
            int itemCount = mSections.get(index).getItemCount();

            if (position >= itemCount) {
                position -= itemCount;
                continue;
            }

            break;
        }

        return mSections.get(index).getItem(position);
    }

    public View createView(ViewGroup parent, int viewType) {
        View inflate = null;
        if (VIEW_TYPE_NAV == viewType) {
            if (USING_INFLATE) {
                inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_nav, parent, false);
            } else {
                inflate = MainActivityHelper.createTextView(parent.getContext());
            }
        } else if (VIEW_TYPE_CAT == viewType) {
            if (USING_INFLATE) {
                inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_cat, parent, false);
            } else {
                inflate = MainActivityHelper.createTextView(parent.getContext());
            }
        } else if (VIEW_TYPE_ENTRY == viewType) {
            if (USING_INFLATE) {
                inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_entry, parent, false);
            } else {
                inflate = MainActivityHelper.createTextView(parent.getContext());
            }
        } else if (VIEW_TYPE_ITEM == viewType || VIEW_TYPE_FIXED_ITEM == viewType) {
            if (USING_INFLATE) {
                inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_item, parent, false);
            } else {
                inflate = MainActivityHelper.createItemView(parent.getContext(), false);
            }
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)inflate.getLayoutParams();
            if (VIEW_TYPE_ITEM == viewType) {
                if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                    lp.height += (int) (Math.random() * 200);
                    ((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(false);
                }
            } else {
                if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                    ((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(true);
                }
            }

        } else if (VIEW_TYPE_ITEM_HALF_SIZE == viewType) {
            if (USING_INFLATE) {
                inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_item_v, parent, false);
            } else {
                inflate = MainActivityHelper.createItemView(parent.getContext(), true);
            }
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)inflate.getLayoutParams();
            if (VIEW_TYPE_ITEM == viewType) {
                if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                    lp.height += (int) (Math.random() * 200);
                    ((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(false);
                }
            } else {
                if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                    ((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(true);
                }
            }

        } else if (VIEW_TYPE_FULL_SPAN == viewType) {
            if (USING_INFLATE) {
                inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_full_span, parent, false);
            } else {
                inflate = MainActivityHelper.createTextView(parent.getContext());
            }
        } else if (VIEW_TYPE_WEBVIEW == viewType) {
            /*
            PageWebView webView = buildWebViewForPagination(parent, mPage);
            inflate = webView;
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)inflate.getLayoutParams();
            if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(false);
            }

             */
        } else if (VIEW_TYPE_PADDINGBOTTOM == viewType) {

            inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_padding, parent, false);
            /*
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)inflate.getLayoutParams();
            lp.height = mPaddingBottom;
            // inflate.setLayoutParams(lp);
            */
        } else {
            inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_item, parent, false);
        }

        return inflate;
    }

    public void bindCellView(int position, RecyclerView.ViewHolder holder) {
        CellData cellData = getCellData(position);

        if (holder.itemView instanceof TextView) {
            TextView textView = (TextView)holder.itemView;

            textView.setBackgroundColor(cellData.backgroundColor);
            // setViewBorderColor(holder.itemView, NavBarColor);

            textView.setText(cellData.text);
            textView.setTextColor(cellData.textColor);
        }
        else if (holder.itemView instanceof ItemView) {
            ItemView itemView = (ItemView)holder.itemView;

            itemView.updateData(cellData);
        } else if (holder.itemView instanceof PageWebView) {
            PageWebView webView = (PageWebView)holder.itemView;
            // webView.setId(R.id.web_view);
            // webView.setLayoutParams(buildLayoutParamsForRecyclerView(mRecyclerView));
            if (cellData.url != null && !(cellData.url.isEmpty())) {
                webView.loadUrl(cellData.imageUrl);
            } else {
                webView.loadUrl("about:blank");
            }
        } else {

            holder.itemView.setBackgroundColor(cellData.backgroundColor);

            // setViewBorderColor(holder.itemView, getItemForegroundColor(item + getCatIndex()));

            TextView textView = holder.itemView.findViewById(android.R.id.title);
            if (textView != null) {
                textView.setText(cellData.text);
                textView.setTextColor(cellData.textColor);
            }
            NetImageView imageView = holder.itemView.findViewById(R.id.netImageView);
            if (imageView != null) {
                imageView.setImageBitmap(null);
                if (cellData.imageUrl != null && !(cellData.imageUrl.isEmpty())) {
                    imageView.loadUrl(cellData.imageUrl);
                }
            }
        }
    }

    boolean isVertical() {
        return ORIENTATION == FlexLayoutManager.VERTICAL;
    }

    void initializeDataSource() {

        DataSourceColors dataSourceColors = new DataSourceColors();

        CellData cellData = null;
        int boundWidth = isVertical() ? mBoundWidth - (RECYCLERVIEW_PADDING.left + RECYCLERVIEW_PADDING.right) :
                mBoundHeight - (RECYCLERVIEW_PADDING.top + RECYCLERVIEW_PADDING.bottom);


        for (int idx = 0; idx < NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length; idx++) {
            int position = 0;
            if (mSections.size() > 0) {
                SectionData lastSectionData = mSections.get(mSections.size() - 1);
                position = lastSectionData.getPosition() + lastSectionData.getReservedItemCount();
            }

            SectionData sectionData = new SectionData(mSections.size(), NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART[idx], position);
            mSections.add(sectionData);
            mFixedData.addSection(sectionData);
        }

        for (int page = 0; page < NUM_OF_ITEMS_IN_SECTION_FOR_PAGES.length; page++) {
            PageData pageData = new PageData(mFixedData);
            mPages.add(pageData);

            int[] sections = NUM_OF_ITEMS_IN_SECTION_FOR_PAGES[page];
            for (int idx = 0; idx < sections.length; idx++) {
                int position = 0;
                if (mSections.size() > 0) {
                    SectionData lastSectionData = mSections.get(mSections.size() - 1);
                    position = lastSectionData.getPosition() + lastSectionData.getReservedItemCount();
                }

                SectionData sectionData = new SectionData(mSections.size(), sections[idx], position);
                mSections.add(sectionData);
                pageData.addSection(sectionData);
            }
        }

        // Navigation Bar
        SectionData section = mSections.get(SECTION_INDEX_NAVBAR);

        cellData = new CellData();
        cellData.viewType = VIEW_TYPE_NAV;
        cellData.backgroundColor = dataSourceColors.NavBarColor;
        cellData.text = "Navigation Bar pos=" + section.getPosition();
        setCellWidth(cellData, section.calcCellWidth(isVertical(), mBoundWidth));
        setCellHeight(cellData, toPixel(ITEM_HEIGHT_IN_SECTION[section.getSection()]));
        section.addCellData(cellData);

        int colorIdx = 0;
        for (int idx = 1; idx < section.getReservedItemCount(); idx++, colorIdx++) {
            cellData = new CellData();
            cellData.viewType = VIEW_TYPE_ENTRY;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(colorIdx);
            cellData.text = "Entry " + idx + " pos=" + (section.getPosition() + idx);
            setCellWidth(cellData, section.calcCellWidth(isVertical(), mBoundWidth));
            setCellHeight(cellData, toPixel(ITEM_HEIGHT_IN_SECTION[section.getSection()]));
            section.addCellData(cellData);
        }

        // Entry
        section = mSections.get(SECTION_INDEX_ENTRY);
        colorIdx = 0;
        for (int idx = 0; idx < section.getReservedItemCount(); idx++, colorIdx++) {
            cellData = new CellData();
            cellData.viewType = VIEW_TYPE_ENTRY;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(colorIdx);
            cellData.text = "Entry " + idx + " pos=" + (section.getPosition() + idx);
            setCellWidth(cellData, section.calcCellWidth(isVertical(), mBoundWidth));
            setCellHeight(cellData, toPixel(ITEM_HEIGHT_IN_SECTION[section.getSection()]));
            section.addCellData(cellData);
        }

        // Test 1
        colorIdx += 4;
        section = mSections.get(SECTION_INDEX_TEST1);
        for (int idx = 0; idx < section.getReservedItemCount(); idx++, colorIdx++) {
            cellData = new CellData();
            cellData.viewType = VIEW_TYPE_ENTRY;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(colorIdx);
            if (idx == 0) {
                cellData.text = section.toString() + "\r\nTest1 " + idx + " pos=" + (section.getPosition() + idx);
            } else {
                cellData.text = "Test1 " + idx + " pos=" + (section.getPosition() + idx);
            }
            setCellWidth(cellData, section.calcCellWidth(isVertical(), mBoundWidth));
            setCellHeight(cellData, toPixel(ITEM_HEIGHT_IN_SECTION[section.getSection()]));
            section.addCellData(cellData);
        }

        // Test 2
        section = mSections.get(SECTION_INDEX_TEST2);
        // section.insets = Insets.of(10, 10, 10, 10);
        section.setLineSpacing(20);
        section.setInteritemSpacing(20);
        colorIdx += 4;
        for (int idx = 0; idx < section.getReservedItemCount(); idx++, colorIdx++) {
            cellData = new CellData();
            cellData.viewType = VIEW_TYPE_ENTRY;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(colorIdx);
            if (idx == 0) {
                cellData.text = section.toString() + "\r\nTest2 " + idx + " pos=" + (section.getPosition() + idx);
            } else {
                cellData.text = "Test2 " + idx + " pos=" + (section.getPosition() + idx);
            }
            setCellWidth(cellData, section.calcCellWidth(isVertical(), mBoundWidth));
            setCellHeight(cellData, toPixel(ITEM_HEIGHT_IN_SECTION[section.getSection()]));
            section.addCellData(cellData);
        }

        // Catbar
        section = mSections.get(SECTION_INDEX_CATBAR);
        for (int idx = 0; idx < section.getReservedItemCount(); idx++) {
            cellData = new CellData();
            cellData.viewType = VIEW_TYPE_CAT;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(idx);
            cellData.text = "Category Bar pos=" + (section.getPosition() + idx);
            setCellWidth(cellData, section.calcCellWidth(isVertical(), mBoundWidth));
            setCellHeight(cellData, toPixel(ITEM_HEIGHT_IN_SECTION[section.getSection()]));
            section.addCellData(cellData);
        }

        int offsetBase = NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length;
        for (int page = 0; page < NUM_OF_ITEMS_IN_SECTION_FOR_PAGES.length; page++) {
            PageData pageData = mPages.get(page);

            if (pageData.getNumberOfPageSections() > 0) {

                section = pageData.getPageSection(0);

                section.setColumns(2);
                section.setWaterfallMode(true);

                int[] itemHeights1 = {80, 145, 135, 150, 160, 175, 165, 180};
                initializeItems(page, section, ITEM_TEXT_ITEM, dataSourceColors, itemHeights1);
            }

            if (pageData.getNumberOfPageSections() > 1) {
                section = pageData.getPageSection(1);
                section.setColumns(3);
                int[] itemHeights2 = ITEM_HEIGHT_ITEM2;
                initializeItems(page, section, ITEM_TEXT_ITEM2, dataSourceColors, itemHeights2);
            }

            pageData.calcItemCount();
        }

        SectionData prevSection = null;
        for (SectionData sectionData : mSections) {
            if (prevSection != null) {
                sectionData.setPosition(prevSection.getPosition() + prevSection.getItemCount());
            }
            prevSection = sectionData;
        }

        for (PageData pageData : mPages) {
            pageData.calcItemCount();
        }
    }

    void initializeItems(int page, SectionData section, String textFormat, DataSourceColors dsColors, int[] itemHeights) {

        int bgIndex = 4 * page;
        int imageColorIndex = 16 * page;

        Insets insets = section.getInsets();

        int boundWidth = isVertical() ? mBoundWidth - (RECYCLERVIEW_PADDING.left + RECYCLERVIEW_PADDING.right) :
                mBoundHeight - (RECYCLERVIEW_PADDING.top + RECYCLERVIEW_PADDING.bottom);


        for (int idx = 0; idx < section.getReservedItemCount(); idx++, imageColorIndex+=8, bgIndex++) {
            CellData cellData = new CellData();

            if (section.getSection() == SECTION_INDEX_WITH_FULL_SPAN && section.getColumns() > 1) {
                if (idx == ITEM_INDEX_WITH_FULL_SPAN + page * 2) {
                    cellData.fullSpan = true;
                }
            }

            if (cellData.fullSpan) {
                cellData.viewType = VIEW_TYPE_FULL_SPAN;
                cellData.backgroundColor = dsColors.itemColors[bgIndex % dsColors.itemColors.length];
                setCellWidth(cellData, section.calcFullSpanWidth(isVertical(), boundWidth));
                setCellHeight(cellData, toPixel(ITEM_HEIGHT_FULL_SPAN));
                cellData.text = String.format("FullSpan Page:%d, SI=%d-%d", page, section.getSection(), idx) + " pos=" + (section.getPosition() + idx);
            } else {
                cellData.viewType = (section.getColumns() == 1) ? VIEW_TYPE_ITEM : VIEW_TYPE_ITEM_HALF_SIZE;
                cellData.backgroundColor = dsColors.itemColors[bgIndex % dsColors.itemColors.length];
                setCellWidth(cellData, section.calcCellWidth(isVertical(), boundWidth));
                if (section.getColumns() == 1) {
                    setCellHeight(cellData, toPixel(itemHeights[(idx % itemHeights.length)]));
                } else {
                    setCellHeight(cellData, (int)Math.floor(cellData.width) + toPixel(itemHeights[(idx % itemHeights.length)]));
                }
                cellData.text = String.format(textFormat, page, section.getSection(), idx) + " pos=" + (section.getPosition() + idx);
            }

            cellData.imageUrl = "";

            cellData.imageBackgroundColor = dsColors.imageColors[(imageColorIndex % dsColors.imageColors.length)];

            section.addCellData(cellData);
        }
    }

    private int toPixel(int dip) {
        Resources r = mContext.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    public class DataSourceColors {
        public int NavBarColor = Color.rgb(255, 127, 80);    // coral
        public int CatColor = Color.rgb(244, 164, 96);       // sandybrown
        public int FooterColor = Color.rgb(211, 211, 211);   // lightgray
        public int LoadMoreColor = Color.rgb(0xFF, 0xDC, 0xB4);

        public int FullSpanColor = Color.YELLOW;

        public int[] itemColors = {0xFFB0E0E6, 0xFF87CEFA, 0xFF87CEEB, 0xFF00BFFF, 0xFF1E90FF, 0xFF6495ED, 0xFF4169E1, 0xFF0000FF, 0xFFB0E0E6, 0xFF87CEFA, 0xFF87CEEB, 0xFF00BFFF, 0xFF1E90FF, 0xFF6495ED, 0xFF4169E1, 0xFF0000FF};
        public int[] imageColors = {0xFF800000, 0xFF8B0000, 0xFFA52A2A, 0xFFB22222, 0xFFDC143C, 0xFFFF0000, 0xFFFF6347, 0xFFFF7F50, 0xFFCD5C5C, 0xFFF08080, 0xFFE9967A, 0xFFFA8072, 0xFFFFA07A, 0xFFFF4500, 0xFFFF8C00, 0xFFFFA500, 0xFFFFD700, 0xFFB8860B, 0xFFDAA520, 0xFFEEE8AA, 0xFFBDB76B, 0xFFF0E68C, 0xFF808000, 0xFFFFFF00, 0xFF9ACD32, 0xFF556B2F, 0xFF6B8E23, 0xFF7CFC00, 0xFF7FFF00, 0xFFADFF2F, 0xFF006400, 0xFF008000, 0xFF228B22, 0xFF00FF00, 0xFF32CD32, 0xFF90EE90, 0xFF98FB98, 0xFF8FBC8F, 0xFF00FA9A, 0xFF00FF7F, 0xFF2E8B57, 0xFF66CDAA, 0xFF3CB371, 0xFF20B2AA, 0xFF2F4F4F, 0xFF008080, 0xFF008B8B, 0xFF00FFFF, 0xFF00FFFF, 0xFFE0FFFF, 0xFF00CED1, 0xFF40E0D0, 0xFF48D1CC, 0xFFAFEEEE, 0xFF7FFFD4, 0xFFB0E0E6, 0xFF5F9EA0, 0xFF4682B4, 0xFF6495ED, 0xFF00BFFF, 0xFF1E90FF, 0xFFADD8E6, 0xFF87CEEB, 0xFF87CEFA, 0xFF191970, 0xFF000080, 0xFF00008B, 0xFF0000CD, 0xFF0000FF, 0xFF4169E1, 0xFF8A2BE2, 0xFF4B0082, 0xFF483D8B, 0xFF6A5ACD, 0xFF7B68EE, 0xFF9370DB, 0xFF8B008B, 0xFF9400D3, 0xFF9932CC, 0xFFBA55D3, 0xFF800080, 0xFFD8BFD8, 0xFFDDA0DD, 0xFFEE82EE, 0xFFFF00FF, 0xFFDA70D6, 0xFFC71585, 0xFFDB7093, 0xFFFF1493, 0xFFFF69B4, 0xFFFFB6C1, 0xFFFFC0CB, 0xFFFAEBD7, 0xFFF5F5DC, 0xFFFFE4C4, 0xFFFFEBCD, 0xFFF5DEB3, 0xFFFFF8DC, 0xFFFFFACD, 0xFFFAFAD2, 0xFFFFFFE0, 0xFF8B4513, 0xFFA0522D, 0xFFD2691E, 0xFFCD853F, 0xFFF4A460, 0xFFDEB887, 0xFFD2B48C, 0xFFBC8F8F, 0xFFFFE4B5, 0xFFFFDEAD, 0xFFFFDAB9, 0xFFFFE4E1, 0xFFFFF0F5, 0xFFFAF0E6, 0xFFFDF5E6, 0xFFFFEFD5, 0xFFFFF5EE, 0xFFF5FFFA, 0xFF708090, 0xFF778899, 0xFFB0C4DE, 0xFFE6E6FA, 0xFFFFFAF0, 0xFFF0F8FF, 0xFFF8F8FF, 0xFFF0FFF0, 0xFFFFFFF0, 0xFFF0FFFF, 0xFFFFFAFA, 0xFF000000, 0xFF696969, 0xFF808080, 0xFFA9A9A9, 0xFFC0C0C0, 0xFFD3D3D3, 0xFFDCDCDC, 0xFFF5F5F5, 0xFFFFFFFF};

        int[] entryColors = {0xFF7CFC00, 0xFF32CD32, 0xFF006400, 0xFF9ACD32, 0xFF00FA9A, 0xFF98FB98, 0xFF808000, 0xFF6B8E23};

        public int getEntryBackgroundColor(int index) {
            return entryColors[index % entryColors.length];
        }
    }

}
