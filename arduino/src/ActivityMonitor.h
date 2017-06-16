
#ifndef __ACTIVITY_MONITOR_H__
#define __ACTIVITY_MONITOR_H__

#include <Arduino.h>
#include "config.h"


/*************************************************************************************
 * log
 *************************************************************************************/

#ifdef DEBUG
#define LOG(msg) Serial.print(msg)
#define LOGLN(msg) Serial.println(msg)
#else
#define LOG(msg)
#define LOGLN(msg)
#endif

class Accelerometer {
public:
  enum TYPE {TYPE_ADXL345, TYPE_GY521};

  static Accelerometer* getSensor(TYPE type);
  virtual void getAcceleration(long& ts, float&x, float& y, float& z) = 0;
};

class ANNConsumer {
public:
    virtual void consume(double standing, double hopping, double walking) = 0;
};

class ANNImpl;

class ANN {
private:
  ANNImpl* impl;
public:
  static ANN* instance(ANNConsumer* consumer);
  void addAccelerometerReading(long timestamp, float x, float y, float z);
};

#endif
