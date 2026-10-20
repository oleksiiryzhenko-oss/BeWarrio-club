package com.katadojo.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.data.model.Belt
import com.katadojo.app.data.model.ExperienceLevel
import com.katadojo.app.data.model.KarateStyle
import com.katadojo.app.data.model.UserProfile
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = viewModel.draft
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmDemo by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        ScreenList(topBar = { DojoTopBar("Profile", onBack = onBack) }) {
            if (state.loading) {
                item { LoadingBox(Modifier.height(300.dp)) }
                return@ScreenList
            }
            val p = state.profile
            item {
                DojoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(68.dp).clip(CircleShape).background(CharcoalLight), contentAlignment = Alignment.Center) {
                            Enso(Modifier.size(58.dp))
                            Text(p.displayName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Rice)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.displayName, style = MaterialTheme.typography.headlineMedium, color = Rice)
                            Text("${p.style.label} · ${p.experience.label}", style = MaterialTheme.typography.bodyMedium, color = Stone)
                            Text("${p.belt.label} belt · ${state.level.level.label} app level", style = MaterialTheme.typography.bodySmall, color = Beige)
                        }
                    }
                    if (p.trainingGoal.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Overline("Training goal")
                        Text(p.trainingGoal, style = MaterialTheme.typography.bodyLarge, color = Rice)
                    }
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Edit Profile", onClick = viewModel::startEditing, height = 44.dp)
                    Text("Stored only on this device. No online account.", style = MaterialTheme.typography.bodySmall, color = StoneDark, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item { SectionTitle("Career Summary") }
            item {
                DojoCard {
                    val s = state.stats
                    StatGrid(
                        listOf(
                            "${s.trainingSessions}" to "Training sessions",
                            "${s.kataSessions}" to "Kata sessions",
                            Formatters.thousands(s.trainingMinutes) to "Training minutes",
                            (if (s.kataSessions > 0) Formatters.score(s.bestKataScore) else "-") to "Best kata score",
                            "${s.currentStreak} days" to "Current streak",
                            Formatters.thousands(s.xp) to "Training Points"
                        )
                    )
                }
            }
            item { SectionTitle("App") }
            item {
                DojoCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                    ListRow("Notification Settings", subtitle = "Timer sounds, vibration, screen", onClick = { onNavigate(Routes.SETTINGS) })
                    ListRow("Personal Goals", subtitle = "Weekly and kata goals", onClick = { onNavigate(Routes.GOALS) })
                    ListRow("Weekly Schedule", subtitle = "${state.registrations} dojo registrations saved on this device", onClick = { onNavigate(Routes.SCHEDULE) })
                    ListRow("Privacy Policy", onClick = { onNavigate(Routes.PRIVACY) })
                    ListRow("Terms of Use", onClick = { onNavigate(Routes.TERMS) })
                }
            }
            item { SectionTitle("Local Data") }
            item {
                DojoCard {
                    Text(
                        "Your profile, registrations, kata sessions, workouts, goals and Training Points are stored only on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StoneLight
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Load Demo Data", onClick = { confirmDemo = true }, enabled = !viewModel.busy, height = 46.dp, color = Beige)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Reset Local Data", onClick = { confirmReset = true }, enabled = !viewModel.busy, height = 48.dp, color = DojoRedDark)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    if (draft != null) {
        ProfileEditor(
            draft = draft,
            nameError = viewModel.nameError,
            onChange = viewModel::updateDraft,
            onSave = viewModel::saveDraft,
            onCancel = viewModel::cancelEditing
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Reset local data?",
            text = "This permanently deletes your profile, registrations, kata sessions, workouts, goals and Training Points from this device.",
            confirmLabel = "Delete everything",
            onConfirm = {
                confirmReset = false
                viewModel.resetLocalData()
            },
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmDemo) {
        ConfirmDialog(
            title = "Load demo data?",
            text = "Your current local data will be replaced with example sessions, goals and registrations.",
            confirmLabel = "Load demo",
            onConfirm = {
                confirmDemo = false
                viewModel.loadDemoData()
            },
            onDismiss = { confirmDemo = false }
        )
    }
}

@Composable
private fun ProfileEditor(
    draft: UserProfile,
    nameError: String?,
    onChange: ((UserProfile) -> UserProfile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Charcoal,
        title = { Text("Edit Profile", color = Rice) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DojoTextField(
                    value = draft.displayName,
                    onValueChange = { v -> onChange { it.copy(displayName = v.take(24)) } },
                    label = "Display name",
                    error = nameError,
                    capitalization = KeyboardCapitalization.Words
                )
                Overline("Karate style")
                ChipGrid(KarateStyle.entries, draft.style, { it.label }, { s -> onChange { it.copy(style = s) } }, columns = 2)
                Overline("Experience level")
                ChipGrid(ExperienceLevel.entries, draft.experience, { it.label }, { e -> onChange { it.copy(experience = e) } })
                Overline("Current belt")
                ChipGrid(Belt.entries, draft.belt, { it.label }, { b -> onChange { it.copy(belt = b) } }, columns = 4)
                DojoTextField(
                    value = draft.trainingGoal,
                    onValueChange = { v -> onChange { it.copy(trainingGoal = v.take(80)) } },
                    label = "Training goal (optional)",
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                )
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onSave) { Text("Save", color = DojoRed) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onCancel) { Text("Cancel", color = StoneLight) } }
    )
}
