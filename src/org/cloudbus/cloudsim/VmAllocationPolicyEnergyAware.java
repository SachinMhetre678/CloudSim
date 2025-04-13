package org.cloudbus.cloudsim;

import java.util.List;
import java.util.Comparator;
import java.util.Map;

public class VmAllocationPolicyEnergyAware extends VmAllocationPolicy {

    public VmAllocationPolicyEnergyAware(List<? extends Host> list) {
        super(list);
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        // Select host with lowest current CPU utilization (energy-aware)
        Host selectedHost = getHostList().stream()
            .filter(host -> host.isSuitableForVm(vm))
            .min(Comparator.comparingDouble((Host host) -> calculateCurrentUtilization(host)))
            .orElse(null);

        if (selectedHost != null && selectedHost.vmCreate(vm)) {
            Log.printLine(String.format(
                "[Energy-Aware] VM %d allocated to Host %d (Utilization: %.2f%%)",
                vm.getId(), 
                selectedHost.getId(),
                calculateCurrentUtilization(selectedHost) * 100));
            return true;
        }
        return false;
    }

    private double calculateCurrentUtilization(Host host) {
        double usedMips = host.getVmList().stream()
            .mapToDouble(vm -> vm.getCurrentRequestedTotalMips())
            .sum();
        return host.getTotalMips() > 0 ? usedMips / host.getTotalMips() : 0;
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        return host.vmCreate(vm);
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        vm.getHost().vmDestroy(vm);
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
        // No optimization - can be implemented for load balancing
        return null;
    }
}