package com.example.fibo.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.fibo.model.IOperation
import com.example.fibo.model.IProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

sealed class ProductSearchState {
    object Idle : ProductSearchState()
    data class Loading(val query: String) : ProductSearchState()
    data class Empty(val query: String) : ProductSearchState()
    data class Error(val message: String, val query: String) : ProductSearchState()
    data class Success(val products: List<IProduct>, val query: String) : ProductSearchState()

    val currentQuery: String? get() = when (this) {
        is Idle -> null
        is Loading -> query
        is Empty -> query
        is Error -> query
        is Success -> query
    }
}
// 5. Definir los estados UI para el diálogo del PDF
sealed class PdfDialogUiState {
    object Initial : PdfDialogUiState()
    object Loading : PdfDialogUiState()
    data class Success(
        val operation: IOperation,
        val printers: List<BluetoothDevice> = emptyList(),
        val selectedPrinter: BluetoothDevice? = null
    ) : PdfDialogUiState()
    data class Error(val message: String) : PdfDialogUiState()
    object ScanningPrinters : PdfDialogUiState()
    data class PrintersFound(val printers: List<BluetoothDevice>) : PdfDialogUiState()
    object BluetoothDisabled : PdfDialogUiState()
    object Printing : PdfDialogUiState()
    object PrintComplete : PdfDialogUiState()
}
// Estado adicional para manejar la generación del PDF
sealed class PdfGenerationState {
    object Loading : PdfGenerationState()
    object Success : PdfGenerationState()
    class Error(val message: String) : PdfGenerationState()
}

sealed class QuotationState {
    object WaitingForUser : QuotationState()
    object Loading : QuotationState()
    data class Success(val data: List<IOperation>) : QuotationState()
    data class Error(val message: String) : QuotationState()
}
sealed class NoteOfSaleState {
    object WaitingForUser : NoteOfSaleState()
    object Loading : NoteOfSaleState()
    data class Success(val data: List<IOperation>) : NoteOfSaleState()
    data class Error(val message: String) : NoteOfSaleState()
}
sealed class OperationState {
    object Loading : OperationState()
    data class Success(val operation: IOperation) : OperationState()
    data class Error(val message: String) : OperationState()
}
// Define Bluetooth states
sealed class BluetoothState {
    object Disabled : BluetoothState()
    object Enabled : BluetoothState()
    object Scanning : BluetoothState()
    object DevicesFound : BluetoothState()
    object Connected : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}

// Define PDF states
sealed class PdfState {
    object Loading : PdfState()
    data class Success(val file: File) : PdfState()
    data class Error(val message: String) : PdfState()
}
// BluetoothUtils.kt
suspend fun downloadAndSavePdf(context: Context, url: String, fileName: String): File {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/pdf")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to download PDF: ${response.code}")
        }

        val body = response.body?.bytes() ?: throw IOException("Empty PDF response")
        val file = File(context.cacheDir, fileName)
        file.writeBytes(body)
        file
    }
}
//---bluetooth---
sealed class MyBluetoothState {
    object Disabled : MyBluetoothState() {
        override fun toString() = "Bluetooth desactivado"
    }

    object Enabling : MyBluetoothState() {
        override fun toString() = "Activando Bluetooth..."
    }

    object Enabled : MyBluetoothState() {
        override fun toString() = "Bluetooth activado"
    }

    object Scanning : MyBluetoothState() {
        override fun toString() = "Buscando dispositivos..."
    }

    data class DevicesFound(val devices: List<BluetoothDevice>) : MyBluetoothState() {
        override fun toString() = "${devices.size} dispositivos encontrados"
    }

    data class Connecting(val device: BluetoothDevice) : MyBluetoothState() {
        override fun toString() = try {
            "Conectando a ${device.name ?: "Dispositivo Bluetooth"}..."
        } catch (e: SecurityException) {
            "Conectando a dispositivo Bluetooth..."
        }
    }

    data class Connected(val device: BluetoothDevice) : MyBluetoothState() {
        override fun toString() = try {
            "Conectado a ${device.name ?: "Dispositivo Bluetooth"}"
        } catch (e: SecurityException) {
            "Conectado a dispositivo Bluetooth"
        }
    }

    data class Printing(val device: BluetoothDevice) : MyBluetoothState() {
        override fun toString() = try {
            "Imprimiendo en ${device.name ?: "Dispositivo Bluetooth"}..."
        } catch (e: SecurityException) {
            "Imprimiendo en dispositivo Bluetooth..."
        }
    }

    data class Error(val message: String, val retryAction: (() -> Unit)? = null) : MyBluetoothState() {
        override fun toString() = "Error: $message"
    }

    object Disconnected : MyBluetoothState() {
        override fun toString() = "Desconectado"
    }
}