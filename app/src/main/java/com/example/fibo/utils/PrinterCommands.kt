package com.example.fibo.utils

/**
 * Clase utilitaria que contiene comandos ESC/POS para impresoras térmicas
 * Optimizado para la impresora Advance ADV 7010n
 */
class PrinterCommands {
    companion object {
        // Comandos básicos ESC/POS
        val INIT = byteArrayOf(0x1B, 0x40)  // Initialize printer
        val FEED_LINE = byteArrayOf(0x0A)   // Line feed
        val FEED_PAPER_AND_CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x10)  // Feed paper and cut

        // Alineación de texto
        val TEXT_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val TEXT_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val TEXT_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)

        // Estilo de texto
        val TEXT_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        val TEXT_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        val TEXT_DOUBLE_HEIGHT_ON = byteArrayOf(0x1B, 0x21, 0x10)
        val TEXT_DOUBLE_HEIGHT_OFF = byteArrayOf(0x1B, 0x21, 0x00)
        val TEXT_UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01)
        val TEXT_UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00)

        // Tamaño de fuente
        val TEXT_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)  // Normal font
        val TEXT_LARGE_SIZE = byteArrayOf(0x1B, 0x21, 0x30)   // Double height and width

        /**
         * Convierte texto a bytes para enviar a la impresora
         */
        fun textToBytes(text: String): ByteArray {
            return text.toByteArray()
        }

        /**
         * Combina múltiples comandos en un solo byte array
         */
        fun combine(vararg commands: ByteArray): ByteArray {
            return commands.reduce { acc, bytes -> acc + bytes }
        }

        /**
         * Crea una línea divisoria
         */
        fun divider(length: Int = 32): ByteArray {
            return "-".repeat(length).plus("\n").toByteArray()
        }

        /**
         * Formatea una línea para impresión
         * @param text Texto a imprimir
         * @param align Alineación (L, C, R)
         */
        fun formatLine(text: String, align: Char = 'L'): ByteArray {
            val alignCommand = when (align) {
                'C' -> TEXT_ALIGN_CENTER
                'R' -> TEXT_ALIGN_RIGHT
                else -> TEXT_ALIGN_LEFT
            }

            return combine(alignCommand, "$text\n".toByteArray())
        }
    }
}