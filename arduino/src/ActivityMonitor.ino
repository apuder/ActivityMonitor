
#include "ActivityMonitor.h"

const int PIN_STANDING = 3;
const int PIN_WALKING = 5;
const int PIN_HOPPING = 6;

static Accelerometer* accelerometer;
static ANN* ann;

class ActivityMonitorANNConsumer : public virtual ANNConsumer {
private:
  int scale(double v) {
    int s = (int) (v * 300.0 - 55.0);
    if (s < 0) s = 0;
    return s;
  };

public:
  void consume(double standing, double hopping, double walking) {
    analogWrite(PIN_STANDING, scale(standing));
    analogWrite(PIN_HOPPING, scale(hopping));
    analogWrite(PIN_WALKING, scale(walking));

#ifdef DEBUG
    double s = standing;
    double h = hopping;
    double w = walking;

    if (s > h && s > w) {
        LOG("S      ");
    }
    if (h > s && h > w) {
        LOG("  H    ");
    }
    if (w > s && w > h) {
        LOG("    W  ");
    }
    LOG(s);
    LOG(" ");
    LOG(h);
    LOG(" ");
    LOGLN(w);
#endif
  }
};

void setup() {
#ifdef DEBUG
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect.
  }
#endif

  pinMode(PIN_STANDING, OUTPUT);
  pinMode(PIN_WALKING, OUTPUT);
  pinMode(PIN_HOPPING, OUTPUT);

  accelerometer = Accelerometer::getSensor(Accelerometer::SENSOR_TYPE);
  ann = ANN::instance(new ActivityMonitorANNConsumer());
}

void loop() {
  delay(1000 / SAMPLING_RATE);
  long ts;
  float valX0;
  float valY0;
  float valZ0;
  accelerometer->getAcceleration(ts, valX0, valY0, valZ0);
  ann->addAccelerometerReading(ts, valX0, valY0, valZ0);
}
