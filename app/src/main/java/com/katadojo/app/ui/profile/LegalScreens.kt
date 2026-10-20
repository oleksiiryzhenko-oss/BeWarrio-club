package com.katadojo.app.ui.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katadojo.app.ui.components.DojoCard
import com.katadojo.app.ui.components.DojoTopBar
import com.katadojo.app.ui.components.ScreenList
import com.katadojo.app.ui.theme.Rice
import com.katadojo.app.ui.theme.Stone
import com.katadojo.app.ui.theme.StoneLight

private data class LegalSection(val title: String, val body: String)

private val privacySections = listOf(
    LegalSection(
        "Overview",
        "BeеWarrior club is an offline karate training companion. It works without an online account and without a backend server. This policy explains what information the app stores and where."
    ),
    LegalSection(
        "Registration information is stored locally",
        "When you register for a dojo training, the name, email, phone number, experience level, optional belt, preferred training type and comment you enter are saved in a database on your device. They are not sent to BeеWarrior club, to a dojo or to any other party."
    ),
    LegalSection(
        "Training and performance data are stored locally",
        "Kata sessions, section results, self-assessment ratings, workouts, goals, Training Points and statistics are calculated and stored only on your device."
    ),
    LegalSection(
        "No online account",
        "You never need to create an account. Your local profile (display name, karate style, experience, belt and training goal) stays on the device."
    ),
    LegalSection(
        "No sensitive information is intentionally collected",
        "The app does not ask for health data, precise location, contacts, photos or payment details. Please avoid entering sensitive personal information in free-text fields such as comments."
    ),
    LegalSection(
        "No analytics or advertising",
        "BeеWarrior club contains no advertising, no analytics SDKs and no tracking. The app does not request internet access."
    ),
    LegalSection(
        "Deleting your information",
        "Use Profile > Reset Local Data to delete all locally stored information at any time. Uninstalling the app also removes it. Device backups are disabled for the app's data."
    )
)

private val termsSections = listOf(
    LegalSection(
        "Personal training tool",
        "BeеWarrior club helps you organize and reflect on your karate practice. It does not replace instruction from a qualified instructor."
    ),
    LegalSection(
        "Self-assessment, not grading",
        "Personal Kata Scores, accuracy percentages and section results are based on your own ratings. They are personal training metrics, not official instructor grades or examination results."
    ),
    LegalSection(
        "App Progress Levels are not ranks",
        "Training Points (XP) and App Progress Levels such as White, Green or Black are a motivational feature of this app only. They do not represent, imply or replace an official karate rank, belt or promotion."
    ),
    LegalSection(
        "Kata content",
        "Kata names, sequences and availability differ between karate styles and organizations. The section descriptions are practice guides; follow your own dojo's version."
    ),
    LegalSection(
        "Dojo listings",
        "Dojo sessions shown in the app are example listings stored on the device. Registrations are saved locally and are not transmitted to any dojo."
    ),
    LegalSection(
        "Safety",
        "Warm up properly, train within your limits and stop if you feel pain or dizziness. Consult a medical professional before starting a new training program if you have health concerns."
    ),
    LegalSection(
        "No gambling",
        "BeеWarrior club contains no betting, wagering or gambling features. Training Points have no monetary value and cannot be exchanged."
    )
)

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) = LegalScreen("Privacy Policy", privacySections, onBack)

@Composable
fun TermsScreen(onBack: () -> Unit) = LegalScreen("Terms of Use", termsSections, onBack)

@Composable
private fun LegalScreen(title: String, sections: List<LegalSection>, onBack: () -> Unit) {
    ScreenList(topBar = { DojoTopBar(title, onBack = onBack) }) {
        sections.forEach { section ->
            item {
                DojoCard {
                    Text(section.title, style = MaterialTheme.typography.titleMedium, color = Rice)
                    Spacer(Modifier.height(6.dp))
                    Text(section.body, style = MaterialTheme.typography.bodyMedium, color = StoneLight)
                }
            }
        }
        item { Text("Last updated: September 2026", style = MaterialTheme.typography.bodySmall, color = Stone) }
    }
}
