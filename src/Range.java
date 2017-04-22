/**
 * Range object that needs to be made available for Lookup Table
 */
public class Range {
    private int min;
    private int max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public Boolean withinRange(int num) {
        if (num >= this.min && num <= this.getMax()) {
            return true;
        }
        else {
            return false;
        }
    }


    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}
