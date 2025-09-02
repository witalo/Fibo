package com.example.fibo.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Environment
import android.util.Log
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_DOC
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_ID
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_IGV
import com.example.fibo.datastore.PreferencesManager.Companion.COMPANY_LOGO
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
import com.itextpdf.kernel.colors.ColorConstants
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
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.layout.properties.VerticalAlignment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import javax.inject.Inject
import javax.inject.Singleton

import com.itextpdf.layout.borders.Border
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
            logo = prefs[COMPANY_LOGO] ?: "",
            percentageIgv = prefs[COMPANY_IGV] ?: 18.0
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
        val fileName = "${operation.documentTypeReadable} ${operation.serial}-${operation.correlative}.pdf"
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(path, fileName)

        // 1. Primero calculamos el contenido para determinar la altura
        val contentHeight = estimateContentHeight(operation)
        val pageSize = PageSize(226f, contentHeight + 30) // 226px ≈ 80mm + margen

        val pdfWriter = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(pdfWriter)
        val document = Document(pdf, pageSize).apply {
            setMargins(8f, 8f, 8f, 8f) // Márgenes más ajustados
        }

        // --- SECCIÓN DE ENCABEZADO ---
        // Logo de la empresa
        try {
            if (company.logo != "") {
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

                val logo = Image(ImageDataFactory.create(roundedLogoBytes)).apply {
                    setWidth(80f) // Increased from 60f to 80f
                    setHorizontalAlignment(HorizontalAlignment.CENTER)
                    setMarginBottom(5f)
                }
                document.add(logo)
                document.add(Paragraph(company.businessName).apply {
                    setTextAlignment(TextAlignment.CENTER)
                    setBold()
                    setFontSize(12f)
                })
            } else {
                // Fallback to text if logo is null
                document.add(Paragraph(company.businessName).apply {
                    setTextAlignment(TextAlignment.CENTER)
                    setBold()
                    setFontSize(12f)
                })
            }
        } catch (e: Exception) {
            // Error handling - fallback to text
            document.add(Paragraph(company.businessName).apply {
                setTextAlignment(TextAlignment.CENTER)
                setBold()
                setFontSize(12f)
            })
        }
//        try {
//            if (company.logo != "") {
//                // Check if logo contains the data URL prefix and remove it
//                val logoData = company.logo
//                val base64Data = if (logoData.contains("data:image")) {
//                    logoData.substring(logoData.indexOf(",") + 1)
//                } else {
//                    logoData
//                }
//
//                // Decode base64 to bytes
//                val logoBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
//                val logo = Image(ImageDataFactory.create(logoBytes)).apply {
//                    setWidth(60f)
//                    setHorizontalAlignment(HorizontalAlignment.CENTER)
//                    setMarginBottom(5f)
//                }
//                document.add(logo)
//            } else {
//                // Fallback to text if logo is null
//                document.add(Paragraph(company.businessName).apply {
//                    setTextAlignment(TextAlignment.CENTER)
//                    setBold()
//                    setFontSize(12f)
//                })
//            }
//        } catch (e: Exception) {
//            // Error handling - fallback to text
//            document.add(Paragraph(company.businessName).apply {
//                setTextAlignment(TextAlignment.CENTER)
//                setBold()
//                setFontSize(12f)
//            })
//        }

        // Datos de la empresa
        document.add(Paragraph("RUC: ${company.doc}").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(10f)
        })
        document.add(Paragraph("DIRECCION: ${subsidiary.address}").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(10f)
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
//        document.add(Paragraph("\n"))

        // --- SECCIÓN DE CLIENTE ---
        document.add(Paragraph("DATOS DEL CLIENTE").apply {
            setBold()
            setFontSize(9f)
        })
        document.add(
            Paragraph("${formatDocumentType(operation.client?.documentType)}: ${operation.client?.documentNumber ?: ""}").apply {
                setBold()
                setFontSize(8f)
            }
        )
        document.add(Paragraph("DENOMINACIÓN: ${operation.client?.names ?: ""}").apply {
            setBold()
            setFontSize(8f)
        })
        if (!operation.client?.phone.isNullOrEmpty()) {
            document.add(Paragraph("TELEFONO: ${operation.client?.phone}").apply {
                setBold()
                setFontSize(8f)
            })
        }
        document.add(Paragraph("DIRECCION: ${operation.client?.address ?: ""}").apply {
            setBold()
            setFontSize(8f)
        })
        document.add(
            Paragraph("FECHA: ${"${operation.emitDate} ${operation.emitTime}".formatToDisplayDateTime()}").apply {
                setBold()
                setFontSize(8f)
            }
        )

        document.addDivider()
//        document.add(Paragraph("\n"))

        // --- TABLA DE PRODUCTOS ---
        val productTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 35f, 15f, 15f, 20f))).apply {
            setWidth(UnitValue.createPercentValue(100f))
            setFontSize(7f)
        }

        // Encabezados de tabla
        productTable.addHeaderCell(createHeaderCell("Cant").setTextAlignment(TextAlignment.CENTER))
        productTable.addHeaderCell(createHeaderCell("Descripción").setTextAlignment(TextAlignment.CENTER))
        productTable.addHeaderCell(createHeaderCell("P.Unit").setTextAlignment(TextAlignment.CENTER))
        productTable.addHeaderCell(createHeaderCell("Dscto").setTextAlignment(TextAlignment.CENTER))
        productTable.addHeaderCell(createHeaderCell("Importe").setTextAlignment(TextAlignment.CENTER))

        // Añadir productos
        operation.operationDetailSet.forEach { detail ->
            // Cantidad (alineada a la derecha)
            productTable.addCell(createCell(numberFormat.format(detail.quantity)).setTextAlignment(TextAlignment.RIGHT))

            // Descripción (alineada a la izquierda por defecto)
//            productTable.addCell(createCell(detail.tariff.productName.take(25) ))
            productTable.addCell(
                createCell(
                    if (detail.description != null && detail.description.isNotBlank()) {
                        "${detail.tariff.productName} (${detail.description})"
                    } else {
                        detail.tariff.productName.take(25)
                    }
                )
            )

            // Precio Unitario (alineado a la derecha)
            productTable.addCell(createCell(numberFormat.format(detail.unitPrice)).setTextAlignment(TextAlignment.RIGHT))

            // Descuento (alineado a la derecha)
            productTable.addCell(createCell(numberFormat.format(detail.totalDiscount)).setTextAlignment(TextAlignment.RIGHT))

            // Importe (alineado a la derecha)
            productTable.addCell(createCell(numberFormat.format(detail.totalAmount)).setTextAlignment(TextAlignment.RIGHT))
        }

        document.add(productTable)
        document.addDivider()

        // --- SECCIÓN QR + TOTALES MEJORADA ---
//        document.add(Paragraph("\n"))

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
                add(createQrCode("${operation.serial}|${operation.correlative}|${operation.totalAmount}|${operation.emitDate}|${operation.emitTime}|${operation.client?.documentNumber}|${operation.client?.names}"))
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
//        document.add(Paragraph("\n"))
        document.add(Paragraph("4 SOLUCIONES").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(7f)
        })
        document.add(Paragraph("https://www.tuf4ct.com").apply {
            setTextAlignment(TextAlignment.CENTER)
            setFontSize(6f)
            setItalic()
        })

        document.close()
        return file
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
    private fun createHeaderCell(text: String, alignment: TextAlignment = TextAlignment.CENTER): Cell {
        val azulHeader = DeviceRgb(0, 120, 215) // Define el color una sola vez

        return Cell().apply {
            add(Paragraph(text).apply {
                setFontSize(8f)
                setBold()
                setFontColor(ColorConstants.WHITE)
            })
            setTextAlignment(alignment)
            setVerticalAlignment(VerticalAlignment.MIDDLE)
            setBackgroundColor(azulHeader) // Fondo azul
            setPadding(1f) // Aumenté el padding para mejor legibilidad
            setBorder(SolidBorder(azulHeader, 1f)) // Borde del MISMO color azul
        }
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
        addTotalRow("DESCUENTO:", operation.totalDiscount)
        addTotalRow("EXONERADA:", operation.totalExonerated ?: 0.0)
        addTotalRow("INAFECTA:", operation.totalUnaffected ?: 0.0)
        addTotalRow("GRATUITA:", operation.totalFree ?: 0.0)
        addTotalRow("GRAVADA:", operation.totalTaxed)
        addTotalRow("IGV(${company.percentageIgv}%):", operation.totalIgv)
        addTotalRow("TOTAL:", operation.totalAmount, true)

        return table
    }
}
