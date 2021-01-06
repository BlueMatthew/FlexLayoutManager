package org.wakin.flexlayout.app.cells;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wakin.flexlayout.app.NetImageView;
import org.wakin.flexlayout.R;
import org.wakin.flexlayout.app.models.CellData;

public class ItemView extends ViewGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    NetImageView mImageView;
    TextView mTextView;
    TextView mLoadingView;
    int mOrientation;
    int mUpdateId = 0;


    public ItemView(Context context) {
        super(context);

        initChildViews();
    }

    public ItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initChildViews();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        requestLayout();
    }

    void setImage(String url) {
        mImageView.loadUrl(url);
    }

    void setText(String text) {
        mTextView.setText(text);
        requestLayout();
    }

    public void updateData(CellData cellData) {

        mUpdateId++;

        Log.i("Flex", "Start loading cell: " + cellData.text);

        setBackgroundColor(cellData.backgroundColor);

       // setViewBorderColor(holder.itemView, getItemForegroundColor(item + getCatIndex()));

        mTextView.setText(cellData.text);
        mTextView.setTextColor(cellData.textColor);

        mImageView.setImageBitmap(null);
        if (cellData.imageUrl != null && !(cellData.imageUrl.isEmpty())) {
            mLoadingView.setText("");
            if (cellData.imageBackgroundColor != 0) {
                mImageView.setBackgroundColor(cellData.imageBackgroundColor);
            }
            mImageView.loadUrl(cellData.imageUrl);
        } else {
            if (cellData.imageBackgroundColor != 0) {
                if (!cellData.isLoaded()) {
                    // first display
                    cellData.startLoading();
                    setImageViewColorDelayed(cellData.imageBackgroundColor, cellData.backgroundColor, cellData.getRemainingLoadingTime());
                } else {
                    mImageView.setBackgroundColor(cellData.imageBackgroundColor);
                    mLoadingView.setText("I'm an image");
                    mLoadingView.setTextColor(reverseColor(cellData.imageBackgroundColor));
                }
            }
        }
    }

    private void setImageViewColorDelayed(final int imageColor, final int backgroundColor, final long delayedTime) {
        final int updateId = mUpdateId;

        setViewBorder(mImageView, imageColor);
        mLoadingView.setText("Loading...");
        mLoadingView.setTextColor(reverseColor(backgroundColor));

        // int delayedTime = 3000 + (int)(Math.random() * 1500);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (updateId == mUpdateId) {
                    mImageView.setBackgroundColor(imageColor);
                    mLoadingView.setText("I'm an image");
                    mLoadingView.setTextColor(reverseColor(imageColor));
                }
            }
        }, delayedTime);
    }

    int reverseColor(int color) {
        int red = 255 - Color.red(color);
        int green = 255 - Color.green(color);
        int blue = 255 - Color.blue(color);
        return Color.rgb(red, green, blue);
    }

    private void setViewBorder(View view, int color) {
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        border.setStroke(16, color);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(border);
        } else {
            view.setBackground(border);
        }
    }

    private void initChildViews() {

        setPadding(20, 20, 20, 20);

        mImageView = new NetImageView(getContext());
        mImageView.setId(R.id.netImageView);

        addView(mImageView);

        mLoadingView = new TextView(getContext());
        // mLoadingView.setBackgroundColor(Color.TRANSPARENT);
        mLoadingView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        mLoadingView.setTextAppearance(getContext(), android.R.style.TextAppearance_Small);
        addView(mLoadingView);

        mTextView = new TextView(getContext());
        // mTextView.setTextDirection(TEXT_DIRECTION_LTR);
        mTextView.setGravity(Gravity.CENTER_VERTICAL);
        mTextView.setPadding(20, 0, 0, 0);
        mTextView.setId(R.id.title);
        mTextView.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);

        addView(mTextView);
    }

    /**
     * Any layout manager that doesn't scroll will want this.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    /**
     * Ask all children to measure themselves and compute the measurement of this
     * layout based on the children.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mOrientation == VERTICAL) {
            int width = widthSize - getPaddingLeft() - getPaddingRight();
            int height = width;
            mImageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

            mLoadingView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

            height = heightSize - getPaddingTop() - getPaddingTop() - height - getPaddingBottom();
            mTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

        } else {
            int height = heightSize - getPaddingTop() - getPaddingBottom();
            int width = height;
            mImageView.measure(MeasureSpec.makeMeasureSpec(width, widthMode), MeasureSpec.makeMeasureSpec(height, heightMode));

            mLoadingView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

            width = widthSize - getPaddingLeft() - getPaddingLeft() - width - getPaddingRight();
            mTextView.measure(MeasureSpec.makeMeasureSpec(width, widthMode), MeasureSpec.makeMeasureSpec(height, heightMode));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        if (mOrientation == VERTICAL) {
            int childLeft = getPaddingLeft();
            int childTop = getPaddingTop();
            int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
            int height = width;

            mImageView.layout(childLeft, childTop, childLeft + width, childTop + height);

            mLoadingView.layout(childLeft, childTop, childLeft + width, childTop + height);

            childTop += height + getPaddingTop();
            height = getMeasuredHeight() - childTop - getPaddingBottom();

            mTextView.layout(childLeft, childTop, childLeft + width, childTop + height);
        } else {

            int childLeft = getPaddingLeft();
            int childTop = getPaddingTop();
            int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
            int width = height;

            mImageView.layout(childLeft, childTop, childLeft + width, childTop + height);

            mLoadingView.layout(childLeft, childTop, childLeft + width, childTop + height);

            childLeft += width + getPaddingLeft();
            width = getMeasuredWidth() - childLeft - getPaddingRight();

            mTextView.layout(childLeft, childTop, childLeft + width, childTop + height);
        }
    }

    // ----------------------------------------------------------------------
    // The rest of the implementation is for custom per-child layout parameters.
    // If you do not need these (for example you are writing a layout manager
    // that does fixed positioning of its children), you can drop all of this.

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }



}
