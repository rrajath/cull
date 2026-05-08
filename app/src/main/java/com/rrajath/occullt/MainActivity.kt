package com.rrajath.occullt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.navigation.OcculltNavHost
import com.rrajath.occullt.ui.theme.CatppuccinAccents
import com.rrajath.occullt.ui.theme.OcculltTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by mutableStateOf(true)
            var accentIndex by mutableStateOf(0)

            LaunchedEffect(settingsRepository) {
                settingsRepository.darkTheme.collectLatest { isDark ->
                    darkTheme = isDark
                }
            }

            LaunchedEffect(settingsRepository) {
                settingsRepository.accentHue.collectLatest { hue ->
                    accentIndex = if (hue in CatppuccinAccents.indices) hue else 0
                }
            }

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
