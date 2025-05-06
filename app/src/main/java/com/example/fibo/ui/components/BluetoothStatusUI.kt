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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Estado actual con indicador visual
        BluetoothStateIndicator(state)

        Spacer(modifier = Modifier.height(4.dp))
        Divider()
        Spacer(modifier = Modifier.height(4.dp))

        // Acciones según el estado
        when (state) {
            is MyBluetoothState.Disabled -> {
                InfoMessage(
                    message = "El Bluetooth está desactivado. Actívalo para conectarte a una impresora.",
                    icon = Icons.Default.BluetoothDisabled,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionButton(
                    text = "Activar Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    onClick = onEnableBluetooth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            is MyBluetoothState.Enabling -> {
                InfoMessage(
                    message = "Activando Bluetooth...",
                    icon = Icons.Default.BluetoothSearching,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LoadingButton(text = "Activando...")
            }

            is MyBluetoothState.Enabled -> {
                InfoMessage(
                    message = "Bluetooth activado. Busca dispositivos para conectarte.",
                    icon = Icons.Default.Bluetooth,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionButton(
                    text = "Buscar Dispositivos",
                    icon = Icons.Default.Search,
                    onClick = onScanDevices,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            is MyBluetoothState.Scanning -> {
                InfoMessage(
                    message = "Buscando dispositivos Bluetooth...",
                    icon = Icons.Default.Search,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LoadingButton(text = "Buscando dispositivos...")
            }

            is MyBluetoothState.DevicesFound -> {
                InfoMessage(
                    message = "Selecciona un dispositivo para conectar:",
                    icon = Icons.Default.Devices,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                DevicesList(
                    devices = state.devices,
                    onDeviceSelected = onConnectDevice
                )
            }

            is MyBluetoothState.Connecting -> {
                InfoMessage(
                    message = "Conectando al dispositivo...",
                    icon = Icons.Default.BluetoothConnected,
//                    icon = Icons.Default.BluetoothConnecting,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val deviceName = try {
                    state.device.name ?: "Dispositivo Bluetooth"
                } catch (e: SecurityException) {
                    "Dispositivo Bluetooth"
                }
                LoadingButton(text = "Conectando a $deviceName...")
            }

            is MyBluetoothState.Connected -> {
                ConnectedDeviceUI(
                    device = state.device,
                    onPrint = onPrint,
                    onDisconnect = onDisconnect
                )
            }

            is MyBluetoothState.Printing -> {
                InfoMessage(
                    message = "Imprimiendo documento...",
                    icon = Icons.Default.Print,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LoadingButton(text = "Imprimiendo...")
            }

            is MyBluetoothState.Error -> {
                ErrorStateUI(
                    message = state.message,
                    retryAction = state.retryAction
                )
            }

            is MyBluetoothState.Disconnected -> {
                InfoMessage(
                    message = "Desconectado. Conéctate a un dispositivo para imprimir.",
                    icon = Icons.Default.BluetoothDisabled,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionButton(
                    text = "Buscar Dispositivos",
                    icon = Icons.Default.Search,
                    onClick = onScanDevices,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun BluetoothStateIndicator(state: MyBluetoothState) {
    val (icon, color, stateText) = when (state) {
        is MyBluetoothState.Disabled -> Triple(
            Icons.Default.BluetoothDisabled,
            Color.Red,
            "Bluetooth desactivado"
        )
        is MyBluetoothState.Enabling -> Triple(
            Icons.Default.BluetoothSearching,
            Color(0xFFFFA000),
            "Activando Bluetooth..."
        )
        is MyBluetoothState.Enabled -> Triple(
            Icons.Default.Bluetooth,
            Color.Blue,
            "Bluetooth activado"
        )
        is MyBluetoothState.Scanning -> Triple(
            Icons.Default.Search,
            Color.Blue,
            "Buscando dispositivos..."
        )
        is MyBluetoothState.DevicesFound -> Triple(
            Icons.Default.BluetoothSearching,
            Color.Green,
            "${state.devices.size} dispositivos encontrados"
        )
        is MyBluetoothState.Connecting -> Triple(
            Icons.Default.BluetoothConnected,
            Color(0xFFFFA000),
            "Conectando..."
        )
        is MyBluetoothState.Connected -> Triple(
            Icons.Default.BluetoothConnected,
            Color.Green,
            "Conectado"
        )
        is MyBluetoothState.Printing -> Triple(
            Icons.Default.Print,
            Color.Blue,
            "Imprimiendo..."
        )
        is MyBluetoothState.Error -> Triple(
            Icons.Default.Error,
            Color.Red,
            "Error"
        )
        is MyBluetoothState.Disconnected -> Triple(
            Icons.Default.BluetoothDisabled,
            Color.Gray,
            "Desconectado"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stateText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DevicesList(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .padding(8.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    onClick = { onDeviceSelected(device) }
                )
            }
        }
    }
}
@Composable
private fun InfoMessage(message: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Conectado a:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Text(
                text = "Puedes imprimir ahora tu nota de salida",
                style = MaterialTheme.typography.bodyMedium
            )

            ActionButton(
                text = "Imprimir Nota",
                icon = Icons.Default.Print,
                onClick = onPrint,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary // Color del texto/icono
                )
            )

            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Desconectar")
            }
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
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(), // <- Asegúrate de que sea de M3
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = colors, // Pasa los colores correctamente
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
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