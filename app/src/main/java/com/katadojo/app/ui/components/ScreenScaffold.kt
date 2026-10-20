package com.katadojo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.theme.*

@Composable
fun DojoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Ink)
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Rice)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Rice,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

/** Top bar of the four main sections: wordmark + top-right menu. */
@Composable
fun BrandTopBar(onNavigate: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Ink)
            .statusBarsPadding()
            .height(60.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DojoWordmark(Modifier.weight(1f))
        MainMenuButton(onNavigate)
    }
}

private data class MenuEntry(val label: String, val icon: ImageVector, val route: String)

private val menuEntries = listOf(
    MenuEntry("Profile", Icons.Filled.Person, Routes.PROFILE),
    MenuEntry("Personal Goals", Icons.Filled.Star, Routes.GOALS),
    MenuEntry("Training History", Icons.AutoMirrored.Filled.List, Routes.HISTORY),
    MenuEntry("Start a Workout", Icons.Filled.PlayArrow, Routes.WORKOUT_SETUP_DEFAULT),
    MenuEntry("Weekly Schedule", Icons.Filled.DateRange, Routes.SCHEDULE),
    MenuEntry("Notification Settings", Icons.Filled.Settings, Routes.SETTINGS),
    MenuEntry("Privacy Policy", Icons.Filled.Lock, Routes.PRIVACY),
    MenuEntry("Terms of Use", Icons.Filled.Info, Routes.TERMS)
)

@Composable
fun MainMenuButton(onNavigate: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Rice)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menuEntries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label, style = MaterialTheme.typography.bodyLarge, color = Rice) },
                    leadingIcon = { Icon(entry.icon, contentDescription = null, tint = StoneLight) },
                    onClick = {
                        expanded = false
                        onNavigate(entry.route)
                    }
                )
            }
        }
    }
}

/** Standard screen: a top bar above a padded lazy list. */
@Composable
fun ScreenList(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    Column(modifier.fillMaxSize().background(Ink)) {
        topBar()
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}
