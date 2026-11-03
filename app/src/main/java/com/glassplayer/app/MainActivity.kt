package com.glassplayer.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.glassplayer.app.ui.GlassPlayerApp

class MainActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        requestHighestRefreshRate()
        setContent { GlassPlayerApp(vm) }
    }

    override fun onStart() {
        super.onStart()
        vm.refresh() // pick up newly added songs each time the app comes back
    }

    override fun onResume() {
        super.onResume()
        requestHighestRefreshRate()
    }

    /** Asks Android for the panel's top refresh rate (120 Hz on this phone) instead of the 60 Hz default. */
    @Suppress("DEPRECATION")
    private fun requestHighestRefreshRate() {
        val d = (if (Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay) ?: return
        val cur = d.mode
        val best = d.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        val lp = window.attributes
        if (lp.preferredDisplayModeId == best.modeId) return
        lp.preferredDisplayModeId = best.modeId
        lp.preferredRefreshRate = best.refreshRate
        window.attributes = lp
    }
}
