package org.cloudbus.cloudsim;

import java.util.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;

public class VmAllocationPolicyEnergyAware extends VmAllocationPolicy {
    private final Map<Integer, Double> hostEnergyConsumption = new HashMap<>();
    private final Map<Integer, List<Double>> hostUtilizationHistory = new HashMap<>();
    private final Map<Integer, Double> lastUpdateTime = new HashMap<>();
    private final Map<Integer, Integer> maxVmsPerHost = new HashMap<>();
    private final Map<Integer, Boolean> hostEverUtilized = new HashMap<>();
    private double simulationStartTime = 0.0;

    public VmAllocationPolicyEnergyAware(List<? extends Host> list) {
        super(list);
        initializeHostTracking(list);
    }

    private void initializeHostTracking(List<? extends Host> hosts) {
        for (Host host : hosts) {
            int hostId = host.getId();
            hostEnergyConsumption.put(hostId, 0.0);
            hostUtilizationHistory.put(hostId, new ArrayList<>());
            lastUpdateTime.put(hostId, simulationStartTime);
            hostEverUtilized.put(hostId, false);
            maxVmsPerHost.put(hostId, 0);
        }
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        boolean result = host.vmCreate(vm);
        if (result) {
            updateHostMetrics(host);
            maxVmsPerHost.merge(host.getId(), 1, Integer::sum);
            hostEverUtilized.put(host.getId(), true);
        }
        return result;
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        Host selectedHost = selectHostForVm(vm);
        return selectedHost != null && allocateHostForVm(vm, selectedHost);
    }

    private Host selectHostForVm(Vm vm) {
        return getHostList().stream()
            .filter(host -> host.isSuitableForVm(vm))
            .min(Comparator.comparingDouble(this::calculateCurrentUtilization))
            .orElse(null);
    }

    private void updateHostMetrics(Host host) {
        double currentTime = CloudSim.clock();
        double utilization = calculateCurrentUtilization(host);
        
        recordHostUtilization(host, utilization);
        
        double timeDelta = currentTime - lastUpdateTime.get(host.getId());
        updateHostEnergy(host, utilization, timeDelta);
        
        lastUpdateTime.put(host.getId(), currentTime);
    }

    public void recordHostUtilization(Host host, double utilization) {
        hostUtilizationHistory.get(host.getId()).add(utilization);
    }
    
    public void updateHostEnergy(Host host, double utilization, double timeDelta) {
        double energy = calculateEnergyConsumption(host, utilization, timeDelta);
        hostEnergyConsumption.merge(host.getId(), energy, Double::sum);
    }

    private double calculateEnergyConsumption(Host host, double utilization, double timeDelta) {
        if (host instanceof PowerHost) {
            PowerHost powerHost = (PowerHost)host;
            return powerHost.getPower() * utilization * timeDelta;
        }
        return (100 + 100 * utilization) * timeDelta;
    }

    private void logAllocation(Vm vm, Host host) {
        double utilization = calculateCurrentUtilization(host) * 100;
        Log.printLine(String.format(
            "[Energy-Aware] VM %d allocated to Host %d (Utilization: %.2f%%)",
            vm.getId(), host.getId(), utilization));
    }

    protected double calculateCurrentUtilization(Host host) {
        double usedMips = host.getVmList().stream()
            .mapToDouble(vm -> vm.getCurrentRequestedTotalMips())
            .sum();
        double utilization = host.getTotalMips() > 0 ? usedMips / host.getTotalMips() : 0;
        return Math.min(utilization, 1.0);
    }

    public double getPeakUtilization(int hostId) {
        List<Double> utilizations = getUtilizationHistory(hostId);
        if (utilizations == null || utilizations.isEmpty()) {
            return 0.0;
        }
        return utilizations.stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
    }

    public double getAverageUtilization(int hostId) {
        List<Double> utilizations = hostUtilizationHistory.get(hostId);
        return utilizations == null || utilizations.isEmpty() ? 0.0 :
            utilizations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double getTotalEnergy(int hostId) {
        return hostEnergyConsumption.getOrDefault(hostId, 0.0);
    }

    public List<Double> getUtilizationHistory(int hostId) {
        return hostUtilizationHistory.getOrDefault(hostId, Collections.emptyList());
    }

    public int getActiveHostCount() {
        return (int) hostEverUtilized.values().stream().filter(Boolean::booleanValue).count();
    }

    public int getHostsUtilizedCount() {
        return (int) hostEverUtilized.values().stream().filter(Boolean::booleanValue).count();
    }

    public int getMaxVmsPerHost(int hostId) {
        return maxVmsPerHost.getOrDefault(hostId, 0);
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        Host host = vm.getHost();
        if (host != null) {
            updateHostMetrics(host);
            host.vmDestroy(vm);
        }
    }

    @Override
    public Host getHost(Vm vm) {
        return vm.getHost();
    }

    @Override
    public Host getHost(int vmId, int userId) {
        return getHostList().stream()
            .filter(h -> h.getVm(vmId, userId) != null)
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        return Collections.emptyList();
    }
}