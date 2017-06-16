
Activity Monitor
================

This repository hosts the source code for a simple proof-of-concept
of a neural network running on an Arduino. The sketch can distinguish
three different types of human activities: standing, walking, and
hopping. Three LEDs serve as indicators for the respective activities.
Three different features are extracted from a two second sliding window
of the raw data coming from an accelerometer: maximum zero-crossing
rate, cumulative absolute velocity of the acceleration vector sum, and
the maximum acceleration vector sum. The features are fed into a
3-5-3 layer neural network to determine the activity.

The repository contains several sub-projects in different directories:

* ``app``: an Android application that is used to collect the acceleration
  data for training purposes. The main activity of this app also demonstrates
  the resulting neural network.
* ``data``: contains a total of 24 minutes of raw acceleration values at a
  sampling rate of 25 Hz that were recorded with the help of the Android app
  while performing the three activities. The data is stored in CSV files.
* ``ann``: contains the feature extraction algorithms and the resulting
  artificial neural network.
* ``sgd``: contains the implementation of a stochastic gradient descent
  learning algorithm. Running the command ``./gradlew run`` will first
  determine the feature scaling, then create the training data that is stored
  in the file ``data/training-data.csv`` and finally perform the gradient descent
  to compute the weights and biases of the neural network. The resulting
  values are printed to the console and need to be manually pasted to the
  source code (ugly, I know).
* ``arduino``: contains an Arduino sketch that is basically a C++ version
  of the Java implementation of the neural network contained in directory ``ann``.
  The sketch needs to be opened with the PlatformIO IDE (NOT the Arduino IDE).

The Arduino version of the Activity Monitor runs on a standard Arduino Uno.
It makes use of the MPU6050 accelerometer and three colored LEDs for the
various activities: red (standing), green (walking), and blue (hopping).
The following image depicts the wiring:

![Activity Monitor](/arduino/activity-monitor_bb.png?raw=true "Fritzing for Activity Monitor")

License
-------

This work is released under the Apache 2 License.

