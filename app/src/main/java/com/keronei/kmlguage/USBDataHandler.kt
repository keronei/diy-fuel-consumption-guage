package com.keronei.kmlguage

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object USBDataHandler : SerialInputOutputManager.Listener {
    var usbSerialPort: UsbSerialPort? = null

    var ioManager: SerialInputOutputManager? = null

    private val _data: MutableStateFlow<EngineData?> = MutableStateFlow(null)

    val incomingData: StateFlow<EngineData?> = _data

    fun initializeHandler() {
        ioManager = usbSerialPort?.let { po -> SerialInputOutputManager(po, this) }
        ioManager?.start()
    }

    override fun onNewData(data: ByteArray?) {
        val receivedString = java.lang.String(data, Charsets.UTF_8)
        try {
            val receivedData = receivedString.split(",").toList()

            Log.d("Incoming", "$receivedData")

            val crunchedData = EngineData(
                rpm = receivedData[0].trim().toDouble().toInt(),
                instantSpeed = receivedData[1].trim().toDouble().toInt(),
                instantConsumptionPerKm = receivedData[2].trim().toDouble(),
                instantConsumptionPerHour = receivedData[4].trim().toDouble(),
                currentConsumptionPerKm = receivedData[3].trim().toDouble(),
                currentConsumedLitres = receivedData[5].trim().toDouble(),
                currentTripDistance = receivedData[6].trim().toDouble(),
                approximateRemainingTank = receivedData[7].trim().toDouble(),
                currentTripTime = receivedData[8].trim(),
                currentAverageSpeed = receivedData[9].trim().toDouble()
            )

            _data.value = crunchedData

        } catch (exception: Exception) {
            FirebaseCrashlytics.getInstance().recordException(exception)
            exception.printStackTrace()
        }
    }

    fun parseTripTime(encodedTime: Long): String {
        val hours = encodedTime / 100  // Extract hours
        val minutes = encodedTime % 100 // Extract minutes
        return String.format("%03d:%02d", hours, minutes) // Format as HH:MM
    }


    override fun onRunError(e: Exception?) {
        Log.d("Arduino-P", "Error registered")
        if (e != null) {
             FirebaseCrashlytics.getInstance().recordException(e)
        }
        e?.printStackTrace()
    }
}