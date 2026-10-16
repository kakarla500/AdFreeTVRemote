package dev.adfree.tvremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.adfree.tvremote.model.ConnectionState
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.model.TvDevice
import dev.adfree.tvremote.service.RemoteSessionManager
import dev.adfree.tvremote.ui.components.*
import dev.adfree.tvremote.ui.theme.*

enum class NavigationMode {
    DPAD, TRACKPAD
}

@Composable
fun RemoteScreen(sessionManager: RemoteSessionManager) {
    val connectionState by sessionManager.connectionState.collectAsState()
    val activeDevice by sessionManager.activeDevice.collectAsState()
    val discoveredDevicesMap by sessionManager.discovery.discoveredDevices.collectAsState()
    val isScanning by sessionManager.discovery.isScanning.collectAsState()

    var showDiscoverySheet by remember { mutableStateOf(false) }
    var showKeyboardDialog by remember { mutableStateOf(false) }
    var keyboardInputText by remember { mutableStateOf("") }
    var navMode by remember { mutableStateOf(NavigationMode.DPAD) }

    val isConnected = connectionState is ConnectionState.Connected

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            RemoteTopBar(
                activeDevice = activeDevice,
                connectionState = connectionState,
                navMode = navMode,
                onModeToggle = {
                    navMode = if (navMode == NavigationMode.DPAD) NavigationMode.TRACKPAD else NavigationMode.DPAD
                },
                onDeviceClick = { showDiscoverySheet = true },
                onKeyboardClick = { showKeyboardDialog = true },
                onPowerClick = { sessionManager.sendKey(RemoteKey.POWER) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Selector Pill
            NavigationModeSelector(
                selectedMode = navMode,
                onSelectMode = { navMode = it }
            )

            // Center Navigation: D-Pad or Trackpad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (navMode == NavigationMode.DPAD) {
                    DpadControl(onKeyPress = { sessionManager.sendKey(it) })
                } else {
                    TrackpadControl(onKeyPress = { sessionManager.sendKey(it) })
                }
            }

            // System Row: Back, Home, Settings
            SystemNavRow(
                onKeyPress = { sessionManager.sendKey(it) }
            )

            // Volume & Media Controls
            VolumeAndMediaControls(
                onKeyPress = { sessionManager.sendKey(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Device Discovery Bottom Sheet
    if (showDiscoverySheet) {
        DeviceDiscoverySheet(
            devices = discoveredDevicesMap.values.toList(),
            connectionState = connectionState,
            isScanning = isScanning,
            onDeviceSelect = { device ->
                sessionManager.connectToDevice(device)
            },
            onPairPinSubmit = { pin ->
                sessionManager.pairWithPin(pin)
            },
            onManualConnect = { ip ->
                sessionManager.discovery.addManualDevice(ip)
                sessionManager.connectToDevice(TvDevice(name = "Android TV ($ip)", host = ip))
            },
            onRescan = { sessionManager.startScanning() },
            onDismiss = { showDiscoverySheet = false }
        )
    }

    // Text Input to TV Dialog
    if (showKeyboardDialog) {
        AlertDialog(
            onDismissRequest = { showKeyboardDialog = false },
            title = { Text("Send Text to TV", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = keyboardInputText,
                    onValueChange = { keyboardInputText = it },
                    placeholder = { Text("Search movie, show, or URL…", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderDark
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionManager.sendTextInput(keyboardInputText)
                        keyboardInputText = ""
                        showKeyboardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyboardDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun RemoteTopBar(
    activeDevice: TvDevice?,
    connectionState: ConnectionState,
    navMode: NavigationMode,
    onModeToggle: () -> Unit,
    onDeviceClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    onPowerClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Device Info & Status Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .clickable { onDeviceClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (connectionState is ConnectionState.Connected) AccentGreen else AccentRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = activeDevice?.name ?: "Select TV",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Action Buttons: Keyboard, Mode Toggle, Power
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onKeyboardClick) {
                Icon(imageVector = Icons.Default.Keyboard, contentDescription = "Keyboard", tint = TextPrimary)
            }
            IconButton(onClick = onPowerClick) {
                Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = AccentRed)
            }
        }
    }
}

@Composable
private fun NavigationModeSelector(
    selectedMode: NavigationMode,
    onSelectMode: (NavigationMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(4.dp)
    ) {
        val dpadBg = if (selectedMode == NavigationMode.DPAD) PrimaryBlue else Color.Transparent
        val trackpadBg = if (selectedMode == NavigationMode.TRACKPAD) PrimaryBlue else Color.Transparent

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(dpadBg)
                .clickable { onSelectMode(NavigationMode.DPAD) }
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text("D-Pad", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(trackpadBg)
                .clickable { onSelectMode(NavigationMode.TRACKPAD) }
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text("Trackpad", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

