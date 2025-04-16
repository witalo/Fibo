package com.example.fibo.reports

class PrinterCommands {
    companion object {
        // Comandos básicos de impresora ESC/POS
        val INIT = byteArrayOf(0x1B, 0x40) // Inicializar impresora
        val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // Alineación central
        val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // Alineación izquierda
        val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // Alineación derecha
        val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // Activar negrita
        val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // Desactivar negrita
        val ESC_FEED_PAPER_AND_CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Avanzar y cortar




        // Comandos básicos ESC/POS
//        val INIT = byteArrayOf(0x1B, 0x40)
        val FEED_LINE = byteArrayOf(0x0A)
        val FEED_PAPER_AND_CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
        val TEXT_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val TEXT_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val TEXT_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        val TEXT_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        val TEXT_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        val TEXT_DOUBLE_HEIGHT_ON = byteArrayOf(0x1B, 0x21, 0x10)
        val TEXT_DOUBLE_HEIGHT_OFF = byteArrayOf(0x1B, 0x21, 0x00)
        // Tamaños de texto
        val TEXT_LARGE_SIZE = byteArrayOf(0x1D, 0x21, 0x11) // doble ancho y alto
        val TEXT_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00) // tamaño normal

        // Función para convertir texto a bytes
        fun textToBytes(text: String): ByteArray {
            return text.toByteArray()
        }

        // Función para combinar múltiples comandos
        fun combine(vararg commands: ByteArray): ByteArray {
            return commands.reduce { acc, bytes -> acc + bytes }
        }
        // 🟦 Divider (línea separadora)
        fun divider(): ByteArray {
            return "------------------------------\n".toByteArray()
        }
    }
}