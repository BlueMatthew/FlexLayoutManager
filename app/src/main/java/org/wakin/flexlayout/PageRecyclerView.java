package org.wakin.flexlayout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

public class PageRecyclerView extends RecyclerView implements PaginationView {

    private boolean mUpdateEnabled;
    private Bitmap mBitmap;
    private Paint mPaint = null;

    public PageRecyclerView(Context context) {
        super(context);
        mUpdateEnabled = true;
    }

    public PageRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mUpdateEnabled = true;
    }

    public PageRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mUpdateEnabled = true;
    }

    public void onDraw (Canvas c) {
        if (!mUpdateEnabled && null != mBitmap) {
            Log.i("ITH", String.format("Draw Buffer Bitmap: %d, %d", mBitmap.getWidth(), mBitmap.getHeight()));

            if (null == mPaint) {
                mPaint = new Paint();
                mPaint.setStrokeWidth(3);
                mPaint.setTextSize(60);
                mPaint.setColor(Color.RED);
                mPaint.setTextAlign(Paint.Align.LEFT);
            }

            c.drawBitmap(mBitmap, new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight()), new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight()), mPaint);

            // c.drawText("View Screenshot onDraw", 394, 100, mPaint);
            return;
        }
        super.onDraw(c);
    }

    public void enableUpdate(boolean enabled) {
        mUpdateEnabled = enabled;
        mBitmap = enabled ? null : getCacheBitmapFromView(this);
    }


    private Bitmap getCacheBitmapFromView(View view) {
        // 开启view的bitmap cache
        view.setDrawingCacheEnabled(true);
        // 获取view的bitmap cache
        Bitmap drawingCache = view.getDrawingCache(); // getDrawingCache()
        // val bitmap: Bitmap?
        Bitmap bitmap = null;
        if (drawingCache != null) {
            //创建一个DrawingCache的拷贝，因为DrawingCache得到的位图在禁用后会被回收
            bitmap = Bitmap.createBitmap(drawingCache);
            // 将cache关闭 节省内存
            view.setDrawingCacheEnabled(false);
            view.destroyDrawingCache();
        } else {
            bitmap = null;
        }
        return bitmap;
    }


    @Override
    public void offsetView(float offsetX, float offsetY) {
        if (getTranslationX() != offsetX) setTranslationX(offsetX);
        if (getTranslationY() != offsetY) setTranslationY(offsetY);
    }

    @Override
    public void swap(RecyclerView recyclerView, int minPagbleItemPosition, boolean inSticky) {
        swapAdapter(null, true);
    }

    @Override
    public void releaseReusableViews() {
        // enableUpdate(true);
        // swapAdapter(null, true);
    }

    @Override
    public void removeFromParentViewGroup() {
        ViewGroup viewGroup = (ViewGroup) getParent();
        viewGroup.removeView(this);
    }

    @Override
    public int getFirstVisibleItemPosition() {
        return MainActivityHelper.findFirstVisibleItemPosition(this);
    }

    @Override
    public int getFirstVisibleItemOffset() {
        RecyclerView.ViewHolder pageViewHolder = findViewHolderForAdapterPosition(getFirstVisibleItemPosition());
        return pageViewHolder.itemView.getTop();
    }

}
