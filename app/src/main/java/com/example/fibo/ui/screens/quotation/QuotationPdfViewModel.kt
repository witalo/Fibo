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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ICompany
import com.example.fibo.model.IOperation
import com.example.fibo.model.ISubsidiary
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.BluetoothState
import com.example.fibo.utils.OperationState
import com.example.fibo.utils.PdfState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.properties.BorderRadius
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.VerticalAlignment
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.EnumMap
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class QuotationPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
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

    private val _quotationState = MutableStateFlow<OperationState>(OperationState.Loading)
    val quotationState: StateFlow<OperationState> = _quotationState

    // Estados para los datos necesarios
    private val _companyData = MutableStateFlow<ICompany?>(null)
    private val _subsidiaryData = MutableStateFlow<ISubsidiary?>(null)

    init {
        loadCompanyData()
    }

    private fun loadCompanyData() {
        viewModelScope.launch {
            preferencesManager.companyData.collect { company ->
                _companyData.value = company
            }
        }
        viewModelScope.launch {
            preferencesManager.subsidiaryData.collect { subsidiary ->
                _subsidiaryData.value = subsidiary
            }
        }
    }


    fun getQuotationById(quotationId: Int) {
        viewModelScope.launch {
            _quotationState.value = OperationState.Loading
            Log.d("PdfDialogViewModel", "Iniciando carga de operación $quotationId")

            try {
                val quotation = operationRepository.getOperationById(quotationId)
                _quotationState.value =OperationState.Success(quotation)
            } catch (e: ApolloException) {
                _quotationState.value = OperationState.Error(e.message ?: "Error consulta")
            } catch (e: Exception) {
                _quotationState.value = OperationState.Error(e.message ?: "Error desconocido")
            }
        }
    }

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
     * Scan for available Bluetooth devices with complete permission handling
     */
    fun scanForDevices(context: Context) {
        viewModelScope.launch {
            try {
                _bluetoothState.value = BluetoothState.Scanning

                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter

                if (bluetoothAdapter == null) {
                    _bluetoothState.value = BluetoothState.Error("Bluetooth no disponible")
                    return@launch
                }

                // Limpiar la lista de dispositivos antes de escanear
                _devicesList.value = emptyList()

                // Obtener dispositivos emparejados con manejo de permisos
                val pairedDevices = getPairedDevices(bluetoothAdapter)

                // Mostrar inmediatamente los dispositivos emparejados
                if (pairedDevices.isNotEmpty()) {
                    _devicesList.value = pairedDevices
                }

                // Desregistrar el receptor anterior si existe
                receiver?.let {
                    try {
                        context.unregisterReceiver(it)
                    } catch (e: IllegalArgumentException) {
                        // El receptor ya estaba desregistrado
                    }
                    receiver = null
                }

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
                                // Cambiar estado sólo si hay dispositivos o si ya había encontrado antes
                                if (_devicesList.value.isNotEmpty()) {
                                    _bluetoothState.value = BluetoothState.DevicesFound
                                } else {
                                    // Si no hay dispositivos, mostrar mensaje de error
                                    _bluetoothState.value = BluetoothState.Error("No se encontraron dispositivos")
                                }
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

                // Cambiar estado a dispositivos encontrados si ya hay emparejados
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
                    val pdfDir = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                        "cotizaciones"
                    ).apply { mkdirs() }

                    val fileName = "cotizacion_${quotation.serial}_${quotation.correlative}.pdf"
                    val file = File(pdfDir, fileName)

                    FileOutputStream(file).use { outputStream ->
                        val writer = PdfWriter(outputStream)
                        val pdf = PdfDocument(writer)
                        val document = Document(pdf, PageSize.A4).apply {
                            setMargins(40f, 36f, 60f, 36f)
                        }

                        // 1. Encabezado con logo, datos empresa y cuadro de cotización
                        addCustomHeader(document, quotation, context)

                        // 2. Datos del cliente
                        addCustomerInfo(document, quotation)

                        // 3. Detalles de la cotización
                        addQuotationDetails(document, quotation)

                        // 4. Tabla de items con bordes redondeados
                        addItemsTable(document, quotation)

                        // 5. Totales con diseño mejorado
                        addEnhancedTotals(document, quotation)
                        val qrString = "|${quotation.serial}-${quotation.correlative.toString().padStart(6, '0')}|${quotation.client.documentType}|${quotation.client.documentNumber}|${quotation.client.names}"
                        // 6. Pie de página con QR y términos
                        addFooterWithQR(document, context, qrString)

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

    private suspend fun addCustomHeader(document: Document, quotation: IOperation, context: Context) {
        // Esperar a que ambos datos estén disponibles
        val company = _companyData.filterNotNull().first()
        val subsidiary = _subsidiaryData.filterNotNull().first()
        // Tabla de 3 columnas para el encabezado
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 50f, 25f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)


        // Columna 1: Logo (debes reemplazar con tu imagen real)
        // Columna 1: Logo
        val logoCell = Cell().apply {
            try {
                if (company.logo.isNotEmpty()) {
                    // Check if logo contains the data URL prefix and remove it
                    val logoData = company.logo
                    val base64Data = if (logoData.contains("data:image")) {
                        logoData.substring(logoData.indexOf(",") + 1)
                    } else {
                        logoData
                    }

                    // Decode base64 to bytes
                    val logoBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)

                    // Create a bitmap from the logo bytes
                    val bitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)

                    // Create a rounded bitmap
                    val roundedBitmap = getRoundedBitmap(bitmap)

                    // Convert back to bytes for iText
                    val outputStream = ByteArrayOutputStream()
                    roundedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val roundedLogoBytes = outputStream.toByteArray()

                    // Create logo image for iText
                    val logo = Image(ImageDataFactory.create(roundedLogoBytes)).apply {
                        setWidth(80f)
                        setHorizontalAlignment(HorizontalAlignment.LEFT)
                        setMarginBottom(5f)
                    }

                    // Add the logo to the cell
                    add(logo)
                } else {
                    // Fallback to text if logo is empty
                    add(Paragraph("")
                        .setTextAlignment(TextAlignment.LEFT)
                        .setBold()
                        .setFontSize(12f))
                }
            } catch (e: Exception) {
                // Error handling - fallback to text
                add(Paragraph("")
                    .setTextAlignment(TextAlignment.LEFT)
                    .setBold()
                    .setFontSize(12f))
            }

            setBorder(Border.NO_BORDER)
            setPadding(5f)
        }
        headerTable.addCell(logoCell)

        // Columna 2: Datos de la empresa (centrado)
        val companyInfo = Paragraph()
            .add(Paragraph(company.businessName)
                .setFontSize(14f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER))
            .add(Paragraph(company.doc)
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER))
            .add(Paragraph(subsidiary.address)
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER))
//            .add(Paragraph("Tel: (04) 123-4567")
//                .setFontSize(10f)
//                .setTextAlignment(TextAlignment.CENTER))

        val companyCell = Cell().apply {
            add(companyInfo)
            setBorder(Border.NO_BORDER)
            setTextAlignment(TextAlignment.CENTER)
        }
        headerTable.addCell(companyCell)

        // Columna 3: Cuadro de cotización con fondo colorido
        val quoteBox = Table(1).apply {
            setWidth(UnitValue.createPercentValue(100f))
            setBackgroundColor(hexToDeviceRgb("#065FCC"))// Color AZUL
            setBorderRadius(BorderRadius(7f))

            val quoteText = Paragraph("COTIZACIÓN")
                .setFontSize(13f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(0f)
                .setMarginBottom(0f)

            val numberText = Paragraph("${quotation.serial}-${quotation.correlative.toString().padStart(6, '0')}")
                .setFontSize(12f)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(0f)
                .setMarginBottom(0f)

            addCell(Cell().add(Paragraph("RUC: 20123456789")
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(0f)
                .setMarginBottom(0f)
            ).setBorder(Border.NO_BORDER))
            addCell(Cell().add(quoteText).setBorder(Border.NO_BORDER))
            addCell(Cell().add(numberText).setBorder(Border.NO_BORDER))
        }

        val quoteCell = Cell().apply {
            add(quoteBox)
            setBorder(Border.NO_BORDER)
            setPaddingTop(5f)
        }
        headerTable.addCell(quoteCell)

        document.add(headerTable)
        document.add(Paragraph("\n"))
    }
    private fun getRoundedBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint()
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)
        val roundPx = width.coerceAtMost(height) / 2f

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = Color.BLACK
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return outputBitmap
    }
    private fun addCustomerInfo(document: Document, quotation: IOperation) {
        val customerTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 80f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(5f)

        // Estilo para las celdas de título
        val titleStyle = { text: String ->
            Paragraph(text).setBold().setFontSize(10f)
        }

        with(customerTable) {
            addCell(createCell(titleStyle("Cliente:"), true))
            addCell(createCell(Paragraph(quotation.client.names ?: "")))

            addCell(createCell(titleStyle("Documento:"), true))
            addCell(createCell(Paragraph("${formatDocumentType(quotation.client.documentType)}: ${quotation.client.documentNumber ?: ""}")))

            addCell(createCell(titleStyle("Dirección:"), true))
            addCell(createCell(Paragraph(quotation.client.address ?: "")))

            addCell(createCell(titleStyle("Email:"), true))
            addCell(createCell(Paragraph(quotation.client.email ?: "")))
        }

        document.add(customerTable)
//        document.add(Paragraph("\n"))
    }
    private fun formatDocumentType(documentType: String?): String {
        // Si es nulo, retornamos DOCUMENTO directamente
        if (documentType == null) return "DOCUMENTO"

        // Procesamos el código para quitar el prefijo A_ si existe
        val processedType = if (documentType.startsWith("A_")) {
            documentType.substring(2) // Quita los primeros 2 caracteres (A_)
        } else {
            documentType
        }

        // Procesamos el tipo de documento usando el valor limpio
        return when (processedType.uppercase()) {
            "01", "1" -> "DNI"
            "06", "6" -> "RUC"
            "07", "7", "CE" -> "CARNET DE EXTRANJERÍA"
            "08", "8", "PAS" -> "PASAPORTE"
            "09", "9", "CDI" -> "CÉDULA DE IDENTIDAD"
            "OTR" -> "OTROS"
            else -> processedType
        }
    }
    private fun addQuotationDetails(document: Document, quotation: IOperation) {
        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(5f)

        with(detailsTable) {
            addCell(createCell(Paragraph("Fecha Emisión:"), true))
            addCell(createCell(Paragraph(quotation.emitDate)))

            addCell(createCell(Paragraph("Moneda:"), true))
            addCell(createCell(Paragraph(quotation.currencyType)))

            addCell(createCell(Paragraph("Forma de Pago:"), true))
            addCell(createCell(Paragraph("CONTADO")))
        }

        document.add(detailsTable)
//        document.add(Paragraph("\n"))
    }

    private fun addItemsTable(document: Document, quotation: IOperation) {
        // Tabla con bordes redondeados
        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(8f, 42f, 12f, 12f, 12f, 14f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(5f)

        // Estilo para celdas de encabezado
        fun createHeaderCell(text: String): Cell {
            return Cell().apply {
                add(
                    Paragraph(text)
                        .setBold()
                        .setFontSize(9f)
                        .setFontColor(ColorConstants.WHITE) // Establece el texto en blanco
                )
                setBackgroundColor(hexToDeviceRgb("#065FCC")) // Fondo azul
                setTextAlignment(TextAlignment.CENTER)
                setPadding(5f)
            }
        }


        // Encabezados
        with(itemsTable) {
            addHeaderCell(createHeaderCell("Cant."))
            addHeaderCell(createHeaderCell("Descripción"))
            addHeaderCell(createHeaderCell("P. Unit."))
            addHeaderCell(createHeaderCell("Dscto"))
            addHeaderCell(createHeaderCell("IGV"))
            addHeaderCell(createHeaderCell("Total"))
        }

        // Items
        quotation.operationDetailSet.forEach { detail ->
            itemsTable.addCell(createRightAlignedCell(detail.quantity.toString()))
            itemsTable.addCell(createCell(Paragraph("${detail.tariff.productName} (${detail.description})")))
            itemsTable.addCell(createRightAlignedCell("S/ ${"%.2f".format(detail.unitPrice)}"))
            itemsTable.addCell(createRightAlignedCell("S/ ${"%.2f".format(detail.totalDiscount)}"))
            itemsTable.addCell(createRightAlignedCell("S/ ${"%.2f".format(detail.totalIgv)}"))
            itemsTable.addCell(createRightAlignedCell("S/ ${"%.2f".format(detail.totalAmount)}"))
        }

        document.add(itemsTable)
    }

    private fun addEnhancedTotals(document: Document, quotation: IOperation) {
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(5f)

        fun createTotalCell(label: String, value: String, isBold: Boolean = false): Cell {
            return Cell().apply {
                add(Paragraph(label).setTextAlignment(TextAlignment.RIGHT))
                setBorder(Border.NO_BORDER)
                setPadding(5f)
            }
        }

        fun createValueCell(value: String, isBold: Boolean = false): Cell {
            return Cell().apply {
                add(Paragraph(value).setTextAlignment(TextAlignment.RIGHT).run {
                    if (isBold) setBold() else this
                })
                setBorder(Border.NO_BORDER)
                setPadding(5f)
            }
        }

        with(totalsTable) {
            // Operaciones gravadas
            addCell(createTotalCell("Op. Gravadas:", ""))
            addCell(createValueCell("S/ ${"%.2f".format(quotation.totalTaxed)}"))

            // Descuento global
            if (quotation.discountGlobal > 0) {
                addCell(createTotalCell("Descuento Global:", ""))
                addCell(createValueCell("-S/ ${"%.2f".format(quotation.discountGlobal)}"))
            }

            // IGV
            addCell(createTotalCell("IGV (18%):", ""))
            addCell(createValueCell("S/ ${"%.2f".format(quotation.totalIgv)}"))

            // Total
            addCell(createTotalCell("TOTAL:", ""))
            addCell(createValueCell("S/ ${"%.2f".format(quotation.totalToPay)}", true))
        }

        document.add(totalsTable)
    }

    private fun addFooterWithQR(document: Document, context: Context, qrContent: String) {
        val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // Columna izquierda: Términos y condiciones
        val terms = Paragraph()
            .add(Paragraph("TÉRMINOS Y CONDICIONES")
                .setBold()
                .setFontSize(10f))
            .add(Paragraph("• Válido por 15 días"))
            .add(Paragraph("• Precios sujetos a cambio sin previo aviso"))
            .add(Paragraph("• Formas de pago: Efectivo, Transferencia"))
            .setFontSize(8f)

        footerTable.addCell(Cell().add(terms).setBorder(Border.NO_BORDER))

        // Columna derecha: Código QR generado
        val qrCell = Cell().apply {
            // Generar el código QR
            val qrBitmap = generateQRCode(qrContent)

            if (qrBitmap != null) {
                // Convertir bitmap a formato que iText pueda utilizar
                val stream = ByteArrayOutputStream()
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val qrImageData = stream.toByteArray()

                // Crear imagen con iText
                val qrImage = Image(ImageDataFactory.create(qrImageData))
                    .setWidth(100f)  // Ajusta el tamaño según necesites
                    .setHorizontalAlignment(HorizontalAlignment.RIGHT)

                add(qrImage)
                add(Paragraph("Escanea para más información")
                    .setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER))
            } else {
                // Fallback en caso de error
                add(Paragraph("ERROR AL GENERAR QR")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(8f))
            }

            setBorder(Border.NO_BORDER)
        }

        footerTable.addCell(qrCell)

        document.add(footerTable)
        document.add(Paragraph("\n"))

        // Firma
        val signature = Paragraph("__________________________\nResponsable: Nombre del Vendedor")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f)
            .setMarginTop(20f)

        document.add(signature)
    }
    private fun generateQRCode(content: String): Bitmap? {
        try {
            // Necesitas añadir la dependencia de ZXing a tu proyecto:
            // implementation 'com.google.zxing:core:3.4.1'

            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 2

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                300, // Ancho del QR
                300, // Alto del QR
                hints
            )

            // Crear bitmap a partir de la matriz de bits
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    // Funciones auxiliares mejoradas
    private fun createCell(paragraph: Paragraph, isBold: Boolean = false): Cell {
        return Cell().add(paragraph).apply {
            if (isBold) setBold()
            setPadding(5f)
        }
    }

    private fun createRightAlignedCell(text: String): Cell {
        return Cell().add(Paragraph(text).setTextAlignment(TextAlignment.RIGHT))
            .setPadding(5f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }
//   ------------------------------------------------------------------------------------------------
    fun hexToDeviceRgb(hexColor: String): DeviceRgb {
        val cleanHex = hexColor.replace("#", "")
        val colorInt = cleanHex.toLong(16).toInt()
        return DeviceRgb(
            (colorInt shr 16 and 0xFF) / 255f,
            (colorInt shr 8 and 0xFF) / 255f,
            (colorInt and 0xFF) / 255f
        )
    }
//   ------------------------------------------------------------------------------------------------
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
                receiver?.let {
                    try {
                        context.unregisterReceiver(it)
                    } catch (e: IllegalArgumentException) {
                        // Receptor ya desregistrado, ignorar
                    }
                }
                receiver = null

                // Reset state
                _selectedDevice.value = null
                _devicesList.value = emptyList()
                _bluetoothState.value = BluetoothState.Disabled
            } catch (e: Exception) {
                // Ignorar excepciones de limpieza
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
    fun resetAll() {
        viewModelScope.launch {
            cleanup()
            _selectedDevice.value = null
            _devicesList.value = emptyList()
            _bluetoothState.value = BluetoothState.Disabled
            _pdfState.value = PdfState.Loading
            pdfFile = null
        }
    }
}