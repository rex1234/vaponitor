# Server Monitor

A Kotlin-based server monitoring application built with Ktor that tracks application uptime and system resource usage.

## Features

- **Application Monitoring**: Monitor HTTP/HTTPS endpoints and command-based applications
- **Resource Monitoring**: Track CPU usage, RAM usage, disk space, and system temperatures
- **Discord Integration**: Send alerts and notifications to Discord channels
- **Web Dashboard**: View monitoring status and historical data through a web interface
- **SQLite Database**: Store historical monitoring data
- **Configurable Intervals**: Customize monitoring frequencies and data retention

## Prerequisites

- Java 17 or higher
- Gradle 7.0 or higher
- For Linux resource monitors: `mpstat` (part of sysstat package)

## Project Setup

### 1. Clone and Navigate

```powershell
git clone <repository-url>
cd servermonitor
```

### 2. Create Configuration Files

#### Environment Configuration (.env)

Create a `.env` file in the project root with the following variables:

```env
APP_NAME=ServerMonitor
DISCORD_TOKEN=your_discord_bot_token_here
DISCORD_CHANNEL_ID=your_discord_channel_id_here
```

#### Monitor Configuration (monitorconfig.yaml)

Copy the example configuration and customize it:

```powershell
cp monitorconfig.example.yaml monitorconfig.yaml
```

### 3. Build and Run

#### Development Mode

```powershell
./gradlew run
```

#### Build JAR with ShadowJar

```powershell
./gradlew shadowJar
```

#### Run the JAR file

```powershell
java -jar build/libs/ktor-app.jar
```

The application will start on the default port (usually 8080). Access the web dashboard at `http://localhost:8080`

## Configuration Guide

### Monitor Configuration (monitorconfig.yaml)

The main configuration file controls all monitoring behavior:

```yaml
# Monitoring intervals (in seconds)
app_monitor_interval_s: 60          # How often to check applications
resource_monitor_interval_s: 15     # How often to check system resources

# Data retention
history_duration_m: 1660            # How long to keep data in memory (minutes)
db_purge_days: 30                   # Global purge rule for application data & default for resources (unless overridden)

# Resource monitors (list each resource; optional per-item db_purge_days overrides or disables purging)
resources:
   items:
      - id: "RVolume"                 # Disk usage monitoring (no db_purge_days -> not purged)
      - id: "RRam"                    # RAM usage monitoring
        db_purge_days: 14             # Purge RAM metrics older than 14 days
      - id: "RCpu"                    # CPU usage monitoring
        db_purge_days: 7              # Purge CPU metrics older than 7 days
      - id: "RTemp"                   # CPU temperature monitoring
        db_purge_days: 90             # Purge temp metrics older than 90 days
      - id: "RDht"                    # DHT22 sensor monitoring
        db_purge_days: 60             # Purge DHT metrics older than 60 days

# Application monitors
apps:
  - name: "My Website"
    description: "Main website monitoring"
    url: "example.com"              # HTTP endpoint
    https: "secure.example.com"     # HTTPS endpoint (optional)
    basicAuthUsername: "user"       # Basic auth (optional)
    basicAuthPassword: "pass"       # Basic auth (optional)

  - name: "Local Service"
    description: "Local application"
    command: "systemctl is-active my-service"  # Command-based monitoring
```

### Configuration Fields Explained

#### Timing Configuration

- `app_monitor_interval_s`: Frequency for checking application status (recommended: 30-120 seconds)
- `resource_monitor_interval_s`: Frequency for system resource checks (recommended: 10-30 seconds)
- `history_duration_m`: In-memory data retention for dashboard graphs
- `db_purge_days` (global): Default purge window (in days) for application monitor data. Resource monitors inherit this
  unless they specify their own `db_purge_days`. If a resource item omits `db_purge_days`, its data is retained
  indefinitely (no purging) to allow long-term historical tracking.

#### Per-Resource Retention (`db_purge_days` overrides)

Each resource entry under `resources.items` may include its own `db_purge_days` to customize (or disable) purging:

- Present with a number: Purge measurements for that resource older than the specified days.
- Omitted: Do not purge that resource's historical data (kept indefinitely).
- Global `db_purge_days`: Always applies to application monitoring data and any resource without an override unless that
  resource omits the field (in which case it is exempt from purging).

#### Resource Monitors

Available resource monitors:

| ID        | Name                  | Description                              | Requirements                  | Supports override |
|-----------|-----------------------|------------------------------------------|-------------------------------|-------------------|
| `RCpu`    | CPU Usage             | Current CPU utilization percentage       | Linux with `mpstat`           | Yes               |
| `RRam`    | RAM Usage             | Memory usage in MB                       | Cross-platform (OSHI library) | Yes               |
| `RVolume` | Disk Usage            | Disk space usage for all mounted volumes | Cross-platform                | Yes               |
| `RTemp`   | CPU Temperature       | CPU temperature in Celsius               | Linux/Raspberry Pi            | Yes               |
| `RDht`    | Environmental Sensors | DHT22 temperature/humidity sensor        | Custom Python script setup    | Yes               |

#### Application Monitors

Applications can be monitored in two ways:

**HTTP/HTTPS Monitoring:**

- `url`: HTTP endpoint (format: `domain.com:port` or `domain.com`)
- `https`: HTTPS endpoint (format: `domain.com:port` or `domain.com`)
- `basicAuthUsername`/`basicAuthPassword`: For basic authentication

**Command-based Monitoring:**

- `command`: Shell command that returns exit code 0 for success
- Examples: `systemctl is-active service-name`, `pgrep process-name`, custom scripts

### Discord Integration

To enable Discord notifications:

1. Create a Discord bot in the Discord Developer Portal
2. Add the bot to your server with appropriate permissions
3. Get the bot token and channel ID
4. Configure the `.env` file with these values

The bot will send notifications when:

- Applications go down or come back up
- Resource usage exceeds thresholds (configurable)
- System errors occur

## Resource Monitor Details

### CPU Usage Monitor (`RCpu`)

- **Platform**: Linux only
- **Dependency**: `mpstat` command (install with `sudo apt install sysstat`)
- **Measurement**: CPU utilization percentage averaged across all cores

### RAM Usage Monitor (`RRam`)

- **Platform**: Cross-platform
- **Dependency**: Built-in OSHI library
- **Measurement**: Used memory in MB out of total available

### Disk Usage Monitor (`RVolume`)

- **Platform**: Cross-platform
- **Dependency**: Built-in Java NIO
- **Measurement**: Used space for each mounted volume over 1GB

### CPU Temperature Monitor (`RTemp`)

- **Platform**: Linux/Raspberry Pi
- **Dependency**: `/sys/class/thermal/thermal_zone0/temp` file
- **Measurement**: CPU temperature in Celsius

### DHT22 Environmental Monitor (`RDht`)

- **Platform**: Custom setup (typically Raspberry Pi)
- **Dependency**: Custom Python script at specified path
- **Measurement**: Temperature and humidity from DHT22 sensor

## Database

The application uses SQLite for data persistence:

- Database file: `measurements.db`
- Automatic table creation on first run
- Configurable data purging (global + per-resource overrides)
- Stores both application status and resource measurements with timestamps

## Web Dashboard

Access the monitoring dashboard at `http://localhost:8080`:

- Real-time status indicators for all monitored applications
- Historical graphs for resource usage
- Responsive design using Tabler UI framework

## Troubleshooting

### Common Issues

1. **Resource monitors not working on Windows**
    - Some monitors (CPU, Temperature) are Linux-specific
    - Only RAM and Disk monitors work cross-platform

2. **Discord notifications not working**
    - Verify `.env` file exists with correct token and channel ID
    - Check bot permissions in Discord server

3. **Application monitoring failing**
    - Verify network connectivity to monitored endpoints
    - Check firewall settings for outbound connections
    - Validate URL formats in configuration

4. **Build failures**
    - Ensure Java 17+ is installed
    - Run `./gradlew clean` before rebuilding

### Logs

Application logs are available in the console output. For production deployments, consider redirecting output to log
files:

```powershell
java -jar build/libs/ktor-app.jar > servermonitor.log 2>&1
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Copyright (c) 2025 Ján Mjartan (mjartan.jan@gmail.com). All rights reserved.
