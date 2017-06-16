package org.puder.activitymonitor.ann;

class Feature1 extends Feature {

    private double max;


    @Override
    public void reset() {
        super.reset();
        max = 0;
    }

    @Override
    protected void consume() {
        AccelerometerReading v = n0();
        double vector = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        max = Math.max(max, vector);
    }

    @Override
    public double getFeatureValue() {
        return max;
    }
}
