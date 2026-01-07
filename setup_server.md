# Complete Asterisk Server Setup Guide for ISRO App Voice Calling

Complete step-by-step guide to set up Asterisk server on Ubuntu/Linux for IAX-based voice communication between Android devices.

## 📋 Table of Contents

1. [System Requirements](#system-requirements)
2. [Pre-Installation Setup](#pre-installation-setup)
3. [Install Asterisk](#install-asterisk)
4. [Configure IAX (Voice Protocol)](#configure-iax-voice-protocol)
5. [Configure Dialplan (Call Routing)](#configure-dialplan-call-routing)
6. [Configure Firewall](#configure-firewall)
7. [Start and Verify Asterisk](#start-and-verify-asterisk)
8. [Android App Configuration](#android-app-configuration)
9. [Testing Your Setup](#testing-your-setup)
10. [Troubleshooting Guide](#troubleshooting-guide)
11. [Advanced Configuration](#advanced-configuration)
12. [Maintenance and Monitoring](#maintenance-and-monitoring)

---

## 🖥️ System Requirements

### Minimum Requirements
- **OS**: Ubuntu 20.04 or later (also works on Debian, CentOS)
- **RAM**: 1 GB (2 GB recommended)
- **CPU**: 1 core (2 cores recommended)
- **Disk**: 2 GB free space
- **Network**: Ethernet or WiFi with static IP (recommended)

### Supported Operating Systems
- Ubuntu 20.04, 22.04, 24.04
- Debian 10, 11, 12
- CentOS 7, 8
- Rocky Linux 8, 9

### What You'll Need
- Root or sudo access
- Internet connection (for installation only)
- Android devices on the same network
- Basic command-line knowledge

---

## 🔧 Pre-Installation Setup

### Step 1: Update Your System

```bash
sudo apt update
sudo apt upgrade -y
```

**Why?** Ensures you have the latest security patches and package versions.

### Step 2: Install Essential Build Tools

```bash
sudo apt install -y build-essential wget curl git
```

**What this installs:**
- `build-essential`: C/C++ compilers needed for Asterisk
- `wget` & `curl`: Download utilities
- `git`: Version control (optional, for source builds)

### Step 3: Install Asterisk Dependencies

```bash
sudo apt install -y \
    libssl-dev \
    libncurses5-dev \
    libnewt-dev \
    libxml2-dev \
    linux-headers-$(uname -r) \
    libsqlite3-dev \
    uuid-dev \
    libedit-dev \
    libjansson-dev
```

**What these libraries do:**
- `libssl-dev`: SSL/TLS encryption support
- `libncurses5-dev`: Terminal UI support
- `libxml2-dev`: XML parsing for configurations
- `libsqlite3-dev`: Database support for CDR (call logs)

### Step 4: Find Your Server IP Address

```bash
hostname -I
```

**Example output:** `192.168.1.100 172.17.0.1`

**Note the first IP** (e.g., `192.168.1.100`) - this is what Android devices will connect to.

**Alternatively:**
```bash
ip addr show | grep "inet "
```

### Step 5: Set Static IP (Recommended)

**Why?** Prevents your server IP from changing and breaking Android connections.

**For Ubuntu 22.04+ (using Netplan):**

```bash
sudo nano /etc/netplan/00-installer-config.yaml
```

**Add/modify:**
```yaml
network:
  version: 2
  renderer: networkd
  ethernets:
    eth0:  # Change to your interface name (use 'ip addr' to find)
      dhcp4: no
      addresses:
        - 192.168.1.100/24  # Your chosen static IP
      gateway4: 192.168.1.1  # Your router IP
      nameservers:
        addresses:
          - 8.8.8.8
          - 8.8.4.4
```

**Apply changes:**
```bash
sudo netplan apply
```

**Verify:**
```bash
ip addr show
```

---

## 📦 Install Asterisk

### Method 1: Quick Install (Recommended for Beginners)

```bash
sudo apt install -y asterisk
```

**Pros:**
- Fast installation (5 minutes)
- Automatically configured
- Easy updates via `apt`

**Cons:**
- Might not be the latest version
- Limited customization during install

### Method 2: Install from Source (Advanced Users)

**Step 1: Download Asterisk**

```bash
cd /usr/src
sudo wget https://downloads.asterisk.org/pub/telephony/asterisk/asterisk-20-current.tar.gz
```

**Step 2: Extract**

```bash
sudo tar -xzf asterisk-20-current.tar.gz
cd asterisk-20*/
```

**Step 3: Install Prerequisites**

```bash
sudo contrib/scripts/install_prereq install
```

**Step 4: Configure Build**

```bash
sudo ./configure --with-jansson-bundled
```

**Step 5: Select Modules (Optional)**

```bash
sudo make menuselect
```

**Navigate to:**
- Core Sound Packages → Select WAV format
- Music On Hold → Select WAV format
- Press 'x' to exit and save

**Step 6: Compile and Install**

```bash
sudo make -j$(nproc)
sudo make install
sudo make samples
sudo make config
```

**What each command does:**
- `make -j$(nproc)`: Compile using all CPU cores
- `make install`: Install compiled binaries
- `make samples`: Install sample configuration files
- `make config`: Install systemd service

**Step 7: Create Asterisk User**

```bash
sudo groupadd asterisk
sudo useradd -r -d /var/lib/asterisk -g asterisk asterisk
sudo usermod -aG audio,dialout asterisk
sudo chown -R asterisk:asterisk /etc/asterisk
sudo chown -R asterisk:asterisk /var/{lib,log,spool}/asterisk
sudo chown -R asterisk:asterisk /usr/lib/asterisk
```

### Step 8: Verify Installation

```bash
asterisk -V
```

**Expected output:**
```
Asterisk 20.x.x
```

---

## 🎙️ Configure IAX (Voice Protocol)

IAX2 (Inter-Asterisk eXchange version 2) is the protocol used for voice calls between Android devices.

### Step 1: Backup Original Configuration

```bash
sudo cp /etc/asterisk/iax.conf /etc/asterisk/iax.conf.backup
```

### Step 2: Edit IAX Configuration

```bash
sudo nano /etc/asterisk/iax.conf
```

### Step 3: Replace Content with This Configuration

```ini
; =============================================
; IAX2 Configuration for ISRO App
; No authentication - prototype setup
; =============================================

[general]
; === NETWORK BINDING ===
bindport=4569                    ; IAX2 standard port
bindaddr=0.0.0.0                 ; Listen on all interfaces

; === CODEC SETTINGS ===
disallow=all                     ; Disable all codecs first
allow=ulaw                       ; Enable ulaw (8kHz, 8-bit) - PRIMARY
allow=alaw                       ; Enable alaw (8kHz, 8-bit) - FALLBACK
allow=gsm                        ; Enable GSM (13kbps) - FALLBACK

; === JITTER BUFFER ===
jitterbuffer=no                  ; Disable jitter buffer (low latency)
forcejitterbuffer=no             ; Don't force jitter buffer
maxjitterbuffer=200              ; Max jitter buffer (if enabled)
maxjitterinterps=10              ; Max jitter interpolations

; === QUALITY OF SERVICE ===
tos=lowdelay                     ; QoS: Minimize delay (for voice)
cos=5                            ; 802.1p Class of Service

; === SECURITY SETTINGS ===
requirecalltoken=no              ; Disable call token (no auth)
autokill=yes                     ; Auto-kill dead connections
delayreject=yes                  ; Slight delay on reject (security)

; === PERFORMANCE ===
trunktimestamps=yes              ; Use trunk timestamps
rtcachefriends=yes               ; Cache IAX friends in memory
iaxcompat=yes                    ; Enable IAX compatibility mode

; === LOGGING ===
; Enable detailed IAX debugging (comment out in production)
; iaxdebug=yes

; =============================================
; USER DEFINITIONS
; Add one section per Android device
; =============================================

; === TEMPLATE FOR USERS ===
; Copy this template for each device
; Replace 'deviceXXX' with actual device IDs from Android app

[device001]
type=peer                        ; This is a peer connection
username=device001               ; Must match Android device ID
host=dynamic                     ; Accept from any IP (device can move)
context=internal                 ; Use 'internal' dialplan context
disallow=all                     ; Clear codecs
allow=ulaw                       ; Allow ulaw codec
allow=alaw                       ; Allow alaw codec
requirecalltoken=no              ; No authentication required
qualify=yes                      ; Send keepalive packets
qualifyfreqok=60000              ; Keepalive every 60 seconds
qualifyfreqnotok=10000           ; Check every 10 seconds if down

[device002]
type=peer
username=device002
host=dynamic
context=internal
disallow=all
allow=ulaw
allow=alaw
requirecalltoken=no
qualify=yes
qualifyfreqok=60000
qualifyfreqnotok=10000

[device003]
type=peer
username=device003
host=dynamic
context=internal
disallow=all
allow=ulaw
allow=alaw
requirecalltoken=no
qualify=yes
qualifyfreqok=60000
qualifyfreqnotok=10000

; =============================================
; ADD MORE DEVICES HERE
; Just copy the template and change device ID
; =============================================

; Example: For a device with ID "pi_rover_01"
[pi_rover_01]
type=peer
username=pi_rover_01
host=dynamic
context=internal
disallow=all
allow=ulaw
allow=alaw
requirecalltoken=no
qualify=yes

; Example: For a device with ID "android_phone_123"
[android_phone_123]
type=peer
username=android_phone_123
host=dynamic
context=internal
disallow=all
allow=ulaw
allow=alaw
requirecalltoken=no
qualify=yes
```

### Step 4: Save and Exit

Press `Ctrl + X`, then `Y`, then `Enter`

### Important Configuration Explanations

| Setting | Value | Why? |
|---------|-------|------|
| `bindport=4569` | Standard IAX port | Required by protocol |
| `bindaddr=0.0.0.0` | All interfaces | Accepts from any network |
| `allow=ulaw` | μ-law codec | 8kHz, low bandwidth, good quality |
| `host=dynamic` | Dynamic IP | Devices can connect from anywhere |
| `context=internal` | Dialplan context | Routes calls to internal extensions |
| `requirecalltoken=no` | No auth | Simplifies prototype (add auth for production) |
| `qualify=yes` | Keepalive | Detects when devices disconnect |

---

## 📞 Configure Dialplan (Call Routing)

The dialplan defines how calls are routed between devices.

### Step 1: Backup Original Configuration

```bash
sudo cp /etc/asterisk/extensions.conf /etc/asterisk/extensions.conf.backup
```

### Step 2: Edit Dialplan Configuration

```bash
sudo nano /etc/asterisk/extensions.conf
```

### Step 3: Replace Content with This Configuration

```ini
; =============================================
; Dialplan Configuration for ISRO App
; Simple peer-to-peer calling
; =============================================

[general]
static=yes                       ; Static configuration
writeprotect=no                  ; Allow writing
autofallthrough=yes              ; Auto-hangup after dialplan end
clearglobalvars=no               ; Keep global variables

; =============================================
; INTERNAL CONTEXT (Main calling context)
; All IAX users use this context
; =============================================

[internal]

; === DYNAMIC USER CALLING ===
; This pattern matches ANY device ID and routes calls directly
; Example: device001 calls device002
;   → Asterisk dials IAX2/device002
;   → device002 receives call

exten => _X.,1,NoOp(Call from ${CALLERID(num)} to ${EXTEN})
exten => _X.,n,Set(CALLERID(name)=${CALLERID(num)})
exten => _X.,n,Dial(IAX2/${EXTEN},30)
exten => _X.,n,Hangup()

; === EXPLICIT DEVICE EXTENSIONS (Optional) ===
; You can define explicit extensions for specific devices
; Example: Call 1001 to reach device001

exten => 1001,1,NoOp(Calling device001)
exten => 1001,n,Dial(IAX2/device001,30)
exten => 1001,n,Hangup()

exten => 1002,1,NoOp(Calling device002)
exten => 1002,n,Dial(IAX2/device002,30)
exten => 1002,n,Hangup()

exten => 1003,1,NoOp(Calling device003)
exten => 1003,n,Dial(IAX2/device003,30)
exten => 1003,n,Hangup()

; === TEST EXTENSIONS ===

; Echo test - dial 600 to hear your own voice
exten => 600,1,NoOp(Echo test)
exten => 600,n,Answer()
exten => 600,n,Playback(demo-echotest)
exten => 600,n,Echo()
exten => 600,n,Hangup()

; Music on hold test - dial 601
exten => 601,1,NoOp(Music on hold test)
exten => 601,n,Answer()
exten => 601,n,MusicOnHold()

; Conference room - dial 700
exten => 700,1,NoOp(Conference room)
exten => 700,n,Answer()
exten => 700,n,ConfBridge(conference1)
exten => 700,n,Hangup()

; === INVALID EXTENSION ===
exten => i,1,NoOp(Invalid extension dialed)
exten => i,n,Playback(invalid)
exten => i,n,Hangup()

; === TIMEOUT HANDLER ===
exten => t,1,NoOp(Timeout - no response)
exten => t,n,Hangup()
```

### Step 4: Save and Exit

Press `Ctrl + X`, then `Y`, then `Enter`

### Dialplan Explanation

**Pattern Matching:**
- `_X.` matches any extension (X = digit, . = more digits)
- Example: device001, device002, android_123, pi_rover_01 all match

**Dial Command:**
```
Dial(IAX2/${EXTEN},30)
```
- `IAX2/`: Use IAX2 protocol
- `${EXTEN}`: Called device ID
- `30`: Ring for 30 seconds before timeout

**Call Flow Example:**
```
device001 calls device002:
1. device001 sends IAX NEW frame with called number "device002"
2. Asterisk receives call in [internal] context
3. Matches pattern _X. (device002)
4. Executes Dial(IAX2/device002,30)
5. Asterisk sends IAX NEW frame to device002
6. device002 receives incoming call notification
7. device002 accepts → IAX ACCEPT frame sent
8. Call connected, audio flows bidirectionally
```

---

## 🔥 Configure Firewall

### For Ubuntu (UFW Firewall)

**Step 1: Check Firewall Status**

```bash
sudo ufw status
```

**Step 2: Allow IAX Port**

```bash
sudo ufw allow 4569/udp comment 'Asterisk IAX2'
```

**Step 3: Allow SSH (Important!)**

```bash
sudo ufw allow 22/tcp comment 'SSH'
```

**Step 4: Enable Firewall**

```bash
sudo ufw enable
```

**Step 5: Verify Rules**

```bash
sudo ufw status numbered
```

**Expected output:**
```
Status: active

     To                         Action      From
     --                         ------      ----
[ 1] 22/tcp                     ALLOW IN    Anywhere                  # SSH
[ 2] 4569/udp                   ALLOW IN    Anywhere                  # Asterisk IAX2
```

### For CentOS/RHEL (firewalld)

```bash
sudo firewall-cmd --permanent --add-port=4569/udp
sudo firewall-cmd --reload
sudo firewall-cmd --list-ports
```

### For iptables (Advanced)

```bash
sudo iptables -A INPUT -p udp --dport 4569 -j ACCEPT
sudo iptables-save | sudo tee /etc/iptables/rules.v4
```

---

## 🚀 Start and Verify Asterisk

### Step 1: Start Asterisk Service

```bash
sudo systemctl start asterisk
```

### Step 2: Enable Auto-Start on Boot

```bash
sudo systemctl enable asterisk
```

### Step 3: Check Service Status

```bash
sudo systemctl status asterisk
```

**Expected output:**
```
● asterisk.service - LSB: Asterisk PBX
     Loaded: loaded (/etc/init.d/asterisk; generated)
     Active: active (running) since Mon 2025-01-06 10:30:00 UTC; 5s ago
```

**Look for:** `Active: active (running)` in green

### Step 4: Verify IAX Port is Listening

```bash
sudo netstat -ulnp | grep 4569
```

**Expected output:**
```
udp        0      0 0.0.0.0:4569            0.0.0.0:*                           12345/asterisk
```

**Alternative command:**
```bash
sudo ss -ulnp | grep 4569
```

### Step 5: Connect to Asterisk CLI

```bash
sudo asterisk -rvvv
```

**What the flags mean:**
- `-r`: Reconnect to running Asterisk
- `-vvv`: Verbose level 3 (more logs)

**You should see:**
```
Asterisk 20.x.x, Copyright (C) 1999-2023 Sangoma Technologies Corporation and others.
Created by Mark Spencer <markster@digium.com>
Asterisk comes with ABSOLUTELY NO WARRANTY; type 'core show warranty' for details.
This is free software, with components licensed under the GNU General Public
License version 2 and other licenses; you are welcome to redistribute it under
certain conditions. Type 'core show license' for details.
=========================================================================
Connected to Asterisk 20.x.x currently running on localhost (pid = 12345)
localhost*CLI>
```

### Step 6: Test Asterisk Commands

**Check IAX module:**
```
localhost*CLI> iax2 show peers
```

**Expected output:**
```
Name/Username    Host                 Mask             Port      Status      
device001        (Unspecified)        (D)  0.0.0.0             0         UNKNOWN     
device002        (Unspecified)        (D)  0.0.0.0             0         UNKNOWN     
device003        (Unspecified)        (D)  0.0.0.0             0         UNKNOWN     
3 iax2 peers [0 online, 0 offline, 3 unmonitored]
```

**Check dialplan:**
```
localhost*CLI> dialplan show internal
```

**Expected output:**
```
[ Context 'internal' created by 'pbx_config' ]
  '_X.' =>          1. NoOp(Call from ${CALLERID(num)} to ${EXTEN})
                    2. Set(CALLERID(name)=${CALLERID(num)})
                    3. Dial(IAX2/${EXTEN},30)
                    4. Hangup()
```

**Exit CLI:**
```
localhost*CLI> exit
```

Or press `Ctrl + C`

---

## 📱 Android App Configuration

### Step 1: Update Server IP in MainActivity

**File:** `app/src/main/java/com/example/isro_app/MainActivity.kt`

**Find this line (around line 176):**
```kotlin
iaxManager = IaxManager(this, "192.168.29.242")
```

**Change to your server IP:**
```kotlin
iaxManager = IaxManager(this, "192.168.1.100")  // Your Asterisk server IP
```

### Step 2: Update Server Settings

Create a settings screen or update `ServerSettings.kt`:

```kotlin
data class ServerSettings(
    val asteriskServerIp: String = "192.168.1.100",  // Your server IP
    val tileServerUrl: String = "http://192.168.1.100:8080/tiles/",
    val attachmentServerUrl: String = "http://192.168.1.100:8090"
)
```

### Step 3: Ensure Device IDs Match IAX Users

**In Android app**, device IDs come from MQTT Manager:
```kotlin
val myDeviceId = mqttManager.myId  // Example: "device001"
```

**In Asterisk**, ensure matching user exists in `/etc/asterisk/iax.conf`:
```ini
[device001]
type=peer
username=device001
...
```

**They must match exactly** (case-sensitive)

### Step 4: Rebuild Android App

```bash
./gradlew clean assembleDebug
```

Or click the Run button (▶️) in Android Studio.

---

## 🧪 Testing Your Setup

### Test 1: Verify Asterisk is Running

```bash
sudo systemctl status asterisk
```

**Should show:** `active (running)`

### Test 2: Check IAX Port

```bash
sudo netstat -ulnp | grep 4569
```

**Should show:** Asterisk listening on port 4569

### Test 3: Ping Server from Android Device

**On Android device:**
1. Install "Network Utilities" app from Play Store
2. Ping your server IP: `192.168.1.100`
3. Should get responses: `Reply from 192.168.1.100: time=5ms`

**Alternative:** Use Termux app:
```bash
ping 192.168.1.100
```

### Test 4: Test IAX Connection with Android

**Step 1: Open Android App**
- App should automatically connect to Asterisk server
- Check logcat for IAX connection logs

**Step 2: Check Asterisk CLI**

```bash
sudo asterisk -rvvv
```

**Watch for connection attempts:**
```
iax2 show peers
```

**Once Android connects, you'll see:**
```
Name/Username    Host                 Mask             Port      Status      
device001        192.168.1.101        (D)  255.255.255.255   4569      OK (60 ms)
```

### Test 5: Make Test Call Between Two Devices

**Requirements:**
- 2 Android devices with the app installed
- Both connected to same WiFi as Asterisk server
- Different device IDs (e.g., device001 and device002)

**Procedure:**

1. **Device A (device001):**
   - Open app
   - Select device002 from device list
   - Press call button (📞)

2. **Device B (device002):**
   - Should receive incoming call dialog
   - Press "Accept"

3. **Expected Result:**
   - Call connects
   - Both devices can hear each other
   - Active call interface shows
   - Either can end call

**Watch Asterisk logs:**
```bash
sudo asterisk -rvvv
```

**You should see:**
```
-- Called IAX2/device002
-- IAX2/device002-1 is ringing
-- IAX2/device002-1 answered
-- Channel IAX2/device001-1 joined 'simple_bridge' basic-bridge
-- Channel IAX2/device002-1 joined 'simple_bridge' basic-bridge
```

### Test 6: Echo Test (Without Android)

**Using softphone (optional):**

1. Install Zoiper or Linphone on your computer
2. Configure IAX account:
   - Server: Your Asterisk IP
   - Username: device999
   - Port: 4569
3. Add device999 to iax.conf
4. Dial 600 for echo test

---

## 🔧 Troubleshooting Guide

### Issue 1: Asterisk Won't Start

**Symptom:**
```bash
sudo systemctl status asterisk
# Shows: Failed to start Asterisk PBX
```

**Solution:**

**Check logs:**
```bash
sudo journalctl -u asterisk -n 50
```

**Common causes:**

1. **Port already in use:**
```bash
sudo netstat -ulnp | grep 4569
# If another process is using port 4569
sudo systemctl stop asterisk
sudo killall asterisk
sudo systemctl start asterisk
```

2. **Configuration syntax error:**
```bash
sudo asterisk -cvvv
# Look for error messages
```

**Fix:** Check iax.conf and extensions.conf for typos

3. **Permission issues:**
```bash
sudo chown -R asterisk:asterisk /etc/asterisk
sudo chown -R asterisk:asterisk /var/lib/asterisk
sudo systemctl restart asterisk
```

---

### Issue 2: Android Can't Connect to Server

**Symptom:** No IAX peers showing in Asterisk

**Diagnostics:**

**Step 1: Check network connectivity**
```bash
# On server
ping 192.168.1.101  # Android device IP
```

**Step 2: Check firewall**
```bash
sudo ufw status
# Ensure port 4569 is allowed
```

**Step 3: Check Android logs**
```bash
adb logcat | grep IAX
```

**Look for:**
- Connection attempts
- Timeout errors
- Permission denied

**Solutions:**

1. **Firewall blocking:**
```bash
sudo ufw allow 4569/udp
sudo ufw reload
```

2. **Wrong server IP in Android app:**
   - Verify IP matches: `hostname -I`
   - Update MainActivity.kt

3. **Android device on different network:**
   - Ensure both on same WiFi
   - Check router settings

4. **Asterisk not listening:**
```bash
sudo systemctl restart asterisk
sudo netstat -ulnp | grep 4569
```

---

### Issue 3: Call Connects But No Audio

**Symptom:** Call accepted but can't hear anything

**Diagnostics:**

**Check codec negotiation:**
```bash
sudo asterisk -rvvv
# Make a call
# Watch for codec messages
```

**Look for:**
```
-- Accepting UNAUTHENTICATED call from 192.168.1.101:
format: ulaw
```

**Solutions:**

1. **Codec mismatch:**

**In iax.conf:**
```ini
allow=ulaw
allow=alaw
```

**In Android (IaxManager.java):** Ensure ulaw is used

2. **Firewall blocking RTP:**

Asterisk sends audio on random high ports. Allow range:
```bash
# In /etc/asterisk/rtp.conf
rtpstart=10000
rtpend=20000

# Allow in firewall
sudo ufw allow 10000:20000/udp
```

3. **Audio permissions in Android:**

**Check AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

**Request at runtime:**
```kotlin
ActivityCompat.requestPermissions(
    this,
    arrayOf(Manifest.permission.RECORD_AUDIO),
    101
)
```

4. **Audio routing issue:**

**In CallController.kt:**
```kotlin
private fun routeAudio() {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    audioManager.isSpeakerphoneOn = false
    Log.d(TAG, "Audio routed for communication mode")
}
```

---

### Issue 4: Calls Timeout / Ring Forever

**Symptom:** Call rings but never connects

**Check Asterisk logs:**
```bash
sudo asterisk -rvvv
# Make call
# Look for "No answer" or "User busy"
```

**Solutions:**

1. **Target device offline:**

Check peer status:
```
iax2 show peers
```

If shows UNREACHABLE:
```
Name/Username    Host                 Status      
device002        (Unspecified)        UNREACHABLE
```

**Fix:** Ensure target Android device is running app

2. **IAX user not defined:**

Add user to `/etc/asterisk/iax.conf`:
```ini
[device002]
type=peer
username=device002
host=dynamic
context=internal
disallow=all
allow=ulaw
requirecalltoken=no
```

Reload:
```bash
sudo asterisk -rx "iax2 reload"
```

3. **Dialplan issue:**

Check dialplan:
```
dialplan show internal
```

Ensure pattern matches:
```
'_X.' => 1. Dial(IAX2/${EXTEN},30)
```

---

### Issue 5: One-Way Audio

**Symptom:** Only one person can hear

**Diagnostics:**

**Check NAT/firewall:**
```bash
# In iax.conf, ensure:
nat=yes
qualify=yes
```

**Check Android audio permissions:**
```bash
adb logcat | grep AUDIO
adb logcat | grep RECORD_AUDIO
```

**Solutions:**

1. **Microphone not working:**

Test with device mic:
- Record voice memo
- Call another app (WhatsApp, etc.)

If mic works elsewhere, check Android app permissions.

2. **Speaker not working:**

Test playback in app:
```kotlin
// Add test button to play sound
mediaPlayer.start()
```

3. **Codec issue:**

Force specific codec in iax.conf:
```ini
disallow=all
allow=ulaw
```

---

### Issue 6: Multiple Devices Can't Register

**Symptom:** Only first device connects

**Check:**
```
iax2 show peers
```

**Ensure each device has unique username:**
```ini
[device001]
username=device001  # Must be unique

[device002]
username=device002  # Must be unique
```

**In Android app**, ensure unique device IDs:
```kotlin
val myDeviceId = Settings.Secure.getString(
    contentResolver,
    Settings.Secure.ANDROID_ID
)
```

---