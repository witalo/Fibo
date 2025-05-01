package com.example.fibo.ui.screens.quotation
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.model.IOperation
import com.example.fibo.utils.BluetoothState
import com.example.fibo.utils.PdfState
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.borders.SolidBorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class QuotationPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _pdfState = MutableStateFlow<PdfState>(PdfState.Loading)
    val pdfState: StateFlow<PdfState> = _pdfState.asStateFlow()

    private val _bluetoothState = MutableStateFlow<BluetoothState>(BluetoothState.Disabled)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private val _devicesList = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devicesList: StateFlow<List<BluetoothDevice>> = _devicesList.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice.asStateFlow()

    private var bluetoothSocket: BluetoothSocket? = null
    private var pdfFile: File? = null

    // UUID for Bluetooth Serial Port Profile (SPP)
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // BroadcastReceiver for Bluetooth device discovery
    private var receiver: BroadcastReceiver? = null

    /**
     * Check if Bluetooth permissions are granted
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Para versiones anteriores a Android 12
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }
    /**
     * Get paired devices with proper permission handling
     */
    private fun getPairedDevices(bluetoothAdapter: BluetoothAdapter): List<BluetoothDevice> {
        return try {
            if (hasBluetoothPermissions()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bluetoothAdapter.getBondedDevices().toList()
                } else {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.bondedDevices.toList()
                }
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
    /**
     * Start Bluetooth discovery with proper permission handling
     */
    private fun startBluetoothDiscovery(bluetoothAdapter: BluetoothAdapter): Boolean {
        return try {
            if (hasBluetoothPermissions()) {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
                bluetoothAdapter.startDiscovery()
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }
    /**
     * Cancel Bluetooth discovery with proper permission handling
     */
    private fun cancelBluetoothDiscovery(bluetoothAdapter: BluetoothAdapter): Boolean {
        return try {
            if (hasBluetoothPermissions() && bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }
    /**
     * Enable Bluetooth adapter
     */
    fun enableBluetooth(context: Context) {
        viewModelScope.launch {
            try {
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter

                if (bluetoothAdapter != null) {
                    if (bluetoothAdapter.isEnabled) {
                        _bluetoothState.value = BluetoothState.Enabled
                    } else {
                        if (hasBluetoothPermissions()) {
                            // Note: El encendido real se manejará mediante el activity result launcher
                            _bluetoothState.value = BluetoothState.Disabled
                        } else {
                            _bluetoothState.value = BluetoothState.Error("Permisos de Bluetooth requeridos")
                        }
                    }
                } else {
                    _bluetoothState.value =
                        BluetoothState.Error("Bluetooth no disponible en este dispositivo")
                }
            } catch (e: Exception) {
                _bluetoothState.value =
                    BluetoothState.Error("Error al activar Bluetooth: ${e.message}")
            }
        }
    }
    /**
     * Scan for available Bluetooth devices
     */
//    fun scanForDevices(context: Context) {
//        viewModelScope.launch {
//            try {
//
//                if (!hasBluetoothPermissions()) {
//                    _bluetoothState.value = BluetoothState.Error("Permisos de Bluetooth requeridos")
//                    return@launch
//                }
//
//                _bluetoothState.value = BluetoothState.Scanning
//                _devicesList.value = emptyList()
//
//                val bluetoothManager =
//                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//                val bluetoothAdapter = bluetoothManager.adapter
//
//                if (bluetoothAdapter != null) {
//                    // Verificar permisos nuevamente antes de proceder
//                    if (!hasBluetoothPermissions()) {
//                        _bluetoothState.value = BluetoothState.Error("Permisos de Bluetooth requeridos")
//                        return@launch
//                    }
//                    // Get already paired devices
//                    val pairedDevices = if (hasBluetoothPermissions()) {
//                        bluetoothAdapter.bondedDevices.toList()
//                    } else {
//                        emptySet<BluetoothDevice>().toList()
//                    }
//                    _devicesList.value = pairedDevices
//
//                    // Register for broadcasts when a device is discovered
//                    receiver = object : BroadcastReceiver() {
//                        override fun onReceive(context: Context, intent: Intent) {
//                            when (intent.action) {
//                                BluetoothDevice.ACTION_FOUND -> {
//                                    // Discovery has found a device
//                                    val device =
//                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
//                                            intent.getParcelableExtra(
//                                                BluetoothDevice.EXTRA_DEVICE,
//                                                BluetoothDevice::class.java
//                                            )
//                                        } else {
//                                            @Suppress("DEPRECATION")
//                                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                                        }
//
//                                    device?.let {
//                                        val currentDevices = _devicesList.value.toMutableList()
//                                        // Check if device is not already in the list
//                                        if (!currentDevices.any { d -> d.address == it.address }) {
//                                            currentDevices.add(it)
//                                            _devicesList.value = currentDevices
//                                        }
//                                    }
//                                }
//
//                                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                                    // Discovery finished
//                                    _bluetoothState.value = BluetoothState.DevicesFound
//                                }
//                            }
//                        }
//                    }
//
//                    // Register the BroadcastReceiver
//                    val filter = IntentFilter().apply {
//                        addAction(BluetoothDevice.ACTION_FOUND)
//                        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//                    }
//                    context.registerReceiver(receiver, filter)
//
//                    // Start discovery
//                    if (bluetoothAdapter.isDiscovering) {
//                        bluetoothAdapter.cancelDiscovery()
//                    }
//                    bluetoothAdapter.startDiscovery()
//
//                    // For better UX, we can show paired devices immediately
//                    if (pairedDevices.isNotEmpty()) {
//                        _bluetoothState.value = BluetoothState.DevicesFound
//                    }
//                } else {
//                    _bluetoothState.value = BluetoothState.Error("Bluetooth no disponible")
//                }
//            } catch (e: Exception) {
//                _bluetoothState.value =
//                    BluetoothState.Error("Error al buscar dispositivos: ${e.message}")
//            }
//        }
//    }
    /**
     * Scan for available Bluetooth devices with complete permission handling
     */
    fun scanForDevices(context: Context) {
        viewModelScope.launch {
            try {
                _bluetoothState.value = BluetoothState.Scanning
                _devicesList.value = emptyList()

                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter

                if (bluetoothAdapter == null) {
                    _bluetoothState.value = BluetoothState.Error("Bluetooth no disponible")
                    return@launch
                }

                // Obtener dispositivos emparejados con manejo de permisos
                val pairedDevices = getPairedDevices(bluetoothAdapter)
                _devicesList.value = pairedDevices

                // Registrar BroadcastReceiver
                receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            BluetoothDevice.ACTION_FOUND -> {
                                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                }
                                device?.let {
                                    val currentDevices = _devicesList.value.toMutableList()
                                    if (!currentDevices.any { d -> d.address == it.address }) {
                                        currentDevices.add(it)
                                        _devicesList.value = currentDevices
                                    }
                                }
                            }
                            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                                _bluetoothState.value = BluetoothState.DevicesFound
                            }
                        }
                    }
                }

                // Registrar el BroadcastReceiver
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(receiver, filter)

                // Iniciar descubrimiento con manejo de permisos
                if (!startBluetoothDiscovery(bluetoothAdapter)) {
                    _bluetoothState.value = BluetoothState.Error("Permisos de Bluetooth requeridos")
                }

                // Mostrar dispositivos emparejados inmediatamente
                if (pairedDevices.isNotEmpty()) {
                    _bluetoothState.value = BluetoothState.DevicesFound
                }
            } catch (e: SecurityException) {
                _bluetoothState.value = BluetoothState.Error("Permisos insuficientes: ${e.message}")
            } catch (e: Exception) {
                _bluetoothState.value = BluetoothState.Error("Error al buscar dispositivos: ${e.message}")
            }
        }
    }
    /**
     * Select a device from the list
     */
    fun selectDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            _selectedDevice.value = device
        }
    }

    /**
     * Update Bluetooth state
     */
    fun updateBluetoothState(state: BluetoothState) {
        viewModelScope.launch {
            _bluetoothState.value = state
        }
    }

    /**
     * Generate PDF from quotation data
     */
    fun generatePdf(quotation: IOperation, context: Context) {
        viewModelScope.launch {
            _pdfState.value = PdfState.Loading

            try {
                withContext(Dispatchers.IO) {
                    // Create PDF directory if it doesn't exist
                    val pdfDir = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                        "cotizaciones"
                    )
                    if (!pdfDir.exists()) {
                        pdfDir.mkdirs()
                    }

                    // Create the PDF file
                    val fileName = "cotizacion_${quotation.serial}_${quotation.correlative}.pdf"
                    val file = File(pdfDir, fileName)

                    // Generate PDF using iText
                    FileOutputStream(file).use { outputStream ->
                        val writer = PdfWriter(outputStream)
                        val pdf = PdfDocument(writer)
                        val document = Document(pdf)

                        // Add company information header
                        addCompanyHeader(document, quotation)

                        // Add customer information
                        addCustomerInfo(document, quotation)

                        // Add quotation details
                        addQuotationDetails(document, quotation)

                        // Add items table
                        addItemsTable(document, quotation)

                        // Add totals
                        addTotals(document, quotation)

                        // Add footer
                        addFooter(document)

                        document.close()
                    }

                    pdfFile = file
                    _pdfState.value = PdfState.Success(file)
                }
            } catch (e: Exception) {
                _pdfState.value = PdfState.Error("Error al generar el PDF: ${e.message}")
            }
        }
    }

    /**
     * Add company header to the PDF document
     */
    private fun addCompanyHeader(document: Document, quotation: IOperation) {
        val paragraph = Paragraph("COTIZACIÓN")
            .setFontSize(18f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
        document.add(paragraph)

        // Add document number
        document.add(
            Paragraph("${quotation.serial}-${quotation.correlative}")
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        // Add date
        document.add(
            Paragraph("Fecha: ${quotation.emitDate}")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.RIGHT)
        )

        document.add(Paragraph("\n"))
    }

    /**
     * Add customer information to the PDF document
     */
    private fun addCustomerInfo(document: Document, quotation: IOperation) {
        // Customer info table with 2 columns
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // Add client information
        table.addCell(createCell("Cliente:", true))
        table.addCell(createCell(quotation.client.names ?: ""))

        table.addCell(createCell("Documento:", true))
        table.addCell(createCell("${quotation.client.documentType ?: ""}: ${quotation.client.documentNumber ?: ""}"))

        table.addCell(createCell("Dirección:", true))
        table.addCell(createCell(quotation.client.address ?: ""))

        document.add(table)
        document.add(Paragraph("\n"))
    }

    /**
     * Add quotation details to the PDF document
     */
    private fun addQuotationDetails(document: Document, quotation: IOperation) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addCell(createCell("Tipo de Documento:", true))
        table.addCell(createCell(quotation.documentTypeReadable))

        table.addCell(createCell("Moneda:", true))
        table.addCell(createCell("SOLES"))

        table.addCell(createCell("Forma de Pago:", true))
        table.addCell(createCell("CONTADO"))

        document.add(table)
        document.add(Paragraph("\n"))
    }

    /**
     * Add items table to the PDF document
     */
    private fun addItemsTable(document: Document, quotation: IOperation) {
        // Headers
        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(10f, 40f, 15f, 15f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // Add header row
        itemsTable.addHeaderCell(createHeaderCell("Cant."))
        itemsTable.addHeaderCell(createHeaderCell("Descripción"))
        itemsTable.addHeaderCell(createHeaderCell("P. Unit"))
        itemsTable.addHeaderCell(createHeaderCell("Dscto"))
        itemsTable.addHeaderCell(createHeaderCell("Total"))

        // Add items
        // Note: In a real implementation, you would iterate through items from quotation
        // For this example, we'll add sample data
        itemsTable.addCell(createCell("1"))
        itemsTable.addCell(createCell("Producto o servicio de ejemplo"))
        itemsTable.addCell(createCell("S/ 100.00"))
        itemsTable.addCell(createCell("S/ 0.00"))
        itemsTable.addCell(createCell("S/ 100.00"))

        document.add(itemsTable)
        document.add(Paragraph("\n"))
    }

    /**
     * Add totals section to the PDF document
     */
    private fun addTotals(document: Document, quotation: IOperation) {
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // Add subtotal, IGV, and total rows
        totalsTable.addCell(createCell("Subtotal:", true).setTextAlignment(TextAlignment.RIGHT))
        totalsTable.addCell(createCell("S/ ${String.format("%.2f", quotation.totalTaxed)}"))

        totalsTable.addCell(createCell("IGV (18%):", true).setTextAlignment(TextAlignment.RIGHT))
        totalsTable.addCell(createCell("S/ ${String.format("%.2f", quotation.totalIgv)}"))

        totalsTable.addCell(createCell("Total:", true).setTextAlignment(TextAlignment.RIGHT))
        totalsTable.addCell(
            createCell("S/ ${String.format("%.2f", quotation.totalToPay)}")
                .setBold()
                .setFontSize(12f)
        )

        document.add(totalsTable)
    }

    /**
     * Add footer to the PDF document
     */
    private fun addFooter(document: Document) {
        document.add(Paragraph("\n"))
        document.add(
            Paragraph("Gracias por su preferencia")
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
        )
    }

    /**
     * Create a cell with the specified text
     */
    private fun createCell(text: String, isBold: Boolean = false): Cell {
        val cell = Cell().add(Paragraph(text))

        if (isBold) {
            cell.setBold()
        }

        return cell.setPadding(5f)
    }

    /**
     * Create a header cell with background color
     */
    private fun createHeaderCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setPadding(5f)
    }

    /**
     * Connect to the selected Bluetooth device and print the quotation
     */
//    fun connectAndPrint(context: Context, device: BluetoothDevice, operation: IOperation) {
//        viewModelScope.launch {
//            try {
//                val bluetoothManager =
//                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//                val bluetoothAdapter = bluetoothManager.adapter
//
//                // Cancel discovery because it's resource intensive
//                bluetoothAdapter.cancelDiscovery()
//
//                // Get a BluetoothSocket for connection with the given device
//                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
//
//                // Connect to the remote device
//                withContext(Dispatchers.IO) {
//                    bluetoothSocket?.connect()
//                }
//
//                // Get the output stream from the socket
//                val outputStream = bluetoothSocket?.outputStream
//
//                if (outputStream != null) {
//                    // Now send the print data
//                    sendPrintQuotation(outputStream, operation)
//
//                    // Update the state to Connected
//                    _bluetoothState.value = BluetoothState.Connected
//                } else {
//                    _bluetoothState.value =
//                        BluetoothState.Error("Error al obtener stream de salida")
//                }
//            } catch (e: IOException) {
//                // Unable to connect or IO error
//                _bluetoothState.value = BluetoothState.Error("Error de conexión: ${e.message}")
//                closeConnection()
//            } catch (e: Exception) {
//                // Other errors
//                _bluetoothState.value = BluetoothState.Error("Error al imprimir: ${e.message}")
//                closeConnection()
//            }
//        }
//    }
    /**
     * Connect to the selected Bluetooth device with complete permission handling
     */
    fun connectAndPrint(context: Context, device: BluetoothDevice, operation: IOperation) {
        viewModelScope.launch {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter

                if (bluetoothAdapter == null) {
                    _bluetoothState.value = BluetoothState.Error("Bluetooth no disponible")
                    return@launch
                }

                // Cancelar descubrimiento con manejo de permisos
                if (!cancelBluetoothDiscovery(bluetoothAdapter)) {
                    _bluetoothState.value = BluetoothState.Error("No se pudo cancelar el descubrimiento")
                    return@launch
                }

                // Crear socket con manejo de permisos
                bluetoothSocket = try {
                    if (hasBluetoothPermissions()) {
                        device.createRfcommSocketToServiceRecord(SPP_UUID)
                    } else {
                        null
                    }
                } catch (e: SecurityException) {
                    null
                }

                if (bluetoothSocket == null) {
                    _bluetoothState.value = BluetoothState.Error("Permisos insuficientes para conectar")
                    return@launch
                }

                // Conectar al dispositivo
                withContext(Dispatchers.IO) {
                    try {
                        bluetoothSocket?.connect()
                        _bluetoothState.value = BluetoothState.Connected

                        // Obtener el outputStream de manera segura
                        val outputStream = bluetoothSocket?.outputStream ?: throw IOException("No se pudo obtener el stream de salida")

                        // Llamar a la función de impresión
                        sendPrintQuotation(outputStream, operation)
                    } catch (e: SecurityException) {
                        throw SecurityException("Permisos insuficientes para conectar")
                    } catch (e: IOException) {
                        throw IOException("Error de conexión Bluetooth: ${e.message}")
                    }
                }
            } catch (e: SecurityException) {
                _bluetoothState.value = BluetoothState.Error("Permisos insuficientes: ${e.message}")
                closeConnection()
            } catch (e: IOException) {
                _bluetoothState.value = BluetoothState.Error("Error de conexión: ${e.message}")
                closeConnection()
            } catch (e: Exception) {
                _bluetoothState.value = BluetoothState.Error("Error al imprimir: ${e.message}")
                closeConnection()
            }
        }
    }
    /**
     * Send quotation data to the printer
     */
    private fun sendPrintQuotation(outputStream: OutputStream, operation: IOperation) {
        try {
            // This is a simplified example of sending ESC/POS commands to a thermal printer
            // In a real implementation, you would use a dedicated library like ESCPOS-ThermalPrinter

            // Reset printer
            outputStream.write(byteArrayOf(0x1B, 0x40))

            // Center align
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01))

            // Bold text
            outputStream.write(byteArrayOf(0x1B, 0x45, 0x01))

            // Print company name (example)
            outputStream.write("EMPRESA S.A.C.\n".toByteArray())

            // Cancel bold
            outputStream.write(byteArrayOf(0x1B, 0x45, 0x00))

            // Print document type
            outputStream.write("COTIZACIÓN\n".toByteArray())
            outputStream.write("${operation.serial}-${operation.correlative}\n".toByteArray())

            // Left align
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00))

            // Print date
            outputStream.write("Fecha: ${operation.emitDate}\n".toByteArray())

            // Print client info
            outputStream.write("Cliente: ${operation.client.names ?: ""}\n".toByteArray())
            outputStream.write("Doc: ${operation.client.documentNumber ?: ""}\n".toByteArray())

            // Print separator
            outputStream.write("--------------------------------\n".toByteArray())

            // Print items (this is simplified - in a real app, you'd iterate through items)
            outputStream.write("CANT  DESCRIPCIÓN        PRECIO\n".toByteArray())
            outputStream.write("--------------------------------\n".toByteArray())

            // In a real implementation, you would iterate through items here
            outputStream.write("1     Producto ejemplo    100.00\n".toByteArray())

            // Print separator
            outputStream.write("--------------------------------\n".toByteArray())

            // Print totals
            outputStream.write(
                "SUBTOTAL:             S/ ${
                    String.format(
                        "%.2f",
                        operation.totalTaxed
                    )
                }\n".toByteArray()
            )
            outputStream.write(
                "IGV (18%):            S/ ${
                    String.format(
                        "%.2f",
                        operation.totalIgv
                    )
                }\n".toByteArray()
            )
            outputStream.write(
                "TOTAL:                S/ ${
                    String.format(
                        "%.2f",
                        operation.totalToPay
                    )
                }\n".toByteArray()
            )

            // Center align
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01))

            // Print footer
            outputStream.write("\nGracias por su preferencia\n\n".toByteArray())

            // Cut paper (if supported by printer)
            outputStream.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))

            // Flush and close the outputStream
            outputStream.flush()
        } catch (e: IOException) {
            throw IOException("Error al enviar datos a la impresora: ${e.message}")
        }
    }

    /**
     * Close Bluetooth connection and clean up resources
     */
    private fun closeConnection() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
        } catch (e: IOException) {
            // Ignore close exceptions
        }
    }

    /**
     * Clean up resources when the dialog is dismissed
     */
    fun cleanup() {
        viewModelScope.launch {
            try {
                // Close Bluetooth connection
                closeConnection()

                // Unregister the BroadcastReceiver
                receiver?.let { receiver ->
                    try {
                        // Using local context to avoid memory leaks
                        // This will be handled by the Fragment/Activity that uses this ViewModel
                    } catch (e: IllegalArgumentException) {
                        // Receiver not registered, ignore
                    }
                }
                receiver = null

                // Reset state
                _selectedDevice.value = null
            } catch (e: Exception) {
                // Ignore cleanup exceptions
            }
        }
    }

    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}