package com.example.fibo.ui.components

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.fibo.utils.BluetoothUtils.hasBluetoothConnectPermission
import com.example.fibo.utils.MyBluetoothState

@Composable
fun BluetoothStatusUI(
    state: MyBluetoothState,
    onEnableBluetooth: () -> Unit,
    onScanDevices: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onPrint: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Estado actual
        BluetoothStateIndicator(state)

        Spacer(modifier = Modifier.height(8.dp))

        // Acciones según el estado
        when (state) {
            is MyBluetoothState.Disabled -> {
                ActionButton(
                    text = "Activar Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    onClick = onEnableBluetooth
                )
            }
            is MyBluetoothState.Enabling -> {
                LoadingButton(text = "Activando...")
            }
            is MyBluetoothState.Enabled -> {
                ActionButton(
                    text = "Buscar Dispositivos",
                    icon = Icons.Default.Search,
                    onClick = onScanDevices
                )
            }
            is MyBluetoothState.Scanning -> {
                LoadingButton(text = "Buscando...")
            }
            is MyBluetoothState.DevicesFound -> {
                DevicesList(
                    devices = state.devices,
                    onDeviceSelected = onConnectDevice
                )
            }
            is MyBluetoothState.Connecting -> {
                LoadingButton( text = "Conectando a ${state.device}...")
            }
            is MyBluetoothState.Connected -> {
                ConnectedDeviceUI(
                    device = state.device,
                    onPrint = onPrint,
                    onDisconnect = onDisconnect
                )
            }
            is MyBluetoothState.Printing -> {
                LoadingButton(text = "Imprimiendo...")
            }
            is MyBluetoothState.Error -> {
                ErrorStateUI(
                    message = state.message,
                    retryAction = state.retryAction
                )
            }
            is MyBluetoothState.Disconnected -> {
                Text(
                    text = "Desconectado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun BluetoothStateIndicator(state: MyBluetoothState) {
    val (icon, color) = when (state) {
        is MyBluetoothState.Disabled -> Pair(Icons.Default.BluetoothDisabled, Color.Red)
        is MyBluetoothState.Enabling -> Pair(Icons.Default.BluetoothSearching, Color(0xFFFFA000))
        is MyBluetoothState.Enabled -> Pair(Icons.Default.Bluetooth, Color.Blue)
        is MyBluetoothState.Scanning -> Pair(Icons.Default.Search, Color.Blue)
        is MyBluetoothState.DevicesFound -> Pair(Icons.Default.BluetoothAudio, Color.Green)
        is MyBluetoothState.Connecting -> Pair(Icons.Default.BluetoothSearching, Color(0xFFFFA000))
        is MyBluetoothState.Connected -> Pair(Icons.Default.BluetoothConnected, Color.Green)
        is MyBluetoothState.Printing -> Pair(Icons.Default.Print, Color.Blue)
        is MyBluetoothState.Error -> Pair(Icons.Default.Error, Color.Red)
        is MyBluetoothState.Disconnected -> Pair(Icons.Default.BluetoothDisabled, Color.Gray)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = state.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DevicesList(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        items(devices) { device ->
            DeviceItem(
                device = device,
                onClick = { onDeviceSelected(device) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val deviceName = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name ?: "Dispositivo Bluetooth"
            } else {
                "Dispositivo (permisos requeridos)"
            }
        } else {
            device.name ?: "Dispositivo Bluetooth"
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = device.name ?: "Dispositivo desconocido",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ConnectedDeviceUI(
    device: BluetoothDevice,
    onPrint: () -> Unit,
    onDisconnect: () -> Unit
) {
    val deviceName = try {
        device.name ?: "Dispositivo desconocido"
    } catch (e: SecurityException) {
        "Dispositivo Conectado"
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Conectado a:",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        ActionButton(
            text = "Imprimir Nota",
            icon = Icons.Default.Print,
            onClick = onPrint
        )

        OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Desconectar")
        }
    }
}

@Composable
private fun ErrorStateUI(
    message: String,
    retryAction: (() -> Unit)?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        retryAction?.let { action ->
            ActionButton(
                text = "Reintentar",
                icon = Icons.Default.Refresh,
                onClick = action
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

@Composable
fun LoadingButton(
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {},
        enabled = false,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
    }
}