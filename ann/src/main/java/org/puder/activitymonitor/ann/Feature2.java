package org.puder.activitymonitor.ann;

public class Feature2 extends Feature {

    private int zcX;
    private int zcY;
    private int zcZ;


    @Override
    protected void consume() {
        AccelerometerReading n0 = n0();
        AccelerometerReading n1 = n1();

        if (n0.x * n1.x < 0) {
            zcX++;
        }

        if (n0.y * n1.y < 0) {
            zcY++;
        }

        if (n0.z * n1.z < 0) {
            zcZ++;
        }
    }

    @Override
    public void reset() {
        super.reset();
        zcX = 0;
        zcY = 0;
        zcZ = 0;
    }

    @Override
    public double getFeatureValue() {
        return Math.max(Math.max(zcX, zcY), zcZ);
    }
}
