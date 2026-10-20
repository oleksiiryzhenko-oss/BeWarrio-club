package com.katadojo.app.ui.dojo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.data.model.Belt
import com.katadojo.app.data.model.ExperienceLevel
import com.katadojo.app.data.model.TrainingType
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    onBack: () -> Unit,
    onConfirmed: (Long) -> Unit,
    viewModel: RegistrationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val training by viewModel.training.collectAsStateWithLifecycle()
    val form = viewModel.form
    val errors = viewModel.errors

    LaunchedEffect(viewModel.confirmedRegistrationId) {
        viewModel.confirmedRegistrationId?.let {
            viewModel.onConfirmationShown()
            onConfirmed(it)
        }
    }

    Column(Modifier.fillMaxSize().background(Ink)) {
        DojoTopBar("Join Training", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            training?.let { t ->
                DojoCard(accent = DojoRed) {
                    Overline(t.type.label, color = DojoRed)
                    Text(t.title, style = MaterialTheme.typography.titleLarge, color = Rice)
                    Text(
                        "${Formatters.date(t.date)} · ${Formatters.timeRange(t.startTime, t.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StoneLight
                    )
                    Text("${t.dojo} · ${t.instructor} · ${t.style.label}", style = MaterialTheme.typography.bodySmall, color = Stone)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${t.placesLeft} of ${t.capacity} places available",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (t.isFull) ErrorRed else SuccessGreen
                    )
                }
            }

            Text(
                "No account needed. Your details are saved only on this device and shown to you at check-in.",
                style = MaterialTheme.typography.bodySmall,
                color = Stone
            )

            SectionTitle("Your details")
            DojoTextField(
                value = form.fullName,
                onValueChange = { v -> viewModel.update { it.copy(fullName = v) } },
                label = "Full Name *",
                error = errors.fullName,
                capitalization = KeyboardCapitalization.Words
            )
            DojoTextField(
                value = form.email,
                onValueChange = { v -> viewModel.update { it.copy(email = v) } },
                label = "Email *",
                error = errors.email,
                keyboardType = KeyboardType.Email
            )
            DojoTextField(
                value = form.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = "Phone Number *",
                error = errors.phone,
                keyboardType = KeyboardType.Phone
            )

            SectionTitle("Experience level *")
            ChipGrid(
                options = ExperienceLevel.entries,
                selected = form.experience,
                label = { it.label },
                onSelect = { level -> viewModel.update { it.copy(experience = level) } }
            )
            errors.experience?.let { ErrorText(it) }

            SectionTitle("Current belt (optional)")
            ChipGrid(
                options = listOf<Belt?>(null) + Belt.entries,
                selected = form.belt,
                label = { it?.label ?: "Not set" },
                onSelect = { belt -> viewModel.update { it.copy(belt = belt) } }
            )

            SectionTitle("Preferred training type (optional)")
            ChipGrid(
                options = listOf<TrainingType?>(null) + TrainingType.entries,
                selected = form.preferredType,
                label = { it?.label ?: "No preference" },
                onSelect = { type -> viewModel.update { it.copy(preferredType = type) } }
            )

            DojoTextField(
                value = form.comment,
                onValueChange = { v -> viewModel.update { it.copy(comment = v.take(320)) } },
                label = "Comment (optional)",
                error = errors.comment,
                helper = "${form.comment.length} / 300 · injuries, goals or questions for the instructor",
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
                singleLine = false,
                minLines = 3
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.update { it.copy(consent = !it.consent) } }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = form.consent,
                    onCheckedChange = { checked -> viewModel.update { it.copy(consent = checked) } },
                    colors = CheckboxDefaults.colors(
                        checkedColor = DojoRed,
                        uncheckedColor = if (errors.consent != null) ErrorRed else Stone,
                        checkmarkColor = Rice
                    )
                )
                Text(
                    "I agree to the processing of my information for training registration purposes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Rice
                )
            }
            errors.consent?.let { ErrorText(it) }

            viewModel.message?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.titleSmall,
                    color = ErrorRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.1f))
                        .padding(12.dp)
                )
            }

            PrimaryButton(
                if (viewModel.submitting) "Submitting..." else "Submit Registration",
                onClick = viewModel::submit,
                enabled = !viewModel.submitting && training?.isFull != true,
                height = 60.dp
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.padding(start = 4.dp))
}
