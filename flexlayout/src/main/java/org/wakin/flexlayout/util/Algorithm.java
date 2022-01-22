package org.wakin.flexlayout.util;

import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

public class Algorithm {

    private static final int BINARYSEARCH_THRESHOLD   = 8096;

    public static <T1, T2> int binarySearch(List<? extends T1> list, T2 key, Comparator<? super T1, ? super T2> c) {
        if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
            return Algorithm.indexedBinarySearch(list, key, c);
        else
            return Algorithm.iteratorBinarySearch(list, key, c);
    }

    public static <T1, T2> int lowerBound(List<? extends T1> list, T2 key, Comparator<? super T1, ? super T2> c) {
        if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
            return Algorithm.indexedLowerBound(list, key, c);
        else
            return Algorithm.iteratorLowerBound(list, key, c);
    }

    public static <T1, T2> int upperBound(List<? extends T1> list, T2 key, Comparator<? super T1, ? super T2> c) {
        if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
            return Algorithm.indexedUpperBound(list, key, c);
        else
            return Algorithm.iteratorUpperBound(list, key, c);
    }

    private static <T1, T2> int indexedBinarySearch(List<? extends T1> l, T2 key, Comparator<? super T1, ? super T2> c) {
        int low = 0;
        int high = l.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T1 midVal = l.get(mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    private static <T1, T2> int indexedLowerBound(List<? extends T1> l, T2 key, Comparator<? super T1, ? super T2> c) {
        int low = 0;
        int high = l.size()-1;

        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            T1 midVal = l.get(mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else {
                result = mid; // key found
                high = mid - 1;
            }
        }
        return result;
    }

    private static <T1, T2> int indexedUpperBound(List<? extends T1> l, T2 key, Comparator<? super T1, ? super T2> c) {
        int low = 0;
        int high = l.size()-1;

        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            T1 midVal = l.get(mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else {
                result = mid; // key found
                low = mid + 1;
            }
        }
        return result;
    }

    private static <T1, T2> int iteratorBinarySearch(List<? extends T1> l, T2 key, Comparator<? super T1, ? super T2> c) {
        int low = 0;
        int high = l.size()-1;
        ListIterator<? extends T1> i = l.listIterator();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T1 midVal = get(i, mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    private static <T1, T2> int iteratorLowerBound(List<? extends T1> l, T2 key, Comparator<? super T1, ? super T2> c) {
        int low = 0;
        int high = l.size()-1;
        ListIterator<? extends T1> i = l.listIterator();

        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            T1 midVal = get(i, mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else {
                result = mid; // key found
                high = mid - 1;
            }
        }
        return result;
    }

    private static <T1, T2> int iteratorUpperBound(List<? extends T1> l, T2 key, Comparator<? super T1, ? super T2> c) {
        int low = 0;
        int high = l.size()-1;
        ListIterator<? extends T1> i = l.listIterator();

        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            T1 midVal = get(i, mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else {
                result = mid; // key found
                low = mid + 1;
            }
        }
        return result;
    }

    /**
     * Gets the ith element from the given list by repositioning the specified
     * list listIterator.
     */
    private static <T> T get(ListIterator<? extends T> i, int index) {
        T obj = null;
        int pos = i.nextIndex();
        if (pos <= index) {
            do {
                obj = i.next();
            } while (pos++ < index);
        } else {
            do {
                obj = i.previous();
            } while (--pos > index);
        }
        return obj;
    }

}
