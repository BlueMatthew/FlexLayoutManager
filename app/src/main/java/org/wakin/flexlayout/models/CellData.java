package org.wakin.flexlayout.models;

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

    public boolean displayed = false;
}
