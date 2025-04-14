document.addEventListener('DOMContentLoaded', function() {
    // DOM elements
    const refreshBtn = document.getElementById('refresh-btn');
    const resultsTable = document.getElementById('results-table');
    const lastUpdated = document.getElementById('last-updated');
    const searchInput = document.getElementById('search-input');
    const filterSelect = document.getElementById('filter-select');
    const totalCloudlets = document.getElementById('total-cloudlets');
    const activeHosts = document.getElementById('active-hosts');
    const totalVms = document.getElementById('total-vms');
    const totalEnergy = document.getElementById('total-energy');
    
    // Chart objects
    let executionTimeChart, energyConsumptionChart, cpuUtilizationChart, vmAllocationChart;
    
    // Chart configuration and themes
    const chartConfig = {
        executionTime: {
            type: 'bar',
            gradient: {
                from: 'rgba(59, 130, 246, 0.8)',
                to: 'rgba(30, 58, 138, 0.8)'
            },
            border: 'rgba(30, 58, 138, 1)'
        },
        energyConsumption: {
            type: 'bar',
            gradient: {
                from: 'rgba(239, 68, 68, 0.8)',
                to: 'rgba(185, 28, 28, 0.8)'
            },
            border: 'rgba(185, 28, 28, 1)'
        },
        cpuUtilization: {
            type: 'line',
            gradient: {
                from: 'rgba(16, 185, 129, 0.8)',
                to: 'rgba(6, 95, 70, 0.8)'
            },
            border: 'rgba(6, 95, 70, 1)'
        },
        vmAllocation: {
            type: 'doughnut',
            colors: [
                'rgba(59, 130, 246, 0.8)',
                'rgba(16, 185, 129, 0.8)',
                'rgba(239, 68, 68, 0.8)',
                'rgba(245, 158, 11, 0.8)',
                'rgba(139, 92, 246, 0.8)',
                'rgba(236, 72, 153, 0.8)'
            ]
        }
    };
    
    // Application data storage
    let appData = {
        cloudlets: [],
        hosts: [],
        vms: []
    };
    
    // Initial load
    loadResults();
    
    // Event listeners
    refreshBtn.addEventListener('click', function() {
        refreshBtn.innerHTML = '<i class="fas fa-sync-alt fa-spin"></i> Refreshing...';
        loadResults().then(() => {
            refreshBtn.innerHTML = '<i class="fas fa-sync-alt"></i> Refresh Data';
        });
    });
    
    searchInput.addEventListener('input', function() {
        filterTableData();
    });
    
    filterSelect.addEventListener('change', function() {
        filterTableData();
    });
    
    // Add animation to card action buttons
    document.querySelectorAll('.card-action-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const card = this.closest('.card');
            card.classList.toggle('expanded');
            
            const icon = this.querySelector('i');
            if (icon.classList.contains('fa-expand')) {
                icon.classList.remove('fa-expand');
                icon.classList.add('fa-compress');
            } else {
                icon.classList.remove('fa-compress');
                icon.classList.add('fa-expand');
            }
        });
    });
    
    function loadResults() {
        return fetch('../results/summary.csv')
            .then(response => response.text())
            .then(data => {
                const results = parseCSV(data);
                processData(results);
                displayResults();
                updateCharts();
                updateSummaryStats();
                updateTimestamp();
            })
            .catch(error => {
                console.error('Error loading results:', error);
                resultsTable.innerHTML = `
                    <div class="error">
                        <i class="fas fa-exclamation-circle"></i>
                        <span>Error loading results. Make sure summary.csv exists.</span>
                    </div>`;
            });
    }
    
    function parseCSV(csvData) {
        const lines = csvData.trim().split('\n');
        const headers = lines[0].split(',').map(h => h.trim());
        const results = [];
        
        for (let i = 1; i < lines.length; i++) {
            if (lines[i].trim() === '') continue;
            
            const values = lines[i].split(',');
            const entry = {};
            
            headers.forEach((header, index) => {
                entry[header] = values[index] ? values[index].trim() : '';
            });
            
            results.push(entry);
        }
        
        return { headers, data: results };
    }
    
    function processData(results) {
        // Reset data
        appData = {
            cloudlets: [],
            hosts: [],
            vms: []
        };
        
        // Sort and organize data
        results.data.forEach(row => {
            const type = row.Type;
            const metric = row.Metric;
            const id = parseInt(row.ID);
            const value = row.Value;
            
            if (type === 'Cloudlet') {
                if (!appData.cloudlets[id]) {
                    appData.cloudlets[id] = { id };
                }
                appData.cloudlets[id][metric] = value;
            } else if (type === 'Host') {
                if (!appData.hosts[id]) {
                    appData.hosts[id] = { id };
                }
                appData.hosts[id][metric] = value;
            } else if (type === 'VM') {
                if (!appData.vms[id]) {
                    appData.vms[id] = { id };
                }
                appData.vms[id][metric] = value;
            }
        });
        
        // Clean up arrays (remove null entries)
        appData.cloudlets = appData.cloudlets.filter(Boolean);
        appData.hosts = appData.hosts.filter(Boolean);
        appData.vms = appData.vms.filter(Boolean);
    }
    
    function updateSummaryStats() {
        totalCloudlets.textContent = appData.cloudlets.length;
        
        const activeHostCount = appData.hosts.filter(host => 
            parseFloat(host.CPUUtilization) > 0 || parseInt(host.VMsCount) > 0
        ).length;
        activeHosts.textContent = activeHostCount + '/' + appData.hosts.length;
        
        totalVms.textContent = appData.vms.length;
        
        const totalEnergyValue = appData.hosts.reduce((sum, host) => 
            sum + parseFloat(host.EnergyConsumed || 0), 0
        );
        totalEnergy.textContent = totalEnergyValue.toFixed(2) + ' Wh';
    }
    
    function displayResults() {
        if (appData.cloudlets.length === 0 && appData.hosts.length === 0 && appData.vms.length === 0) {
            resultsTable.innerHTML = `
                <div class="no-data">
                    <i class="fas fa-database"></i>
                    <span>No results found in summary.csv</span>
                </div>`;
            return;
        }
        
        // Create tables for each data type
        let html = '<div class="tabs-container">';
        html += `<div class="tabs">
                    <button class="tab-btn active" data-tab="cloudlets-table">Cloudlets</button>
                    <button class="tab-btn" data-tab="hosts-table">Hosts</button>
                    <button class="tab-btn" data-tab="vms-table">Virtual Machines</button>
                </div>`;
        
        // Cloudlets Table
        html += `<div id="cloudlets-table" class="tab-content active">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Start Time</th>
                                <th>Finish Time</th>
                                <th>Execution Time</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>`;
        
        appData.cloudlets.forEach(cloudlet => {
            html += `<tr>
                        <td>${cloudlet.id}</td>
                        <td>${parseFloat(cloudlet.StartTime).toFixed(2)}</td>
                        <td>${parseFloat(cloudlet.FinishTime).toFixed(2)}</td>
                        <td>${parseFloat(cloudlet.ExecutionTime).toFixed(2)}</td>
                        <td>
                            <span class="status-badge ${cloudlet.Status === 'Success' ? 'success' : 'failed'}">
                                ${cloudlet.Status}
                            </span>
                        </td>
                    </tr>`;
        });
        
        html += `</tbody></table></div>`;
        
        // Hosts Table
        html += `<div id="hosts-table" class="tab-content">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>CPU Utilization</th>
                                <th>Energy Consumed</th>
                                <th>VMs Count</th>
                            </tr>
                        </thead>
                        <tbody>`;
        
        appData.hosts.forEach(host => {
            html += `<tr>
                        <td>${host.id}</td>
                        <td>${(parseFloat(host.CPUUtilization) * 100).toFixed(2)}%</td>
                        <td>${parseFloat(host.EnergyConsumed).toFixed(2)} Wh</td>
                        <td>${host.VMsCount}</td>
                    </tr>`;
        });
        
        html += `</tbody></table></div>`;
        
        // VMs Table
        html += `<div id="vms-table" class="tab-content">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Host</th>
                            </tr>
                        </thead>
                        <tbody>`;
        
        appData.vms.forEach(vm => {
            html += `<tr>
                        <td>${vm.id}</td>
                        <td>${vm.Host === '-1' ? 'Unallocated' : 'Host ' + vm.Host}</td>
                    </tr>`;
                });
        
                html += `</tbody></table></div>`;
                
                html += '</div>'; // Close tabs-container
                
                resultsTable.innerHTML = html;
                
                // Add tab switching functionality
                document.querySelectorAll('.tab-btn').forEach(button => {
                    button.addEventListener('click', function() {
                        // Deactivate all tabs
                        document.querySelectorAll('.tab-btn').forEach(btn => {
                            btn.classList.remove('active');
                        });
                        document.querySelectorAll('.tab-content').forEach(content => {
                            content.classList.remove('active');
                        });
                        
                        // Activate the clicked tab
                        this.classList.add('active');
                        document.getElementById(this.getAttribute('data-tab')).classList.add('active');
                    });
                });
            }
            
            function filterTableData() {
                const searchTerm = searchInput.value.toLowerCase();
                const filterType = filterSelect.value;
                
                // Apply filters to table rows
                document.querySelectorAll('.tab-content table tbody tr').forEach(row => {
                    const rowText = row.textContent.toLowerCase();
                    const rowParent = row.closest('.tab-content');
                    let rowType = '';
                    
                    if (rowParent.id === 'cloudlets-table') rowType = 'Cloudlet';
                    else if (rowParent.id === 'hosts-table') rowType = 'Host';
                    else if (rowParent.id === 'vms-table') rowType = 'VM';
                    
                    const matchesSearch = rowText.includes(searchTerm);
                    const matchesFilter = filterType === 'all' || rowType === filterType;
                    
                    row.style.display = matchesSearch && matchesFilter ? '' : 'none';
                });
            }
            
            function updateCharts() {
                createExecutionTimeChart();
                createEnergyConsumptionChart();
                createCPUUtilizationChart();
                createVMAllocationChart();
            }
            
            function createExecutionTimeChart() {
                const ctx = document.getElementById('executionTimeChart').getContext('2d');
                
                // Create gradient for bar chart
                const gradient = ctx.createLinearGradient(0, 0, 0, 400);
                gradient.addColorStop(0, chartConfig.executionTime.gradient.from);
                gradient.addColorStop(1, chartConfig.executionTime.gradient.to);
                
                // Sort cloudlets by ID for consistency
                const sortedCloudlets = [...appData.cloudlets].sort((a, b) => a.id - b.id);
                
                if (executionTimeChart) {
                    executionTimeChart.destroy();
                }
                
                executionTimeChart = new Chart(ctx, {
                    type: chartConfig.executionTime.type,
                    data: {
                        labels: sortedCloudlets.map(item => `Cloudlet ${item.id}`),
                        datasets: [{
                            label: 'Execution Time (seconds)',
                            data: sortedCloudlets.map(item => parseFloat(item.ExecutionTime)),
                            backgroundColor: gradient,
                            borderColor: chartConfig.executionTime.border,
                            borderWidth: 1,
                            borderRadius: 4,
                            barThickness: 20,
                            maxBarThickness: 30
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                backgroundColor: 'rgba(0, 0, 0, 0.7)',
                                padding: 10,
                                titleFont: {
                                    size: 14,
                                    weight: 'bold'
                                },
                                bodyFont: {
                                    size: 13
                                },
                                callbacks: {
                                    label: function(context) {
                                        return `Execution time: ${context.parsed.y.toFixed(2)} seconds`;
                                    }
                                }
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: {
                                    color: 'rgba(0, 0, 0, 0.05)',
                                    drawBorder: false
                                },
                                ticks: {
                                    font: {
                                        size: 12
                                    }
                                },
                                title: {
                                    display: true,
                                    text: 'Time (seconds)',
                                    font: {
                                        size: 13,
                                        weight: 'bold'
                                    },
                                    padding: 10
                                }
                            },
                            x: {
                                grid: {
                                    display: false
                                },
                                ticks: {
                                    font: {
                                        size: 12
                                    }
                                }
                            }
                        }
                    }
                });
            }
            
            function createEnergyConsumptionChart() {
                const ctx = document.getElementById('energyConsumptionChart').getContext('2d');
                
                // Create gradient for bar chart
                const gradient = ctx.createLinearGradient(0, 0, 0, 400);
                gradient.addColorStop(0, chartConfig.energyConsumption.gradient.from);
                gradient.addColorStop(1, chartConfig.energyConsumption.gradient.to);
                
                // Sort hosts by ID for consistency
                const sortedHosts = [...appData.hosts].sort((a, b) => a.id - b.id);
                
                if (energyConsumptionChart) {
                    energyConsumptionChart.destroy();
                }
                
                energyConsumptionChart = new Chart(ctx, {
                    type: chartConfig.energyConsumption.type,
                    data: {
                        labels: sortedHosts.map(item => `Host ${item.id}`),
                        datasets: [{
                            label: 'Energy Consumed (Wh)',
                            data: sortedHosts.map(item => parseFloat(item.EnergyConsumed)),
                            backgroundColor: gradient,
                            borderColor: chartConfig.energyConsumption.border,
                            borderWidth: 1,
                            borderRadius: 4,
                            barThickness: 20,
                            maxBarThickness: 30
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                backgroundColor: 'rgba(0, 0, 0, 0.7)',
                                padding: 10,
                                titleFont: {
                                    size: 14,
                                    weight: 'bold'
                                },
                                bodyFont: {
                                    size: 13
                                },
                                callbacks: {
                                    label: function(context) {
                                        return `Energy: ${context.parsed.y.toFixed(2)} Wh`;
                                    }
                                }
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: {
                                    color: 'rgba(0, 0, 0, 0.05)',
                                    drawBorder: false
                                },
                                ticks: {
                                    font: {
                                        size: 12
                                    }
                                },
                                title: {
                                    display: true,
                                    text: 'Energy (Wh)',
                                    font: {
                                        size: 13,
                                        weight: 'bold'
                                    },
                                    padding: 10
                                }
                            },
                            x: {
                                grid: {
                                    display: false
                                },
                                ticks: {
                                    font: {
                                        size: 12
                                    }
                                }
                            }
                        }
                    }
                });
            }
            
            function createCPUUtilizationChart() {
                const ctx = document.getElementById('cpuUtilizationChart').getContext('2d');
                
                // Sort hosts by ID for consistency
                const sortedHosts = [...appData.hosts].sort((a, b) => a.id - b.id);
                
                // Create gradient for line area
                const gradient = ctx.createLinearGradient(0, 0, 0, 400);
                gradient.addColorStop(0, 'rgba(16, 185, 129, 0.3)');
                gradient.addColorStop(1, 'rgba(16, 185, 129, 0.0)');
                
                if (cpuUtilizationChart) {
                    cpuUtilizationChart.destroy();
                }
                
                cpuUtilizationChart = new Chart(ctx, {
                    type: chartConfig.cpuUtilization.type,
                    data: {
                        labels: sortedHosts.map(item => `Host ${item.id}`),
                        datasets: [{
                            label: 'CPU Utilization (%)',
                            data: sortedHosts.map(item => parseFloat(item.CPUUtilization) * 100),
                            backgroundColor: gradient,
                            borderColor: chartConfig.cpuUtilization.border,
                            borderWidth: 2,
                            pointBackgroundColor: chartConfig.cpuUtilization.border,
                            pointBorderColor: '#fff',
                            pointBorderWidth: 2,
                            pointRadius: 5,
                            pointHoverRadius: 7,
                            tension: 0.3,
                            fill: true
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                backgroundColor: 'rgba(0, 0, 0, 0.7)',
                                padding: 10,
                                titleFont: {
                                    size: 14,
                                    weight: 'bold'
                                },
                                bodyFont: {
                                    size: 13
                                },
                                callbacks: {
                                    label: function(context) {
                                        return `Utilization: ${context.parsed.y.toFixed(2)}%`;
                                    }
                                }
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                suggestedMax: 100,
                                grid: {
                                    color: 'rgba(0, 0, 0, 0.05)',
                                    drawBorder: false
                                },
                                ticks: {
                                    font: {
                                        size: 12
                                    },
                                    callback: function(value) {
                                        return value + '%';
                                    }
                                },
                                title: {
                                    display: true,
                                    text: 'Utilization (%)',
                                    font: {
                                        size: 13,
                                        weight: 'bold'
                                    },
                                    padding: 10
                                }
                            },
                            x: {
                                grid: {
                                    display: false
                                },
                                ticks: {
                                    font: {
                                        size: 12
                                    }
                                }
                            }
                        }
                    }
                });
            }
            
            function createVMAllocationChart() {
                const ctx = document.getElementById('vmAllocationChart').getContext('2d');
                
                // Count VMs per host
                const hostCounts = {};
                appData.vms.forEach(vm => {
                    const hostId = vm.Host;
                    if (!hostCounts[hostId]) {
                        hostCounts[hostId] = 0;
                    }
                    hostCounts[hostId]++;
                });
                
                // Prepare data for chart
                const labels = Object.keys(hostCounts).sort((a, b) => {
                    if (a === '-1') return 1;
                    if (b === '-1') return -1;
                    return parseInt(a) - parseInt(b);
                }).map(hostId => hostId === '-1' ? 'Unallocated' : `Host ${hostId}`);
                
                const values = labels.map(label => {
                    const hostId = label === 'Unallocated' ? '-1' : label.replace('Host ', '');
                    return hostCounts[hostId] || 0;
                });
                
                if (vmAllocationChart) {
                    vmAllocationChart.destroy();
                }
                
                vmAllocationChart = new Chart(ctx, {
                    type: chartConfig.vmAllocation.type,
                    data: {
                        labels: labels,
                        datasets: [{
                            data: values,
                            backgroundColor: chartConfig.vmAllocation.colors,
                            borderColor: '#ffffff',
                            borderWidth: 2,
                            hoverOffset: 15
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        cutout: '60%',
                        plugins: {
                            legend: {
                                position: 'right',
                                labels: {
                                    font: {
                                        size: 12
                                    },
                                    padding: 15,
                                    boxWidth: 15,
                                    boxHeight: 15
                                }
                            },
                            tooltip: {
                                backgroundColor: 'rgba(0, 0, 0, 0.7)',
                                padding: 10,
                                titleFont: {
                                    size: 14,
                                    weight: 'bold'
                                },
                                bodyFont: {
                                    size: 13
                                },
                                callbacks: {
                                    label: function(context) {
                                        const value = context.parsed;
                                        const total = context.dataset.data.reduce((acc, data) => acc + data, 0);
                                        const percentage = ((value / total) * 100).toFixed(1);
                                        return `VMs: ${value} (${percentage}%)`;
                                    }
                                }
                            }
                        }
                    }
                });
            }
            
            function updateTimestamp() {
                const now = new Date();
                const options = { 
                    year: 'numeric', 
                    month: 'short', 
                    day: 'numeric', 
                    hour: '2-digit', 
                    minute: '2-digit', 
                    second: '2-digit' 
                };
                lastUpdated.innerHTML = `<i class="fas fa-clock"></i> Last updated: ${now.toLocaleDateString('en-US', options)}`;
            }
            
            // Add CSS for additional styling
            const style = document.createElement('style');
            style.textContent = `
                .tabs-container {
                    width: 100%;
                }
                
                .tabs {
                    display: flex;
                    margin-bottom: 15px;
                    border-bottom: 1px solid var(--border-color);
                }
                
                .tab-btn {
                    padding: 10px 20px;
                    background: none;
                    border: none;
                    border-bottom: 3px solid transparent;
                    font-size: 14px;
                    font-weight: 500;
                    color: var(--text-light);
                    cursor: pointer;
                    transition: all 0.3s ease;
                }
                
                .tab-btn:hover {
                    color: var(--primary-color);
                }
                
                .tab-btn.active {
                    color: var(--primary-color);
                    border-bottom-color: var(--primary-color);
                }
                
                .tab-content {
                    display: none;
                }
                
                .tab-content.active {
                    display: block;
                }
                
                .status-badge {
                    display: inline-block;
                    padding: 4px 8px;
                    border-radius: 12px;
                    font-size: 12px;
                    font-weight: 500;
                }
                
                .status-badge.success {
                    background-color: rgba(16, 185, 129, 0.1);
                    color: var(--secondary-color);
                }
                
                .status-badge.failed {
                    background-color: rgba(239, 68, 68, 0.1);
                    color: var(--danger-color);
                }
                
                .card.expanded {
                    grid-column: 1 / -1;
                    transition: all 0.3s ease;
                }
                
                .card.expanded .chart-container {
                    height: 400px;
                }
                
                /* Animation for refresh */
                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                
                .fade-in {
                    animation: fadeIn 0.5s ease-in-out;
                }
            `;
            document.head.appendChild(style);
        });