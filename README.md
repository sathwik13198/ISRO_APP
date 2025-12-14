# ISRO App

A modern Android application for real-time device tracking, GPS monitoring, and MQTT-based communication. Built with Jetpack Compose and designed for offline-first operation with online synchronization capabilities.

## 🚀 Features

### Core Functionality
- **Real-time GPS Tracking**: Continuous location monitoring with high accuracy
- **MQTT Communication**: Real-time device discovery and messaging via MQTT protocol
- **Offline Maps**: Full offline map support using OSMDroid with MBTiles
- **Multi-Device Support**: Track and communicate with multiple devices on a network
- **Chat System**: Real-time messaging between devices
- **Responsive UI**: Adaptive layouts for mobile, tablet, and desktop screens

### Key Highlights
- ✅ **Offline-First**: Maps and GPS work without internet connection
- ✅ **Non-Blocking UI**: MQTT connection runs in background, never freezes the app
- ✅ **Error Resilient**: Graceful handling of MQTT connection failures
- ✅ **Modern Architecture**: Built with Jetpack Compose, MVVM pattern, and Kotlin Coroutines

## 📋 Requirements

- **Android SDK**: Minimum SDK 24 (Android 7.0), Target SDK 34 (Android 14)
- **Java**: Version 11
- **Kotlin**: Latest stable version
- **Android Studio**: Hedgehog or later recommended

## 🛠️ Setup Instructions

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ISRO
```

### 2. Open in Android Studio
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned directory
4. Wait for Gradle sync to complete

### 3. Configure MQTT Broker
Edit `app/src/main/java/com/example/isro_app/mqtt/MqttManager.kt`:

```kotlin
private val brokerUri = "tcp://YOUR_BROKER_IP:1883"  // Change this
private val username = "your_username"              // Change this
private val password = "your_password"              // Change this
```

### 4. Build and Run
```bash
./gradlew assembleDebug
```

Or use Android Studio's Run button (▶️)

## 📦 Dependencies

### Core Libraries
- **Jetpack Compose**: Modern declarative UI framework
- **Material 3**: Latest Material Design components
- **Lifecycle ViewModel**: State management
- **Kotlin Coroutines**: Asynchronous programming

### MQTT
- **Eclipse Paho MQTT Client** (v1.2.5): Standard MQTT client (no Android service)
- **AndroidX LocalBroadcastManager**: Compatibility support

### Location Services
- **Google Play Services Location**: High-accuracy GPS tracking

### Maps
- **OSMDroid** (v6.1.18): Offline map rendering
- **OSMDroid MapsForge**: Vector map support
- **OSMDroid WMS**: Web Map Service support

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/example/isro_app/
├── MainActivity.kt          # Main UI and navigation
├── LocationViewModel.kt      # GPS location management
├── MyApplication.kt          # Application initialization
├── OfflineMapComposable.kt  # Map rendering component
├── location/
│   └── LocationState.kt    # Location data model
└── mqtt/
    └── MqttManager.kt       # MQTT communication handler
```

### Key Components

#### MqttManager
- Handles all MQTT communication
- Manages device discovery via GPS topic
- Processes incoming chat messages
- Publishes GPS coordinates
- Connection state management

#### LocationViewModel
- Tracks device GPS location
- Provides location updates via StateFlow
- Handles location permissions

#### MainActivity
- Main UI composable
- Responsive layouts (Mobile/Tablet/Desktop)
- Device list and chat interface
- Map integration

## 🔧 Configuration

### MQTT Broker Settings
Default configuration in `MqttManager.kt`:
- **Broker URI**: `tcp://192.168.10.100:1883`
- **Client ID**: Auto-generated with timestamp
- **Topics**:
  - GPS: `gps/location`
  - Inbox: `{clientId}/inbox`

### Permissions
Required permissions (already configured in `AndroidManifest.xml`):
- `INTERNET`: MQTT communication
- `ACCESS_NETWORK_STATE`: Network status checking
- `ACCESS_FINE_LOCATION`: GPS tracking
- `ACCESS_COARSE_LOCATION`: Approximate location
- `WRITE_EXTERNAL_STORAGE`: Map tile caching (Android 10 and below)

### OSMDroid Configuration
OSMDroid is initialized in:
- `MyApplication.kt`: Base path configuration
- `MainActivity.onCreate()`: User agent setup

## 📱 Usage

### Initial Setup
1. **Grant Permissions**: App will request location permissions on first launch
2. **MQTT Connection**: App automatically connects to MQTT broker
3. **GPS Tracking**: Location tracking starts automatically

### Features Usage

#### Device Discovery
- Devices are automatically discovered via MQTT GPS topic
- Device list updates in real-time
- Status indicators show online/offline state

#### GPS Tracking
- Current location is displayed on the map
- GPS coordinates are published to MQTT every update
- Map centers on your current location when GPS fix is available

#### Chat
- Select a device from the list
- Type a message and send
- Messages are delivered via MQTT

#### Maps
- Map always displays (even without MQTT)
- Uses current GPS location by default
- Shows device locations when available
- Fully offline-capable

## 🐛 Troubleshooting

### MQTT Connection Issues

**Problem**: "MQTT server unreachable" toast appears
- **Solution**: Check broker IP address and network connectivity
- Verify broker is running and accessible
- Check firewall settings

**Problem**: App freezes during connection
- **Solution**: Already fixed! Connection runs in background coroutine
- If still occurs, check for network blocking

### GPS Issues

**Problem**: "Waiting for GPS…" message persists
- **Solution**: 
  - Ensure location permissions are granted
  - Move to open area for better GPS signal
  - Check device location settings

**Problem**: Location not updating
- **Solution**: 
  - Restart the app
  - Check LocationViewModel is running
  - Verify location services are enabled

### Map Issues

**Problem**: Black screen or map not loading
- **Solution**: 
  - Ensure OSMDroid is initialized (check MyApplication)
  - Verify storage permissions for map tiles
  - Check if MBTiles file is present (if using offline tiles)

### Build Issues

**Problem**: Gradle sync fails
- **Solution**: 
  - Check internet connection
  - Invalidate caches: File → Invalidate Caches / Restart
  - Update Gradle wrapper if needed

**Problem**: "NoClassDefFoundError: LocalBroadcastManager"
- **Solution**: Already fixed! Using standard MqttClient instead of MqttAndroidClient

## 🔒 Security Notes

- MQTT credentials are hardcoded in `MqttManager.kt` (change for production)
- Consider using secure MQTT (TLS/SSL) for production deployments
- Location data is transmitted via MQTT - ensure broker security

## 🚧 Known Limitations

- MQTT broker IP is hardcoded (should be configurable)
- No message encryption (plain text MQTT)
- Map tiles require manual setup for offline use
- Device list doesn't persist across app restarts

## 🔮 Future Enhancements

- [ ] Configurable MQTT broker settings (UI)
- [ ] Message encryption
- [ ] Persistent device list
- [ ] Map tile downloader
- [ ] Route tracking and history
- [ ] Push notifications for messages
- [ ] Multi-broker support


## 🙏 Acknowledgments

- **OSMDroid**: Offline mapping library
- **Eclipse Paho**: MQTT client library
- **Jetpack Compose**: Modern Android UI toolkit

---


