package org.puder.activitymonitor.ann;

class FeatureSet {

    private Feature featureSet[];
    private long epochEnds;
    private long offset;


    FeatureSet(int offset) {
        featureSet = new Feature[3];
        featureSet[0] = new Feature1();
        featureSet[1] = new Feature2();
        featureSet[2] = new Feature3();
        epochEnds = -1;
        this.offset = offset;
    }

    long getEpochEnds() {
        return epochEnds;
    }

    void setEpochEnds(long epochEnds) {
        this.epochEnds = epochEnds;
    }

    Feature getFeature(int i) {
        return featureSet[i];
    }

    void addFilteredAccelerometerReading(long ts, float x, float y, float z) {
        if (epochEnds == -1) {
            epochEnds = ts + offset;
        }
        for (Feature feature : featureSet) {
            feature.addFilteredAccelerometerReading(ts, x, y, z);
        }
    }

    void reset() {
        for (Feature feature : featureSet) {
            feature.reset();
        }
    }
}
