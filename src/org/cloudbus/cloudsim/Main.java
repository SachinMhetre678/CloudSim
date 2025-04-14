package org.cloudbus.cloudsim;

import org.cloudbus.cloudsim.core.CloudSim;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;

public class Main {
    private static List<Host> hostList;
    private static List<Vm> vmList;
    private static List<Cloudlet> cloudletList;
    private static double lastUpdateTime = 0.0;

    public static void main(String[] args) {
        try {
            Log.printLine("Starting Energy-Aware CloudSim Simulation...");

            // Initialize CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUsers, calendar, traceFlag);

            // Create Datacenter with energy-aware policy
            Datacenter datacenter = createDatacenter();
            DatacenterBroker broker = createBroker();

            // Create VMs and Cloudlets
            vmList = createVms(broker.getId());
            cloudletList = createCloudlets(broker.getId());
            
            // Submit VMs and Cloudlets to broker
            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            // Start simulation
            CloudSim.startSimulation();

            // Update metrics during simulation
            updateMetrics(datacenter);

            // Collect and print results
            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            printResults(finishedCloudlets, vmList, hostList, datacenter);

            Log.printLine("Energy-Aware Simulation finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation failed!");
        }
    }

    private static Datacenter createDatacenter() throws Exception {
        hostList = new ArrayList<>();
        
        // Create 3 hosts with different capacities
        int[] mips = {1000, 1500, 2000};
        for (int i = 0; i < 3; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(mips[i])));

            hostList.add(new Host(
                i,
                new RamProvisionerSimple(2048), // 2GB RAM
                new BwProvisionerSimple(10000), // 10Gbps bandwidth
                1000000, // Storage
                peList,
                new VmSchedulerTimeShared(peList) // Time-shared scheduling
            ));
        }

        // Datacenter characteristics
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.1, 0.1);

        // Energy-aware VM allocation policy
        VmAllocationPolicy allocationPolicy = new VmAllocationPolicyEnergyAware(hostList);

        return new Datacenter(
            "GreenDatacenter",
            characteristics,
            allocationPolicy,
            new LinkedList<Storage>(),
            0);
    }

    private static DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("Broker");
    }

    private static List<Vm> createVms(int brokerId) {
        List<Vm> vms = new ArrayList<>();
        
        // Create 3 VMs with different requirements
        for (int i = 0; i < 3; i++) {
            vms.add(new Vm(
                i, brokerId, 
                1000, // MIPS
                1,    // Number of CPUs
                1024, // RAM (MB)
                1000,  // Bandwidth
                10000, // Storage (MB)
                "Xen", // VMM
                new CloudletSchedulerTimeShared() // Time-shared cloudlet scheduler
            ));
        }
        return vms;
    }

    private static List<Cloudlet> createCloudlets(int brokerId) {
        UtilizationModel utilizationModel = new UtilizationModelFull();
        List<Cloudlet> cloudlets = new ArrayList<>();
        
        // Create 6 cloudlets with different lengths
        int[] lengths = {5000, 10000, 15000, 20000, 25000, 30000};
        for (int i = 0; i < lengths.length; i++) {
            Cloudlet cloudlet = new Cloudlet(
                i, lengths[i], 
                1,    // PEs required
                300,  // Input size
                300,  // Output size
                utilizationModel, 
                utilizationModel, 
                utilizationModel
            );
            cloudlet.setUserId(brokerId);
            cloudlets.add(cloudlet);
        }
        return cloudlets;
    }

    private static void updateMetrics(Datacenter datacenter) {
        double currentTime = CloudSim.clock();
        VmAllocationPolicyEnergyAware policy = 
            (VmAllocationPolicyEnergyAware) datacenter.getVmAllocationPolicy();
        
        for (Host host : hostList) {
            double utilization = policy.calculateCurrentUtilization(host);
            
            // Changed from trackHostUtilization to recordHostUtilization
            policy.recordHostUtilization(host, utilization);
            
            // Changed from updateEnergyConsumption to updateHostEnergy
            double timeDiff = currentTime - lastUpdateTime;
            policy.updateHostEnergy(host, utilization, timeDiff);
        }
        lastUpdateTime = currentTime;
    }

    private static void printResults(List<Cloudlet> cloudlets, List<Vm> vms, 
                                   List<Host> hosts, Datacenter datacenter) {
                                    try {
                                        // Create results directory
                                        java.io.File dir = new java.io.File("results");
                                        if (!dir.exists()) {
                                            dir.mkdir();
                                        }
                                
                                        PrintWriter writer = new PrintWriter(new java.io.File("results/summary.csv"));
            writer.println("Type,ID,Metric,Value");
    
            DecimalFormat df = new DecimalFormat("0.00");
    
            // ========== TERMINAL OUTPUT ==========
            Log.printLine("\n====== CLOUDSIM SIMULATION RESULTS ======\n");
    
            // Cloudlet Execution Table
            Log.printLine("Cloudlet ID | Status   | Datacenter | VM ID | Start Time | Finish Time");
            Log.printLine("---------------------------------------------------------------");
    
            for (Cloudlet c : cloudlets) {
                Log.printLine(String.format("%-10d | %-8s | %-10d | %-5d | %-10s | %-10s",
                    c.getCloudletId(),
                    Cloudlet.getStatusString(c.getStatus()),
                    c.getResourceId(),
                    c.getVmId(),
                    df.format(c.getExecStartTime()),
                    df.format(c.getFinishTime())));
            }
    
            // Host Utilization Table
            VmAllocationPolicyEnergyAware policy = 
                (VmAllocationPolicyEnergyAware) datacenter.getVmAllocationPolicy();
            
            Log.printLine("\nHost ID | CPU Util (%) | Energy (J) | VMs Count");
            Log.printLine("----------------------------------------------");
    
            for (Host h : hosts) {
                Log.printLine(String.format("%-7d | %-13s | %-10s | %-9d",
                    h.getId(),
                    df.format(policy.getAverageUtilization(h.getId()) * 100),
                    df.format(policy.getTotalEnergy(h.getId())),
                    h.getVmList().size()));
            }
    
            // Summary Statistics
            Log.printLine("\nMetric                    | Value");
            Log.printLine("----------------------------------");
            Log.printLine(String.format("%-25s | %-6d", "Total Cloudlets", cloudlets.size()));
            Log.printLine(String.format("%-25s | %-6d", "Successful Cloudlets", 
                cloudlets.stream().filter(c -> c.getStatus() == Cloudlet.SUCCESS).count()));
            Log.printLine(String.format("%-25s | %-6d", "VMs Created", vms.size()));
            Log.printLine(String.format("%-25s | %-6d", "Hosts Utilized", 
                hosts.stream().filter(h -> h.getVmList().size() > 0).count()));
    
            // Save to CSV
            for (Cloudlet c : cloudlets) {
                writer.printf("Cloudlet,%d,StartTime,%s%n", c.getCloudletId(), df.format(c.getExecStartTime()));
                writer.printf("Cloudlet,%d,FinishTime,%s%n", c.getCloudletId(), df.format(c.getFinishTime()));
                writer.printf("Cloudlet,%d,ExecutionTime,%s%n", c.getCloudletId(), df.format(c.getActualCPUTime()));
                writer.printf("Cloudlet,%d,Status,%s%n", c.getCloudletId(), Cloudlet.getStatusString(c.getStatus()));
            }
    
            for (Vm vm : vms) {
                writer.printf("VM,%d,Host,%d%n", vm.getId(), vm.getHost() != null ? vm.getHost().getId() : -1);
            }
    
            for (Host h : hosts) {
                writer.printf("Host,%d,CPUUtilization,%s%n", h.getId(), 
                    df.format(policy.getAverageUtilization(h.getId()) * 100));
                writer.printf("Host,%d,EnergyConsumed,%s%n", h.getId(), 
                    df.format(policy.getTotalEnergy(h.getId())));
                writer.printf("Host,%d,VMsCount,%d%n", h.getId(), h.getVmList().size());
            }
    
            writer.close();
            Log.printLine("\nDetailed metrics saved to results/summary.csv");
    
        } catch (Exception e) {
            Log.printLine("Error writing results: " + e.getMessage());
        }
    }
}