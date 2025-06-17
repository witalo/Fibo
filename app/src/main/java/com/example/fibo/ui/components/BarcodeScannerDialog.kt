package com.example.fibo.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var isScanning by remember { mutableStateOf(true) }
    var lastScannedCode by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    CameraPreview(
                        onBarcodeDetected = { barcode ->
                            if (isScanning && barcode != lastScannedCode) {
                                isScanning = false
                                lastScannedCode = barcode
                                onBarcodeDetected(barcode)
                                onDismiss()
                            }
                        }
                    )

                    // Overlay con área de escaneo
                    ScannerOverlay()

                    // Línea de escaneo animada
                    AnimatedScannerLine()

                    // Texto de instrucciones
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Coloque el código de barras dentro del área",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El escaneo es automático",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    // Solicitar permiso de cámara
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Se requiere permiso de cámara para escanear códigos de barras",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { launcher.launch(Manifest.permission.CAMERA) }
                        ) {
                            Text("Conceder permiso")
                        }
                    }
                }

                // Botón de cerrar
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
                        it.setAnalyzer(executor, BarcodeAnalyzer { barcode ->
                            onBarcodeDetected(barcode)
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
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
private fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanAreaSize = ComposeSize(size.width * 0.8f, size.height * 0.3f)
        val topLeft = Offset(
            (size.width - scanAreaSize.width) / 2,
            (size.height - scanAreaSize.height) / 2
        )

        // Área oscura alrededor del área de escaneo
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size
        )

        // Área de escaneo transparente
        drawRoundRect(
            color = Color.Transparent,
            topLeft = topLeft,
            size = scanAreaSize,
            cornerRadius = CornerRadius(16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Marco del área de escaneo
        drawRoundRect(
            color = Color.Red,
            topLeft = topLeft,
            size = scanAreaSize,
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // Esquinas reforzadas
        drawScannerCorners(topLeft, scanAreaSize)
    }
}

private fun DrawScope.drawScannerCorners(topLeft: Offset, scanSize: ComposeSize) {
    val cornerLength = 30.dp.toPx()
    val strokeWidth = 5.dp.toPx()

    // Esquina superior izquierda
    drawLine(
        color = Color.Red,
        start = topLeft,
        end = Offset(topLeft.x + cornerLength, topLeft.y),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = topLeft,
        end = Offset(topLeft.x, topLeft.y + cornerLength),
        strokeWidth = strokeWidth
    )

    // Esquina superior derecha
    drawLine(
        color = Color.Red,
        start = Offset(topLeft.x + scanSize.width, topLeft.y),
        end = Offset(topLeft.x + scanSize.width - cornerLength, topLeft.y),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = Offset(topLeft.x + scanSize.width, topLeft.y),
        end = Offset(topLeft.x + scanSize.width, topLeft.y + cornerLength),
        strokeWidth = strokeWidth
    )

    // Esquina inferior izquierda
    drawLine(
        color = Color.Red,
        start = Offset(topLeft.x, topLeft.y + scanSize.height),
        end = Offset(topLeft.x + cornerLength, topLeft.y + scanSize.height),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = Offset(topLeft.x, topLeft.y + scanSize.height),
        end = Offset(topLeft.x, topLeft.y + scanSize.height - cornerLength),
        strokeWidth = strokeWidth
    )

    // Esquina inferior derecha
    drawLine(
        color = Color.Red,
        start = Offset(topLeft.x + scanSize.width, topLeft.y + scanSize.height),
        end = Offset(topLeft.x + scanSize.width - cornerLength, topLeft.y + scanSize.height),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = Offset(topLeft.x + scanSize.width, topLeft.y + scanSize.height),
        end = Offset(topLeft.x + scanSize.width, topLeft.y + scanSize.height - cornerLength),
        strokeWidth = strokeWidth
    )
}

@Composable
private fun AnimatedScannerLine() {
    val infiniteTransition = rememberInfiniteTransition()
    val linePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanAreaHeight = size.height * 0.3f
        val scanAreaTop = (size.height - scanAreaHeight) / 2
        val lineY = scanAreaTop + (scanAreaHeight * linePosition)
        val scanAreaWidth = size.width * 0.8f
        val scanAreaLeft = (size.width - scanAreaWidth) / 2

        // Línea de escaneo con gradiente
        drawLine(
            color = Color.Red.copy(alpha = 0.8f),
            start = Offset(scanAreaLeft + 20.dp.toPx(), lineY),
            end = Offset(scanAreaLeft + scanAreaWidth - 20.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )

        // Efecto de brillo
        drawLine(
            color = Color.Red.copy(alpha = 0.3f),
            start = Offset(scanAreaLeft + 20.dp.toPx(), lineY - 2.dp.toPx()),
            end = Offset(scanAreaLeft + scanAreaWidth - 20.dp.toPx(), lineY - 2.dp.toPx()),
            strokeWidth = 4.dp.toPx()
        )
        drawLine(
            color = Color.Red.copy(alpha = 0.3f),
            start = Offset(scanAreaLeft + 20.dp.toPx(), lineY + 2.dp.toPx()),
            end = Offset(scanAreaLeft + scanAreaWidth - 20.dp.toPx(), lineY + 2.dp.toPx()),
            strokeWidth = 4.dp.toPx()
        )
    }
}

private class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onBarcodeDetected(value)
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




@Composable
fun BarcodeIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(24.dp)) {
        drawBarcode(color)
    }
}

private fun DrawScope.drawBarcode(color: Color) {
    val barWidth = size.width / 20
    val barHeight = size.height * 0.7f
    val startY = (size.height - barHeight) / 2

    // Patrón de código de barras
    val pattern = listOf(1, 2, 1, 3, 2, 1, 2, 1, 3, 1, 2, 1)
    var currentX = size.width * 0.1f

    pattern.forEachIndexed { index, width ->
        if (index % 2 == 0) {
            // Dibujar barra
            for (i in 0 until width) {
                drawLine(
                    color = color,
                    start = Offset(currentX, startY),
                    end = Offset(currentX, startY + barHeight),
                    strokeWidth = barWidth * 0.8f,
                    cap = StrokeCap.Square
                )
                currentX += barWidth
            }
        } else {
            // Espacio
            currentX += barWidth * width
        }
    }
}

@Composable
fun QRCodeIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(24.dp)) {
        drawQRCode(color)
    }
}

private fun DrawScope.drawQRCode(color: Color) {
    val cornerSize = size.width * 0.3f
    val strokeWidth = 2.dp.toPx()
    val padding = 2.dp.toPx()

    // Esquinas del QR
    // Superior izquierda
    drawLine(
        color = color,
        start = Offset(padding, padding + cornerSize),
        end = Offset(padding, padding),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color,
        start = Offset(padding, padding),
        end = Offset(padding + cornerSize, padding),
        strokeWidth = strokeWidth
    )

    // Superior derecha
    drawLine(
        color = color,
        start = Offset(size.width - padding - cornerSize, padding),
        end = Offset(size.width - padding, padding),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color,
        start = Offset(size.width - padding, padding),
        end = Offset(size.width - padding, padding + cornerSize),
        strokeWidth = strokeWidth
    )

    // Inferior izquierda
    drawLine(
        color = color,
        start = Offset(padding, size.height - padding - cornerSize),
        end = Offset(padding, size.height - padding),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color,
        start = Offset(padding, size.height - padding),
        end = Offset(padding + cornerSize, size.height - padding),
        strokeWidth = strokeWidth
    )

    // Inferior derecha
    drawLine(
        color = color,
        start = Offset(size.width - padding - cornerSize, size.height - padding),
        end = Offset(size.width - padding, size.height - padding),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color,
        start = Offset(size.width - padding, size.height - padding - cornerSize),
        end = Offset(size.width - padding, size.height - padding),
        strokeWidth = strokeWidth
    )

    // Puntos centrales del QR
    val centerX = size.width / 2
    val centerY = size.height / 2
    val dotSize = 2.dp.toPx()

    drawCircle(
        color = color,
        radius = dotSize,
        center = Offset(centerX - 4.dp.toPx(), centerY - 4.dp.toPx())
    )
    drawCircle(
        color = color,
        radius = dotSize,
        center = Offset(centerX + 4.dp.toPx(), centerY - 4.dp.toPx())
    )
    drawCircle(
        color = color,
        radius = dotSize,
        center = Offset(centerX - 4.dp.toPx(), centerY + 4.dp.toPx())
    )
    drawCircle(
        color = color,
        radius = dotSize,
        center = Offset(centerX + 4.dp.toPx(), centerY + 4.dp.toPx())
    )
}
//@Composable
//fun BarcodeScannerDialog(
//    onDismiss: () -> Unit,
//    onBarcodeDetected: (String) -> Unit
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    var hasCameraPermission by remember {
//        mutableStateOf(
//            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//        )
//    }
//
//    val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        hasCameraPermission = isGranted
//    }
//
//    var isScanning by remember { mutableStateOf(true) }
//    var lastScannedCode by remember { mutableStateOf("") }
//
//    Dialog(
//        onDismissRequest = onDismiss,
//        properties = DialogProperties(
//            dismissOnBackPress = true,
//            dismissOnClickOutside = false,
//            usePlatformDefaultWidth = false
//        )
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            shape = RoundedCornerShape(16.dp),
//            color = Color.Black
//        ) {
//            Box(modifier = Modifier.fillMaxSize()) {
//                if (hasCameraPermission) {
//                    CameraPreview(
//                        onBarcodeDetected = { barcode ->
//                            if (isScanning && barcode != lastScannedCode) {
//                                isScanning = false
//                                lastScannedCode = barcode
//                                onBarcodeDetected(barcode)
//                                onDismiss()
//                            }
//                        }
//                    )
//
//                    // Overlay con área de escaneo
//                    ScannerOverlay()
//
//                    // Línea de escaneo animada
//                    AnimatedScannerLine()
//                } else {
//                    // Solicitar permiso de cámara
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(24.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.Center
//                    ) {
//                        Text(
//                            "Se requiere permiso de cámara para escanear códigos de barras",
//                            style = MaterialTheme.typography.bodyLarge,
//                            color = Color.White
//                        )
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Button(
//                            onClick = { launcher.launch(Manifest.permission.CAMERA) }
//                        ) {
//                            Text("Conceder permiso")
//                        }
//                    }
//                }
//
//                // Botón de cerrar
//                IconButton(
//                    onClick = onDismiss,
//                    modifier = Modifier
//                        .align(Alignment.TopEnd)
//                        .padding(8.dp)
//                        .background(
//                            Color.Black.copy(alpha = 0.5f),
//                            RoundedCornerShape(50)
//                        )
//                ) {
//                    Icon(
//                        Icons.Default.Close,
//                        contentDescription = "Cerrar",
//                        tint = Color.White
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun CameraPreview(
//    onBarcodeDetected: (String) -> Unit
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
//
//    AndroidView(
//        factory = { ctx ->
//            val previewView = PreviewView(ctx)
//            val executor = Executors.newSingleThreadExecutor()
//
//            cameraProviderFuture.addListener({
//                val cameraProvider = cameraProviderFuture.get()
//
//                val preview = Preview.Builder().build().also {
//                    it.setSurfaceProvider(previewView.surfaceProvider)
//                }
//
//                val imageAnalyzer = ImageAnalysis.Builder()
//                    .setTargetResolution(Size(1280, 720))
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    .build()
//                    .also {
//                        it.setAnalyzer(executor, BarcodeAnalyzer { barcode ->
//                            onBarcodeDetected(barcode)
//                        })
//                    }
//
//                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//                try {
//                    cameraProvider.unbindAll()
//                    cameraProvider.bindToLifecycle(
//                        lifecycleOwner,
//                        cameraSelector,
//                        preview,
//                        imageAnalyzer
//                    )
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }, ContextCompat.getMainExecutor(context))
//
//            previewView
//        },
//        modifier = Modifier.fillMaxSize()
//    )
//}
//
//@Composable
//private fun ScannerOverlay() {
//    Canvas(modifier = Modifier.fillMaxSize()) {
//        val scanAreaSize = ComposeSize(size.width * 0.8f, size.height * 0.3f)
//        val topLeft = Offset(
//            (size.width - scanAreaSize.width) / 2,
//            (size.height - scanAreaSize.height) / 2
//        )
//
//        // Área oscura alrededor del área de escaneo
//        drawRect(
//            color = Color.Black.copy(alpha = 0.6f),
//            size = size
//        )
//
//        // Área de escaneo transparente
//        drawRoundRect(
//            color = Color.Transparent,
//            topLeft = topLeft,
//            size = scanAreaSize,
//            cornerRadius = CornerRadius(16.dp.toPx()),
//            blendMode = BlendMode.Clear
//        )
//
//        // Marco del área de escaneo
//        drawRoundRect(
//            color = Color.Red,
//            topLeft = topLeft,
//            size = scanAreaSize,
//            cornerRadius = CornerRadius(16.dp.toPx()),
//            style = Stroke(width = 3.dp.toPx())
//        )
//
//        // Esquinas reforzadas
//        drawScannerCorners(topLeft, scanAreaSize)
//    }
//}
//
//private fun DrawScope.drawScannerCorners(topLeft: Offset, scanSize: ComposeSize) {
//    val cornerLength = 30.dp.toPx()
//    val strokeWidth = 5.dp.toPx()
//
//    // Esquina superior izquierda
//    drawLine(
//        color = Color.Red,
//        start = topLeft,
//        end = Offset(topLeft.x + cornerLength, topLeft.y),
//        strokeWidth = strokeWidth
//    )
//    drawLine(
//        color = Color.Red,
//        start = topLeft,
//        end = Offset(topLeft.x, topLeft.y + cornerLength),
//        strokeWidth = strokeWidth
//    )
//
//    // Esquina superior derecha
//    drawLine(
//        color = Color.Red,
//        start = Offset(topLeft.x + scanSize.width, topLeft.y),
//        end = Offset(topLeft.x + scanSize.width - cornerLength, topLeft.y),
//        strokeWidth = strokeWidth
//    )
//    drawLine(
//        color = Color.Red,
//        start = Offset(topLeft.x + scanSize.width, topLeft.y),
//        end = Offset(topLeft.x + scanSize.width, topLeft.y + cornerLength),
//        strokeWidth = strokeWidth
//    )
//
//    // Esquina inferior izquierda
//    drawLine(
//        color = Color.Red,
//        start = Offset(topLeft.x, topLeft.y + scanSize.height),
//        end = Offset(topLeft.x + cornerLength, topLeft.y + scanSize.height),
//        strokeWidth = strokeWidth
//    )
//    drawLine(
//        color = Color.Red,
//        start = Offset(topLeft.x, topLeft.y + scanSize.height),
//        end = Offset(topLeft.x, topLeft.y + scanSize.height - cornerLength),
//        strokeWidth = strokeWidth
//    )
//
//    // Esquina inferior derecha
//    drawLine(
//        color = Color.Red,
//        start = Offset(topLeft.x + scanSize.width, topLeft.y + scanSize.height),
//        end = Offset(topLeft.x + scanSize.width - cornerLength, topLeft.y + scanSize.height),
//        strokeWidth = strokeWidth
//    )
//    drawLine(
//        color = Color.Red,
//        start = Offset(topLeft.x + scanSize.width, topLeft.y + scanSize.height),
//        end = Offset(topLeft.x + scanSize.width, topLeft.y + scanSize.height - cornerLength),
//        strokeWidth = strokeWidth
//    )
//}
//
//@Composable
//private fun AnimatedScannerLine() {
//    val infiniteTransition = rememberInfiniteTransition()
//    val linePosition by infiniteTransition.animateFloat(
//        initialValue = 0f,
//        targetValue = 1f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(2000, easing = LinearEasing),
//            repeatMode = RepeatMode.Reverse
//        ), label = ""
//    )
//
//    Canvas(modifier = Modifier.fillMaxSize()) {
//        val scanAreaHeight = size.height * 0.3f
//        val scanAreaTop = (size.height - scanAreaHeight) / 2
//        val lineY = scanAreaTop + (scanAreaHeight * linePosition)
//        val scanAreaWidth = size.width * 0.8f
//        val scanAreaLeft = (size.width - scanAreaWidth) / 2
//
//        // Línea de escaneo con gradiente
//        drawLine(
//            color = Color.Red.copy(alpha = 0.8f),
//            start = Offset(scanAreaLeft + 20.dp.toPx(), lineY),
//            end = Offset(scanAreaLeft + scanAreaWidth - 20.dp.toPx(), lineY),
//            strokeWidth = 2.dp.toPx()
//        )
//
//        // Efecto de brillo
//        drawLine(
//            color = Color.Red.copy(alpha = 0.3f),
//            start = Offset(scanAreaLeft + 20.dp.toPx(), lineY - 2.dp.toPx()),
//            end = Offset(scanAreaLeft + scanAreaWidth - 20.dp.toPx(), lineY - 2.dp.toPx()),
//            strokeWidth = 4.dp.toPx()
//        )
//        drawLine(
//            color = Color.Red.copy(alpha = 0.3f),
//            start = Offset(scanAreaLeft + 20.dp.toPx(), lineY + 2.dp.toPx()),
//            end = Offset(scanAreaLeft + scanAreaWidth - 20.dp.toPx(), lineY + 2.dp.toPx()),
//            strokeWidth = 4.dp.toPx()
//        )
//    }
//}
//
//private class BarcodeAnalyzer(
//    private val onBarcodeDetected: (String) -> Unit
//) : ImageAnalysis.Analyzer {
//    private val scanner = BarcodeScanning.getClient()
//
//    @OptIn(ExperimentalGetImage::class)
//    override fun analyze(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image
//        if (mediaImage != null) {
//            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//            scanner.process(image)
//                .addOnSuccessListener { barcodes ->
//                    for (barcode in barcodes) {
//                        barcode.rawValue?.let { value ->
//                            onBarcodeDetected(value)
//                        }
//                    }
//                }
//                .addOnCompleteListener {
//                    imageProxy.close()
//                }
//        } else {
//            imageProxy.close()
//        }
//    }
//    }