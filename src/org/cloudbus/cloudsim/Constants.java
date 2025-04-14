package org.cloudbus.cloudsim;

public class Constants {
    // Host configurations
    public static final int NO_OF_HOSTS = 5;
    public static final String ARCHITECTURE = "x86";
    public static final String OS = "Linux";
    public static final String VMM = "Xen";
    public static final double TIME_ZONE = 10.0;
    public static final double COST = 3.0;
    public static final double COST_PER_MEM = 0.05;
    public static final double COST_PER_STORAGE = 0.001;
    public static final double COST_PER_BW = 0.0;
    public static final double SCHEDULING_INTERVAL = 0.1;

    // VM configurations
    public static final int NO_OF_VMS = 5;
    public static final int[] VM_MIPS = {1000, 1500, 2000, 2500, 3000};
    public static final int VM_PES = 1;
    public static final int VM_RAM = 512;
    public static final int VM_BW = 1000;
    public static final int VM_SIZE = 10000;

    // Cloudlet configurations
    public static final int NO_OF_CLOUDLETS = 10;
    public static final int CLOUDLET_PES = 1;
    public static final int CLOUDLET_LENGTH = 400000;
    public static final int CLOUDLET_FILE_SIZE = 300;
    public static final int CLOUDLET_OUTPUT_SIZE = 300;
}