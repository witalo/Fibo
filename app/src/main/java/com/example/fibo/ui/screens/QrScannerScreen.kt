package com.example.fibo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius

@Composable
fun QrScannerScreen(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Estados para controlar el flujo
    var isProcessing by remember { mutableStateOf(false) }
    var isScannerActive by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            isScannerActive = true
        }
    }

    LifecycleStartEffect(Unit) {
        onStopOrDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A2980),
                        Color(0xFF26D0CE)
                    )
                )
            )
    ) {
        when {
            !isScannerActive -> {
                // Pantalla inicial con botón para activar escáner
                WelcomeScreen(
                    onScanClick = {
                        if (hasCameraPermission) {
                            isScannerActive = true
                        } else {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }
            isProcessing -> {
                // Pantalla de procesamiento
                ProcessingScreen()
            }
            else -> {
                // Cámara activa con overlay mejorado
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(
                        onQrCodeScanned = { qrCode ->
                            if (!isProcessing) {
                                isProcessing = true
                                onQrCodeScanned(qrCode)
                            }
                        }
                    )
                    
                    // Overlay mejorado sin fondo oscuro completo
                    ImprovedScannerOverlay(
                        onBackClick = { isScannerActive = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo o ícono
        Card(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.2f)
            ),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Título
        Text(
            text = "Bienvenido",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtítulo
        Text(
            text = "Escanea el código QR para registrarte",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Botón principal
        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1A2980)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Escanear Código QR",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Texto de ayuda
        Text(
            text = "Asegúrate de tener el código QR a mano",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun ProcessingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A2980),
                        Color(0xFF26D0CE)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF1A2980),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Procesando autenticación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A2980)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Por favor espera...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, QrCodeAnalyzer { qrCode ->
                            onQrCodeScanned(qrCode)
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        context as androidx.lifecycle.LifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ImprovedScannerOverlay(
    onBackClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_line"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas para el fondo y el recuadro
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanAreaSize = size.width * 0.7f
            val scanAreaOffset = Offset(
                (size.width - scanAreaSize) / 2,
                (size.height - scanAreaSize) / 2
            )
            
            // Dibujar fondo semi-transparente con recorte
            drawPath(
                Path().apply {
                    addRect(
                        androidx.compose.ui.geometry.Rect(
                            offset = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(size.width, size.height)
                        )
                    )
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                offset = scanAreaOffset,
                                size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize)
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                        )
                    )
                },
                color = Color(0xFF1A2980).copy(alpha = 0.8f), // Azul oscuro semi-transparente
                blendMode = androidx.compose.ui.graphics.BlendMode.SrcOut
            )
            
            // Dibujar esquinas del recuadro con diseño mejorado
            drawModernScannerCorners(scanAreaOffset, scanAreaSize)
            
            // Dibujar línea de escaneo animada
            val lineY = scanAreaOffset.y + (scanAreaSize * scanLinePosition)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00FF44),
                        Color(0xFF00FF44),
                        Color.Transparent
                    )
                ),
                start = Offset(scanAreaOffset.x + 10, lineY),
                end = Offset(scanAreaOffset.x + scanAreaSize - 10, lineY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f), 0f
                )
            )
        }
        
        // Botón de retroceso
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .size(48.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }
        
        // Instrucciones en la parte inferior
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alinea el código QR dentro del recuadro",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawModernScannerCorners(offset: Offset, size: Float) {
    val cornerLength = 40.dp.toPx()
    val strokeWidth = 3.dp.toPx()
    val cornerRadius = 20.dp.toPx()
    
    val cornerColor = Color(0xFF00FF44)
    
    // Esquina superior izquierda
    drawLine(
        color = cornerColor,
        start = Offset(offset.x + cornerRadius, offset.y),
        end = Offset(offset.x + cornerLength, offset.y),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawLine(
        color = cornerColor,
        start = Offset(offset.x, offset.y + cornerRadius),
        end = Offset(offset.x, offset.y + cornerLength),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawArc(
        color = cornerColor,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = offset,
        size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
    
    // Esquina superior derecha
    drawLine(
        color = cornerColor,
        start = Offset(offset.x + size - cornerLength, offset.y),
        end = Offset(offset.x + size - cornerRadius, offset.y),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawLine(
        color = cornerColor,
        start = Offset(offset.x + size, offset.y + cornerRadius),
        end = Offset(offset.x + size, offset.y + cornerLength),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawArc(
        color = cornerColor,
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(offset.x + size - cornerRadius * 2, offset.y),
        size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
    
    // Esquina inferior izquierda
    drawLine(
        color = cornerColor,
        start = Offset(offset.x + cornerRadius, offset.y + size),
        end = Offset(offset.x + cornerLength, offset.y + size),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawLine(
        color = cornerColor,
        start = Offset(offset.x, offset.y + size - cornerRadius),
        end = Offset(offset.x, offset.y + size - cornerLength),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawArc(
        color = cornerColor,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(offset.x, offset.y + size - cornerRadius * 2),
        size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
    
    // Esquina inferior derecha
    drawLine(
        color = cornerColor,
        start = Offset(offset.x + size - cornerLength, offset.y + size),
        end = Offset(offset.x + size - cornerRadius, offset.y + size),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawLine(
        color = cornerColor,
        start = Offset(offset.x + size, offset.y + size - cornerRadius),
        end = Offset(offset.x + size, offset.y + size - cornerLength),
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawArc(
        color = cornerColor,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(offset.x + size - cornerRadius * 2, offset.y + size - cornerRadius * 2),
        size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
}

private class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onQrCodeScanned(value)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
