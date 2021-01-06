package org.wakin.flexlayout.app.models;

import android.graphics.Color;

public class CellData {

    public int viewType;

    public int width;
    public int height;

    public boolean fullSpan = false;

    public String text = "";
    public String url = "";
    public String action = "";
    public int foregroundColor = Color.BLACK;
    public int backgroundColor = Color.TRANSPARENT;
    public int textColor = Color.BLACK;
    public int imageBackgroundColor = Color.TRANSPARENT;

    public String imageUrl;

    public long firstDisplayTime = 0;
    public long loadingTime = 0;

    public void startLoading() {
        if (firstDisplayTime == 0) {
            firstDisplayTime = System.currentTimeMillis();
            loadingTime = 300 + (int)(Math.random() * 1500);
        }
    }

    public boolean isLoaded() {
        return firstDisplayTime > 0 && System.currentTimeMillis() - firstDisplayTime > loadingTime;
    }

    public long getRemainingLoadingTime() {
        long ts = System.currentTimeMillis() - firstDisplayTime;
        return ts < loadingTime ? (loadingTime - ts) : 0;
    }

}
