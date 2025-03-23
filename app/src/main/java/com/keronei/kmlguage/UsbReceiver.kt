package com.keronei.kmlguage

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RawRes
import com.keronei.kmlgauge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsbReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val eventViewModel = (context?.applicationContext as GuageApp).eventViewModel

        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager

        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "gauge:WakeLock"
        )

        wakeLock.acquire(2000)

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = keyguardManager.newKeyguardLock("gauge:KeyguardLock")
        keyguardLock.disableKeyguard()

        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            Log.d("Connected", "Launching app & playing sound")

            context.playAlarmSound(R.raw.connected) {
                Log.d("Connected", "Audio played")
            }
            val launchedIntent =
                context.packageManager?.getLaunchIntentForPackage("com.keronei.kmlguage")
            launchedIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchedIntent)

            CoroutineScope(Dispatchers.Main).launch {
                eventViewModel.sendEvent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            }
        }

        if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
            Log.d("Disconnected", "Going to summary screen**")

            context.playAlarmSound(R.raw.disconnected) {
                Log.d("Disconnected", "Audio played")
            }

            CoroutineScope(Dispatchers.Main).launch {
                eventViewModel.sendEvent(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }

        }
    }

    private fun Context.playAlarmSound(@RawRes audioId: Int, onAudioComplete: () -> Unit) {
        var mediaPlayer = MediaPlayer()
        // Set audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        mediaPlayer.setAudioAttributes(audioAttributes)

        val uri =
            Uri.parse(("android.resource://" + this.packageName) + "/" + audioId)
        mediaPlayer.setDataSource(this, uri)

        mediaPlayer.setOnCompletionListener {

            mediaPlayer.release()
            onAudioComplete()

        }
        // Prepare and start playback
        mediaPlayer.prepare()
        mediaPlayer.start()
    }
}