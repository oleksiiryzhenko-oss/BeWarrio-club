package com.katadojo.app.ui.kata

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.data.model.KataLibrary
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.KataCardState
import com.katadojo.app.viewmodel.KataLibraryViewModel

@Composable
fun KataLibraryScreen(
    onNavigate: (String) -> Unit,
    viewModel: KataLibraryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenList(topBar = { BrandTopBar(onNavigate) }) {
        item {
            Column {
                Text("Kata", style = MaterialTheme.typography.headlineLarge, color = Rice)
                Text("Practice section by section with Kata Coach.", style = MaterialTheme.typography.bodyMedium, color = Stone)
            }
        }
        item {
            DojoCard(containerColor = CharcoalLight, contentPadding = PaddingValues(14.dp)) {
                Text(KataLibrary.STYLE_DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = StoneLight)
            }
        }
        if (!state.loading && state.totalSessions == 0) {
            item {
                EmptyState(
                    title = "Your kata journey starts here.",
                    message = "Complete your first session to see your progress.",
                    actionLabel = "Start Heian Shodan",
                    onAction = { onNavigate(Routes.kataCoach("heian_shodan")) }
                )
            }
        }
        state.groups.forEach { (difficulty, cards) ->
            item(key = "h_" + difficulty.name) { SectionTitle(difficulty.label) }
            cards.forEach { card ->
                item(key = card.kata.id) { KataCard(card) { onNavigate(Routes.kataDetail(card.kata.id)) } }
            }
        }
    }
}

@Composable
private fun KataCard(card: KataCardState, onClick: () -> Unit) {
    val kata = card.kata
    val p = card.progress
    DojoCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(p.masteryPercent / 100f, Modifier.size(62.dp), strokeWidth = 6.dp) {
                Text("${p.masteryPercent}%", style = MaterialTheme.typography.titleSmall, color = Rice)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(kata.name, style = MaterialTheme.typography.titleLarge, color = Rice)
                Text("${kata.difficulty.label} · ${kata.durationText} · ${kata.sectionCount} sections", style = MaterialTheme.typography.bodySmall, color = Stone)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (p.lastPracticed == null) "Not practiced yet"
                    else "Best ${p.bestScore?.let { Formatters.score(it) } ?: "-"} · Last ${Formatters.relativeDay(p.lastPracticed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.lastPracticed == null) StoneDark else Beige
                )
            }
        }
    }
}
