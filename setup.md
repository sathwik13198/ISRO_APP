# Asterisk IAX Setup Guide for Ubuntu

Complete guide to set up Asterisk server on Ubuntu for IAX voice communication between Android devices. **No authentication required** - simple prototype setup.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Install Asterisk](#install-asterisk)
3. [Configure IAX Users](#configure-iax-users)
4. [Configure Dialplan](#configure-dialplan)
5. [Configure Bridge](#configure-bridge)
6. [Firewall Configuration](#firewall-configuration)
7. [Start and Test Asterisk](#start-and-test-asterisk)
8. [Android App Configuration](#android-app-configuration)
9. [Testing](#testing)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Ubuntu 20.04 or later (tested on Ubuntu 22.04)
- Root or sudo access
- Network connectivity
- Android devices on the same network

---

## Install Asterisk

### Step 1: Update System

```bash
sudo apt update
sudo apt upgrade -y
```

### Step 2: Install Dependencies

```bash
sudo apt install -y build-essential wget libssl-dev libncurses5-dev libnewt-dev libxml2-dev linux-headers-$(uname -r) libsqlite3-dev uuid-dev
```

### Step 3: Install Asterisk

**Option A: Install from Ubuntu Repository (Recommended for prototype)**

```bash
sudo apt install -y asterisk
```

**Option B: Install from Source (if you need latest version)**

```bash
cd /tmp
wget http://downloads.asterisk.org/pub/telephony/asterisk/asterisk-20-current.tar.gz
tar -xzf asterisk-20-current.tar.gz
cd asterisk-20*/
./configure
make
sudo make install
sudo make samples
sudo make config
```

### Step 4: Verify Installation

```bash
asterisk -V
```

You should see the Asterisk version number.

---

## Configure IAX Users

### Step 1: Edit IAX Configuration File

```bash
sudo nano /etc/asterisk/iax.conf
```

### Step 2: Add General Settings

Add or modify the `[general]` section:

```ini
[general]
bindport=4569
bindaddr=0.0.0.0
disallow=all
allow=ulaw
allow=alaw
allow=gsm
jitterbuffer=no
forcejitterbuffer=no
tos=lowdelay
autokill=yes
```

### Step 3: Add IAX Users (No Authentication)

Add user definitions at the end of the file. Replace `user1` and `user2` with your device identifiers (e.g., device IDs from your Android app):

```ini
[user1]
type=peer
username=user1
host=dynamic
context=internal
disallow=all
allow=ulaw
allow=alaw
requirecalltoken=no

[user2]
type=peer
username=user2
host=dynamic
context=internal
disallow=all
allow=ulaw
allow=alaw
requirecalltoken=no
```

**Note**: For dynamic user creation based on Android device IDs, you can add more users following the same pattern. The Android app will use the device ID as the username.

### Step 4: Save and Exit

Press `Ctrl+X`, then `Y`, then `Enter` to save.

---

## Configure Dialplan

### Step 1: Edit Extensions Configuration

```bash
sudo nano /etc/asterisk/extensions.conf
```

### Step 2: Add Internal Context

Add or modify the `[internal]` context section:

```ini
[internal]
; Extension 1001 routes to user1
exten => 1001,1,Dial(IAX2/user1,20)
exten => 1001,2,Hangup()

; Extension 1002 routes to user2
exten => 1002,1,Dial(IAX2/user2,20)
exten => 1002,2,Hangup()

; Generic extension pattern for dynamic users
; This allows calling any user by their username
exten => _X.,1,NoOp(Calling IAX user ${EXTEN})
exten => _X.,2,Dial(IAX2/${EXTEN},20)
exten => _X.,3,Hangup()
```

**Explanation**:
- `1001` and `1002` are example extensions
- `Dial(IAX2/user1,20)` dials the IAX user with 20 second timeout
- `_X.` pattern matches any extension (for dynamic routing)
- `${EXTEN}` is the dialed extension number

### Step 3: Save and Exit

Press `Ctrl+X`, then `Y`, then `Enter` to save.

---

## Configure Bridge

### Step 1: Create Bridge Configuration File

Create a bridge configuration file for dynamic user routing:

```bash
sudo nano /etc/asterisk/iax_bridge.conf
```

### Step 2: Add Bridge Context

Add the following content:

```ini
[bridge]
; Bridge context for connecting two IAX users directly
; Usage: Call extension "bridge/user1/user2" to connect user1 and user2

exten => bridge/${ARG1}/${ARG2},1,NoOp(Bridging ${ARG1} and ${ARG2})
exten => bridge/${ARG1}/${ARG2},2,Dial(IAX2/${ARG1}&IAX2/${ARG2},30)
exten => bridge/${ARG1}/${ARG2},3,Hangup()

; Simple bridge - call user2 when user1 calls
exten => _X.,1,NoOp(Calling ${EXTEN} via IAX)
exten => _X.,2,Dial(IAX2/${EXTEN},30)
exten => _X.,3,Hangup()
```

### Step 3: Include Bridge in Main Extensions

Edit `/etc/asterisk/extensions.conf` and add at the top:

```ini
[general]
static=yes
writeprotect=no
autofallthrough=yes

#include => "iax_bridge.conf"
```

### Step 4: Alternative - Add Bridge Directly to extensions.conf

Alternatively, you can add the bridge logic directly in `extensions.conf`:

```ini
[internal]
; Direct call routing - call user by their username/extension
exten => _X.,1,NoOp(Calling IAX user ${EXTEN})
exten => _X.,2,Dial(IAX2/${EXTEN},30)
exten => _X.,3,Hangup()
```

---

## Firewall Configuration

### Step 1: Allow IAX Port (4569)

```bash
sudo ufw allow 4569/udp
```

### Step 2: Allow Asterisk Manager Port (Optional, for monitoring)

```bash
sudo ufw allow 5038/tcp
```

### Step 3: Reload Firewall

```bash
sudo ufw reload
```

### Step 4: Verify Firewall Status

```bash
sudo ufw status
```

---

## Start and Test Asterisk

### Step 1: Start Asterisk Service

```bash
sudo systemctl start asterisk
```

### Step 2: Enable Auto-Start on Boot

```bash
sudo systemctl enable asterisk
```

### Step 3: Check Asterisk Status

```bash
sudo systemctl status asterisk
```

You should see `active (running)` status.

### Step 4: Reload Configuration

After making configuration changes, reload Asterisk:

```bash
sudo asterisk -rx "reload"
```

Or reload specific modules:

```bash
sudo asterisk -rx "iax2 reload"
sudo asterisk -rx "dialplan reload"
```

### Step 5: Connect to Asterisk CLI (Optional)

```bash
sudo asterisk -rvvv
```

This opens the Asterisk CLI with verbose logging. Useful for debugging.

### Step 6: Check IAX Status

In the Asterisk CLI, run:

```bash
iax2 show peers
```

This shows registered IAX peers. Initially, it will be empty until Android devices connect.

---

## Android App Configuration

### Step 1: Update Asterisk IP Address

Edit `app/src/main/java/com/example/iax/IaxUdpTransport.java`:

```java
private static final int PORT = 4569;  // Change from 5000 to 4569 (IAX port)
```

Update the `start()` method to use the provided host:

```java
public void start(String host) {
    try {
        socket = new DatagramSocket();
        remoteAddr = InetAddress.getByName(host);  // Use provided host instead of HOST constant
        running = true;
        // ... rest of the code
    }
}
```

### Step 2: Update MainActivity

The Asterisk IP is already configured in `MainActivity.kt`:

```kotlin
iaxManager = IaxManager("192.168.29.242")  // Change this to your Ubuntu server IP
```

**Find your Ubuntu server IP**:

```bash
ip addr show
```

Or:

```bash
hostname -I
```

### Step 3: Map Device IDs to IAX Usernames

The Android app uses device IDs (from MQTT) as peer identifiers. Ensure these device IDs match the IAX usernames configured in `/etc/asterisk/iax.conf`.

For example, if your Android device ID is `device123`, add this to `iax.conf`:

```ini
[device123]
type=peer
username=device123
host=dynamic
context=internal
disallow=all
allow=ulaw
allow=alaw
requirecalltoken=no
```

---

## Testing

### Test 1: Verify Asterisk is Running

```bash
sudo systemctl status asterisk
```

### Test 2: Check IAX Port is Listening

```bash
sudo netstat -ulnp | grep 4569
```

Or:

```bash
sudo ss -ulnp | grep 4569
```

You should see Asterisk listening on port 4569.

### Test 3: Test from Android App

1. **Start Asterisk server** on Ubuntu
2. **Configure Android app** with correct Asterisk IP
3. **Run Android app** on two devices
4. **Device 1**: Press Call button for Device 2
5. **Device 2**: Should receive call notification and can accept
6. **Both devices**: Should be able to talk to each other

### Test 4: Monitor Asterisk Logs

```bash
sudo tail -f /var/log/asterisk/messages
```

Or in Asterisk CLI:

```bash
sudo asterisk -rvvv
```

Watch for IAX connection attempts and call setup messages.

### Test 5: Check IAX Peers

In Asterisk CLI:

```bash
iax2 show peers
iax2 show registry
```

---

## Troubleshooting

### Problem: Asterisk won't start

**Solution**:
```bash
sudo systemctl status asterisk
sudo journalctl -u asterisk -n 50
```

Check for configuration errors in the logs.

### Problem: IAX port 4569 not accessible

**Solution**:
1. Check firewall: `sudo ufw status`
2. Check if port is listening: `sudo netstat -ulnp | grep 4569`
3. Verify bindaddr in `iax.conf` is `0.0.0.0`

### Problem: Android devices can't connect

**Solution**:
1. Verify Asterisk IP address in Android app matches Ubuntu server IP
2. Check network connectivity: `ping <asterisk-ip>` from Android device
3. Check Asterisk logs: `sudo tail -f /var/log/asterisk/messages`
4. Verify IAX user exists in `iax.conf` with matching username

### Problem: Calls not connecting

**Solution**:
1. Check dialplan: `sudo asterisk -rx "dialplan show internal"`
2. Verify extensions are configured correctly
3. Check IAX peers: `sudo asterisk -rx "iax2 show peers"`
4. Review Asterisk logs for errors

### Problem: No audio

**Solution**:
1. Verify codecs are allowed: `allow=ulaw` in `iax.conf`
2. Check Android app has RECORD_AUDIO permission
3. Verify audio routing in Android app
4. Check Asterisk logs for codec negotiation issues

### Problem: Configuration changes not taking effect

**Solution**:
```bash
sudo asterisk -rx "reload"
```

Or reload specific modules:
```bash
sudo asterisk -rx "iax2 reload"
sudo asterisk -rx "dialplan reload"
```

### Problem: Need to add more users dynamically

**Solution**: Add more user entries to `/etc/asterisk/iax.conf` following the same pattern, then reload:

```bash
sudo asterisk -rx "iax2 reload"
```

---

## Quick Reference

### Important Files

- `/etc/asterisk/iax.conf` - IAX user configuration
- `/etc/asterisk/extensions.conf` - Dialplan configuration
- `/var/log/asterisk/messages` - Asterisk log file

### Common Commands

```bash
# Start Asterisk
sudo systemctl start asterisk

# Stop Asterisk
sudo systemctl stop asterisk

# Restart Asterisk
sudo systemctl restart asterisk

# Reload configuration
sudo asterisk -rx "reload"

# View Asterisk CLI
sudo asterisk -rvvv

# Check IAX peers
sudo asterisk -rx "iax2 show peers"

# Check dialplan
sudo asterisk -rx "dialplan show internal"

# View logs
sudo tail -f /var/log/asterisk/messages
```

### Default Ports

- **IAX**: UDP 4569
- **SIP**: UDP 5060 (not used in this setup)
- **AMI**: TCP 5038 (optional, for management)

---

## Next Steps

1. Configure Android app with correct Asterisk IP
2. Test call between two Android devices
3. Monitor Asterisk logs during calls
4. Adjust codec settings if needed for better audio quality

---

## Support

For issues:
1. Check Asterisk logs: `/var/log/asterisk/messages`
2. Check Android logcat for IAX-related errors
3. Verify network connectivity between devices and server
4. Ensure firewall allows UDP port 4569

---

**Last Updated**: 2025-01-XX  
**Asterisk Version**: Tested with Asterisk 18+  
**Ubuntu Version**: Tested with Ubuntu 20.04, 22.04

