package com.wntechs.tankcontroller
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import com.wntechs.tankcontroller.ui.AppRoot
import com.wntechs.tankcontroller.ui.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 2. This ensures the status bar icons (clock/battery)
        // stay visible (dark icons for light theme, vice-versa)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val tankJson = assets.open("tank-measurements.json").bufferedReader().use { it.readText() }
        
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                AppRoot(factory = AppViewModelFactory(
                    container = (application as WaterTankApplication).container,
                    tankMeasurementsJson = tankJson
                ))
            }
        }
    }
}