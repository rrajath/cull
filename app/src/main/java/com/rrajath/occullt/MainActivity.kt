package com.rrajath.occullt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.navigation.OcculltNavHost
import com.rrajath.occullt.ui.theme.OcculltTheme

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OcculltTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OcculltNavHost(
                        modifier = Modifier.padding(innerPadding),
                        settingsRepository = settingsRepository
                    )
                }
            }
        }
    }
}
