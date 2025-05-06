package com.example.fibo.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import com.example.fibo.model.IOperation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class BluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var currentDevice: BluetoothDevice? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    // UUID para SPP (Serial Port Profile)
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Socket y output stream para la impresión
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    @SuppressLint("MissingPermission")
    fun enableBluetooth() {
        if (bluetoothAdapter == null) {
            throw BluetoothException("Bluetooth no soportado en este dispositivo")
        }

        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(enableBtIntent)
        }
    }

    @SuppressLint("MissingPermission")
    fun scanForDevices(): List<BluetoothDevice> {
        if (bluetoothAdapter == null) {
            throw BluetoothException("Bluetooth no soportado en este dispositivo")
        }

        if (!bluetoothAdapter!!.isEnabled) {
            throw BluetoothException("Bluetooth no está activado")
        }

        return bluetoothAdapter!!.bondedDevices.toList()
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun connectToDevice(device: BluetoothDevice) {
        try {
            // Crea un socket seguro usando el UUID SPP
            bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
        } catch (e: IOException) {
            closeConnection()
            throw BluetoothException("Error al conectar con el dispositivo: ${e.message}")
        }
    }

    fun printNote(note: IOperation) {
        try {
            val printData = formatNoteForPrinting(note)
            outputStream?.write(printData.toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            throw BluetoothException("Error al imprimir: ${e.message}")
        }
    }

    private fun formatNoteForPrinting(note: IOperation): String {
        // Formatea los datos de la nota para impresión térmica
        return buildString {
            appendln("\n\n") // Espacios iniciales
            appendln("NOTA DE SALIDA")
            appendln("${note.serial}-${note.correlative}")
            appendln("Fecha: ${note.emitDate}")
            appendln("Cliente: ${note.client.names}")
            appendln("-----------------------------")
            note.operationDetailSet.forEach { item ->
                appendln("${item.quantity} x ${item.tariff.productName} - S/. ${item.unitPrice}")
            }
            appendln("-----------------------------")
            appendln("TOTAL: S/. ${note.totalToPay}")
            appendln("\n\n") // Espacios finales
        }
    }

    fun closeConnection() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // Ignorar errores al cerrar
        } finally {
            outputStream = null
            bluetoothSocket = null
        }
    }
}

class BluetoothException(message: String) : Exception(message)