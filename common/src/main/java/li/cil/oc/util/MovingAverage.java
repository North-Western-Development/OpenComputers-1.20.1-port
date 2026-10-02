package li.cil.oc.util;

public class MovingAverage {
    public final int size;
    private final int[] data;
    private int head = 0;
    private int cachedAverage = 0;
    private boolean dirty = true;

    public MovingAverage(int size) {
        this.size = size;
        this.data = new int[size];
    }

    /** Scala `apply()`: the current average. */
    public int apply() {
        if (dirty) {
            int sum = 0;
            for (int value : data) sum += value;
            cachedAverage = sum / size;
            dirty = false;
        }
        return cachedAverage;
    }

    /** Scala `+=`: adds a sample. */
    public MovingAverage add(int value) {
        data[head] = value;
        head = (head + 1) % size;
        dirty = true;
        return this;
    }
}
