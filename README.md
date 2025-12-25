# ISRO App

A modern Android application for real-time device tracking, GPS monitoring, and MQTT-based communication. Built with Jetpack Compose and designed for offline-first operation with online synchronization capabilities.

## 🚀 Features

### Core Functionality
- **Real-time GPS Tracking**: Continuous location monitoring with high accuracy
- **MQTT Communication**: Real-time device discovery and messaging via MQTT protocol
- **Voice Calling**: IAX-based voice communication between devices via Asterisk server
- **LAN-Based Map Tiles**: Custom tile server for offline/private network map rendering
- **Interactive Maps**: Full-screen map mode with pan, zoom, and follow controls
- **Multi-Device Support**: Track and communicate with multiple devices on a network
- **Chat System**: Real-time messaging between devices
- **File Attachments**: Send and receive files (images, PDFs, etc.) via chat
- **Responsive UI**: Adaptive layouts for mobile, tablet, and desktop screens

### Map Features ✨
- **LAN Tile Server**: Fetch map tiles from local network server (no internet required)
- **Custom Marker Icons**: Distinct markers for self (red) and other devices (blue)
- **Follow Mode**: Auto-center map on GPS location with manual toggle
- **Pan Mode**: Free drag navigation without auto-centering
- **Fullscreen Map**: Immersive map view with floating controls
- **Zoom Controls**: Manual zoom in/out buttons
- **GPS Fallback**: Shows GPS marker when MQTT is disconnected

### Key Highlights
- ✅ **Offline-First**: Maps and GPS work without internet connection
- ✅ **Private Network**: LAN tile server for secure, local map tiles
- ✅ **Voice Communication**: IAX protocol for peer-to-peer voice calls via Asterisk
- ✅ **Non-Blocking UI**: MQTT connection runs in background, never freezes the app
- ✅ **Error Resilient**: Graceful handling of MQTT connection failures
- ✅ **Modern Architecture**: Built with Jetpack Compose, MVVM pattern, and Kotlin Coroutines

## 📋 Requirements

- **Android SDK**: Minimum SDK 24 (Android 7.0), Target SDK 34 (Android 14)
- **Java**: Version 11
- **Kotlin**: Latest stable version
- **Android Studio**: Hedgehog or later recommended
- **Python 3**: For running the tile server (if using LAN tiles)

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

### 4. Set Up Tile Server (For LAN Maps)

#### 4.1. Prepare Tile Directory Structure
Create a directory structure for your map tiles:

```
your_tiles_directory/
├── tile_server.py          # Tile server script (copy from below)
└── tiles/                  # Map tiles folder
    ├── 0/
    │   ├── 0/
    │   │   └── 0.png
    │   └── 1/
    │       └── 0.png
    ├── 1/
    │   └── ...
    └── ...
```

**Important**: The `tile_server.py` file must be placed **in the same directory** as the `tiles/` folder.

#### 4.2. Create Tile Server Script
Create `tile_server.py` in your tiles directory:

```python
from http.server import HTTPServer, SimpleHTTPRequestHandler
import os
from datetime import datetime

PORT = 8080
MIN_ZOOM = 0
MAX_ZOOM = 14

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
TILES_DIR = os.path.join(BASE_DIR, "tiles")


def log(message):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] {message}")


class TileHandler(SimpleHTTPRequestHandler):

    def do_GET(self):
        # Expected path: /tiles/z/x/y.png
        if not self.path.startswith("/tiles/"):
            log(f"INVALID PATH → {self.path}")
            self.send_error(404)
            return

        log(f"REQUEST → {self.path}")

        try:
            _, z, x, y_png = self.path.strip("/").split("/")
            y = y_png.replace(".png", "")
            z = int(z)
            x = int(x)
            y = int(y)
        except Exception:
            log("ERROR → INVALID TILE FORMAT")
            self.send_error(400, "Invalid tile format")
            return

        # Zoom validation
        if z < MIN_ZOOM or z > MAX_ZOOM:
            log(f"REJECTED → ZOOM OUT OF RANGE ({z}) ❌")
            self.send_error(404, "Zoom level not supported")
            return

        tile_path = os.path.join(TILES_DIR, str(z), str(x), f"{y}.png")

        log(f"CHECKING → {tile_path}")

        if os.path.isfile(tile_path):
            log("STATUS → TILE EXISTS ✅")

            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.end_headers()

            with open(tile_path, "rb") as f:
                self.wfile.write(f.read())
        else:
            log("STATUS → TILE NOT FOUND ❌")
            self.send_error(404, "Tile not found")


if __name__ == "__main__":
    os.chdir(BASE_DIR)

    log("======================================")
    log("TILE SERVER STARTED")
    log(f"PORT       → {PORT}")
    log(f"ZOOM RANGE → {MIN_ZOOM} to {MAX_ZOOM}")
    log(f"TILES DIR  → {TILES_DIR}")
    log("======================================")

    server = HTTPServer(("0.0.0.0", PORT), TileHandler)
    server.serve_forever()
```

#### 4.3. Start the Tile Server
```bash
cd your_tiles_directory
python3 tile_server.py
```

The server will start on port 8080 and serve tiles from the `tiles/` directory.

#### 4.4. Configure Tile Server IP in App
Edit `app/src/main/java/com/example/isro_app/OfflineMapComposable.kt`:

```kotlin
private val LanTileSource = XYTileSource(
    "LAN-TILES",
    0,          // min zoom
    14,         // max zoom
    256,        // tile size
    ".png",
    arrayOf("http://YOUR_SERVER_IP:8080/tiles/")  // Change this IP
)
```

Replace `YOUR_SERVER_IP` with the IP address of the machine running the tile server (e.g., `192.168.29.242`).

### 5. Set Up Attachment Server (For File Sharing)

The attachment server handles file uploads and downloads for the chat attachment feature.

#### 5.1. Create Attachment Server Script
Create `attachment_server.py` in any directory (can be same as tile server or separate):

```python
from http.server import HTTPServer, BaseHTTPRequestHandler
import os
import json
import uuid
import mimetypes

# ---------------- CONFIG ----------------

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOAD_DIR = os.path.join(BASE_DIR, "received_files")

PORT = 8090
SERVER_IP = "192.168.29.242"   # change only if IP changes

os.makedirs(UPLOAD_DIR, exist_ok=True)

# ---------------- HANDLER ----------------

class AttachmentHandler(BaseHTTPRequestHandler):

    # ---- Common headers (CORS) ----
    def _set_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")

    # ---- OPTIONS (CORS preflight) ----
    def do_OPTIONS(self):
        self.send_response(200)
        self._set_headers()
        self.end_headers()

    # ---- UPLOAD ----
    def do_POST(self):
        if self.path != "/upload":
            self.send_error(404)
            return

        try:
            content_length = int(self.headers.get("Content-Length", 0))
            filename = self.headers.get("X-Filename", "unknown_file")

            file_id = str(uuid.uuid4())
            safe_filename = filename.replace("/", "_").replace("\\", "_")
            save_path = os.path.join(UPLOAD_DIR, f"{file_id}_{safe_filename}")

            remaining = content_length
            with open(save_path, "wb") as f:
                while remaining > 0:
                    chunk = self.rfile.read(min(4096, remaining))
                    if not chunk:
                        break
                    f.write(chunk)
                    remaining -= len(chunk)

            response = {
                "file_id": file_id,
                "filename": safe_filename,
                "download_url": f"http://{SERVER_IP}:{PORT}/download/{file_id}"
            }

            self.send_response(200)
            self._set_headers()
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())

            print(f"[UPLOAD] {safe_filename} saved")

        except Exception as e:
            print("[ERROR] Upload failed:", e)
            self.send_error(500, "Upload failed")

    # ---- DOWNLOAD (✅ MIME FIX HERE) ----
    def do_GET(self):
        if not self.path.startswith("/download/"):
            self.send_error(404)
            return

        file_id = self.path.split("/")[-1]

        for file in os.listdir(UPLOAD_DIR):
            if file.startswith(file_id):
                file_path = os.path.join(UPLOAD_DIR, file)
                original_filename = file.split("_", 1)[1]

                # ✅ Detect correct MIME type
                mime_type, _ = mimetypes.guess_type(original_filename)
                if mime_type is None:
                    mime_type = "application/octet-stream"

                self.send_response(200)
                self._set_headers()
                self.send_header("Content-Type", mime_type)
                self.send_header(
                    "Content-Disposition",
                    f'attachment; filename="{original_filename}"'
                )
                self.send_header("Content-Length", str(os.path.getsize(file_path)))
                self.end_headers()

                with open(file_path, "rb") as f:
                    while True:
                        chunk = f.read(4096)
                        if not chunk:
                            break
                        self.wfile.write(chunk)

                print(f"[DOWNLOAD] {original_filename} ({mime_type})")
                return

        self.send_error(404, "File not found")

# ---------------- RUN SERVER ----------------

if __name__ == "__main__":
    print(f"[ATTACHMENT SERVER] Running on 0.0.0.0:{PORT}")
    HTTPServer(("0.0.0.0", PORT), AttachmentHandler).serve_forever()
```

#### 5.2. Configure Server IP
Edit the `SERVER_IP` variable in `attachment_server.py`:

```python
SERVER_IP = "192.168.29.242"   # Change this to your server's IP address
```

#### 5.3. Start the Attachment Server
```bash
python3 attachment_server.py
```

The server will:
- Start on port 8090
- Create a `received_files/` directory automatically
- Handle file uploads at `/upload` endpoint
- Serve file downloads at `/download/{file_id}` endpoint

#### 5.4. Configure Attachment Server IP in App
The attachment server IP is already configured in `app/src/main/java/com/example/isro_app/mqtt/MqttManager.kt`:

```kotlin
private val attachmentServer = "http://192.168.29.242:8090"
```

Update this IP address to match your attachment server's IP address.

### 6. Set Up Asterisk Server (For Voice Calling)

Voice calling requires an Asterisk server running on your network. See `setup.md` for complete installation and configuration instructions.

**Quick Setup**:
1. Install Asterisk on Ubuntu: `sudo apt install asterisk`
2. Configure IAX users in `/etc/asterisk/iax.conf`
3. Configure dialplan in `/etc/asterisk/extensions.conf`
4. Update Asterisk IP in `MainActivity.kt` (currently `192.168.29.242`)

**Important**: The Android app uses device IDs as IAX usernames. Ensure IAX users are configured in Asterisk matching your device IDs.

For detailed setup instructions, see `setup.md`.

### 7. Build and Run
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
├── OfflineMapComposable.kt  # Map rendering component (LAN tiles + controls)
├── location/
│   └── LocationState.kt    # Location data model
└── mqtt/
    ├── MqttManager.kt       # MQTT communication handler
    └── AttachmentDownloader.kt  # File download handler (DownloadManager)
```

### Key Components

#### MqttManager
- Handles all MQTT communication
- Manages device discovery via GPS topic
- Processes incoming chat messages and attachments
- Publishes GPS coordinates
- Handles file uploads via HTTP
- Sends attachment metadata via MQTT
- Connection state management

#### LocationViewModel
- Tracks device GPS location
- Provides location updates via StateFlow
- Handles location permissions

#### MainActivity
- Main UI composable
- Responsive layouts (Mobile/Tablet/Desktop)
- Device list and chat interface
- Attachment handling (upload/download)
- Map integration with fullscreen mode

#### OfflineMapComposable
- Renders map with LAN tile server
- Manages marker display (self vs others)
- Handles follow/pan mode toggles
- Provides zoom controls
- GPS fallback marker logic

## 🔧 Configuration

### MQTT Broker Settings
Default configuration in `MqttManager.kt`:
- **Broker URI**: `tcp://192.168.29.239:1883`
- **Client ID**: Auto-generated with timestamp
- **Topics**:
  - GPS: `gps/location`
  - Inbox: `{clientId}/inbox`

### Attachment Server Settings
Default configuration in `MqttManager.kt`:
- **Server URL**: `http://192.168.29.242:8090`
- **Port**: 8090
- **Upload Endpoint**: `/upload`
- **Download Endpoint**: `/download/{file_id}`

### Asterisk/IAX Settings
Default configuration in `MainActivity.kt`:
- **Asterisk IP**: `192.168.29.242` (update to match your Asterisk server)
- **IAX Port**: `4569` (standard IAX port)
- **Protocol**: IAX2 (Inter-Asterisk eXchange version 2)
- **Codec**: G.711 μ-law (ulaw) - 8kHz, 8-bit
- **No Authentication**: Simple prototype setup (no registration required)

### Tile Server Settings
Default configuration in `OfflineMapComposable.kt`:
- **Server URL**: `http://192.168.29.242:8080/tiles/`
- **Port**: 8080
- **Zoom Range**: 0 to 14
- **Tile Format**: PNG (256x256)

### Attachment Server Details

#### Directory Structure
```
attachment_server_directory/
├── attachment_server.py      # Server script
└── received_files/           # Auto-created directory for uploaded files
    └── {file_id}_{filename}  # Stored files
```

#### API Endpoints

**POST /upload**
- Accepts file uploads
- Headers:
  - `X-Filename`: Original filename
  - `Content-Length`: File size in bytes
- Returns JSON:
  ```json
  {
    "file_id": "uuid",
    "filename": "original_filename.ext",
    "download_url": "http://server:8090/download/uuid"
  }
  ```

**GET /download/{file_id}**
- Serves files for download
- Automatically detects MIME type
- Sets appropriate Content-Type and Content-Disposition headers

#### Server Requirements
- Python 3.x
- HTTP server capability (uses standard library)
- Network access from Android devices
- Disk space for uploaded files

### Permissions
Required permissions (already configured in `AndroidManifest.xml`):
- `INTERNET`: MQTT communication, tile fetching, and IAX voice calls
- `ACCESS_NETWORK_STATE`: Network status checking
- `ACCESS_FINE_LOCATION`: GPS tracking
- `ACCESS_COARSE_LOCATION`: Approximate location
- `RECORD_AUDIO`: Voice call audio capture (required for calling)
- `WRITE_EXTERNAL_STORAGE`: Map tile caching (Android 10 and below)

### OSMDroid Configuration
OSMDroid is initialized in:
- `MyApplication.kt`: Base path configuration
- `MainActivity.onCreate()`: User agent setup

## 📱 Usage

### Initial Setup
1. **Grant Permissions**: App will request location permissions on first launch
2. **Start Tile Server**: Ensure tile server is running on your network (if using LAN tiles)
3. **MQTT Connection**: App automatically connects to MQTT broker
4. **GPS Tracking**: Location tracking starts automatically

### Features Usage

#### Device Discovery
- Devices are automatically discovered via MQTT GPS topic
- Device list updates in real-time
- Status indicators show online/offline state

#### GPS Tracking
- Current location is displayed on the map with red marker (self)
- Other devices shown with blue markers
- GPS coordinates are published to MQTT every update
- Map centers on your current location when GPS fix is available

#### Map Controls
- **🎯 Follow Mode**: Tap to enable auto-center on GPS location
- **🖐️ Pan Mode**: Tap to disable auto-center and allow free dragging
- **➕/➖ Zoom**: Manual zoom in/out buttons
- **⛶ Fullscreen**: Tap fullscreen icon to view map in fullscreen mode
- **⬅ Back**: Exit fullscreen mode

#### Chat
- Select a device from the list
- Type a message and send
- Messages are delivered via MQTT

#### Voice Calling
- **Initiate Call**: Tap the call icon (📞) next to a device in the chat interface
- **Incoming Call**: When receiving a call, an alert dialog appears with Accept/Reject options
- **Call Interface**: Once call is accepted, a full-screen call interface appears showing:
  - Peer device ID
  - Call status ("Call in progress...")
  - End call button
- **Call Rejection**: If a call is rejected, a toast notification appears
- **Voice Communication**: Uses IAX protocol via Asterisk server for audio transmission
- **Audio**: Automatically routes to earpiece/speaker based on device settings

#### File Attachments
- Tap the attachment icon (📎) in chat input to select a file
- Files are uploaded to the attachment server via HTTP
- Attachment metadata is sent via MQTT
- Recipients can tap the attachment to download it
- Downloaded files are saved to `/Download/ISRO_ATTACHMENTS/` folder
- Files can be opened from Files app or Gallery

#### Maps
- Map fetches tiles from LAN server (configured IP)
- Works offline if tiles are cached
- Shows device locations when available
- Distinct markers: Red (self), Blue (others)

## 🗺️ Tile Server Details

### Directory Structure
```
tiles_directory/
├── tile_server.py          # Server script (MUST be here)
└── tiles/                  # Map tiles (standard TMS structure)
    ├── {zoom_level}/
    │   ├── {tile_x}/
    │   │   └── {tile_y}.png
    │   └── ...
    └── ...
```

### Tile Naming Convention
Tiles follow the **TMS (Tile Map Service)** format:
- Path: `/tiles/{z}/{x}/{y}.png`
- `z`: Zoom level (0-14)
- `x`: Tile X coordinate
- `y`: Tile Y coordinate

### Server Requirements
- Python 3.x
- HTTP server capability (uses standard library)
- Network access from Android devices
- Tiles directory with proper structure

### Network Configuration
- Server listens on `0.0.0.0:8080` (all interfaces)
- Ensure firewall allows port 8080
- Android devices must be on same network
- Update IP in `OfflineMapComposable.kt` to match server IP

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
  - Ensure tile server is running
  - Check tile server IP address in `OfflineMapComposable.kt`
  - Verify network connectivity between device and tile server
  - Check tile server logs for errors
  - Ensure tiles directory structure is correct

**Problem**: Tiles not displaying
- **Solution**:
  - Verify tile server is accessible from device (ping server IP)
  - Check tile server is serving on correct port (8080)
  - Ensure tile files exist in `tiles/{z}/{x}/{y}.png` format
  - Check tile server console for request logs

**Problem**: Map shows wrong location or no markers
- **Solution**:
  - Verify GPS permissions are granted
  - Check if MQTT is connected (for device markers)
  - Ensure device IDs match between MQTT and app

### Tile Server Issues

**Problem**: Server won't start
- **Solution**:
  - Check if port 8080 is already in use
  - Verify Python 3 is installed
  - Ensure `tile_server.py` is in correct directory (same as `tiles/` folder)

**Problem**: Server starts but no tiles served
- **Solution**:
  - Verify `tiles/` directory exists and contains subdirectories
  - Check tile file paths match expected format
  - Review server console logs for request details

### Attachment Server Issues

**Problem**: Attachment upload fails
- **Solution**:
  - Ensure attachment server is running on port 8090
  - Check server IP address in `MqttManager.kt` matches actual server IP
  - Verify network connectivity between device and server
  - Check server console logs for errors
  - Ensure firewall allows port 8090

**Problem**: Download doesn't start when tapping attachment
- **Solution**:
  - Verify attachment server is accessible
  - Check download URL is valid (HTTP/HTTPS)
  - Ensure device has storage permissions (for Android 10 and below)
  - Check DownloadManager is working (system service)

**Problem**: Files not appearing in Downloads folder
- **Solution**:
  - Files are saved to `/Download/ISRO_ATTACHMENTS/` (not root Downloads)
  - Check Files app → Downloads → ISRO_ATTACHMENTS folder
  - Verify download completed (check notification)
  - Ensure sufficient storage space available

### Build Issues

**Problem**: Gradle sync fails
- **Solution**: 
  - Check internet connection
  - Invalidate caches: File → Invalidate Caches / Restart
  - Update Gradle wrapper if needed

**Problem**: "NoClassDefFoundError: LocalBroadcastManager"
- **Solution**: Already fixed! Using standard MqttClient instead of MqttAndroidClient

### Voice Calling Issues

**Problem**: Call interface doesn't appear when call is accepted
- **Solution**: 
  - Verify audio permissions are granted (RECORD_AUDIO)
  - Check Asterisk server is running and accessible
  - Verify IAX user exists in Asterisk configuration matching device ID
  - Check Android logcat for IAX-related errors

**Problem**: No audio during call
- **Solution**:
  - Ensure RECORD_AUDIO permission is granted
  - Check Asterisk server logs: `sudo tail -f /var/log/asterisk/messages`
  - Verify IAX port 4569 is accessible from Android device
  - Check audio routing in Android (earpiece vs speaker)
  - Verify codec negotiation in Asterisk (ulaw should be allowed)

**Problem**: Call doesn't connect
- **Solution**:
  - Verify Asterisk IP address in `MainActivity.kt` matches server IP
  - Check network connectivity: `ping <asterisk-ip>` from Android device
  - Verify IAX user configuration in `/etc/asterisk/iax.conf`
  - Check Asterisk IAX peers: `sudo asterisk -rx "iax2 show peers"`
  - Review Asterisk logs for connection errors

**Problem**: Call rejected toast appears unexpectedly
- **Solution**: This is normal when the other party rejects the call. Check if they actually rejected it or if there's a network issue.

## 🔒 Security Notes

- MQTT credentials are hardcoded in `MqttManager.kt` (change for production)
- Consider using secure MQTT (TLS/SSL) for production deployments
- Location data is transmitted via MQTT - ensure broker security
- Tile server runs on local network - configure firewall appropriately
- Attachment server runs on local network - configure firewall appropriately
- No authentication on tile server (acceptable for LAN use, add auth for production)
- No authentication on attachment server (acceptable for LAN use, add auth for production)
- Files are stored on server in `received_files/` directory - consider cleanup policy

## 🚧 Known Limitations

- MQTT broker IP is hardcoded (should be configurable)
- Tile server IP is hardcoded (should be configurable)
- Attachment server IP is hardcoded (should be configurable)
- No message encryption (plain text MQTT)
- Tile server has no authentication (LAN-only use)
- Device list doesn't persist across app restarts
- Map tiles require manual setup with proper directory structure

## 🔮 Future Enhancements

- [ ] Configurable MQTT broker settings (UI)
- [ ] Configurable tile server URL (UI)
- [ ] Configurable Asterisk server settings (UI)
- [ ] Message encryption
- [ ] Voice call encryption
- [ ] Persistent device list
- [ ] Map tile downloader/integrator
- [ ] Route tracking and history
- [ ] Push notifications for messages
- [ ] Multi-broker support
- [ ] IAX registration/authentication (currently simplified)
- [ ] Call history and logs
- [ ] Multiple codec support (GSM, Opus)
- [ ] Tile server authentication
- [ ] Attachment server authentication
- [ ] Offline tile caching improvements
- [ ] Attachment preview in chat (images)
- [ ] Attachment size limits
- [ ] Attachment cleanup/expiration policy

## 📝 Notes

### Marker Colors
- **Red Marker** (#ff0a0a): Your device (self)
- **Blue Marker** (#1b0aff): Other devices on network

### Map Behavior
- **Follow Mode ON**: Map automatically centers on your GPS location
- **Follow Mode OFF**: Map allows free panning without auto-center
- Dragging the map automatically disables follow mode
- GPS marker only shows when MQTT is disconnected (fallback)
- MQTT marker for self takes priority over GPS marker

## 🙏 Acknowledgments

- **OSMDroid**: Offline mapping library
- **Eclipse Paho**: MQTT client library
- **Jetpack Compose**: Modern Android UI toolkit
- **Python Standard Library**: HTTP server for tile delivery

---

**Version**: 1.0.0  
**Last Updated**: 17/12/2025
