package com.example.fibo.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_DOC
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_ID
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_IGV
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_NAME
import com.example.fibo.datastore.PreferencesManager.Companion.SUBSIDIARY_ADDRESS
import com.example.fibo.datastore.PreferencesManager.Companion.SUBSIDIARY_ID
import com.example.fibo.datastore.PreferencesManager.Companion.SUBSIDIARY_NAME
import com.example.fibo.datastore.PreferencesManager.Companion.SUBSIDIARY_SERIAL
import com.example.fibo.datastore.PreferencesManager.Companion.SUBSIDIARY_TOKEN
import com.example.fibo.datastore.dataStore
import com.example.fibo.model.ICompany
import com.example.fibo.model.IOperation
import com.example.fibo.model.ISubsidiary
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.kernel.colors.DeviceGray
import com.itextpdf.layout.properties.VerticalAlignment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PdfGenerator @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    // Usa by lazy para cargar los datos una sola vez cuando se necesiten
    val company: ICompany by lazy { loadCompany() }
    val subsidiary: ISubsidiary by lazy { loadSubsidiary() }

    private fun loadCompany(): ICompany = runBlocking {
        // Opción 1: Usando currentUserData
        preferencesManager.currentUserData.first()?.company?.let {
            return@runBlocking it
        }

        // Opción 2: Lectura directa como fallback
        val prefs = preferencesManager.getRawPreferences()
        return@runBlocking ICompany(
            id = prefs[COMPANY_ID] ?: throw IllegalStateException("COMPANY_ID no encontrado"),
            doc = prefs[COMPANY_DOC] ?: "",
            businessName = prefs[COMPANY_NAME] ?: "",
            percentageIgv = prefs[COMPANY_IGV] ?: 0.18
        )
    }

    private fun loadSubsidiary(): ISubsidiary = runBlocking {
        preferencesManager.currentUserData.first()?.subsidiary?.let {
            return@runBlocking it
        }

        val prefs = preferencesManager.getRawPreferences()
        return@runBlocking ISubsidiary(
            id = prefs[SUBSIDIARY_ID]?.toInt() ?: throw IllegalStateException("SUBSIDIARY_ID no encontrado"),
            serial = prefs[SUBSIDIARY_SERIAL] ?: "",
            name = prefs[SUBSIDIARY_NAME] ?: "",
            address = prefs[SUBSIDIARY_ADDRESS] ?: "",
            token = prefs[SUBSIDIARY_TOKEN] ?: ""
        )
    }

    // Método para añadir líneas divisorias mejorado
    private fun Document.addDivider() {
        this.add(Paragraph("")
            .setBorderBottom(SolidBorder(DeviceGray.BLACK, 0.3f))
            .setPaddingBottom(3f)
        )
    }
    // En tu clase PdfGenerator, define numberFormat como propiedad
    private val numberFormat: NumberFormat by lazy {
        NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    fun generatePdf(context: Context, operation: IOperation): File {
        val fileName = "ticket_${operation.serial}-${operation.correlative}.pdf"
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(path, fileName)

        // 1. Primero calculamos el contenido para determinar la altura
        val contentHeight = estimateContentHeight(operation)
        val pageSize = PageSize(226f, contentHeight + 50) // 226px ≈ 80mm + margen

        val pdfWriter = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(pdfWriter)
        val document = Document(pdf, pageSize).apply {
            setMargins(8f, 8f, 8f, 8f) // Márgenes más ajustados
        }

        // --- SECCIÓN DE ENCABEZADO ---
        // Logo de la empresa
        try {
            val logoStream = context.assets.open("logo.png")
            val logoBytes = logoStream.readBytes()
            val logo = Image(ImageDataFactory.create(logoBytes)).apply {
                setWidth(60f)
                setHorizontalAlignment(HorizontalAlignment.CENTER)
                setMarginBottom(5f)
            }
            document.add(logo)
            logoStream.close()
        } catch (e: Exception) {
            document.add(Paragraph(company?.businessName ?: "MI EMPRESA").apply {
                setTextAlignment(TextAlignment.CENTER)
                setBold()
                setFontSize(12f)
            })
        }

        // Datos de la empresa
        document.add(Paragraph("RUC: ${company?.doc ?: "-"}").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(8f)
        })
        document.add(Paragraph("DIRECCION: ${subsidiary?.address ?: "-"}").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(8f)
        })

        // Tipo de documento
        document.add(Paragraph(
            "${operation.documentTypeReadable}\n" +
                    "${operation.serial}-${operation.correlative}"
        ).apply {
            setTextAlignment(TextAlignment.CENTER)
            setBold()
            setFontSize(10f)
            setMarginTop(5f)
        })

        document.addDivider()
        document.add(Paragraph("\n"))

        // --- SECCIÓN DE CLIENTE ---
        document.add(Paragraph("DATOS DEL CLIENTE").apply {
            setBold()
            setFontSize(9f)
        })
        document.add(
            Paragraph("${operation.client.documentType?.formatDocumentType() ?: "DOCUMENTO"}: ${operation.client.documentNumber ?: ""}")
        )
        document.add(Paragraph("DENOMINACIÓN: ${operation.client.names ?: ""}"))
        if (!operation.client.phone.isNullOrEmpty()) {
            document.add(Paragraph("TELEFONO: ${operation.client.phone}"))
        }
        document.add(Paragraph("DIRECCION: ${operation.client.address ?: ""}"))
        document.add(
            Paragraph("FECHA: ${"${operation.emitDate} ${operation.emitTime}".formatToDisplayDateTime()}")
        )

        document.addDivider()
//        document.add(Paragraph("\n"))

        // --- TABLA DE PRODUCTOS ---
        val productTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 35f, 15f, 15f, 20f))).apply {
            setWidth(UnitValue.createPercentValue(100f))
            setFontSize(7f)
        }

        // Encabezados de tabla
        productTable.addHeaderCell(createHeaderCell("Cant"))
        productTable.addHeaderCell(createHeaderCell("Descripción"))
        productTable.addHeaderCell(createHeaderCell("P.Unit"))
        productTable.addHeaderCell(createHeaderCell("Dscto"))
        productTable.addHeaderCell(createHeaderCell("Importe"))

        // Añadir productos
        operation.operationDetailSet.forEach { detail ->
            productTable.addCell(createCell(numberFormat.format(detail.quantity)))
            productTable.addCell(createCell(detail.tariff.productName.take(25))) // Limitar longitud
            productTable.addCell(createCell(numberFormat.format(detail.unitPrice)))
            productTable.addCell(createCell(numberFormat.format(detail.totalDiscount)))
            productTable.addCell(createCell(numberFormat.format(detail.totalAmount)))
        }

        document.add(productTable)
        document.addDivider()

        // --- SECCIÓN QR + TOTALES MEJORADA ---
        document.add(Paragraph("\n"))

        // Tabla de 2 columnas: QR a la izquierda, Totales a la derecha
        val qrTotalsTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f))).apply {
            setWidth(UnitValue.createPercentValue(100f))
            setFontSize(7f)
        }

        // Columna izquierda: QR
        qrTotalsTable.addCell(
            Cell().apply {
                setPadding(2f)
                setVerticalAlignment(VerticalAlignment.MIDDLE)
                add(createQrCode("${operation.serial}|${operation.correlative}|${operation.totalAmount}"))
            }
        )

        // Columna derecha: Totales detallados
        qrTotalsTable.addCell(
            Cell().apply {
                setPadding(2f)
                setTextAlignment(TextAlignment.RIGHT)
                add(createTotalsTable(operation, numberFormat))
            }
        )

        document.add(qrTotalsTable)

        // Pie de página compacto
        document.add(Paragraph("\n"))
        document.add(Paragraph("Gracias por su compra").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(7f)
        })
        document.add(Paragraph("www.misistema.com").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(6f)
            setItalic()
        })

        document.close()
        return file
    }
    fun String.formatDocumentType(): String {
        return when (this.removePrefix("A_")) {
            "6" -> "RUC"
            "1" -> "DNI"
            else -> this  // Opcional: devolver el valor original si no coincide
        }
    }
    fun String.formatToDisplayDateTime(): String {
        return try {
            // Asume que operation.emitDate está en formato "yyyy-MM-dd" y operation.emitTime en "HH:mm:ss"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MM-yyyy h:mm:ss a", Locale.getDefault())

            val dateTimeString = "$this" // Combina fecha y hora si están separadas
            val date = inputFormat.parse(dateTimeString)
            outputFormat.format(date)
        } catch (e: Exception) {
            "Fecha inválida" // Manejo de error si el formato no coincide
        }
    }

    // Funciones auxiliares
    private fun createHeaderCell(text: String): Cell {
        return Cell().add(Paragraph(text).setBold().setFontSize(7f))
    }

    private fun createCell(text: String): Cell {
        return Cell().add(Paragraph(text).setFontSize(7f))
    }

    private fun estimateContentHeight(operation: IOperation): Float {
        // Estimación básica basada en líneas de contenido
        val baseHeight = 800f // Altura base
        val lineHeight = 12f // Altura por línea
        val productLines = operation.operationDetailSet.size * 0.7f

        return baseHeight + (productLines * lineHeight)
    }

    private fun createQrCode(content: String): Image {
        return try {
            val qrSize = 70f // Tamaño para ticket de 80mm
            val sizePx = 150 // Tamaño interno en píxeles

            // Generar matriz de bits del QR
            val hints = mapOf(EncodeHintType.MARGIN to 1).toMutableMap()
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

            // Convertir a Bitmap Android
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            // Convertir a bytes PNG
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

            // Crear imagen para PDF
            Image(ImageDataFactory.create(stream.toByteArray())).apply {
                setWidth(qrSize)
                setAutoScale(true)
            }
        } catch (e: Exception) {
            // Crear QR de error simple
            createErrorQrPlaceholder()
        }
    }
    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        canvas.drawColor(Color.WHITE)

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (matrix.get(x, y)) {
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + 1).toFloat(),
                        (y + 1).toFloat(),
                        paint
                    )
                }
            }
        }

        return bitmap
    }

    private fun createErrorQrPlaceholder(): Image {
        val bitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
        }

        canvas.drawColor(Color.WHITE)
        canvas.drawText("QR Error", 50f, 75f, paint)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageData = ImageDataFactory.create(stream.toByteArray())

        return Image(imageData).apply {
            setWidth(70f)
            setAutoScale(true)
        }
    }

    private fun createTotalsTable(operation: IOperation, numberFormat: NumberFormat): Table {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).apply {
            setWidth(UnitValue.createPercentValue(100f))
            setFontSize(7f)
        }

        fun addTotalRow(label: String, value: Double, bold: Boolean = false) {
            // Celda izquierda (etiqueta)
            table.addCell(
                Cell().add(
                    Paragraph(label).apply {
                        if (bold) setBold()
                    }
                )
            )

            // Celda derecha (valor)
            table.addCell(
                Cell().add(
                    Paragraph(numberFormat.format(value))
                ).apply {
                    if (bold) setBold()
                    setTextAlignment(TextAlignment.RIGHT)
                }
            )
        }

        addTotalRow("GRAVADA:", operation.totalTaxed)
        addTotalRow("EXONERADA:", operation.totalExonerated ?: 0.0)
        addTotalRow("INAFECTA:", operation.totalUnaffected ?: 0.0)
        addTotalRow("GRATUITA:", operation.totalFree ?: 0.0)
        addTotalRow("DESC. GLOBAL:", operation.totalDiscount)
        addTotalRow("IGV:", operation.totalIgv)
        addTotalRow("TOTAL:", operation.totalAmount, true)

        return table
    }
}
//class PdfGenerator {
//    fun generatePdf(context: Context, operation: IOperation): File {
//        val fileName = "factura_${operation.serial}-${operation.correlative}.pdf"
//        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
//        val file = File(path, fileName)
//
//        // Crear el documento PDF
//        val pdfWriter = PdfWriter(FileOutputStream(file))
//        val pdf = PdfDocument(pdfWriter)
//        val document = Document(pdf)
//
//        // Formato para números
//        val numberFormat = NumberFormat.getNumberInstance(Locale("es", "PE"))
//        numberFormat.minimumFractionDigits = 2
//        numberFormat.maximumFractionDigits = 2
//
//        // Agregar encabezado
//        val headerParagraph = Paragraph("EMPRESA DEMO S.A.C.")
//            .setTextAlignment(TextAlignment.CENTER)
//            .setBold()
//            .setFontSize(14f)
//        document.add(headerParagraph)
//
//        // Tipo de documento
//        val docTypeParagraph = Paragraph(
//            "${operation.documentTypeReadable}\n" +
//                    "${operation.serial} - ${operation.correlative}"
//        )
//            .setTextAlignment(TextAlignment.CENTER)
//            .setBold()
//            .setFontSize(12f)
//        document.add(docTypeParagraph)
//
//        // Información de la empresa
//        document.add(Paragraph("RUC: 20123456789"))
//        document.add(Paragraph("Dirección: Av. Principal 123, Lima"))
//        document.add(Paragraph("Teléfono: 01-123-4567"))
//        document.add(Paragraph("Fecha: ${operation.emitDate} ${operation.emitTime}"))
//        document.add(Paragraph("\n"))
//
//        // Información del cliente
//        document.add(Paragraph("DATOS DEL CLIENTE").setBold())
//        document.add(Paragraph("Cliente: ${operation.client.names ?: ""}"))
//        document.add(Paragraph("${operation.client.documentType ?: ""}: ${operation.client.documentNumber ?: ""}"))
//        document.add(Paragraph("Dirección: ${operation.client.address ?: ""}"))
//        document.add(Paragraph("\n"))
//
//        // Tabla de productos
//        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 40f, 10f, 15f, 25f)))
//            .setWidth(UnitValue.createPercentValue(100f))
//
//        // Encabezados de la tabla
//        table.addHeaderCell(Cell().add(Paragraph("Cant.").setBold()))
//        table.addHeaderCell(Cell().add(Paragraph("Descripción").setBold()))
//        table.addHeaderCell(Cell().add(Paragraph("P. Unit").setBold()))
//        table.addHeaderCell(Cell().add(Paragraph("Dscto.").setBold()))
//        table.addHeaderCell(Cell().add(Paragraph("Importe").setBold()))
//
//        // Añadir productos
//        operation.operationDetailSet.forEach { detail ->
//            table.addCell(Cell().add(Paragraph(numberFormat.format(detail.quantity))))
//            table.addCell(Cell().add(Paragraph(detail.tariff.productName)))
//            table.addCell(Cell().add(Paragraph(numberFormat.format(detail.unitPrice))))
//            table.addCell(Cell().add(Paragraph(numberFormat.format(detail.totalDiscount))))
//            table.addCell(Cell().add(Paragraph(numberFormat.format(detail.totalAmount))))
//        }
//
//        document.add(table)
//        document.add(Paragraph("\n"))
//
//        // Resumen
//        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
//            .setWidth(UnitValue.createPercentValue(100f))
//
//        summaryTable.addCell(Cell().add(Paragraph("Sub Total").setBold()))
//        summaryTable.addCell(Cell().add(Paragraph(numberFormat.format(operation.totalTaxed))))
//
//        summaryTable.addCell(Cell().add(Paragraph("IGV").setBold()))
//        summaryTable.addCell(Cell().add(Paragraph(numberFormat.format(operation.totalIgv))))
//
//        summaryTable.addCell(Cell().add(Paragraph("Total").setBold()))
//        summaryTable.addCell(Cell().add(Paragraph(numberFormat.format(operation.totalAmount))))
//
//        document.add(summaryTable)
//
//        // Cerrar el documento
//        document.close()
//
//        return file
//    }
//}
//class PdfGenerator {
//    // Método infalible para añadir líneas divisorias
//    private fun Document.addSolidLine() {
//        this.add(
//            Paragraph(" ") // Espacio en blanco
//                .setBorderBottom(SolidBorder(DeviceGray.BLACK, 0.5f)) // Grosor 0.5
//                .setPaddingBottom(5f) // Espacio después de la línea
//        )
//    }
//    fun generatePdf(context: Context, operation: IOperation): File {
//        val fileName = "factura_${operation.serial}-${operation.correlative}.pdf"
//        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
//        val file = File(path, fileName)
//
//        // Configuración para ticket de 80mm (ancho reducido)
//        val pageSize = PageSize(226f, 1000f) // 226px ≈ 80mm, alto variable
//        val pdfWriter = PdfWriter(FileOutputStream(file))
//        val pdf = PdfDocument(pdfWriter)
//        val document = Document(pdf, pageSize).apply {
//            setMargins(10f, 10f, 10f, 10f) // Márgenes reducidos
//        }
//
//        // Formato para números (se mantiene igual)
//        val numberFormat = NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
//            minimumFractionDigits = 2
//            maximumFractionDigits = 2
//        }
//
//        // --- SECCIÓN DE ENCABEZADO (NUEVO CON LOGO) ---
//        // 1. Logo centrado
//        try {
//            val logoStream = context.assets.open("logo.png")
//            val logoBytes = logoStream.readBytes()
//            val logo = Image(ImageDataFactory.create(logoBytes)).apply {
//                setWidth(60f) // Tamaño adecuado para ticket
//                setHorizontalAlignment(HorizontalAlignment.CENTER)
//                setMarginBottom(5f)
//            }
//            document.add(logo)
//            logoStream.close()
//        } catch (e: Exception) {
//            document.add(Paragraph("EMPRESA DEMO S.A.C.").apply {
//                setTextAlignment(TextAlignment.CENTER)
//                setBold()
//                setFontSize(12f)
//            })
//        }
//
//        // 2. Datos de la empresa (centrados)
//        document.add(Paragraph("RUC: 20123456789").apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setFontSize(8f)
//        })
//
//        document.add(Paragraph("Av. Principal 123, Lima").apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setFontSize(8f)
//        })
//
//        // 3. Tipo de documento (se mantiene igual pero centrado)
//        document.add(Paragraph(
//            "${operation.documentTypeReadable}\n" +
//                    "${operation.serial} - ${operation.correlative}"
//        ).apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setBold()
//            setFontSize(10f)
//            setMarginTop(5f)
//        })
//
//        // Línea divisoria
//        document.addSolidLine()
//        document.add(Paragraph("\n"))
//
//        // --- SECCIÓN DE CLIENTE (ORDEN SOLICITADO) ---
//        // 1. RUC/DNI del cliente
//        document.add(Paragraph("${operation.client.documentType ?: ""}: ${operation.client.documentNumber ?: ""}"))
//
//        // 2. Nombre del cliente
//        document.add(Paragraph("Cliente: ${operation.client.names ?: ""}"))
//
//        // 3. Teléfono (si existe)
//        if (!operation.client.phone.isNullOrEmpty()) {
//            document.add(Paragraph("Teléfono: ${operation.client.phone}"))
//        }
//
//        // 4. Dirección
//        document.add(Paragraph("Dirección: ${operation.client.address ?: ""}"))
//
//        // 5. Fecha y hora
//        document.add(Paragraph("Fecha: ${operation.emitDate} ${operation.emitTime}"))
//
//        document.addSolidLine()
//        document.add(Paragraph("\n"))
//
//        // --- TABLA DE PRODUCTOS (AJUSTADA A 80mm) ---
//        // Fuente más pequeña para el ticket
//        val smallFontSize = 7f
//
//        val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 35f, 15f, 15f, 20f))).apply {
//            setWidth(UnitValue.createPercentValue(100f))
//            setFontSize(smallFontSize)
//        }
//
//        // Encabezados de tabla (más compactos)
//        fun createHeaderCell(text: String) = Cell().add(Paragraph(text).setBold().setFontSize(smallFontSize))
//
//        table.addHeaderCell(createHeaderCell("Cant"))
//        table.addHeaderCell(createHeaderCell("Descripción"))
//        table.addHeaderCell(createHeaderCell("P.Unit"))
//        table.addHeaderCell(createHeaderCell("Dscto"))
//        table.addHeaderCell(createHeaderCell("Importe"))
//
//        // Añadir productos
//        operation.operationDetailSet.forEach { detail ->
//            fun createCell(text: String) = Cell().add(Paragraph(text).setFontSize(smallFontSize))
//
//            table.addCell(createCell(numberFormat.format(detail.quantity)))
//            table.addCell(createCell(detail.tariff.productName.take(25))) // Limitar longitud
//            table.addCell(createCell(numberFormat.format(detail.unitPrice)))
//            table.addCell(createCell(numberFormat.format(detail.totalDiscount)))
//            table.addCell(createCell(numberFormat.format(detail.totalAmount)))
//        }
//
//        document.add(table)
//        document.addSolidLine()
//
//        // --- RESUMEN (MISMO FORMATO PERO COMPACTO) ---
//        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).apply {
//            setWidth(UnitValue.createPercentValue(100f))
//            setFontSize(smallFontSize)
//        }
//
//        fun createSummaryCell(text: String, bold: Boolean = false) =
//            Cell().add(Paragraph(text).apply { if (bold) setBold() })
//
//        summaryTable.addCell(createSummaryCell("Sub Total"))
//        summaryTable.addCell(createSummaryCell(numberFormat.format(operation.totalTaxed)))
//
//        summaryTable.addCell(createSummaryCell("IGV"))
//        summaryTable.addCell(createSummaryCell(numberFormat.format(operation.totalIgv)))
//
//        summaryTable.addCell(createSummaryCell("Total", true))
//        summaryTable.addCell(createSummaryCell(numberFormat.format(operation.totalAmount), true))
//
//        document.add(summaryTable)
//
//        // Pie de página
//        document.add(Paragraph("\n"))
//        document.add(Paragraph("Gracias por su preferencia").apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setFontSize(8f)
//        })
//
//        // Cerrar el documento (se mantiene igual)
//        document.close()
//
//        return file
//    }
//}


//class PdfGenerator {
//    // Método para añadir líneas divisorias mejorado
//    private fun Document.addDivider() {
//        this.add(Paragraph("")
//            .setBorderBottom(SolidBorder(DeviceGray.BLACK, 0.3f))
//            .setPaddingBottom(3f)
//        )
//    }
//    // En tu clase PdfGenerator, define numberFormat como propiedad
//    private val numberFormat: NumberFormat by lazy {
//        NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
//            minimumFractionDigits = 2
//            maximumFractionDigits = 2
//        }
//    }
//
//    fun generatePdf(context: Context, operation: IOperation): File {
//        val fileName = "ticket_${operation.serial}-${operation.correlative}.pdf"
//        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
//        val file = File(path, fileName)
//
//        // 1. Primero calculamos el contenido para determinar la altura
//        val contentHeight = estimateContentHeight(operation)
//        val pageSize = PageSize(226f, contentHeight + 50) // 226px ≈ 80mm + margen
//
//        val pdfWriter = PdfWriter(FileOutputStream(file))
//        val pdf = PdfDocument(pdfWriter)
//        val document = Document(pdf, pageSize).apply {
//            setMargins(8f, 8f, 8f, 8f) // Márgenes más ajustados
//        }
//
//        // --- SECCIÓN DE ENCABEZADO ---
//        // Logo de la empresa
//        try {
//            val logoStream = context.assets.open("logo.png")
//            val logoBytes = logoStream.readBytes()
//            val logo = Image(ImageDataFactory.create(logoBytes)).apply {
//                setWidth(60f)
//                setHorizontalAlignment(HorizontalAlignment.CENTER)
//                setMarginBottom(5f)
//            }
//            document.add(logo)
//            logoStream.close()
//        } catch (e: Exception) {
//            document.add(Paragraph("MI EMPRESA").apply {
//                setTextAlignment(TextAlignment.CENTER)
//                setBold()
//                setFontSize(12f)
//            })
//        }
//
//        // Datos de la empresa
//        document.add(Paragraph("RUC: 20123456789").apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setFontSize(8f)
//        })
//        document.add(Paragraph("Av. Principal 123, Lima").apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setFontSize(8f)
//        })
//
//        // Tipo de documento
//        document.add(Paragraph(
//            "${operation.documentTypeReadable}\n" +
//                    "${operation.serial}-${operation.correlative}"
//        ).apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setBold()
//            setFontSize(10f)
//            setMarginTop(5f)
//        })
//
//        document.addDivider()
//        document.add(Paragraph("\n"))
//
//        // --- SECCIÓN DE CLIENTE ---
//        document.add(Paragraph("DATOS DEL CLIENTE").apply {
//            setBold()
//            setFontSize(9f)
//        })
//        document.add(Paragraph("${operation.client.documentType ?: "DNI"}: ${operation.client.documentNumber ?: ""}"))
//        document.add(Paragraph("Nombre: ${operation.client.names ?: ""}"))
//        if (!operation.client.phone.isNullOrEmpty()) {
//            document.add(Paragraph("Teléfono: ${operation.client.phone}"))
//        }
//        document.add(Paragraph("Dirección: ${operation.client.address ?: ""}"))
//        document.add(Paragraph("Fecha: ${operation.emitDate} ${operation.emitTime}"))
//
//        document.addDivider()
//        document.add(Paragraph("\n"))
//
//        // --- TABLA DE PRODUCTOS ---
//        val productTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 35f, 15f, 15f, 20f))).apply {
//            setWidth(UnitValue.createPercentValue(100f))
//            setFontSize(7f)
//        }
//
//        // Encabezados de tabla
//        productTable.addHeaderCell(createHeaderCell("Cant"))
//        productTable.addHeaderCell(createHeaderCell("Descripción"))
//        productTable.addHeaderCell(createHeaderCell("P.Unit"))
//        productTable.addHeaderCell(createHeaderCell("Dscto"))
//        productTable.addHeaderCell(createHeaderCell("Importe"))
//
//        // Añadir productos
//        operation.operationDetailSet.forEach { detail ->
//            productTable.addCell(createCell(numberFormat.format(detail.quantity)))
//            productTable.addCell(createCell(detail.tariff.productName.take(25))) // Limitar longitud
//            productTable.addCell(createCell(numberFormat.format(detail.unitPrice)))
//            productTable.addCell(createCell(numberFormat.format(detail.totalDiscount)))
//            productTable.addCell(createCell(numberFormat.format(detail.totalAmount)))
//        }
//
//        document.add(productTable)
//        document.addDivider()
//
//        // --- SECCIÓN QR + TOTALES MEJORADA ---
//        document.add(Paragraph("\n"))
//
//        // Tabla de 2 columnas: QR a la izquierda, Totales a la derecha
//        val qrTotalsTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f))).apply {
//            setWidth(UnitValue.createPercentValue(100f))
//            setFontSize(7f)
//        }
//
//        // Columna izquierda: QR
//        qrTotalsTable.addCell(
//            Cell().apply {
//                setPadding(2f)
//                setVerticalAlignment(VerticalAlignment.MIDDLE)
//                add(createQrCode("${operation.serial}|${operation.correlative}|${operation.totalAmount}"))
//            }
//        )
//
//        // Columna derecha: Totales detallados
//        qrTotalsTable.addCell(
//            Cell().apply {
//                setPadding(2f)
//                setTextAlignment(TextAlignment.RIGHT)
//                add(createTotalsTable(operation, numberFormat))
//            }
//        )
//
//        document.add(qrTotalsTable)
//
//        // Pie de página compacto
//        document.add(Paragraph("\n"))
//        document.add(Paragraph("Gracias por su compra").apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setFontSize(7f)
//        })
//        document.add(Paragraph("www.misistema.com").apply {
//            setTextAlignment(TextAlignment.CENTER)
//            setFontSize(6f)
//            setItalic()
//        })
//
//        document.close()
//        return file
//    }
//    // Funciones auxiliares
//    private fun createHeaderCell(text: String): Cell {
//        return Cell().add(Paragraph(text).setBold().setFontSize(7f))
//    }
//
//    private fun createCell(text: String): Cell {
//        return Cell().add(Paragraph(text).setFontSize(7f))
//    }
//
//    private fun estimateContentHeight(operation: IOperation): Float {
//        // Estimación básica basada en líneas de contenido
//        val baseHeight = 800f // Altura base
//        val lineHeight = 12f // Altura por línea
//        val productLines = operation.operationDetailSet.size * 0.7f
//
//        return baseHeight + (productLines * lineHeight)
//    }
//
//    private fun createQrCode(content: String): Image {
//        return try {
//            val qrSize = 70f // Tamaño para ticket de 80mm
//            val sizePx = 150 // Tamaño interno en píxeles
//
//            // Generar matriz de bits del QR
//            val hints = mapOf(EncodeHintType.MARGIN to 1).toMutableMap()
//            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
//
//            // Convertir a Bitmap Android
//            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
//            for (x in 0 until sizePx) {
//                for (y in 0 until sizePx) {
//                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
//                }
//            }
//
//            // Convertir a bytes PNG
//            val stream = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//
//            // Crear imagen para PDF
//            Image(ImageDataFactory.create(stream.toByteArray())).apply {
//                setWidth(qrSize)
//                setAutoScale(true)
//            }
//        } catch (e: Exception) {
//            // Crear QR de error simple
//            createErrorQrPlaceholder()
//        }
//    }
//    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
//        val width = matrix.width
//        val height = matrix.height
//        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        val paint = Paint().apply {
//            color = Color.BLACK
//            style = Paint.Style.FILL
//        }
//
//        canvas.drawColor(Color.WHITE)
//
//        for (x in 0 until width) {
//            for (y in 0 until height) {
//                if (matrix.get(x, y)) {
//                    canvas.drawRect(
//                        x.toFloat(),
//                        y.toFloat(),
//                        (x + 1).toFloat(),
//                        (y + 1).toFloat(),
//                        paint
//                    )
//                }
//            }
//        }
//
//        return bitmap
//    }
//
//    private fun createErrorQrPlaceholder(): Image {
//        val bitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        val paint = Paint().apply {
//            color = Color.BLACK
//            textSize = 20f
//        }
//
//        canvas.drawColor(Color.WHITE)
//        canvas.drawText("QR Error", 50f, 75f, paint)
//
//        val stream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        val imageData = ImageDataFactory.create(stream.toByteArray())
//
//        return Image(imageData).apply {
//            setWidth(70f)
//            setAutoScale(true)
//        }
//    }
//
//    private fun createTotalsTable(operation: IOperation, numberFormat: NumberFormat): Table {
//        val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).apply {
//            setWidth(UnitValue.createPercentValue(100f))
//            setFontSize(7f)
//        }
//
//        fun addTotalRow(label: String, value: Double, bold: Boolean = false) {
//            // Celda izquierda (etiqueta)
//            table.addCell(
//                Cell().add(
//                    Paragraph(label).apply {
//                        if (bold) setBold()
//                    }
//                )
//            )
//
//            // Celda derecha (valor)
//            table.addCell(
//                Cell().add(
//                    Paragraph(numberFormat.format(value))
//                ).apply {
//                    if (bold) setBold()
//                    setTextAlignment(TextAlignment.RIGHT)
//                }
//            )
//        }
//
//        addTotalRow("GRAVADA:", operation.totalTaxed)
//        addTotalRow("EXONERADA:", operation.totalExonerated ?: 0.0)
//        addTotalRow("INAFECTA:", operation.totalUnaffected ?: 0.0)
//        addTotalRow("GRATUITA:", operation.totalFree ?: 0.0)
//        addTotalRow("DESC. GLOBAL:", operation.totalDiscount)
//        addTotalRow("IGV:", operation.totalIgv)
//        addTotalRow("TOTAL:", operation.totalAmount, true)
//
//        return table
//    }
//}