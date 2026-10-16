package dev.adfree.tvremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.adfree.tvremote.model.ConnectionState
import dev.adfree.tvremote.model.TvDevice
import dev.adfree.tvremote.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoverySheet(
    devices: List<TvDevice>,
    connectionState: ConnectionState,
    isScanning: Boolean,
    onDeviceSelect: (TvDevice) -> Unit,
    onPairPinSubmit: (String) -> Unit,
    onManualConnect: (String) -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit
) {
    var showManualIpDialog by remember { mutableStateOf(false) }
    var manualIpText by remember { mutableStateOf("") }

    var pairingPinText by remember { mutableStateOf("") }
    val isPairingRequired = connectionState is ConnectionState.PairingRequired

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Android TVs",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = { showManualIpDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add IP", tint = PrimaryBlue)
                    }
                    IconButton(onClick = onRescan) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan", tint = PrimaryBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isPairingRequired) {
                // PIN Input Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceLightDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Enter Pairing PIN",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Check your TV screen for a pairing code",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pairingPinText,
                            onValueChange = { pairingPinText = it },
                            placeholder = { Text("e.g. 123456", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onPairPinSubmit(pairingPinText) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pair & Connect")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Scanning Wi-Fi network…", color = TextSecondary, fontSize = 14.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Android TVs discovered yet", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onRescan) {
                                Text("Scan Again", color = PrimaryBlue)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(devices) { device ->
                        DeviceItemRow(
                            device = device,
                            isSelected = connectionState is ConnectionState.Connected && connectionState.device.id == device.id,
                            onClick = { onDeviceSelect(device) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showManualIpDialog) {
        AlertDialog(
            onDismissRequest = { showManualIpDialog = false },
            title = { Text("Connect by IP", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = manualIpText,
                    onValueChange = { manualIpText = it },
                    placeholder = { Text("192.168.1.xxx", color = TextSecondary) },
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
                        showManualIpDialog = false
                        onManualConnect(manualIpText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualIpDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun DeviceItemRow(
    device: TvDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else SurfaceLightDark)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = if (isSelected) PrimaryBlue else TextPrimary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = "${device.host}:${device.port}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        if (isSelected) {
            Text(
                text = "Connected",
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

