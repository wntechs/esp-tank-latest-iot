package com.wntechs.tankcontroller
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.wntechs.tankcontroller.ui.AppRoot
import com.wntechs.tankcontroller.ui.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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