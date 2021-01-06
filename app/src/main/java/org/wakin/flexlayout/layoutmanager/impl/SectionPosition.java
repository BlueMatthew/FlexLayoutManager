package org.wakin.flexlayout.layoutmanager.impl;

public class SectionPosition implements Comparable<SectionPosition> {
    public int section;
    public int item;

    public SectionPosition() {
        this.section = 0;
        this.item = 0;
    }

    public SectionPosition(int section, int item) {
        this.section = section;
        this.item = item;
    }

    public void set(int section, int item) {
        this.section = section;
        this.item = item;
    }

    public SectionPosition(long sectionPosition) {
        this.section = (int)(sectionPosition >> 32);
        this.item = (int)(sectionPosition & 0xFFFFFFFF);
    }

    public long toLong() {
        return ((long)section) << 32 + item;
    }

    public static long toLong(int section, int item) {
        return ((long)section) << 32 + item;
    }

    public int compareTo(SectionPosition rhs) {
        if (this == rhs) return 0;
        int result = Integer.compare(section, rhs.section);
        return result == 0 ? Integer.compare(item, rhs.item) : result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SectionPosition) {
            SectionPosition indexPath = (SectionPosition) o;
            return this.section == indexPath.section && this.item == indexPath.item;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return section * 32713 + item;
    }
}
