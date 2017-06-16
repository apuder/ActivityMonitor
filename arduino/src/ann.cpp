
#include "ActivityMonitor.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

#define MAX(a, b) ((a) < (b)) ? b : a
#define ABS(a) ((a) < 0) ? -(a) : (a)

struct AccelerometerReading {
    long timestamp;
    float x;
    float y;
    float z;
};

class Feature {
private:
    AccelerometerReading* tuple;
    int                   n0_;
    int                   n1_;

public:
    Feature() {
        tuple = new AccelerometerReading[2];
        n0_ = 0;
        n1_ = 1;
    }

    virtual ~Feature() {
      delete[] tuple;
    }

    void addFilteredAccelerometerReading(long ts, float x, float y, float z) {
        n0_ = n1_;
        n1_ = (n1_ + 1) % 2;
        AccelerometerReading& v = tuple[n1_];
        v.timestamp = ts;
        v.x = x;
        v.y = y;
        v.z = z;
        consume();
    }

protected:
    AccelerometerReading& get_n0() {
        return tuple[n0_];
    }

    AccelerometerReading& get_n1() {
        return tuple[n1_];
    }

public:
    virtual void reset() = 0;
    virtual void consume() = 0;
    virtual double getFeatureValue() = 0;
};

class Feature1 : public virtual Feature {
private:
    double max;

public:
    void reset() {
        max = 0;
    }

    void consume() {
        AccelerometerReading& v = get_n0();
        double vector = sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        max = MAX(max, vector);
    }

    double getFeatureValue() {
        return max;
    }
};

class Feature2 : public virtual Feature {
private:
    int zcX;
    int zcY;
    int zcZ;

public:
    void consume() {
        AccelerometerReading& n0 = get_n0();
        AccelerometerReading& n1 = get_n1();

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

    void reset() {
        zcX = 0;
        zcY = 0;
        zcZ = 0;
    }

    double getFeatureValue() {
        return MAX(MAX(zcX, zcY), zcZ);
    }
};

class Feature3 : public virtual Feature {
private:
    double cav;

public:
    void reset() {
        cav = 0;
    }

    void consume() {
        AccelerometerReading& n0 = get_n0();
        AccelerometerReading& n1 = get_n1();

        double dt = n1.timestamp - n0.timestamp;
        double v0 = sqrt(n0.x * n0.x + n0.y * n0.y + n0.z * n0.z);
        double v1 = sqrt(n1.x * n1.x + n1.y * n1.y + n1.z * n1.z);

        double amp = (v0 + v1) / 2;

        cav += amp * (dt / 1000);
    }

    double getFeatureValue() {
        return cav;
    }
};

class FeatureSet {
private:
    Feature* featureSet[3];
    long epochEnds;
    long offset;

public:
    FeatureSet(int offset) {
        featureSet[0] = new Feature1();
        featureSet[1] = new Feature2();
        featureSet[2] = new Feature3();
        epochEnds = -1;
        this->offset = offset;
    }

    virtual ~FeatureSet() {
      for (int i = 0; i < 3; i++) {
        delete featureSet[i];
      }
    }
    long getEpochEnds() {
        return epochEnds;
    }

    void setEpochEnds(long epochEnds) {
        this->epochEnds = epochEnds;
    }

    Feature& getFeature(int i) {
        return *featureSet[i];
    }

    void addFilteredAccelerometerReading(long ts, float x, float y, float z) {
        if (epochEnds == -1) {
            epochEnds = ts + offset;
        }
        for (int i = 0; i < 3; i++) {
            featureSet[i]->addFilteredAccelerometerReading(ts, x, y, z);
        }
    }

    void reset() {
        for (int i = 0; i < 3; i++) {
            featureSet[i]->reset();
        }
    }
};

class FeatureConsumer {
public:
    virtual void consume(double feature1, double feature2, double feature3) = 0;
};

class FeatureGenerator {
private:
    float           accX1           = 0;
    float           accY1           = 0;
    float           accZ1           = 0;
    float           valX1           = 0;
    float           valY1           = 0;
    float           valZ1           = 0;

    FeatureSet*     featureSets[4];

    double          featureAdjust[3] = { 1.116530462564178, 26.911815963750435, 0.809525190466737 };
    double          featureScale[3]  = { 1.195808222385743, 6.264660447774289, 0.7906133044607515 };

    FeatureConsumer* consumer;

public:
    FeatureGenerator(FeatureConsumer* consumer) {
        this->consumer = consumer;

        for (int i = 0; i < 4; i++) {
            featureSets[i] = new FeatureSet(i * 500);
        }
    }

    virtual ~FeatureGenerator() {
      for (int i = 0; i < 4; i++) {
          delete featureSets[i];
      }
    }
    /*
     * This method will be called whenever there are sensor events (in this case
     * accelerometer events).
     */
    void addAccelerometerReading(long timestamp, float x, float y, float z) {
        float valX0 = x;
        float valY0 = y;
        float valZ0 = z;

        float accX0 = toground_rt(valX0, valX1, accX1);
        float accY0 = toground_rt(valY0, valY1, accY1);
        float accZ0 = toground_rt(valZ0, valZ1, accZ1);
        addFilteredAccelerometerReading(timestamp, accX0, accY0, accZ0);
        // Assign back of the current value
        valX1 = valX0;
        valY1 = valY0;
        valZ1 = valZ0;

        accX1 = accX0;
        accY1 = accY0;
        accZ1 = accZ0;
    }

    float toground_rt(float x0, float x1, float a1) {
        const float q = 0.8f;
        float b0 = (float) (2.0 / (1 + q));
        return (x0 - x1) / (b0) + q * a1;
    }

    void addFilteredAccelerometerReading(long ts, float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            FeatureSet* featureSet = featureSets[i];
            featureSet->addFilteredAccelerometerReading(ts, x, y, z);
            if (featureSet->getEpochEnds() < ts) {
                double feature1 = featureSet->getFeature(0).getFeatureValue();
                double feature2 = featureSet->getFeature(1).getFeatureValue();
                double feature3 = featureSet->getFeature(2).getFeatureValue();
                feature1 = (feature1 - featureAdjust[0]) / featureScale[0];
                feature2 = (feature2 - featureAdjust[1]) / featureScale[1];
                feature3 = (feature3 - featureAdjust[2]) / featureScale[2];
                consumer->consume(feature1, feature2, feature3);
                featureSet->setEpochEnds(ts + 2000);
                featureSet->reset();
            }
        }
    }
};

class ANNUtils {
public:
  static void dot1(double x[], const double y[][3], double matrix[]) {
    for(int i = 0; i < 5; i++) {
      matrix[i] = 0;
      for(int j = 0; j < 3; j++) {
        matrix[i] += x[j] * y[i][j];
      }
    }
  }

  static void dot2(double x[], const double y[][5], double matrix[]) {
    for(int i = 0; i < 3; i++) {
      matrix[i] = 0;
      for(int j = 0; j < 5; j++) {
        matrix[i] += x[j] * y[i][j];
      }
    }
  }

  static void addSigmoid1(double x[], const double y[], double z[]) {
    for(int i = 0; i < 5; i++) {
      z[i] = sigmoid(x[i] + y[i]);
    }
  }

  static void addSigmoid2(double x[], const double y[], double z[]) {
    for(int i = 0; i < 3; i++) {
      z[i] = sigmoid(x[i] + y[i]);
    }
  }

  static double sigmoid(double z) {
    return 1/(1 + exp(-z));
  }
};

class ANNImpl : public virtual FeatureConsumer {
private:

  const double weights1[5][3] = {
            {2.2211747169, 0.6581830978, 1.9202015400},
            {-2.9966619015, 1.2316974401, -4.8719739914},
            {1.5943980217, 0.1702858359, 2.2640266418},
            {-1.6490993500, -1.3654567003, -1.6381849051},
            {-2.1070358753, -0.5064755082, -1.7927542925}
          };
  const double weights2[3][5] = {
            {-2.1203093529, 7.7758750916, -2.9036183357, -0.7903330326, -1.0034569502},
            {3.2478666306, -1.5088549852, 3.2348179817, -2.0601015091, -3.1487274170},
            {-2.6726157665, -8.0043554306, -2.2252202034, 2.6207652092, 2.0241651535}
          };
  const double biases1[5] = {-0.7904643416, -5.4188251495, -0.5106028318, 1.1552959681, 1.5620769262};
  const double biases2[3] = {-1.9194872379, -0.7610315084, -0.2673718333};

    FeatureGenerator* featureGenerator;
    ANNConsumer*      annConsumer;

public:
    ANNImpl(ANNConsumer* annConsumer) {
        this->annConsumer = annConsumer;
        featureGenerator = new FeatureGenerator(this);

    }

    void addAccelerometerReading(long timestamp, float x, float y, float z) {
        featureGenerator->addAccelerometerReading(timestamp, x, y, z);
    }

    void consume(double feature1, double feature2, double feature3) {
        double features[3] = {feature1, feature2, feature3};
        double output[5];
        ANNUtils::dot1(features, weights1, output);
        ANNUtils::addSigmoid1(output, biases1, output);
        double output1[3];
        ANNUtils::dot2(output, weights2, output1);
        ANNUtils::addSigmoid2(output1, biases2, output1);

        float s = output1[0];
        float h = output1[1];
        float w = output1[2];

        annConsumer->consume(s, h, w);
    }

};

ANN* ANN::instance(ANNConsumer* consumer) {
  ANN* ann = new ANN();
  ann->impl = new ANNImpl(consumer);
  return ann;
}

void ANN::addAccelerometerReading(long timestamp, float x, float y, float z) {
  impl->addAccelerometerReading(timestamp, x, y, z);
}
