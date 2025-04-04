package com.keronei.kmlguage

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.keronei.kmlgauge.BuildConfig
import com.keronei.kmlgauge.R
import com.keronei.kmlgauge.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


class MainActivity : AppCompatActivity() {
    var binding: ActivityMainBinding? = null

    private lateinit var locationManager: LocationManager

    private var ioJob = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val eventViewModel: EventViewModel by lazy { (application as GuageApp).eventViewModel }

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
        val d: Drawable = ProgressDrawable(this)
        rpmGuage?.progressDrawable = d

        rpmGuage?.let { bar ->
            bind.hostRpm.addView(bar, 850, 170)
        }

        rpmGuage?.max = 8000

        setContentView(bind.root)
        gestureDetector = GestureDetector(this, SwipeGestureListener())


        bind.main.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                eventViewModel.eventFlow.collect { event ->
                    if (event == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                        initiateUsb()

                        showEventMessage("Starting handler")
                    } else if (event == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                        USBDataHandler.ioManager?.stop()

                        showEventMessage(
                            "Stopping handler"
                        )
                    }
                }
            }
        }

        handleEngineData()
    }

    override fun onResume() {
        super.onResume()

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        val layoutParams = window.attributes

        layoutParams.screenBrightness = 0.4f
        window.attributes = layoutParams
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private suspend fun showEventMessage(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@MainActivity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun animateDataIndicator() {
        bind.dataFlag.alpha = 1f
        bind.dataFlag.animate().alpha(0f).setDuration(300).start()
    }

    private fun handleEngineData() {
        ioJob.launch {
            USBDataHandler.incomingData.collectLatest { data ->
                withContext(Dispatchers.Main) {
                    Log.d("Received", "$data")

                    data?.let {
                        animateDataIndicator()

                        rpmGuage?.progress = data.rpm
                        bind.currentSpeed.text = "${data.instantSpeed}"

                        if (data.instantSpeed == 0) {
                            bind.instantConsumption.text =
                                if (data.currentConsumptionPerKm == 999999.0) {
                                    "***"
                                } else {
                                    "Inst. ${data.instantConsumptionPerHour} L/H"
                                }
                        } else {
                            bind.instantConsumption.text =
                                if (data.currentConsumptionPerKm == 999999.0) {
                                    "***"
                                } else {
                                    "Inst. ${data.instantConsumptionPerKm} Km/L"
                                }
                        }

                        val remainingDistance = String.format(
                            "%.1f",
                            (data.approximateRemainingTank * data.currentConsumptionPerKm)
                        )

                        val remainingDistanceText =
                            if (data.currentConsumptionPerKm == 999999.0) {
                                "No consumption info"
                            } else {
                                "$remainingDistance Km available"
                            }

                        bind.remainingOnTank.text =
                            "${data.approximateRemainingTank}L on tank - $remainingDistanceText"

                        bind.tripTime.text = "${data.currentTripTime.replace(".", ":")} h"
                        bind.averageSpeed.text = "avg. ${data.currentAverageSpeed} Km/h"
                        bind.tripDistance.text = "Trip ${data.currentTripDistance} Km"
                        bind.currentConsumption.text =
                            if (data.currentConsumptionPerKm == 999999.0) {
                                if (data.instantSpeed == 0) {
                                    if (data.rpm == 0) {
                                        "Engine Off"
                                    } else {
                                        "Idling"
                                    }
                                } else {
                                    "***"
                                }
                            } else {
                                "${data.currentConsumptionPerKm} Km/L"
                            }
                        bind.totalConsumption.text = "Consumed ${data.currentConsumedLitres} L"
                    }
                }
            }
        }
    }

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

        try {
            port = firstAvailableDriver.ports.first()
            port?.open(connection)
            port?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            port?.let {
                USBDataHandler.usbSerialPort = it
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(exception)
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

            return if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
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