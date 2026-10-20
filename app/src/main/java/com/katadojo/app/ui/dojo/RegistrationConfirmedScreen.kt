package com.katadojo.app.ui.dojo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.R
import com.katadojo.app.data.model.workoutType
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.RegistrationConfirmedViewModel

@Composable
fun RegistrationConfirmedScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onTab: (String) -> Unit,
    viewModel: RegistrationConfirmedViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state = viewModel.uiState
    ScreenList(topBar = { DojoTopBar("", onBack = onBack) }) {
        if (state.loading) {
            item { LoadingBox(Modifier.height(300.dp)) }
            return@ScreenList
        }
        val data = state.data
        if (data == null) {
            item {
                EmptyState(
                    title = "Registration not found",
                    message = "It may have been cancelled or local data was reset.",
                    actionLabel = "Back to Training",
                    onAction = { onTab(Routes.DOJO) }
                )
            }
            return@ScreenList
        }
        val training = data.training
        val registration = data.registration

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Enso(Modifier.size(150.dp), color = DojoRed, alpha = 0.9f)
                    Box(
                        Modifier.size(84.dp).clip(CircleShape).background(DojoRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Rice, modifier = Modifier.size(48.dp))
                    }
                }
                Text(
                    "Registration Confirmed!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Rice,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "See you in the dojo, ${registration.fullName.substringBefore(' ')}. Your place is saved on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StoneLight,
                    textAlign = TextAlign.Center
                )
            }
        }
        item {
            DojoCard(accent = DojoRed) {
                InfoRow("Training", training.title)
                InfoRow("Date", "${Formatters.longDate(training.date)}")
                InfoRow("Time", Formatters.timeRange(training.startTime, training.endTime))
                InfoRow("Dojo", training.dojo)
                InfoRow("Instructor", training.instructor)
            }
        }
        item {
            DojoCard {
                Overline("Registered as")
                Spacer(Modifier.height(4.dp))
                InfoRow("Name", registration.fullName)
                InfoRow("Email", registration.email)
                InfoRow("Phone", registration.phone)
                InfoRow("Experience", registration.experience.label)
                registration.currentBelt?.let { InfoRow("Current belt", it.label) }
                registration.preferredType?.let { InfoRow("Preferred type", it.label) }
                if (registration.comment.isNotBlank()) InfoRow("Comment", registration.comment)
            }
        }
        item {
            PrimaryButton(
                "Start Training",
                onClick = { onNavigate(Routes.workoutSetup(training.type.workoutType)) },
                icon = painterResource(R.drawable.ic_timer),
                height = 60.dp
            )
        }
        item { SecondaryButton("View Training", onClick = { onNavigate(Routes.trainingDetail(training.id)) }) }
        item {
            TextButton(onClick = { onTab(Routes.HOME) }, modifier = Modifier.fillMaxWidth()) {
                Text("BACK TO HOME", style = MaterialTheme.typography.labelLarge, color = StoneLight)
            }
        }
    }
}
