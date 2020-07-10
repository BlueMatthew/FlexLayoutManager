package org.wakin.flexlayout;

import java.io.IOException;
import android.content.Context;
import android.content.res.Resources;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.wakin.flexlayout.LayoutManager.FlexLayoutManager;
import org.wakin.flexlayout.cells.ItemView;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class MainActivityHelper {

    private static String TAG = "MainActivityHelper";
    private static boolean DEBUG = false;

    public static byte[] getResource(int id, Context context) throws IOException {
        Resources resources = context.getResources();
        InputStream is = resources.openRawResource(id);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        byte[] readBuffer = new byte[4 * 1024];

        try {
            int read;
            do {
                read = is.read(readBuffer, 0, readBuffer.length);
                if(read == -1) {
                    break;
                }
                bout.write(readBuffer, 0, read);
            } while(true);

            return bout.toByteArray();
        } finally {
            is.close();
        }
    }


    // reads a string resource
    public static String getStringResource(int id, Charset encoding, Context context) throws IOException {
        return new String(MainActivityHelper.getResource(id, context), encoding);
    }

    // reads an UTF-8 string resource
    public static String getStringResource(int id, Context context) throws IOException {
        return new String(MainActivityHelper.getResource(id, context), Charset.forName("UTF-8"));
    }


    static public int findFirstVisibleItemPosition(RecyclerView recyclerView) {
        int position = RecyclerView.NO_POSITION;

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            // position = ((StaggeredGridLayoutManager)layoutManager).findFirstVisibleItemPosition();
        } else if (layoutManager instanceof LinearLayoutManager) {
            position = ((LinearLayoutManager)layoutManager).findFirstVisibleItemPosition();
        }

        return position;
    }

    static public int findFirstVisibleItemOffset(RecyclerView recyclerView) {
        int position = findFirstVisibleItemPosition(recyclerView);

        int offset = 0;
        if (RecyclerView.NO_POSITION != position) {

            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
            if (null != viewHolder && null != viewHolder.itemView) {
                // if (recyclerView.getLayoutManager().get())
                // Should check direction
                offset = (int)viewHolder.itemView.getY();
            }
        }

        return offset;
    }

    static public int findLastVisibleItemPosition(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            // return ((StaggeredGridLayoutManager)layoutManager).findLastVisibleItemPosition();
            return -1;
        } else if (layoutManager instanceof LinearLayoutManager) {
            //
            int lastVisibleItemPosition = RecyclerView.NO_POSITION;
            int viewCount = layoutManager.getChildCount();
            for (int idx = 0; idx < viewCount; idx++) {
                View view = layoutManager.getChildAt(idx);
                int position = recyclerView.getChildAdapterPosition(view);
                if (position > lastVisibleItemPosition) {
                    lastVisibleItemPosition = position;
                }
            }

            return lastVisibleItemPosition;
            // return ((LinearLayoutManager)layoutManager).findLastVisibleItemPosition();
        }

        return RecyclerView.NO_POSITION;
    }

    static public void scrollToPositionWithOffset(RecyclerView recyclerView, int position, int offset) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager)layoutManager).scrollToPositionWithOffset(position, offset);
        } else if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager)layoutManager).scrollToPositionWithOffset(position, offset);
        } else {
            layoutManager.scrollToPosition(position);
        }
    }

    static public TextView createTextView(Context context) {
        TextView view = new TextView(context);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(20, 0, 0, 0);
        view.setId(R.id.title);
        view.setTextAppearance(context, android.R.style.TextAppearance_Medium);

        return view;
    }

    static public View createItemView(Context context, boolean vertical) {

        long debugStartTime = 0;
        long debugEndTime = 0;
        long startTime=System.nanoTime();
        if (DEBUG) {
            debugStartTime = System.nanoTime();
        }

        ItemView itemView = new ItemView(context);
        itemView.setOrientation(vertical ? ItemView.VERTICAL : ItemView.HORIZONTAL);

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "createItemView takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
        }

        return itemView;
    }

    static public View createItemViewByConstraintLayout(Context context, boolean vertical) {

        long debugStartTime = 0;
        long debugEndTime = 0;
        long startTime=System.nanoTime();
        if (DEBUG) {
            debugStartTime = System.nanoTime();
        }

        ConstraintLayout cl = new ConstraintLayout(context);

        NetImageView netImageView = new NetImageView(context);
        netImageView.setId(R.id.netImageView);

        cl.addView(netImageView);

        TextView textView = createTextView(context);
        cl.addView(textView);

        ConstraintSet set = new ConstraintSet();

        if (vertical) {
            set.setDimensionRatio(R.id.image, "1:1");

            set.connect(R.id.image, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, dpToPx(context, 8));
            set.connect(R.id.image, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, dpToPx(context, -8));
            set.connect(R.id.image, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(context, 8));

            set.connect(R.id.title, ConstraintSet.TOP, R.id.image, ConstraintSet.BOTTOM, dpToPx(context, 8));
            set.connect(R.id.title, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, dpToPx(context, 8));
            set.connect(R.id.title, ConstraintSet.RIGHT, R.id.image, ConstraintSet.RIGHT, 0);
            set.connect(R.id.title, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(context, -8));
        } else {
            set.setDimensionRatio(R.id.image, "1:1");

            set.connect(R.id.image, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, dpToPx(context, 8));
            set.connect(R.id.image, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(context, 8));
            set.connect(R.id.image, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(context, -8));

            set.connect(R.id.title, ConstraintSet.LEFT, R.id.image, ConstraintSet.RIGHT, dpToPx(context, 8));
            set.connect(R.id.title, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(context, 8));
            set.connect(R.id.title, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, dpToPx(context, -8));
            set.connect(R.id.title, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(context, -8));
        }
        set.applyTo(cl);

        if (DEBUG) {
            debugEndTime = System.nanoTime();

            Log.d("PERF", "createItemView takes: " + (debugEndTime - debugStartTime) / 1000000 + "ms");
        }

        return cl;
    }

    /*
    // reads a string resource
    public String getStringResource(int id, Charset encoding) throws IOException {
        return new String(MainActivityHelper.getResource(id, this), encoding);
    }

    // reads an UTF-8 string resource
    public String getStringResource(int id) throws IOException {
        return new String(MainActivityHelper.getResource(id, this), Charset.forName("UTF-8"));
    }

     */



    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    static public RecyclerView.LayoutManager createLayoutManagerForPagination(RecyclerView recyclerView) {
        RecyclerView.LayoutManager newLayoutManager = null;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager)layoutManager;
            newLayoutManager = new StaggeredGridLayoutManager(staggeredGridLayoutManager.getSpanCount(), staggeredGridLayoutManager.getOrientation()) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
                @Override
                public boolean canScrollHorizontally() {
                    return false;
                }
            };
        } else if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager)layoutManager;
            linearLayoutManager.setInitialPrefetchItemCount(0);
            newLayoutManager = new LinearLayoutManager(recyclerView.getContext(), linearLayoutManager.getOrientation(), linearLayoutManager.getReverseLayout()) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
                @Override
                public boolean canScrollHorizontally() {
                    return false;
                }
            };
            ((LinearLayoutManager) newLayoutManager).setRecycleChildrenOnDetach(true);
        } else if (layoutManager instanceof FlexLayoutManager) {
            FlexLayoutManager flexLayoutManager = (FlexLayoutManager)layoutManager;
            // linearLayoutManager.setInitialPrefetchItemCount(0);
            newLayoutManager = new FlexLayoutManager(recyclerView.getContext(), flexLayoutManager.getOrientation(), flexLayoutManager.getReverseLayout()) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
                @Override
                public boolean canScrollHorizontally() {
                    return false;
                }
            };
            // ((FlexLayoutManager) newLayoutManager).setRecycleChildrenOnDetach(true);
        }

        return newLayoutManager;
    }

    public static void setViewBorderColor(View view, int color) {
        GradientDrawable border = new GradientDrawable();
        border.setColor(color);
        int red = 255 - Color.red(color);
        int green = 255 - Color.green(color);
        int blue = 255 - Color.blue(color);
        border.setStroke(8, Color.rgb(red, green, blue));
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(border);
        } else {
            view.setBackground(border);
        }
    }
}
