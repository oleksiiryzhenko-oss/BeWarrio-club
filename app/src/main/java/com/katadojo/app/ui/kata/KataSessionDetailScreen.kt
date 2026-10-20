package com.katadojo.app.ui.kata

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.KataSessionDetailViewModel

@Composable
fun KataSessionDetailScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: KataSessionDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.session
    val kata = session?.kata
    val analysis = state.analysis

    ScreenList(topBar = { DojoTopBar(session?.let { Formatters.longDate(it.date) } ?: "Session", onBack = onBack) }) {
        when {
            state.loading -> item { LoadingBox(Modifier.height(300.dp)) }
            session == null || kata == null || analysis == null -> item {
                EmptyState("Session not found", "This session may have been deleted.", actionLabel = "Back", onAction = onBack)
            }
            else -> {
                item { SessionSummaryCard(session, kata, analysis) }
                item { AnalysisCard(session, analysis) }
                if (session.isFullKata) {
                    item { SectionResultsList(kata, session) }
                    item {
                        DojoCard {
                            Overline("Self-assessment")
                            InfoRow("Technique", "${session.technique} / 10")
                            InfoRow("Balance", "${session.balance} / 10")
                            InfoRow("Timing", "${session.timing} / 10")
                            InfoRow("Confidence", "${session.confidence} / 10")
                            InfoRow("Perceived difficulty", session.perceivedDifficulty?.label ?: "-")
                        }
                    }
                }
                state.progress?.let { p -> item { KataProgressCard(p) } }
                item { PrimaryButton("Practice ${kata.name}", onClick = { onNavigate(Routes.kataCoach(kata.id)) }) }
                item { SecondaryButton("Open Kata", onClick = { onNavigate(Routes.kataDetail(kata.id)) }, height = 46.dp) }
            }
        }
    }
}
