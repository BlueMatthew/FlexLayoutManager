//
// Created by Matthew on 2020-07-03.
//

#ifndef FLEXLAYOUTMANAGER_GRAPHICS_H
#define FLEXLAYOUTMANAGER_GRAPHICS_H


// Here we use template for int and float
template<typename T>
struct PointT {
    T x;
    T y;

    PointT() : x(0), y(0) {}
    PointT(T x1, T y1) : x(x1), y(y1) {}
};

template<typename T>
struct SizeT {
    T width;
    T height;

    SizeT() : width(0), height(0) {}
    SizeT(T w, T h) : width(w), height(h) {}
};

template<typename T>
struct RectT {
    typedef PointT<T> Point;
    typedef SizeT<T> Size;

    Point origin;
    Size size;

    RectT() {}
    RectT(T x, T y, T width, T height) : origin(x, y), size(width, height) {}
    RectT(const Point &p, const Size &s) : origin(p), size(s) {}
    RectT(const RectT<T> &rect) : origin(rect.origin), size(rect.size) {}

    inline T left() const { return origin.x; }
    inline T top() const { return origin.y; }
    inline T right() const { return origin.x + size.width; }
    inline T bottom() const { return origin.y + size.height; }

    inline void offset(T dx, T dy) { origin.x += dx; origin.y += dy; }

    inline bool empty() const { return size.width == 0 || size.height == 0; }
    inline bool intersects(const RectT<T> &rect) {
        return (origin.x < rect.right() && right() > rect.origin.x &&
            origin.y > rect.bottom() && bottom() < rect.origin.y);
    }
};

template<typename T>
struct InsetsT
{
    T left;
    T top;
    T right;
    T bottom;

    InsetsT() : left(0), top(0), right(0), bottom(0) {}
};


typedef PointT<int> Point;
typedef SizeT<int> Size;
typedef RectT<int> Rect;
typedef InsetsT<int> Insets;


#endif //FLEXLAYOUTMANAGER_GRAPHICS_H
