
package org.puder.activitymonitor.ann;

public class FeatureGenerator {

    private float           accX1           = 0;
    private float           accY1           = 0;
    private float           accZ1           = 0;
    private float           valX1           = 0;
    private float           valY1           = 0;
    private float           valZ1           = 0;

    private FeatureSet      featureSets[];

    private double                  featureAdjust[] = { 1.116530462564178, 26.911815963750435, 0.809525190466737 };
    private double                  featureScale[]  = { 1.195808222385743, 6.264660447774289, 0.7906133044607515 };

    private int numFeatureSets;
    private FeatureConsumer consumer;
    private boolean         scaleFeatures;


    public FeatureGenerator(boolean scaleFeatures, FeatureConsumer consumer) {
        this.numFeatureSets = 4;
        this.consumer = consumer;
        this.scaleFeatures = scaleFeatures;

        featureSets = new FeatureSet[numFeatureSets];
        for (int i = 0; i < numFeatureSets; i++) {
            featureSets[i] = new FeatureSet(i * 500);
        }
    }

    /*
     * This method will be called whenever there are sensor events (in this case
     * accelerometer events).
     */
    public void addAccelerometerReading(long timestamp, float x, float y, float z) {
        long ts = timestamp / (1000 * 1000);
        final float GRAVITY = 9.80665f;
        float valX0 = x / GRAVITY;
        float valY0 = y / GRAVITY;
        float valZ0 = z / GRAVITY;

        float accX0 = toground_rt(valX0, valX1, accX1);
        float accY0 = toground_rt(valY0, valY1, accY1);
        float accZ0 = toground_rt(valZ0, valZ1, accZ1);
        addFilteredAccelerometerReading(ts, accX0, accY0, accZ0);
        // Assign back of the current value
        valX1 = valX0;
        valY1 = valY0;
        valZ1 = valZ0;

        accX1 = accX0;
        accY1 = accY0;
        accZ1 = accZ0;
    }

    private float toground_rt(float x0, float x1, float a1) {
        final float q = 0.8f;
        float b0 = (float) (2.0 / (1 + q));
        return (x0 - x1) / (b0) + q * a1;
    }

    private void addFilteredAccelerometerReading(long ts, float x, float y, float z) {
        for (int i = 0; i < numFeatureSets; i++) {
            FeatureSet featureSet = featureSets[i];
            featureSet.addFilteredAccelerometerReading(ts, x, y, z);
            if (featureSet.getEpochEnds() < ts) {
                double feature1 = featureSet.getFeature(0).getFeatureValue();
                double feature2 = featureSet.getFeature(1).getFeatureValue();
                double feature3 = featureSet.getFeature(2).getFeatureValue();
                if (scaleFeatures) {
                    feature1 = (feature1 - featureAdjust[0]) / featureScale[0];
                    feature2 = (feature2 - featureAdjust[1]) / featureScale[1];
                    feature3 = (feature3 - featureAdjust[2]) / featureScale[2];
                }
                consumer.consume(feature1, feature2, feature3);
                featureSet.setEpochEnds(ts + 2000);
                featureSet.reset();
            }
        }
    }

    public void reset() {
        for (FeatureSet featureSet : featureSets) {
            featureSet.setEpochEnds(-1);
            featureSet.reset();
        }
    }
}
