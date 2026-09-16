package io.github.lonevertex.smscguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.github.lonevertex.smscguard.ui.theme.SmscGuardTheme

class SettingsActivity : ComponentActivity() {
    val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmscGuardTheme {
                SettingsDashboard(viewModel = viewModel)
            }
        }
    }
}
