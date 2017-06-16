package org.puder.activitymonitor.ann;

abstract class Feature {

    private AccelerometerReading[] tuple;
    private int                    n0;
    private int                    n1;
    private boolean                isFirstReading;


    Feature() {
        tuple = new AccelerometerReading[2];
        tuple[0] = new AccelerometerReading();
        tuple[1] = new AccelerometerReading();
        n0 = 0;
        n1 = 1;
        isFirstReading = true;
    }

    void addFilteredAccelerometerReading(long ts, float x, float y, float z) {
        n0 = n1;
        n1 = (n1 + 1) % 2;
        AccelerometerReading v = tuple[n1];
        v.timestamp = ts;
        v.x = x;
        v.y = y;
        v.z = z;
        if (!isFirstReading) {
            consume();
        }
        isFirstReading = false;
    }

    AccelerometerReading n0() {
        return tuple[n0];
    }

    AccelerometerReading n1() {
        return tuple[n1];
    }

    public void reset() {
        isFirstReading = true;
    }

    protected abstract void consume();

    public abstract double getFeatureValue();
}
