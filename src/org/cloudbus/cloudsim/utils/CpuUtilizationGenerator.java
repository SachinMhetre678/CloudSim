package org.cloudbus.cloudsim.utils;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.UtilizationModelStochastic;
import java.util.Random;

public class CpuUtilizationGenerator {
    private static final Random random = new Random();

    public enum UtilizationPattern {
        CONSTANT,
        RANDOM,
        SINUSOIDAL,
        SPIKING,
        STOCHASTIC
    }

    /**
     * Creates a utilization model based on the specified pattern
     * @param pattern The utilization pattern to use
     * @return Configured UtilizationModel
     */
    public static UtilizationModel create(UtilizationPattern pattern) {
        switch (pattern) {
            case CONSTANT:
                return createConstantModel();
            case RANDOM:
                return createRandomModel();
            case SINUSOIDAL:
                return createSinusoidalModel();
            case SPIKING:
                return createSpikingModel();
            case STOCHASTIC:
                return new UtilizationModelStochastic();
            default:
                return new UtilizationModelFull();
        }
    }

    /**
     * Creates a constant utilization model
     * @param utilization Fixed utilization percentage (0-1)
     */
    public static UtilizationModel createConstantModel(double utilization) {
        return new UtilizationModel() {
            @Override
            public double getUtilization(double time) {
                return Math.min(1.0, Math.max(0.0, utilization));
            }
        };
    }

    /**
     * Creates a constant utilization model with 100% utilization
     */
    public static UtilizationModel createConstantModel() {
        return new UtilizationModelFull();
    }

    /**
     * Creates a random utilization model
     * @param minUtilization Minimum utilization (0-1)
     * @param maxUtilization Maximum utilization (0-1)
     */
    public static UtilizationModel createRandomModel(double minUtilization, double maxUtilization) {
        return new UtilizationModel() {
            @Override
            public double getUtilization(double time) {
                return minUtilization + (maxUtilization - minUtilization) * random.nextDouble();
            }
        };
    }

    /**
     * Creates a random utilization model with default range (0.3-0.8)
     */
    public static UtilizationModel createRandomModel() {
        return createRandomModel(0.3, 0.8);
    }

    /**
     * Creates a sinusoidal utilization model that varies over time
     * @param baseUtilization Base utilization level (0-1)
     * @param amplitude Variation amplitude (0-1)
     * @param period Cycle period in seconds
     */
    public static UtilizationModel createSinusoidalModel(double baseUtilization, double amplitude, double period) {
        return new UtilizationModel() {
            @Override
            public double getUtilization(double time) {
                return Math.min(1.0, Math.max(0.0, 
                    baseUtilization + amplitude * Math.sin(2 * Math.PI * time / period)));
            }
        };
    }

    /**
     * Creates a sinusoidal model with default parameters
     */
    public static UtilizationModel createSinusoidalModel() {
        return createSinusoidalModel(0.5, 0.3, 300);
    }

    /**
     * Creates a spiking utilization model with random spikes
     * @param baseUtilization Base utilization (0-1)
     * @param spikeProbability Probability of spike occurring at any time
     * @param spikeIntensity How intense spikes are (0-1)
     */
    public static UtilizationModel createSpikingModel(double baseUtilization, double spikeProbability, double spikeIntensity) {
        return new UtilizationModel() {
            @Override
            public double getUtilization(double time) {
                if (random.nextDouble() < spikeProbability) {
                    return Math.min(1.0, baseUtilization + spikeIntensity * random.nextDouble());
                }
                return baseUtilization;
            }
        };
    }

    /**
     * Creates a spiking model with default parameters
     */
    public static UtilizationModel createSpikingModel() {
        return createSpikingModel(0.4, 0.05, 0.5);
    }
}