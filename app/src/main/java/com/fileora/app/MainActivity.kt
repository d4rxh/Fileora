package com.fileora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fileora.app.ui.navigation.FileoraNavGraph
import com.fileora.app.ui.theme.FileoraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs = FileoraApp.instance.preferences
            val themeMode by prefs.themeMode.collectAsState()

            FileoraTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    FileoraNavGraph(navController = navController)
                }
            }
        }
    }
}
