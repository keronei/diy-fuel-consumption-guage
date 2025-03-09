package com.keronei.kmlguage

import android.annotation.SuppressLint
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.keronei.kmlgauge.R
import com.keronei.kmlgauge.databinding.ActivityGuagesScreenBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class GuagesScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityGuagesScreenBinding
    private var ioJob = CoroutineScope(Dispatchers.IO + Job())
    private var count = 0
    private val maxCount = 160
    private val delayMillis: Long = 1000  // 1 second interval
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector
    private lateinit var locationManager: LocationManager


    private val runnable = object : Runnable {
        override fun run() {
            if (count <= maxCount) {
                binding.currentSpeed.setSpeedAndRPM(count, 2200, 500L)
                count++
                handler.postDelayed(this, delayMillis)  // Schedule next run
            } else {
                count = 0
            }
        }
    }

    val bind: ActivityGuagesScreenBinding get() = binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuagesScreenBinding.inflate(layoutInflater)

        count = 0  // Reset count

        gestureDetector = GestureDetector(this, SwipeGestureListener())
        setContentView(bind.root)
        handleEngineData()
        bind.main.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun handleEngineData() {
        ioJob.launch {
            USBDataHandler.incomingData.collectLatest { data ->
                withContext(Dispatchers.Main) {
                    data?.let {
                        binding.currentSpeed.setSpeedAndRPM(data.instantSpeed, data.rpm, 300)

                        if (data.instantSpeed == 0) {
                            binding.instantLkm.text =
                                if (data.currentConsumptionPerKm == 999999.0) {
                                    binding.instantUnit.visibility = View.GONE
                                    "***"
                                } else {
                                    binding.instantUnit.visibility = View.VISIBLE
                                    binding.instantUnit.text = "L/H"
                                    data.instantConsumptionPerHour.toString()
                                }
                        } else {
                            binding.instantLkm.text =
                                if (data.currentConsumptionPerKm == 999999.0) {
                                    binding.instantUnit.visibility = View.GONE

                                    "***"
                                } else {
                                    binding.instantUnit.visibility = View.VISIBLE

                                    binding.instantUnit.text = "Km/L"

                                    data.instantConsumptionPerKm.toString()
                                }
                        }

                        binding.currentLkm.text = if (data.currentConsumptionPerKm == 999999.0) {
                            binding.currentConsumptionUnit.visibility = View.GONE
                            "***"
                        } else {
                            binding.currentConsumptionUnit.visibility = View.VISIBLE
                            "${data.currentConsumptionPerKm}"
                        }

                        binding.remainingInTank.text = data.approximateRemainingTank.toString()

                        binding.remainingDistanceInTank.text =
                            if (data.currentConsumptionPerKm == 999999.0) {
                                binding.remainingDistanceLabel.visibility = View.GONE
                                "***"
                            } else {
                                binding.remainingDistanceLabel.visibility = View.VISIBLE

                                String.format(
                                    "%.1f",
                                    (data.currentConsumptionPerKm * data.approximateRemainingTank)
                                )
                            }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun listenToLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, { location ->
            val speed = location.speed
            val kph = speed * 3.6f

            Log.d("LocationSpeed", "$kph")

            bind.currentSpeed.setSpeedAndRPM(kph.roundToInt(), 0, 500)
        })
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(runnable)
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
                    this@GuagesScreenActivity.onBackPressed()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    Toast.makeText(this@GuagesScreenActivity, "Swiped Right", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this@GuagesScreenActivity, "Swiped Left", Toast.LENGTH_SHORT)
                        .show()
                }
                true
            } else {
                false
            }
        }
    }
}