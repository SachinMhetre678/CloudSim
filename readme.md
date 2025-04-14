# ⚡ Energy-Aware Cloud Resource Allocation using CloudSim

A simulation project built on **CloudSim 3.0.3**, focused on minimizing energy consumption and optimizing resource usage through intelligent VM allocation and real-time metrics monitoring.

---

## 📘 Overview

This project simulates **energy-aware cloud resource allocation** in a datacenter environment. It demonstrates the effectiveness of dynamic VM placement strategies and live performance tracking in reducing energy wastage and improving load balancing.

Key aspects include:

- Dynamic VM allocation based on current host utilization  
- Real-time monitoring of resource metrics  
- Energy-efficient modeling and evaluation  
- Full automation via CI/CD pipelines  
- Data visualization and reporting

---

## ✨ Features

- ✅ **Energy-aware VM Allocation Policy**
- 📊 **Host-level real-time metrics (CPU, energy, VM load)**
- ⚙️ **Load balancing across heterogeneous hosts**
- 🧪 **Simulation execution via CI/CD (GitHub Actions)**
- 📈 **Automatic results generation in CSV/HTML**

---

## 🖥 Requirements

- **Java JDK** 11+
- **CloudSim 3.0.3** (included in `/lib`)
- **GitHub Actions** for CI/CD 
---

## 🚀 Quick Start

### ✅ Build and Run Locally

```bash
# Clone the repository
git clone https://github.com/your-repo/cloudsim-energy-aware.git
cd cloudsim-energy-aware

# Compile and run the simulation
javac -Xlint:unchecked -cp "lib/cloudsim-3.0.3.jar" -d bin src/org/cloudbus/cloudsim/*.java
java -cp "bin;lib/cloudsim-3.0.3.jar" org.cloudbus.cloudsim.Main
```

### ✅ Run via CI/CD (GitHub Actions)

Just push to the main branch or open a PR. GitHub Actions will:
- Build the project
- Run the simulation
- Archive the results

---

> 🔗 CI Status: ![CI/CD Pipeline](https://github.com/SachinMhetre678/CloudSim/actions/workflows/maven.yml/badge.svg)

---

## 🔧 Understanding the Simulation

1. **Hosts** and **VMs** are initialized with varying MIPS capacities.
2. VMs are dynamically allocated to the least-utilized suitable host.
3. **Cloudlets (tasks)** are assigned and executed on VMs.
4. The system records CPU utilization, energy consumption, and performance per host.

### 🔍 Metrics Collected

| Metric              | Description                              |
|---------------------|------------------------------------------|
| **CPU Utilization** | % used = Σ VM MIPS / Host MIPS           |
| **Energy Used**     | Based on host power model and utilization|
| **VM Distribution** | How evenly VMs are allocated across hosts|
| **Execution Time**  | Per-cloudlet processing duration         |

---

## 📈 Sample Simulation Output

```
====== CLOUDSIM SIMULATION RESULTS ======

Cloudlet ID | Status   | VM ID | Execution Time
----------------------------------------
0           | SUCCESS  | 0     | 8.10s
1           | SUCCESS  | 1     | 16.10s
2           | SUCCESS  | 2     | 24.10s

Host ID | CPU Util | Energy Used
-------------------------------
0       | 100.00%  | 250.00 J
1       | 66.67%   | 183.33 J
2       | 50.00%   | 162.50 J
```

---

## 🔄 CI/CD Pipeline

```yaml
# .github/workflows/maven.yml
name: CloudSim CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
    - name: Build and Test
      run: |
        mvn clean package
        java -cp target/classes:lib/cloudsim-3.0.3.jar org.cloudbus.cloudsim.Main
    - name: Archive results
      uses: actions/upload-artifact@v2
      with:
        name: simulation-results
        path: results/
```
---

## 💡 Innovation Highlights

### 1. Energy-Aware VM Allocation

```java
public boolean allocateHostForVm(Vm vm) {
    Host selectedHost = getHostList().stream()
        .filter(host -> host.isSuitableForVm(vm))
        .min(Comparator.comparingDouble(this::calculateCurrentUtilization))
        .orElse(null);
    // Allocation logic...
}
```

### 2. Power-Aware Energy Calculation

```java
double energy = (idlePower + (maxPower - idlePower) * utilization) 
              * (simulationTime / 3600);
```

---

## 🗂 Project Structure

```
├── src/
│   └── main/java/org/cloudbus/cloudsim/
│       ├── Main.java
│       └── VmAllocationPolicyEnergyAware.java
├── lib/
│   └── cloudsim-3.0.3.jar
├── results/
│   ├── summary.csv
│   └── index.html
├── .github/workflows/
│   └── cloudsim-ci.yml
```

---

## 📌 Customization

To tweak parameters like:
- Number of VMs or Hosts
- Power model constants
- Task (cloudlet) specifications

Edit:
```bash
src/main/java/org/cloudbus/cloudsim/Main.java
```

---

## 📍 Real-World Applications

- 🔋 **Green Cloud Initiatives**
- 📉 **Datacenter Cost Optimization**
- 🔄 **Adaptive Resource Scheduling**
- 🧠 **Autonomous Infrastructure Management**

---

## 🤝 Contributing

We welcome your contributions!  
Fork the repo → Create a branch → Make changes → Submit a Pull Request 🚀

---

## 📬 Contact

For issues, suggestions, or questions — please open an [issue](https://github.com/your-repo/cloudsim-energy-aware/issues).

---
