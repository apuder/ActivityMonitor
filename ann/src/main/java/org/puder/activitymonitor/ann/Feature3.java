package org.puder.activitymonitor.ann;

class Feature3 extends Feature {

    private double cav;

    @Override
    public void reset() {
        super.reset();
        cav = 0;
    }

    @Override
    protected void consume() {
        AccelerometerReading n0 = n0();
        AccelerometerReading n1 = n1();

        double dt = n1.timestamp - n0.timestamp;
        double v0 = Math.sqrt(n0.x * n0.x + n0.y * n0.y + n0.z * n0.z);
        double v1 = Math.sqrt(n1.x * n1.x + n1.y * n1.y + n1.z * n1.z);

        double amp = (v0 + v1) / 2;

        cav += amp * (dt / 1000);
    }

    @Override
    public double getFeatureValue() {
        return cav;
    }
}
