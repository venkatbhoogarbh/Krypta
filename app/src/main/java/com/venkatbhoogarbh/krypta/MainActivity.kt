package com.venkatbhoogarbh.krypta

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.venkatbhoogarbh.krypta.util.SecurityUtils
import com.venkatbhoogarbh.krypta.util.SessionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Feature 11: Block screenshots and recent-app preview leakage
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        
        bottomNav.setupWithNavController(navController)

        // Initial Auth Gate Navigation
        if (!SecurityUtils.isMasterPasswordSet(this)) {
            navController.navigate(R.id.setupFragment)
        } else if (!SessionManager.isAuthenticated.value) {
            navController.navigate(R.id.loginFragment)
        }

        // Global Session & UI Logic
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    SessionManager.isAuthenticated.collect { authenticated ->
                        bottomNav.visibility = if (authenticated) View.VISIBLE else View.GONE
                        if (!authenticated && SecurityUtils.isMasterPasswordSet(this@MainActivity)) {
                            // Only force redirect if we're not already on setup or login
                            val currentId = navController.currentDestination?.id
                            if (currentId != R.id.loginFragment && currentId != R.id.setupFragment) {
                                navController.navigate(R.id.loginFragment)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Lock when app goes to background
        SessionManager.lock()
    }
}
