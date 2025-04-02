package com.example.fibo.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fibo.R
import com.example.fibo.utils.DeviceUtils
import com.example.fibo.viewmodels.AuthViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Text
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.transformer.DefaultDecoderFactory
import com.example.fibo.utils.rememberCameraPermission
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.Size

@Composable
fun QrScannerScreen(
    onScanSuccess: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val (hasPermission, requestPermission) = rememberCameraPermission()
    val scanResult by authViewModel.scanResult.collectAsState()
    val error by authViewModel.error.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    var showScanner by remember { mutableStateOf(false) }
    // Color de fondo personalizado (puedes cambiarlo)
    val backgroundColor = Color(0xFF101010)  // Color gris oscuro
    // Contenedor principal con el fondo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo (solo visible cuando no está escaneando)
            if (!showScanner) {
                Image(
                    painter = painterResource(id = R.drawable.fibo), // Reemplaza con tu resource
                    contentDescription = "Fibo",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit
                )

                // Texto con estilo personalizado
                Text(
                    text = "Gracias por descargar Fibo, una App para Facturación Electrónica. Para empezar debes vincular una sesión escaneando un QR desde tu proveedor.",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp,
                        color = Color.White,  // Texto en color blanco
                        textAlign = TextAlign.Center  // Texto centrado
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            // Título (siempre visible)
            Text(
                text = "Escanea el código QR",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 16.sp,
            )

            if (showScanner && hasPermission) {
                // Contenedor del escáner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                ) {
                    QrScannerView(
                        onScanSuccess = { token ->
                            val deviceInfo = DeviceUtils.getDeviceDescription(context)
                            val deviceID = DeviceUtils.getDeviceId(context)
                            authViewModel.scanQr(token, deviceID, deviceInfo)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Botón de cancelar (solo visible cuando el escáner está activo)
                Button(
                    onClick = { showScanner = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "CANCELAR ESCANEO",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                // Botón de escanear (solo visible cuando el escáner no está activo)
                Button(
                    onClick = {
                        if (hasPermission) {
                            showScanner = true
                        } else {
                            requestPermission()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF424242)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF424242),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "ESCANEAR QR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Mensajes de estado
            if (!hasPermission && !showScanner) {
                Text(
                    text = "Se necesita permiso de cámara para escanear",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            error?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
    LaunchedEffect(scanResult) {
        if (scanResult?.data?.qrScan?.success == true) {
            onScanSuccess()
        }
    }
}
@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerView(
    onScanSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var isScanning by remember { mutableStateOf(true) }

    val previewView = remember { PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
    } }

    Box(modifier = modifier) {
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

        LaunchedEffect(Unit) {
            val cameraProvider = withContext(Dispatchers.IO) {
                cameraProviderFuture.get()
            }

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        if (isScanning) {
                            val image = InputImage.fromMediaImage(
                                imageProxy.image!!,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { barcode ->
                                        isScanning = false
                                        onScanSuccess(barcode)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch(exc: Exception) {
                Log.e("CameraPreview", "Error al iniciar cámara", exc)
            }
        }

        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Marco de escaneo centrado (igual que tu versión original)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp)
                .border(2.dp, Color.Red, RectangleShape)
                .background(Color.Transparent)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Enfoca el código QR dentro del marco",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(error, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF424242),
                contentColor = Color.White
            )
        ) {
            Text("Reintentar")
        }
    }
}
