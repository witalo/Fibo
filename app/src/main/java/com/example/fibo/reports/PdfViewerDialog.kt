package com.example.fibo.reports

// 7. Importaciones necesarias para Bluetooth
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.utils.PdfDialogUiState
import com.example.fibo.utils.showToast
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PdfViewerDialog(
    isVisible: Boolean,
    operationId: Int,
    onDismiss: () -> Unit,
    viewModel: PdfDialogViewModel = hiltViewModel(),
) {
    val pdfGenerator = viewModel.pdfGenerator
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    // Check Bluetooth state on composition
    // Manejo seguro del estado Bluetooth
    var isBluetoothActive by remember { mutableStateOf(false) }
    // Estado para mantener la referencia al archivo PDF generado
    var pdfFile by remember { mutableStateOf<File?>(null) }

    // Efecto para verificar Bluetooth de forma segura
    LaunchedEffect(Unit) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            isBluetoothActive = bluetoothAdapter?.isEnabled == true

            // Cargar datos iniciales
            viewModel.fetchOperationById(operationId)

            // Verificación periódica más segura
            while (true) {
                try {
                    delay(2000) // Verificar cada 2 segundos
                    isBluetoothActive = bluetoothAdapter?.isEnabled == true
                } catch (e: Exception) {
                    Log.e("BluetoothCheck", "Error verificando estado", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error inicializando estado Bluetooth", e)
            isBluetoothActive = false
        }
    }

    // Generación segura del PDF
    LaunchedEffect(uiState) {
        if (uiState is PdfDialogUiState.Success) {
            try {
                val operation = (uiState as PdfDialogUiState.Success).operation
                pdfFile = withContext(Dispatchers.IO) {
                    try {
                        pdfGenerator.generatePdf(context, operation)
                    } catch (e: Exception) {
                        Log.e("PDF", "Error generando PDF", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("PDF", "Error en LaunchedEffect", e)
            }
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                ) {
                    // Barra superior con título y botón cerrar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vista previa PDF",
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar"
                            )
                        }
                    }
                    Divider()

                    // Contenido principal - Visualizador PDF y controles
                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState) {
                            is PdfDialogUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is PdfDialogUiState.Success -> {
                                // Mostrar PDF
                                if (pdfFile != null) {
                                    // Tu componente para mostrar PDF aquí
                                    AndroidView(
                                        factory = { ctx ->
                                            PDFView(ctx, null).apply {
                                                fromFile(pdfFile)
                                                    .enableSwipe(true)
                                                    .swipeHorizontal(false)
                                                    .enableDoubletap(true)
                                                    .defaultPage(0)
                                                    .load()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("Generando PDF...")
                                }
                            }
                            is PdfDialogUiState.Error -> {
                                Text("Error: ${(uiState as PdfDialogUiState.Error).message}")
                            }
                            else -> {
                                // Manejar otros estados
                                Text("Cargando datos...")
                            }
                        }
                    }

                    // Controles de impresión al final
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        PrintControlsSection(
                            viewModel = viewModel,
                            context = context,
                            pdfFile = pdfFile,
                            isBluetoothActive = isBluetoothActive,
                            onPrintSuccess = onDismiss
                        )
                    }
                }
            }
        }
    }
}