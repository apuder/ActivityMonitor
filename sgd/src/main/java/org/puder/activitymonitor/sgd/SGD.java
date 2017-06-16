package org.puder.activitymonitor.sgd;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.scalar.ScalarDivision;
import org.nd4j.linalg.api.ops.impl.transforms.Sigmoid;
import org.nd4j.linalg.api.ops.impl.transforms.SigmoidDerivative;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.string.NDArrayStrings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SGD {

    class NABLA {
        List<INDArray> nabla_b;
        List<INDArray> nabla_w;
    }

    private int sizes[];
    private int numLayers;
    private List<INDArray> weights;
    private List<INDArray> biases;

    private List<TrainingData> trainingDataSet;

    public SGD(int[] sizes) {
        this.sizes = sizes;
        numLayers = sizes.length;
        init();
    }

    private void init() {
        weights = new ArrayList<>();
        biases = new ArrayList<>();
        for (int i = 1; i < numLayers; i++) {
            biases.add(Nd4j.randn(new int[]{sizes[i], 1}));
        }
        for (int i = 0; i < numLayers - 1; i++) {
            INDArray w = Nd4j.randn(new int[]{sizes[i + 1], sizes[i]});
            double sq = Math.sqrt(sizes[i]);
            w = Nd4j.getExecutioner().execAndReturn(new ScalarDivision(w, sq));
            weights.add(w);
        }
    }

    public void train(List<TrainingData> trainingDataSet, int epochs, int mini_batch_size, double eta) {
        double lmbda = 0;
        this.trainingDataSet = trainingDataSet;
        int n = trainingDataSet.size();
        for (int j = 0; j < epochs; j++) {
            Collections.shuffle(trainingDataSet);
            for (int k = 0; k < n; k += mini_batch_size) {
                int to = Math.min(k + mini_batch_size, n);
                List<TrainingData> miniBatch = trainingDataSet.subList(k, to);
                updateMiniBatch(miniBatch, eta, lmbda, n);
            }
        }
    }

    private void updateMiniBatch(List<TrainingData> miniBatch, double eta, double lmbda, int n) {
        List<INDArray> nabla_b = new ArrayList<>();
        for (INDArray b : biases) {
            nabla_b.add(Nd4j.zeros(b.shape()));
        }
        List<INDArray> nabla_w = new ArrayList<>();
        for (INDArray w : weights) {
            nabla_w.add(Nd4j.zeros(w.shape()));
        }
        for (TrainingData td : miniBatch) {
            NABLA nabla = backprop(td);
            List<INDArray> delta_nabla_b = nabla.nabla_b;
            List<INDArray> delta_nabla_w = nabla.nabla_w;
            for (int i = 0; i < nabla_b.size(); i++) {
                INDArray nb = nabla_b.get(i);
                INDArray dnd = delta_nabla_b.get(i);
                nabla_b.set(i, nb.add(dnd));
            }
            for (int i = 0; i < nabla_w.size(); i++) {
                INDArray nw = nabla_w.get(i);
                INDArray dnw = delta_nabla_w.get(i);
                nabla_w.set(i, nw.add(dnw));
            }
        }
        for (int i = 0; i < weights.size(); i++) {
            INDArray w = weights.get(i);
            INDArray nw = nabla_w.get(i);
            INDArray wp = w.mul(1 - eta * (lmbda / n)).sub(nw.mul(eta / miniBatch.size()));
            weights.set(i, wp);
        }
        for (int i = 0; i < biases.size(); i++) {
            INDArray b = biases.get(i);
            INDArray nb = nabla_b.get(i);
            INDArray bp = b.sub(nb.mul(eta / miniBatch.size()));
            biases.set(i, bp);
        }
    }

    private NABLA backprop(TrainingData trainingData) {
        List<INDArray> nabla_b = new ArrayList<>();
        for (INDArray b : biases) {
            nabla_b.add(Nd4j.zeros(b.shape()));
        }
        List<INDArray> nabla_w = new ArrayList<>();
        for (INDArray w : weights) {
            nabla_w.add(Nd4j.zeros(w.shape()));
        }
        INDArray activation = trainingData.input.dup();
        List<INDArray> activations = new ArrayList<>();
        activations.add(activation.dup());
        List<INDArray> zs = new ArrayList<>();
        for (int i = 0; i < biases.size(); i++) {
            INDArray w = weights.get(i);
            INDArray b = biases.get(i);
            INDArray z = w.mmul(activation).add(b);
            zs.add(z.dup());
            activation = Nd4j.getExecutioner().execAndReturn(new Sigmoid(z));
            activations.add(activation);
        }
        INDArray delta = activations.get(activations.size() - 1).sub(trainingData.output);
        nabla_b.set(nabla_b.size() - 1, delta);
        INDArray t = activations.get(activations.size() - 2).transpose();
        INDArray w = delta.mmul(t);
        nabla_w.set(nabla_w.size() - 1, w);
        for (int l = 2; l < numLayers; l++) {
            INDArray z = zs.get(zs.size() - l);
            INDArray sp = Nd4j.getExecutioner().execAndReturn(new SigmoidDerivative(z));
            t = weights.get(weights.size() + 1 - l).transpose();
            delta = t.mmul(delta).mul(sp);
            nabla_b.set(nabla_b.size() - l, delta);
            t = activations.get(activations.size() - 1 - l).transpose();
            nabla_w.set(nabla_w.size() - l, delta.mmul(t));
        }
        NABLA nabla = new NABLA();
        nabla.nabla_b = nabla_b;
        nabla.nabla_w = nabla_w;
        return nabla;
    }

    private INDArray feedForward(INDArray a) {
        for (int i = 0; i < biases.size(); i++) {
            INDArray w = weights.get(i);
            INDArray b = biases.get(i);
            a = Nd4j.getExecutioner().execAndReturn(new Sigmoid(w.mmul(a).add(b)));
        }
        return a;
    }

    public void printWeightsAndBiases() {
        System.out.println("weights: [");
        for (INDArray w : weights) {
            System.out.println(new NDArrayStrings(10).format(w));
        }
        System.out.println("]");
        System.out.println("biases: [");
        for (INDArray b : biases) {
            System.out.println(new NDArrayStrings(10).format(b));
        }
        System.out.println("]");
    }

    public void validate() {
        int p = 0;
        int n = 0;
        for (TrainingData td : trainingDataSet) {
            INDArray a = feedForward(td.input);
            double r0 = a.getDouble(0);
            double r1 = a.getDouble(1);
            double r2 = a.getDouble(2);
            int m = -1;
            if (r0 > r1 && r0 > r2) m = 0;
            if (r1 > r0 && r1 > r2) m = 1;
            if (r2 > r0 && r2 > r1) m = 2;
            if (td.output.getDouble(m) == 1) {
                p++;
            } else {
                n++;
            }
        }
        System.out.println("Positive: " + p);
        System.out.println("Negative: " + n);
    }
}
