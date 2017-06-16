
package org.puder.activitymonitor.main;

import com.opencsv.CSVReader;

import org.nd4j.linalg.factory.Nd4j;
import org.puder.activitymonitor.ann.Activities;
import org.puder.activitymonitor.ann.FeatureConsumer;
import org.puder.activitymonitor.ann.FeatureGenerator;
import org.puder.activitymonitor.sgd.SGD;
import org.puder.activitymonitor.sgd.TrainingData;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.puder.activitymonitor.ann.Activities.ACTIVITY_HOPPING;
import static org.puder.activitymonitor.ann.Activities.ACTIVITY_STANDING;
import static org.puder.activitymonitor.ann.Activities.ACTIVITY_WALKING;

public class Main {

    private final static String       SAMPLE_DIR        = "data/";
    private final static String       TRAINING_DATA_FN  = "training-data.csv";

    private final static String[]     SAMPLE_FILES      = { "standing-right-hand-down.csv",
            "standing-right-hand-up.csv", "standing-left-hand-down.csv",
            "standing-left-hand-up.csv", "hopping-right-hand-down.csv", "hopping-right-hand-up.csv",
            "hopping-left-hand-down.csv", "hopping-left-hand-up.csv", "walking-right-hand-down.csv",
            "walking-right-hand-up.csv", "walking-left-hand-down.csv", "walking-left-hand-up.csv" };

    private final static Activities[] SAMPLE_CLASSIFIER = { ACTIVITY_STANDING, ACTIVITY_STANDING,
            ACTIVITY_STANDING, ACTIVITY_STANDING, ACTIVITY_HOPPING, ACTIVITY_HOPPING,
            ACTIVITY_HOPPING, ACTIVITY_HOPPING, ACTIVITY_WALKING, ACTIVITY_WALKING,
            ACTIVITY_WALKING, ACTIVITY_WALKING };

    private PrintWriter               writer;

    private FeatureGenerator          featureGenerator;

    private int                       sampleCount;
    private int                       currentActivity;

    private double                    feature1Mean      = 0;
    private double                    feature2Mean      = 0;
    private double                    feature3Mean      = 0;
    private int                       numSamples        = 0;
    private double                    feature1Deviation = 0;
    private double                    feature2Deviation = 0;
    private double                    feature3Deviation = 0;


    private void determineFeatureScaling() throws IOException {
        // https://en.wikipedia.org/wiki/Feature_scaling
        FeatureConsumer consumer = new FeatureConsumer() {
            @Override
            public void consume(double feature1, double feature2, double feature3) {
                feature1Mean += feature1;
                feature2Mean += feature2;
                feature3Mean += feature3;
                numSamples++;
            }
        };
        featureGenerator = new FeatureGenerator(false, consumer);
        processSamples();
        feature1Mean /= numSamples;
        feature2Mean /= numSamples;
        feature3Mean /= numSamples;
        consumer = new FeatureConsumer() {
            @Override
            public void consume(double feature1, double feature2, double feature3) {
                feature1Deviation += (feature1 - feature1Mean) * (feature1 - feature1Mean);
                feature2Deviation += (feature2 - feature2Mean) * (feature2 - feature2Mean);
                feature3Deviation += (feature3 - feature3Mean) * (feature3 - feature3Mean);
            }
        };
        featureGenerator = new FeatureGenerator(false, consumer);
        processSamples();
        feature1Deviation = Math.sqrt(feature1Deviation / numSamples);
        feature2Deviation = Math.sqrt(feature2Deviation / numSamples);
        feature3Deviation = Math.sqrt(feature3Deviation / numSamples);
        System.out.print("featureAdjust: [");
        System.out.println(feature1Mean + ", " + feature2Mean + ", " + feature3Mean + "]");
        System.out.print("featureScale: [");
        System.out.println(
                feature1Deviation + ", " + feature2Deviation + ", " + feature3Deviation + "]");
    }

    private void generateTrainingData() throws IOException {
        FeatureConsumer consumer = new FeatureConsumer() {
            @Override
            public void consume(double feature1, double feature2, double feature3) {
                if (sampleCount++ > 6) {
                    writer.print(currentActivity);
                    writer.print(',');
                    writer.print(feature1);
                    writer.print(',');
                    writer.print(feature2);
                    writer.print(',');
                    writer.println(feature3);
                }
            }
        };
        featureGenerator = new FeatureGenerator(true, consumer);
        writer = new PrintWriter(SAMPLE_DIR + TRAINING_DATA_FN);
        processSamples();
        writer.close();
    }

    private void processSamples() throws IOException {
        for (int i = 0; i < SAMPLE_FILES.length; i++) {
            String file = SAMPLE_FILES[i];
            currentActivity = SAMPLE_CLASSIFIER[i].ordinal();
            sampleCount = 0;
            CSVReader reader = new CSVReader(new FileReader(SAMPLE_DIR + file));
            String[] nextLine;
            featureGenerator.reset();
            while ((nextLine = reader.readNext()) != null) {
                long ts = Long.parseLong(nextLine[0]);
                float x = Float.parseFloat(nextLine[1]);
                float y = Float.parseFloat(nextLine[2]);
                float z = Float.parseFloat(nextLine[3]);
                featureGenerator.addAccelerometerReading(ts, x, y, z);
            }
        }
    }

    private static List<TrainingData> readTestData(String fn) {
        int[] shape = { 3, 1 };
        List<TrainingData> trainingDataSet = new ArrayList<>();
        try {
            CSVReader reader = new CSVReader(new FileReader(fn));
            String[] row;
            while ((row = reader.readNext()) != null) {
                int type = Integer.parseInt(row[0]);
                double f1 = Double.parseDouble(row[1]);
                double f2 = Double.parseDouble(row[2]);
                double f3 = Double.parseDouble(row[3]);
                TrainingData trainingData = new TrainingData();
                trainingData.input = Nd4j.create(new double[] { f1, f2, f3 }, shape);
                trainingData.output = Nd4j.zeros(shape);
                trainingData.output.putScalar(type, (double) 1);
                trainingDataSet.add(trainingData);
            }
        } catch (java.io.IOException e) {
        }
        return trainingDataSet;
    }

    public static void main(String[] args) {
        Main main = new Main();
        try {
            main.determineFeatureScaling();
            main.generateTrainingData();
            List<TrainingData> trainingDataSet = readTestData(SAMPLE_DIR + TRAINING_DATA_FN);
            SGD sgd = new SGD(new int[] { 3, 5, 3 });
            sgd.train(trainingDataSet, 15, 400, 3.0);
            sgd.printWeightsAndBiases();
            sgd.validate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}