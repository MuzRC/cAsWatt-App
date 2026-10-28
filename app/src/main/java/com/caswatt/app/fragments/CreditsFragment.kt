package com.caswatt.app.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.caswatt.app.R

class CreditsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_credits, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        // Paparkan versi aplikasi secara dinamik jika ada id tv_credits_version di layout
        val tvVersion = view.findViewById<TextView?>(R.id.tv_credits_version)
        try {
            val ctx = requireContext()
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }
            tvVersion?.text = "CasWatt Pro v${packageInfo.versionName}"
        } catch (_: Exception) {
            tvVersion?.text = "CasWatt Pro v5.0"
        }
    }
}
