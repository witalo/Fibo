package com.example.fibo.utils

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

// Para obtener la fecha en formato "Y-MM-dd" (o "yyyy-MM-dd" si prefieres año calendario)
fun getCurrentFormattedDate(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    } else {
        val calendar = Calendar.getInstance()
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}

// Para obtener la hora en formato "H:mm:ss" (24 horas)
fun getCurrentFormattedTime(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } else {
        val calendar = Calendar.getInstance()
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
    }
}
fun getAffectationColor(typeAffectationId: Int): Color {
    return when (typeAffectationId) {
        1 -> Color(0xFF058F0C) // Gravado - verde claro
        2 -> Color(0xFF00BCD4) // Inafecto - amarillo claro
        3 -> Color(0xFF00BCD4) // Exonerado - celeste
        4 -> Color(0xFFFF9800) // Gratuita - rosado claro
        else -> Color.White
    }
}
fun getAffectationTypeShort(typeAffectationId: Int): String {
    return when (typeAffectationId) {
        1 -> "GRAV" // Gravada
        2 -> "EXON"  // Exonerada
        3 -> "INAF" // Inafecta
        4 -> "GRAT"  // Gratuita
        else -> "N/D"
    }
}
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Clase para convertir números a su representación en letras
 * Incluye soporte para diferentes monedas y gestión de decimales
 */
object NumberToLetterConverter {
    /**
     * Convierte un número a su representación en palabras
     * @param number El número a convertir
     * @param currency Código de moneda (PEN, USD, etc.)
     * @return Representación en palabras del número con su moneda
     */
    fun convertNumberToLetter(number: Double, currency: String): String {
        val formatter = NumberToWords()

        // Separar parte entera y decimal
        val intPart = number.toInt()
        val decPart = ((number - intPart) * 100).toInt()

        // Determinar el nombre de la moneda y sus unidades/centavos
        val currencyInfo = getCurrencyInfo(currency)

        // Obtener la parte entera en palabras
        val integerWords = formatter.convert(intPart)

        // Construir la representación completa
        return buildFinalString(intPart, integerWords, decPart, currencyInfo)
    }

    /**
     * Construye la cadena final con la representación en palabras
     */
    private fun buildFinalString(
        intPart: Int,
        integerWords: String,
        decPart: Int,
        currencyInfo: CurrencyInfo
    ): String {
        val sb = StringBuilder()

        // Parte entera
        sb.append(integerWords)

        // Agregar el nombre de la moneda en singular o plural según corresponda
        if (intPart == 1) {
            sb.append(" ${currencyInfo.singular}")
        } else {
            sb.append(" ${currencyInfo.plural}")
        }

        // Agregar la parte decimal
        if (decPart > 0) {
            sb.append(" CON ${decPart.toString().padStart(2, '0')}/100 ${currencyInfo.cents}")
        } else {
            sb.append(" CON 00/100 ${currencyInfo.cents}")
        }

        return sb.toString()
    }

    /**
     * Información sobre la moneda (singular, plural, centavos)
     */
    private data class CurrencyInfo(
        val singular: String,  // Nombre en singular
        val plural: String,    // Nombre en plural
        val cents: String      // Nombre de la centésima parte
    )

    /**
     * Obtiene información específica de la moneda
     */
    private fun getCurrencyInfo(currency: String): CurrencyInfo {
        return when(currency.uppercase()) {
            "PEN" -> CurrencyInfo("SOL", "SOLES", "CÉNTIMOS")
            "USD" -> CurrencyInfo("DÓLAR AMERICANO", "DÓLARES AMERICANOS", "CENTAVOS")
            "EUR" -> CurrencyInfo("EURO", "EUROS", "CÉNTIMOS")
            "GBP" -> CurrencyInfo("LIBRA ESTERLINA", "LIBRAS ESTERLINAS", "PENIQUES")
            "MXN" -> CurrencyInfo("PESO MEXICANO", "PESOS MEXICANOS", "CENTAVOS")
            "COP" -> CurrencyInfo("PESO COLOMBIANO", "PESOS COLOMBIANOS", "CENTAVOS")
            "CLP" -> CurrencyInfo("PESO CHILENO", "PESOS CHILENOS", "CENTAVOS")
            "ARS" -> CurrencyInfo("PESO ARGENTINO", "PESOS ARGENTINOS", "CENTAVOS")
            "BOB" -> CurrencyInfo("BOLIVIANO", "BOLIVIANOS", "CENTAVOS")
            "BRL" -> CurrencyInfo("REAL", "REALES", "CENTAVOS")
            "VES" -> CurrencyInfo("BOLÍVAR", "BOLÍVARES", "CÉNTIMOS")
            "PYG" -> CurrencyInfo("GUARANÍ", "GUARANÍES", "CÉNTIMOS")
            "UYU" -> CurrencyInfo("PESO URUGUAYO", "PESOS URUGUAYOS", "CENTÉSIMOS")
            else -> CurrencyInfo(currency, currency, "CÉNTIMOS")
        }
    }
}

/**
 * Clase para convertir números a palabras
 * Incluye soporte para números grandes y manejo de casos especiales
 */
class NumberToWords {
    private val UNIDADES = arrayOf("", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE")
    private val DECENAS = arrayOf("", "DIEZ", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA")
    private val CENTENAS = arrayOf("", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS")
    private val ESPECIALES = mapOf(
        11 to "ONCE", 12 to "DOCE", 13 to "TRECE", 14 to "CATORCE", 15 to "QUINCE",
        16 to "DIECISÉIS", 17 to "DIECISIETE", 18 to "DIECIOCHO", 19 to "DIECINUEVE",
        20 to "VEINTE", 21 to "VEINTIÚN", 22 to "VEINTIDÓS", 23 to "VEINTITRÉS",
        24 to "VEINTICUATRO", 25 to "VEINTICINCO", 26 to "VEINTISÉIS",
        27 to "VEINTISIETE", 28 to "VEINTIOCHO", 29 to "VEINTINUEVE"
    )

    /**
     * Convierte un número entero a su representación en palabras
     */
    fun convert(number: Int): String {
        if (number == 0) return "CERO"

        // Para números negativos
        if (number < 0) return "MENOS ${convert(-number)}"

        return when {
            number < 10 -> UNIDADES[number]
            number < 30 -> ESPECIALES[number] ?: ""  // Casos especiales del 11 al 29
            number < 100 -> {
                val decena = number / 10
                val unidad = number % 10
                if (unidad == 0) {
                    DECENAS[decena]
                } else {
                    "${DECENAS[decena]} Y ${UNIDADES[unidad]}"
                }
            }
            number == 100 -> "CIEN"
            number < 1000 -> {
                val centena = number / 100
                val resto = number % 100
                if (resto == 0) {
                    CENTENAS[centena]
                } else {
                    "${CENTENAS[centena]} ${convert(resto)}"
                }
            }
            number == 1000 -> "MIL"
            number < 1000000 -> {
                val miles = number / 1000
                val resto = number % 1000
                when {
                    miles == 1 -> {
                        if (resto == 0) "MIL" else "MIL ${convert(resto)}"
                    }
                    resto == 0 -> "${convert(miles)} MIL"
                    else -> "${convert(miles)} MIL ${convert(resto)}"
                }
            }
            number == 1000000 -> "UN MILLÓN"
            number < 1000000000 -> {
                val millones = number / 1000000
                val resto = number % 1000000
                when {
                    millones == 1 -> {
                        if (resto == 0) "UN MILLÓN" else "UN MILLÓN ${convert(resto)}"
                    }
                    resto == 0 -> "${convert(millones)} MILLONES"
                    else -> "${convert(millones)} MILLONES ${convert(resto)}"
                }
            }
            else -> {
                val millardos = number / 1000000000
                val resto = number % 1000000000
                when {
                    millardos == 1 -> {
                        if (resto == 0) "UN MIL MILLONES" else "UN MIL MILLONES ${convert(resto)}"
                    }
                    resto == 0 -> "${convert(millardos)} MIL MILLONES"
                    else -> "${convert(millardos)} MIL MILLONES ${convert(resto)}"
                }
            }
        }
    }
}