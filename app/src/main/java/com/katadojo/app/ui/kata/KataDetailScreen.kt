package com.katadojo.app.ui.kata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.R
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.utils.HistoryItem
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.KataDetailViewModel

@Composable
fun KataDetailScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: KataDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val kata = state.kata

    Column(Modifier.fillMaxSize().background(Ink)) {
        DojoTopBar("Kata", onBack = onBack)
        if (kata == null) {
            Box(Modifier.padding(16.dp)) {
                EmptyState("Kata not found", "This kata is not in the library.", actionLabel = "Back", onAction = onBack)
            }
            return@Column
        }
        val progress = state.progress
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DojoHero(overline = "Kata · ${kata.difficulty.label}", title = kata.name, subtitle = kata.styleNote, minHeight = 170.dp)
            }
            item {
                DojoCard {
                    InfoRow("Difficulty", kata.difficulty.label)
                    InfoRow("Estimated duration", kata.durationText)
                    InfoRow("Sections", "${kata.sectionCount}")
                    InfoRow("Last score", progress?.currentScore?.let { "${Formatters.score(it)} / 10" } ?: "-")
                    InfoRow("Last practiced", progress?.lastPracticed?.let { Formatters.date(it) } ?: "Not yet")
                    Spacer(Modifier.height(8.dp))
                    Overline("Progress")
                    Spacer(Modifier.height(6.dp))
                    ThickProgressBar((progress?.masteryPercent ?: 0) / 100f)
                    Text("${progress?.masteryPercent ?: 0}% of sections completed in the latest session", style = MaterialTheme.typography.bodySmall, color = Stone)
                    Spacer(Modifier.height(12.dp))
                    Text(kata.description, style = MaterialTheme.typography.bodyMedium, color = StoneLight)
                }
            }
            item { SectionBreakdownCard(state.sectionStats, state.lastFull) }
            if (progress != null) item { KataProgressCard(progress) }
            if (state.recent.isNotEmpty()) {
                item { SectionTitle("Recent sessions") }
                items(state.recent, key = { it.id }) { s ->
                    HistoryRow(HistoryItem.Kata(s)) { onNavigate(Routes.kataSession(s.id)) }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().background(Charcoal).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton("Start Kata Session", onClick = { onNavigate(Routes.kataCoach(kata.id)) }, icon = painterResource(R.drawable.ic_enso), height = 58.dp)
            progress?.weakestSection?.let { weak ->
                SecondaryButton("Practice Section ${weak.number} only", onClick = { onNavigate(Routes.kataCoach(kata.id, weak.index)) }, height = 46.dp)
            }
        }
    }
}
