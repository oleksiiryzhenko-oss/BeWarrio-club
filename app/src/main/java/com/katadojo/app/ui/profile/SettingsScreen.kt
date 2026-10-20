package com.katadojo.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    ScreenList(topBar = { DojoTopBar("Notification Settings", onBack = onBack) }) {
        item {
            Text(
                "BeеWarrior club does not send push notifications. These settings control in-app alerts during kata sessions and workouts.",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone
            )
        }
        item {
            DojoCard {
                ToggleRow("Timer sounds", "A tone when a workout reaches its target and when a session is saved", s.timerSounds) { v -> viewModel.update { it.copy(timerSounds = v) } }
                ToggleRow("Vibration", "Short vibration when you mark a kata section", s.haptics) { v -> viewModel.update { it.copy(haptics = v) } }
                ToggleRow("Keep screen on", "Prevents the screen from sleeping during sessions", s.keepScreenOn) { v -> viewModel.update { it.copy(keepScreenOn = v) } }
                ToggleRow("Auto-advance sections", "Move to the next kata section after you mark one", s.autoAdvanceSections) { v -> viewModel.update { it.copy(autoAdvanceSections = v) } }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Rice)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Stone)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Rice,
                checkedTrackColor = DojoRed,
                uncheckedThumbColor = Stone,
                uncheckedTrackColor = CharcoalLight,
                uncheckedBorderColor = CharcoalBorder
            )
        )
    }
}
