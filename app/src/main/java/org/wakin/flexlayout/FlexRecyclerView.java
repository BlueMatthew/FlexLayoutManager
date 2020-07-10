package org.wakin.flexlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class FlexRecyclerView extends RecyclerView {

    public static class SectionViewHolder extends RecyclerView.ViewHolder {

        public static int INVALID_SECTION = -1;
        public static int INVALID_ITEM = -1;
        private int mSection = INVALID_SECTION;
        private int mItem = INVALID_ITEM;

        public SectionViewHolder(View itemView) {
            super(itemView);
        }

        public void setSectionAndItem(int section, int item) {
            mSection = section;
            mItem = item;
        }

        public int getSection() {
            return mSection;
        }

        public int getItem() {
            return mItem;
        }
    }

    public FlexRecyclerView(Context context) {
        super(context);
    }

    public FlexRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlexRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // this.mChildHelper
    }


}
