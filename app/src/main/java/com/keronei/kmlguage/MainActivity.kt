package com.keronei.kmlguage

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.usb.UsbManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.keronei.kmlgauge.BuildConfig
import com.keronei.kmlgauge.databinding.ActivityMainBinding
import java.util.Locale
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), SerialInputOutputManager.Listener {
    var binding: ActivityMainBinding? = null

    private lateinit var locationManager: LocationManager

    val bind get() = binding!!

    var port: UsbSerialPort? = null

    var ioManager: SerialInputOutputManager? = null

    private enum class UsbPermission {
        Unknown, Requested, Granted, Denied
    }

    private var mainLooper: Handler? = null

    val INTENT_ACTION_GRANT_USB: String = BuildConfig.APPLICATION_ID + ".GRANT_USB"


    private var usbPermission = UsbPermission.Unknown

    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)


        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        val d: Drawable = ProgressDrawable()
        pb.progressDrawable = d
        //pb.setPadding(20, 10, 20, 0)

        val final = (510.toDouble() / 8000.0) * 100

        Log.d("RPM", "$final")


        bind.hostRpm.addView(pb, 700, 160)

        setContentView(bind.root)

        initiateUsb()

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        listenToLocationUpdates()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (INTENT_ACTION_GRANT_USB == intent.action) {
                    usbPermission =
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                            UsbPermission.Granted
                        else
                            UsbPermission.Denied
                    // connect()
                }
            }
        }
        mainLooper = Handler(Looper.getMainLooper())

        bind.currentSpeed.setOnClickListener {
            initiateUsb()
        }

    }

    @SuppressLint("MissingPermission")
    private fun listenToLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, {
            location ->
            val speed = location.speed
            val kph = speed * 3.6f

            Log.d("LocationSpeed", "$kph")

            bind.currentSpeed.text = kph.roundToInt().toString()
        })
    }

    private fun initiateUsb() {
        if (ioManager != null) {
            ioManager?.stop()
        }

        val usbManager = getSystemService(USB_SERVICE) as UsbManager

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            Log.d("Arduino", "No devices found!")
            return
        }

        val firstAvailableDriver = availableDrivers.first()
        val connection = usbManager.openDevice(firstAvailableDriver.device)

        if (connection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(
                firstAvailableDriver.getDevice()
            )
        ) {
            usbPermission = UsbPermission.Requested
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
            val intent =
                Intent(INTENT_ACTION_GRANT_USB)
            intent.setPackage(this.packageName)
            val usbPermissionIntent = PendingIntent.getBroadcast(this, 0, intent, flags)
            usbManager.requestPermission(firstAvailableDriver.getDevice(), usbPermissionIntent)
            return
        }
        if (connection == null) {
            if (!usbManager.hasPermission(firstAvailableDriver.getDevice())) {
                Log.d("Connection", "connection failed: permission denied")
            } else {
                Log.d("Connection", "connection failed: open failed")
            }
            return
        }

        port = firstAvailableDriver.ports.first()
        port?.open(connection)
        Log.d("Arduino-p", "All ports: ${firstAvailableDriver.ports}")

        port?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        Log.d("Arduino-p", "Port: $port")

        ioManager = port?.let { po -> SerialInputOutputManager(po, this) }

        ioManager?.setReadTimeout(1000)

        ioManager?.start()
    }

    override fun onNewData(data: ByteArray) {
        val receivedString = java.lang.String(data, Charsets.UTF_8)
        Log.d("Arduino-P", "$receivedString")
    }

    override fun onRunError(e: Exception) {
        Log.d("Arduino-P", "Error registered")
        e.printStackTrace()
    }

    override fun onDestroy() {
        super.onDestroy()
        ioManager?.stop()
        port?.close()
    }
}