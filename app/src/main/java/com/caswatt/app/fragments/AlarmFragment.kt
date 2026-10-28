package com.caswatt.app.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.caswatt.app.AlarmRingtoneHelper
import com.caswatt.app.AlarmUiBinder
import com.caswatt.app.ChargingService
import com.caswatt.app.R

class AlarmFragment : Fragment() {

    private var uiBinder: AlarmUiBinder? = null

    // Pelancar Pemilih Nada Penggera Sistem (System Alarm Picker)
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            context?.let { ctx ->
                AlarmRingtoneHelper.saveSelectedRingtone(ctx, uri)
                uiBinder?.updateRingtoneDisplay()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)
        val ctx = requireContext()

        uiBinder = AlarmUiBinder(
            rootView = view,
            context = ctx,
            onPickRingtoneClicked = { launchRingtonePicker() },
            onToggleServiceClicked = { toggleBackgroundService() }
        ).apply {
            bindAll()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateServiceStateDisplay()
        uiBinder?.updateRingtoneDisplay()
    }

    private fun launchRingtonePicker() {
        val ctx = context ?: return
        val intent = AlarmRingtoneHelper.createRingtonePickerIntent(ctx)
        ringtonePickerLauncher.launch(intent)
    }

    private fun toggleBackgroundService() {
        val ctx = context ?: return
        val serviceIntent = Intent(ctx, ChargingService::class.java)

        if (ChargingService.isRunning) {
            ctx.stopService(serviceIntent)
        } else {
            try {
                ContextCompat.startForegroundService(ctx, serviceIntent)
            } catch (e: Exception) {
                Log.e("AlarmFragment", "Gagal memulakan servis: ${e.message}")
                Toast.makeText(ctx, "Gagal memulakan servis: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        view?.postDelayed({
            updateServiceStateDisplay()
        }, 300)
    }

    private fun updateServiceStateDisplay() {
        uiBinder?.updateServiceStatus(ChargingService.isRunning)
    }
}
