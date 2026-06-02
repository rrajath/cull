package com.rrajath.occullt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.navigation.OcculltNavHost
import com.rrajath.occullt.ui.theme.CatppuccinAccents
import com.rrajath.occullt.ui.theme.OcculltTheme
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme by settingsRepository.darkTheme.collectAsState(initial = true)
            val accentHue by settingsRepository.accentHue.collectAsState(initial = 0)
            val accentIndex = if (accentHue in CatppuccinAccents.indices) accentHue else 0

            OcculltTheme(darkTheme = darkTheme, accentIndex = accentIndex) {
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
