package com.keronei.kmlguage

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.keronei.kmlgauge.BuildConfig
import com.keronei.kmlgauge.R
import com.keronei.kmlgauge.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    var binding: ActivityMainBinding? = null

    private lateinit var locationManager: LocationManager

    private var ioJob = CoroutineScope(Dispatchers.IO + Job())

    val bind get() = binding!!

    var port: UsbSerialPort? = null

    var rpmGuage: ProgressBar? = null

    private enum class UsbPermission {
        Unknown, Requested, Granted, Denied
    }

    private var mainLooper: Handler? = null

    val INTENT_ACTION_GRANT_USB: String = BuildConfig.APPLICATION_ID + ".GRANT_USB"

    private var usbPermission = UsbPermission.Unknown

    private var broadcastReceiver: BroadcastReceiver? = null
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        rpmGuage = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        val d: Drawable = ProgressDrawable()
        rpmGuage?.progressDrawable = d

        rpmGuage?.let { bar ->
            bind.hostRpm.addView(bar, 700, 160)
        }


        setContentView(bind.root)
        gestureDetector = GestureDetector(this, SwipeGestureListener())

        initiateUsb()

        bind.main.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        //listenToLocationUpdates()

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

        handleEngineData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun handleEngineData() {
        ioJob.launch {
            USBDataHandler.incomingData.collectLatest { data ->
                withContext(Dispatchers.Main) {
                    data?.let {
                        val finalRpm = (data.rpm.toDouble() / 8000.0) * 100

                        rpmGuage?.progress = finalRpm.toInt()
                        bind.currentSpeed.text = data.instantSpeed.toString()

                        if (data.instantSpeed == 0) {
                            bind.instantConsumption.text =
                                if (data.currentConsumptionPerKm == 999999.0) {
                                    "***"
                                } else {
                                    "Instant ${data.instantConsumptionPerHour} L/H"
                                }
                        } else {
                            bind.instantConsumption.text =
                                if (data.currentConsumptionPerKm == 999999.0) {
                                    "***"
                                } else {
                                    "Instant ${data.instantConsumptionPerHour} Km/L"
                                }
                        }

                        val remainingDistance = String.format(
                            "%.1f",
                            (data.approximateRemainingTank * data.currentConsumptionPerKm)
                        )

                        val remainingDistanceText = if(data.currentConsumptionPerKm == 999999.0){
                            "No consumption info"
                        } else {
                            "$remainingDistance Km available"
                        }

                        bind.remainingOnTank.text =
                            "${data.approximateRemainingTank}L on tank - $remainingDistanceText"

                        bind.tripTime.text = "${data.currentTripTime} h"
                        bind.averageSpeed.text = "avg. ${data.currentAverageSpeed} Km/h"
                        bind.tripDistance.text = "Trip ${data.currentTripDistance} Km"
                        bind.currentConsumption.text =
                            if (data.currentConsumptionPerKm == 999999.0) {
                                "***"
                            } else {
                                "${data.currentConsumptionPerKm} Km/l"
                            }
                        bind.totalConsumption.text = "Consumed ${data.currentConsumedLitres} L"
                    }
                }
            }
        }
    }


//    @SuppressLint("MissingPermission")
//    private fun listenToLocationUpdates() {
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, {
//            location ->
//            val speed = location.speed
//            val kph = speed * 3.6f
//
//            Log.d("LocationSpeed", "$kph")
//
//            bind.currentSpeed.text = kph.roundToInt().toString()
//        })
//    }

    private fun initiateUsb() {
        if (USBDataHandler.ioManager != null) {
            USBDataHandler.ioManager?.stop()
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
        port?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        port?.let {
            USBDataHandler.usbSerialPort = it
        }

        USBDataHandler.initializeHandler()
    }

    override fun onDestroy() {
        super.onDestroy()
        USBDataHandler.ioManager?.stop()
        port?.close()
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val diffX = e2.x - e1.x
            val swipeThreshold = 100
            val swipeVelocityThreshold = 100

            return if (Math.abs(diffX) > swipeThreshold && Math.abs(velocityX) > swipeVelocityThreshold) {
                if (diffX > 0) {
                    Toast.makeText(this@MainActivity, "Swiped Right", Toast.LENGTH_SHORT).show()
                } else {
                    this@MainActivity.startActivity(
                        Intent(
                            this@MainActivity,
                            GuagesScreenActivity::class.java
                        )
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

                    Toast.makeText(this@MainActivity, "Swiped Left", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }
}