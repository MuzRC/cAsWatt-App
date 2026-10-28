package com.caswatt.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.caswatt.app.fragments.AlarmFragment
import com.caswatt.app.fragments.CreditsFragment
import com.caswatt.app.fragments.DashboardFragment
import com.caswatt.app.fragments.HealthFragment
import com.caswatt.app.fragments.StatsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Pastikan storan rekod sesi & tetapan dimuat turun dengan selamat
        ChargingSessionManager.ensureInit(this)

        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNav()

        // 2. Paparkan DashboardFragment sebagai tab lalai semasa mula-mula buka app
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        // 3. Minta kebenaran notifikasi Android 13+ (Tiramisu)
        checkPermissions()
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_stats_graph -> StatsFragment()
                R.id.nav_health -> HealthFragment()
                R.id.nav_settings_alarm -> AlarmFragment()
                R.id.nav_credits -> CreditsFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(selectedFragment)
            true
        }
    }

    /**
     * Menukar paparan di dalam fragment_container dengan kemas dan lancar
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
}
