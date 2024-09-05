package life.vaporized.servermonitor.app.monitor

import life.vaporized.servermonitor.app.monitor.model.MonitorStatus

interface IResourceMonitor : IMonitor<MonitorStatus.ResourceStatus>