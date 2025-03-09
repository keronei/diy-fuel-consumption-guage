package com.keronei.kmlguage

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

object USBDataHandler : SerialInputOutputManager.Listener {
    var usbSerialPort: UsbSerialPort? = null

    var ioManager: SerialInputOutputManager? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _data: MutableStateFlow<EngineData?> = MutableStateFlow(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomingData: StateFlow<EngineData?> = _data

    fun initializeHandler() {
        ioManager = usbSerialPort?.let { po -> SerialInputOutputManager(po, this) }
        ioManager?.setReadTimeout(1000)
        ioManager?.start()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onNewData(data: ByteArray?) {
        val receivedString = java.lang.String(data, Charsets.UTF_8)
        Log.d("Arduino-P", "$receivedString")
        try {
            val receivedData = receivedString.split(",").toList()

            val crunchedData = EngineData(
                rpm = receivedData[0].trim().toDouble().toInt(),
                instantSpeed = receivedData[1].trim().toDouble().toInt(),
                instantConsumptionPerKm = receivedData[2].trim().toDouble(),
                instantConsumptionPerHour = receivedData[4].trim().toDouble(),
                currentConsumptionPerKm = receivedData[3].trim().toDouble(),
                currentConsumedLitres = receivedData[5].trim().toDouble(),
                currentTripDistance = receivedData[6].trim().toDouble(),
                approximateRemainingTank = receivedData[7].trim().toDouble(),
                currentTripTime = if (receivedData[8].trim() == "0.00") {
                    "0:00"
                } else {
                    parseTripTime(receivedData[8].trim().toLong())
                },
                currentAverageSpeed = receivedData[9].trim().toDouble()
            )

            _data.value = crunchedData

        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun parseTripTime(encodedTime: Long): String {
        val hours = encodedTime / 100  // Extract hours
        val minutes = encodedTime % 100 // Extract minutes
        return String.format("%02d:%02d", hours, minutes) // Format as HH:MM
    }


    override fun onRunError(e: Exception?) {
        Log.d("Arduino-P", "Error registered")
        e?.printStackTrace()
    }
}