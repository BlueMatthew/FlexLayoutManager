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

import org.wakin.flexlayout.LayoutManager.Insets;
import org.wakin.flexlayout.LayoutManager.SectionPosition;
import org.wakin.flexlayout.LayoutManager.Size;
import org.wakin.flexlayout.cells.ItemView;
import org.wakin.flexlayout.models.CellData;

import java.util.ArrayList;
import java.util.List;

public class MainActivityDataSource {

    private static boolean DEBUG = true;
    private static boolean USING_INFLATE = false; // For perf comparison

    public int ITEM_COLUMNS = 2;
    public boolean ITEM_LAYOUT_MODE_WATERFALL = true;

    public static Insets RECYCLERVIEW_PADDING = Insets.of(20, 20, 20, 20);


    public String ITEM_TEXT_ITEM =               "Page:%d Item:%d";
    public String ITEM_TEXT_ITEM2 =              "Page:%d Item2:%d";
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
            {800, 10},
            {90, 0},
            {20, 0},
            {120, 0},
            {2, 0},
            {150, 0},
            {1, 0},
            {110, 0}};   // Navbar Product Entry Cat Product Brand
    public int PAGE_INDEX_OF_WEBVIEW = 6;

    public int[] STICKY_SECTIONS = {0, 2};

    public int[] ITEM_HEIGHT_IN_SECTION = {120, 90, 90, 90, 50, 160, 120};
    public int[] ITEM_HEIGHT_ITEM2 = {120};
    public int ITEM_HEIGHT_FULL_SPAN = 60;

    public PageData[] mPages;    // PageData
    public int mBoundWidth = 0;
    public int mBoundHeight = 0;


    private int mPage;
    // private String[] mUrls;
    // private List< List<String> > mPageUrls;

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

    public int getPageSize() {
        return mPages.length;
    }

    public int getItemCount() {
        return mPages[mPage].itemCount;
    }

    public SectionPosition toSectionPosition(int position) {
        PageData pageData = mPages[mPage];
        int section = 0;
        int item = position;
        for (; section < pageData.sections.length; section++) {
            int itemCount = pageData.sections[section].itemCount;

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

    public SectionData getSection(int sectionIndex) {
        return mPages[mPage].sections[sectionIndex];
    }

    public void getItemSize(int section, int item, Size size) {

        SectionData sectionData = mPages[mPage].sections[section];

        CellData cellData = sectionData.items.get(item);

        size.set(cellData.width, cellData.height);
    }

    public boolean isItemFullSpan(int section, int item) {
        SectionData sectionData = mPages[mPage].sections[section];

        CellData cellData = sectionData.items.get(item);

        return cellData.fullSpan;
    }

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
        return mPages[mPage].sections.length;
    }

    boolean isSectionWaterfallMode(int section) {
        return mPages[mPage].sections[section].waterfallMode;
    }

    CellData getCellData(int position) {
        PageData pageData = mPages[mPage];

        CellData cellData = null;

        int index = 0;
        for (; index < pageData.sections.length; index++) {
            int itemCount = pageData.sections[index].itemCount;

            if (position >= itemCount) {
                position -= itemCount;
                continue;
            }

            break;
        }

        return pageData.sections[index].items.get(position);
    }

    public FlexRecyclerView.SectionViewHolder createViewHolder(ViewGroup parent, int viewType) {
        View inflate = null;
        FlexRecyclerView.SectionViewHolder viewHolder = null;
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

                viewHolder = new FlexRecyclerView.SectionViewHolder(inflate);
                // viewHolder.

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

                viewHolder = new FlexRecyclerView.SectionViewHolder(inflate);
                // viewHolder.

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


        return viewHolder == null ? new FlexRecyclerView.SectionViewHolder(inflate) : viewHolder;


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

        cellData.displayed = true;
    }

    void initializeDataSource() {

        DataSourceColors dataSourceColors = new DataSourceColors();

        CellData cellData = null;
        mPages = new PageData[NUM_OF_ITEMS_IN_SECTION_FOR_PAGES.length];

        for (int page = 0; page < NUM_OF_ITEMS_IN_SECTION_FOR_PAGES.length; page++) {

            SectionData[] sections = new SectionData[NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length + NUM_OF_ITEMS_IN_SECTION_FOR_PAGES[page].length];
            for (int idx = 0; idx < sections.length; idx++) {
                if (page == 0 || (idx >= NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length)) {
                    sections[idx] = new SectionData();
                    sections[idx].section = idx;
                } else {
                    sections[idx] = mPages[0].sections[idx];
                }
            }

            int offset = 0;
            if (page == 0) {
                for (int idx = 0; idx < NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length; idx++, offset++) {
                    sections[offset].itemCount = NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART[idx];
                    sections[offset].items = new ArrayList<>(sections[offset].itemCount);

                    if (offset > 0) {
                        sections[offset].position = sections[offset - 1].position + sections[offset - 1].itemCount;
                    }
                }
            } else {
                offset = NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length;
            }

            for (int idx = 0; idx < NUM_OF_ITEMS_IN_SECTION_FOR_PAGES[page].length; idx++, offset++) {
                sections[offset].itemCount = NUM_OF_ITEMS_IN_SECTION_FOR_PAGES[page][idx];
                sections[offset].items = new ArrayList<>(sections[offset].itemCount);

                if (offset > 0) {
                    sections[offset].position = sections[offset - 1].position + sections[offset - 1].itemCount;
                }
            }

            PageData pageData = new PageData();
            pageData.page = page;
            pageData.sections = sections;

            mPages[page] = pageData;
        }

        // Navigation Bar
        SectionData section = mPages[0].sections[SECTION_INDEX_NAVBAR];
        cellData = section.addCellData();
        cellData.viewType = VIEW_TYPE_NAV;
        cellData.backgroundColor = dataSourceColors.NavBarColor;
        cellData.text = "Navigation Bar pos=" + section.position;
        cellData.width = section.calcCellWidth();
        cellData.height = toPixel(ITEM_HEIGHT_IN_SECTION[section.section]);


        // Entry
        section = mPages[0].sections[SECTION_INDEX_ENTRY];
        int colorIdx = 0;
        for (int idx = 0; idx < section.itemCount; idx++, colorIdx++) {
            cellData = section.addCellData();
            cellData.viewType = VIEW_TYPE_ENTRY;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(colorIdx);
            cellData.text = "Entry " + idx + " pos=" + (section.position + idx);
            cellData.width = section.calcCellWidth();
            cellData.height = toPixel(ITEM_HEIGHT_IN_SECTION[section.section]);
        }

        // Test 1
        colorIdx += 4;
        section = mPages[0].sections[SECTION_INDEX_TEST1];
        for (int idx = 0; idx < section.itemCount; idx++, colorIdx++) {
            cellData = section.addCellData();
            cellData.viewType = VIEW_TYPE_ENTRY;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(colorIdx);
            if (idx == 0) {
                cellData.text = section.toString() + "\r\nTest1 " + idx + " pos=" + (section.position + idx);
            } else {
                cellData.text = "Test1 " + idx + " pos=" + (section.position + idx);
            }
            cellData.width = section.calcCellWidth();
            cellData.height = toPixel(ITEM_HEIGHT_IN_SECTION[section.section]);
        }

        // Test 2
        section = mPages[0].sections[SECTION_INDEX_TEST2];
        section.insets = Insets.of(10, 10, 10, 10);
        section.lineSpacing = 20;
        section.interitemSpacing = 20;
        colorIdx += 4;
        for (int idx = 0; idx < section.itemCount; idx++, colorIdx++) {
            cellData = section.addCellData();
            cellData.viewType = VIEW_TYPE_ENTRY;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(colorIdx);
            if (idx == 0) {
                cellData.text = section.toString() + "\r\nTest2 " + idx + " pos=" + (section.position + idx);
            } else {
                cellData.text = "Test2 " + idx + " pos=" + (section.position + idx);
            }
            cellData.width = section.calcCellWidth();
            cellData.height = toPixel(ITEM_HEIGHT_IN_SECTION[section.section]);
        }

        // Catbar
        section = mPages[0].sections[SECTION_INDEX_CATBAR];
        for (int idx = 0; idx < section.itemCount; idx++) {
            cellData = section.addCellData();
            cellData.viewType = VIEW_TYPE_CAT;
            cellData.backgroundColor = dataSourceColors.getEntryBackgroundColor(idx);
            cellData.text = "Category Bar pos=" + (section.position + idx);
            cellData.width = section.calcCellWidth();
            cellData.height = toPixel(ITEM_HEIGHT_IN_SECTION[section.section]);
        }

        int offsetBase = NUM_OF_ITEMS_IN_SECTION_FOR_FIXED_PART.length;
        for (int page = 0; page < NUM_OF_ITEMS_IN_SECTION_FOR_PAGES.length; page++) {
            PageData pageData = mPages[page];

            section = pageData.sections[offsetBase];

            section.columns = 2;
            section.waterfallMode = true;

            int[] itemHeights1 = {80, 145, 135, 150, 160, 175, 165, 180};
            initializeItems(page, section, ITEM_TEXT_ITEM, dataSourceColors, itemHeights1);

            section = pageData.sections[offsetBase + 1];
            section.columns = 3;
            int[] itemHeights2 = ITEM_HEIGHT_ITEM2;
            initializeItems(page, section, ITEM_TEXT_ITEM2, dataSourceColors, itemHeights2);

            pageData.calcItemCount();
        }
    }

    void initializeItems(int page, SectionData section, String textFormat, DataSourceColors dsColors, int[] itemHeights) {

        int bgIndex = 4 * page;
        int imageColorIndex = 16 * page;

        for (int idx = 0; idx < section.itemCount; idx++, imageColorIndex+=8, bgIndex++) {
            CellData cellData = new CellData();

            if (section.section == SECTION_INDEX_WITH_FULL_SPAN && section.columns > 1) {
                if (idx == ITEM_INDEX_WITH_FULL_SPAN + page * 2) {
                    cellData.fullSpan = true;
                }
            }

            if (cellData.fullSpan) {
                cellData.viewType = VIEW_TYPE_FULL_SPAN;
                cellData.backgroundColor = dsColors.itemColors[bgIndex % dsColors.itemColors.length];
                cellData.width = section.calcFullSpanWidth();
                cellData.height = toPixel(ITEM_HEIGHT_FULL_SPAN);
                cellData.text = String.format("FullSpan Item: %d, item=%d", page, idx) + " pos=" + (section.position + idx);
            } else {
                cellData.viewType = (section.columns == 1) ? VIEW_TYPE_ITEM : VIEW_TYPE_ITEM_HALF_SIZE;
                cellData.backgroundColor = dsColors.itemColors[bgIndex % dsColors.itemColors.length];
                cellData.width = section.calcCellWidth();
                if (section.columns == 1) {
                    cellData.height = toPixel(itemHeights[(idx % itemHeights.length)]);
                } else {
                    cellData.height = (int)Math.floor(cellData.width) + toPixel(itemHeights[(idx % itemHeights.length)]);
                }
                cellData.text = String.format(textFormat, page, idx) + " pos=" + (section.position + idx);
            }

            cellData.imageUrl = "";

            cellData.imageBackgroundColor = dsColors.imageColors[(imageColorIndex % dsColors.imageColors.length)];

            section.items.add(cellData);
        }
    }

    void initializeUrls() {
        // mUrls = getUrls(mContext);
        /*
        mPageUrls = new ArrayList<>(mAdapter.getPageSize());
        int numberOfCat = mUrls.length / mAdapter.getPageSize();
        int idx = 0;
        for (int catIdx = 0; catIdx < mAdapter.getPageSize(); catIdx++) {
            List<String> urls = new ArrayList<>(numberOfCat + 1);
            for (; idx < mUrls.length && urls.size() <= numberOfCat; idx++) {
                urls.add(mUrls[idx]);
            }
            mPageUrls.add(urls);
        }

         */
    }

    /*
    protected String buildProductUrl(int item, int page) {
        List<String> urls = mPageUrls.get(page);
        return urls.get(item % urls.size());
    }

    public static String[] getUrls(Context context) {

        String urls = "";
        try {
            urls = MainActivityHelper.getStringResource(R.raw.urls, context);
        } catch (IOException e) {

        }

        return urls.split("\\r?\\n");
    }

    public List<List<String>> getUrls(int count) {

        String[] urls = getUrls(mContext);
        List< List<String> > urlsList = new ArrayList<List<String>>(count);

        int numberOfCat = urls.length / count;
        int idx = 0;
        for (int index = 0; index < count; index++)
        {
            List<String> urlList = new ArrayList<>(numberOfCat);

            for (; idx < urls.length; idx++) {
                urlList.add(urls[idx]);
            }
            urlsList.add(urlList);
        }

        return urlsList;
    }
 */
    private int toPixel(int dip) {
        Resources r = mContext.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    public class SectionData {

        public int section;
        public int position;

        public boolean waterfallMode = false;

        public int itemCount = 0;
        public int columns = 1;
        public List<CellData> items;

        Insets insets = Insets.NONE;

        int lineSpacing = 0;
        int interitemSpacing = 0;

        CellData addCellData() {
            CellData cellData = new CellData();
            items.add(cellData);

            return cellData;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("");
            if (!insets.equals(Insets.NONE)) {
                sb.append("Insets:" + insets.toString() + "\r\n");
            }
            if (lineSpacing != 0) {
                sb.append("lineSpacing:" + lineSpacing + "\r\n");
            }
            if (lineSpacing != 0) {
                sb.append("interitemSpacing:" + interitemSpacing + "\r\n");
            }

            return sb.toString();
        }

        public int calcFullSpanWidth() {
            return (mBoundWidth - insets.left - insets.right);
        }

        public int calcCellWidth() {

            if (columns < 1) {
                columns = 1;
            }
            int width = (columns == 1) ? (mBoundWidth - insets.left - insets.right) : ((mBoundWidth - insets.left - insets.right - (columns - 1) * lineSpacing) / columns);

            return width;
        }

        public int calcCellWidth(int column) {

            if (columns < 1) {
                columns = 1;
            }
            int width = (columns == 1) ? (mBoundWidth - insets.left - insets.right) : ((mBoundWidth - insets.left - insets.right - (columns - 1) * lineSpacing) / columns);

            return width;
        }

    }


    public static class PageData {

        public int page;
        public int itemCount = 0;
        public SectionData[] sections;

        public void calcItemCount() {
            itemCount = 0;
            for (int idx = 0; idx < sections.length; idx++) {
                itemCount += sections[idx].itemCount;
            }
        }
        // public List<CellData> items1;
        // public List<CellData> items2;
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
