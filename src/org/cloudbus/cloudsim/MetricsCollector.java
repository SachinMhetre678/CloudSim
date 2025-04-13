package org.cloudbus.cloudsim;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class MetricsCollector {
    private static final DecimalFormat dft = new DecimalFormat("###.##");
    private static final String INDENT = "    ";
    
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

    public static void collectAndPrintComprehensiveMetrics(List<Cloudlet> cloudlets) {
        if (cloudlets == null || cloudlets.isEmpty()) {
            System.out.println("No cloudlets to analyze");
            return;
        }

        // Filter only successful cloudlets
        List<Cloudlet> successfulCloudlets = cloudlets.stream()
                .filter(c -> c.getCloudletStatus() == Cloudlet.SUCCESS)
                .collect(Collectors.toList());

        if (successfulCloudlets.isEmpty()) {
            System.out.println("No successfully executed cloudlets found");
            return;
        }

        // Basic statistics
        double totalExecutionTime = successfulCloudlets.stream()
                .mapToDouble(Cloudlet::getActualCPUTime)
                .sum();
        double avgExecutionTime = totalExecutionTime / successfulCloudlets.size();
        
        double makespan = successfulCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0);

        // VM-specific metrics
        Map<Integer, List<Cloudlet>> cloudletsByVm = successfulCloudlets.stream()
                .collect(Collectors.groupingBy(Cloudlet::getVmId));

        // Print comprehensive report
        System.out.println("\n========== COMPREHENSIVE SIMULATION METRICS ==========");
        System.out.printf("%-30s: %d%n", "Total Cloudlets", cloudlets.size());
        System.out.printf("%-30s: %d%n", "Successfully Executed", successfulCloudlets.size());
        System.out.printf("%-30s: %s%n", "Total Execution Time", dft.format(totalExecutionTime));
        System.out.printf("%-30s: %s%n", "Average Execution Time", dft.format(avgExecutionTime));
        System.out.printf("%-30s: %s%n", "Makespan (Total Time)", dft.format(makespan));
        
        System.out.println("\n========== VM-WISE DISTRIBUTION ==========");
        System.out.printf("%-8s %-12s %-12s %-12s%n", 
                "VM ID", "Cloudlets", "Total Time", "Avg Time");
        
        cloudletsByVm.forEach((vmId, vmCloudlets) -> {
            double vmTotalTime = vmCloudlets.stream()
                    .mapToDouble(Cloudlet::getActualCPUTime)
                    .sum();
            double vmAvgTime = vmTotalTime / vmCloudlets.size();
            
            System.out.printf("%-8d %-12d %-12s %-12s%n",
                    vmId, vmCloudlets.size(), 
                    dft.format(vmTotalTime), dft.format(vmAvgTime));
        });

        // Print detailed cloudlet info if needed
        System.out.println("\nWould you like to see detailed cloudlet information? (y/n)");
        // In a real implementation, you'd read user input here
    }

    public static Map<String, Object> collectMetrics(List<Cloudlet> cloudlets) {
        Map<String, Object> metrics = new HashMap<>();
        
        List<Cloudlet> successfulCloudlets = cloudlets.stream()
                .filter(c -> c.getCloudletStatus() == Cloudlet.SUCCESS)
                .collect(Collectors.toList());

        // Basic metrics
        metrics.put("totalCloudlets", cloudlets.size());
        metrics.put("successfulCloudlets", successfulCloudlets.size());
        
        // Timing metrics
        double totalTime = successfulCloudlets.stream()
                .mapToDouble(Cloudlet::getActualCPUTime)
                .sum();
        metrics.put("totalExecutionTime", totalTime);
        
        metrics.put("avgExecutionTime", totalTime / successfulCloudlets.size());
        
        metrics.put("makespan", successfulCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0));

        // VM distribution
        Map<Integer, Long> vmDistribution = successfulCloudlets.stream()
                .collect(Collectors.groupingBy(
                        Cloudlet::getVmId, 
                        Collectors.counting()));
        metrics.put("vmDistribution", vmDistribution);

        return metrics;
    }
}