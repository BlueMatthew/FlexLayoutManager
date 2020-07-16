package org.wakin.flexlayout.LayoutManager.Graphics;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.PrintWriter;

public  class Size implements Parcelable {
    public int width;
    public int height;

    public Size() {
        width = 0;
        height = 0;
    }

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size(Size src) {
        this.width = src.width;
        this.height = src.height;
    }

    /**
     * Set the point's x and y coordinates
     */
    public void set(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns true if the point's coordinates equal (x,y)
     */
    public final boolean equals(int width, int height) {
        return this.width == width && this.height == height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        if (width != size.width) return false;
        if (height != size.height) return false;

        return true;
    }

    @Override
    public int hashCode() {
        // android.util.Size
        return height ^ ((width << (Integer.SIZE / 2)) | (width >>> (Integer.SIZE / 2)));
    }

    @Override
    public String toString() {
        return "Size(" + width + ", " + height + ")";
    }

    /** @hide */
    public void printShortString(PrintWriter pw) {
        pw.print("["); pw.print(width); pw.print(","); pw.print(height); pw.print("]");
    }

    /**
     * Parcelable interface methods
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write this point to the specified parcel. To restore a point from
     * a parcel, use readFromParcel()
     * @param out The parcel to write the point's coordinates into
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(width);
        out.writeInt(height);
    }

    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() {
        /**
         * Return a new point from the data in the specified parcel.
         */
        public Point createFromParcel(Parcel in) {
            Point r = new Point();
            r.readFromParcel(in);
            return r;
        }

        /**
         * Return an array of rectangles of the specified size.
         */
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };

    /**
     * Set the point's coordinates from the data stored in the specified
     * parcel. To write a point to a parcel, call writeToParcel().
     *
     * @param in The parcel to read the point's coordinates from
     */
    public void readFromParcel(Parcel in) {
        width = in.readInt();
        height = in.readInt();
    }
};
