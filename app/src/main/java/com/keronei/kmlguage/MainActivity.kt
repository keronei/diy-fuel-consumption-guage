package com.keronei.kmlguage

import android.graphics.drawable.Drawable
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.keronei.kmlgauge.databinding.ActivityMainBinding
import com.keronei.kmlguage.Arduino.Arduino
import com.keronei.kmlguage.Arduino.ArduinoListener
import kotlin.random.Random


class MainActivity : AppCompatActivity(), ArduinoListener {
    var binding: ActivityMainBinding? = null

    val bind get() = binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        val arduino = Arduino(this)

        arduino.setArduinoListener(this)

        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        val d: Drawable = ProgressDrawable()
        pb.progressDrawable = d
        //pb.setPadding(20, 10, 20, 0)

        val final = (510.toDouble()/8000.0) * 100

        Log.d("RPM", "$final")


        bind.hostRpm.addView(pb, 700, 160)

        setContentView(bind.root)

        val handler = Handler(Looper.getMainLooper())
        val interval = 200L // Interval in milliseconds

        val runnable = object : Runnable {
            override fun run() {
                // Your repeated task here
                println("Task running...")

                val nextRpm = Random.nextDouble(6000.0)

                val percentage = (nextRpm/8000.0)*100

                pb.progress = percentage.toInt()
                pb.max = 100

                // Re-schedule the task
                handler.postDelayed(this, interval)
            }
        }

// Start the repeating task
        handler.post(runnable)
    }

    override fun onArduinoAttached(device: UsbDevice?) {
        bind.tripTime.setText("Attached!")

    }

    override fun onArduinoDetached() {
        bind.tripTime.setText("Detached!")

    }

    override fun onArduinoMessage(bytes: ByteArray?) {
        bind.tripDistance.setText("Received: ${bytes.toString()}")
    }

    override fun onArduinoOpened() {

        bind.tripTime.setText("Opened!")

    }

    override fun onUsbPermissionDenied() {
bind.tripTime.setText("No permission")
    }
}