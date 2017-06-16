
#include <I2Cdev.h>
#include <MPU6050.h>
#include <Wire.h>

#include "ActivityMonitor.h"


class GY521 : public virtual Accelerometer {
private:
  MPU6050 mpu6050;
  const float DIVISOR = 16384.0f;//0x3fff;

public:
  GY521() {
    Wire.begin();
    mpu6050.initialize();
    if(!mpu6050.testConnection()) {
      LOG("E1");
    }
  }

  void getAcceleration(long& ts, float&x, float& y, float& z) {
    int16_t accX;
    int16_t accY;
    int16_t accZ;
    mpu6050.getAcceleration(&accX, &accY, &accZ);
    ts = millis();
    x = (float) accX / DIVISOR;
    y = (float) accY / DIVISOR;
    z = (float) accZ / DIVISOR;
  }
};

Accelerometer* Accelerometer::getSensor(TYPE type) {
  switch (type) {
    case TYPE_GY521:
      return new GY521();
  }
  LOG("E2");
  while(1) ;
}
