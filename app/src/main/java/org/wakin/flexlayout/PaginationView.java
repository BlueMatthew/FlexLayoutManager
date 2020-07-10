package org.wakin.flexlayout;

import androidx.recyclerview.widget.RecyclerView;

public interface PaginationView {
    void offsetView(float offsetX, float offsetY);
    void swap(RecyclerView recyclerView, int minPagbleItemPosition, boolean inSticky);
    void releaseReusableViews();
    void removeFromParentViewGroup();
    int getFirstVisibleItemPosition(); // NOT GOOD => context?
    int getFirstVisibleItemOffset(); // NOT GOOD

}
