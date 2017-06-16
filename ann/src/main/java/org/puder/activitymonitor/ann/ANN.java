
package org.puder.activitymonitor.ann;

public class ANN implements FeatureConsumer {

    final static private double weights[][][] = {{
            {2.2211747169, 0.6581830978, 1.9202015400},
            {-2.9966619015, 1.2316974401, -4.8719739914},
            {1.5943980217, 0.1702858359, 2.2640266418},
            {-1.6490993500, -1.3654567003, -1.6381849051},
            {-2.1070358753, -0.5064755082, -1.7927542925}},
            {{-2.1203093529, 7.7758750916, -2.9036183357, -0.7903330326, -1.0034569502},
            {3.2478666306, -1.5088549852, 3.2348179817, -2.0601015091, -3.1487274170},
            {-2.6726157665, -8.0043554306, -2.2252202034, 2.6207652092, 2.0241651535}}
    };

    final static private double biases[][] = {
            {-0.7904643416, -5.4188251495, -0.5106028318, 1.1552959681, 1.5620769262},
            {-1.9194872379, -0.7610315084, -0.2673718333}
    };

    private FeatureGenerator featureGenerator;
    private ANNConsumer      annConsumer;


    public ANN(ANNConsumer annConsumer) {
        this.annConsumer = annConsumer;
        featureGenerator = new FeatureGenerator(true, this);

    }

    public void addAccelerometerReading(long timestamp, float x, float y, float z) {
        featureGenerator.addAccelerometerReading(timestamp, x, y, z);
    }

    @Override
    public void consume(double feature1, double feature2, double feature3) {
        double output[] = feedForward(new double[] { feature1, feature2, feature3 });
        annConsumer.consume(output[Activities.ACTIVITY_STANDING.ordinal()],
                output[Activities.ACTIVITY_HOPPING.ordinal()],
                output[Activities.ACTIVITY_WALKING.ordinal()]);
    }

    private double[] feedForward(double[] features) {
        double[] output = features;
        for (int i = 0; i < weights.length; i++) {
            output = dot(output, weights[i]);
            output = add(output, biases[i]);
            sigmoid(output);
        }
        return output;
    }

    private static double[] dot(double[] x, double[][] y) {
        int size = y.length;
        double[] matrix = new double[size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < x.length; j++) {
                matrix[i] += x[j] * y[i][j];
            }
        }
        return matrix;
    }

    private static double[] add(double[] x, double[] y) {
        double[] z = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            z[i] = x[i] + y[i];
        }
        return z;
    }

    private void sigmoid(double[] z) {
        for (int i = 0; i < z.length; i++) {
            z[i] = sigmoid(z[i]);
        }
    }

    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
