package org.cloudbus.cloudsim;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MetricsCollector {
    private static final DecimalFormat dft = new DecimalFormat("###.##");

    public static void collectAndPrintAllMetrics(List<Cloudlet> cloudlets, 
                                              VmAllocationPolicyEnergyAware allocationPolicy) {
        printCloudletDetails(cloudlets);
        printHostMetrics(allocationPolicy);
        printEnergyMetrics(allocationPolicy);
        printConsolidatedMetrics(cloudlets, allocationPolicy);
    }

    public static void printHostMetrics(VmAllocationPolicyEnergyAware policy) {
        System.out.println("\n========== HOST METRICS ==========");
        System.out.printf("%-8s %-15s %-15s %-12s %-12s%n", 
                "HostID", "Avg Util(%)", "Peak Util(%)", "Max VMs", "Energy(J)");

        policy.getHostList().forEach(host -> {
            int hostId = host.getId();
            System.out.printf("%-8d %-15.2f %-15.2f %-12d %-12.2f%n",
                hostId,
                policy.getAverageUtilization(hostId) * 100,
                getPeakUtilization(policy, hostId) * 100,
                policy.getMaxVmsPerHost(hostId),
                policy.getTotalEnergy(hostId));
        });
    }

    private static double getPeakUtilization(VmAllocationPolicyEnergyAware policy, int hostId) {
        return policy.getUtilizationHistory(hostId).stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
    }

    public static void printCloudletDetails(List<Cloudlet> cloudlets) {
        System.out.println("\n========== CLOUDLET EXECUTION DETAILS ==========");
        System.out.printf("%-12s %-10s %-16s %-8s %-8s %-12s %-12s%n",
                "Cloudlet ID", "STATUS", "Data center ID", "VM ID", 
                "Time", "Start Time", "Finish Time");
        
        cloudlets.forEach(cloudlet -> {
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                System.out.printf("%-12d %-10s %-16d %-8d %-8s %-12s %-12s%n",
                        cloudlet.getCloudletId(),
                        "SUCCESS",
                        cloudlet.getResourceId(),
                        cloudlet.getVmId(),
                        dft.format(cloudlet.getActualCPUTime()),
                        dft.format(cloudlet.getExecStartTime()),
                        dft.format(cloudlet.getFinishTime()));
            }
        });
    }

    private static void printEnergyMetrics(VmAllocationPolicyEnergyAware allocationPolicy) {
        System.out.println("\n========== ENERGY CONSUMPTION METRICS ==========");
        System.out.printf("%-8s %-15s%n", "HostID", "Energy(kWh)");

        allocationPolicy.getHostList().forEach(host -> {
            int hostId = host.getId();
            System.out.printf("%-8d %-15.6f%n",
                hostId, allocationPolicy.getTotalEnergy(hostId) / 3600000);
        });

        System.out.println("\nTotal Energy Consumption: " + 
            dft.format(calculateTotalEnergy(allocationPolicy) / 3600000) + " kWh");
    }

    private static void printConsolidatedMetrics(List<Cloudlet> cloudlets,
                                              VmAllocationPolicyEnergyAware allocationPolicy) {
        System.out.println("\n========== SUMMARY METRICS ==========");
        
        long totalCloudlets = cloudlets.size();
        long successfulCloudlets = cloudlets.stream()
            .filter(c -> c.getCloudletStatus() == Cloudlet.SUCCESS)
            .count();
        
        int totalHosts = allocationPolicy.getHostList().size();
        int activeHosts = allocationPolicy.getHostsUtilizedCount();
        double totalEnergy = calculateTotalEnergy(allocationPolicy);

        System.out.printf("%-30s: %d/%d (%.2f%%)%n", 
            "Cloudlet Completion", successfulCloudlets, totalCloudlets,
            (successfulCloudlets * 100.0) / totalCloudlets);
            
        System.out.printf("%-30s: %d/%d%n",
            "Hosts Utilized", activeHosts, totalHosts);
            
        System.out.printf("%-30s: %.6f kWh%n",
            "Total Energy Consumption", totalEnergy / 3600000);
            
        System.out.printf("%-30s: %.6f kWh%n",
            "Energy per Cloudlet", totalEnergy / (3600000 * successfulCloudlets));
    }

    private static double calculateTotalEnergy(VmAllocationPolicyEnergyAware policy) {
        return policy.getHostList().stream()
            .mapToDouble(host -> policy.getTotalEnergy(host.getId()))
            .sum();
    }

    public static Map<String, Object> exportAllMetrics(List<Cloudlet> cloudlets,
                                                    VmAllocationPolicyEnergyAware policy) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        metrics.put("total_cloudlets", cloudlets.size());
        metrics.put("successful_cloudlets", cloudlets.stream()
            .filter(c -> c.getCloudletStatus() == Cloudlet.SUCCESS)
            .count());
        
        metrics.put("hosts_utilized", policy.getHostsUtilizedCount());
        metrics.put("total_energy_kwh", calculateTotalEnergy(policy) / 3600000);
        
        return metrics;
    }
}