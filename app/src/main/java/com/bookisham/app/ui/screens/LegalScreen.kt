package com.bookisham.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookisham.app.LocalApp
import com.bookisham.app.data.LegalDocs
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.InkSoft
import com.bookisham.app.ui.theme.Paper

/**
 * The Terms & Conditions or the Privacy Policy, fetched as data from the
 * server (the same text the website shows) and rendered natively. Reachable
 * signed out (from signup) and signed in (from Account); [standalone] adds
 * the system-bar padding the signed-in Scaffold otherwise provides.
 */
@Composable
fun LegalScreen(kind: String, standalone: Boolean, onBack: () -> Unit) {
    val app = LocalApp.current
    var docs by remember { mutableStateOf<LegalDocs?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        error = null
        runCatching { app.api.legal() }
            .onSuccess { docs = it }
            .onFailure { error = it.message ?: "Could not load this page." }
    }

    val doc = if (kind == "privacy") docs?.privacy else docs?.terms

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .then(if (standalone) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = InkFaint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Back", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
        }

        when {
            doc != null -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            ) {
                item {
                    Eyebrow("Legal")
                    Spacer(Modifier.height(8.dp))
                    Text(doc.title, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Last updated ${doc.updated}", style = MaterialTheme.typography.bodySmall, color = InkFaint)
                    Spacer(Modifier.height(24.dp))
                }
                items(doc.sections, key = { it.heading }) { section ->
                    Text(section.heading, style = MaterialTheme.typography.titleLarge)
                    section.paragraphs.forEach { p ->
                        Spacer(Modifier.height(8.dp))
                        Text(p, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            error != null -> Column(Modifier.padding(20.dp)) {
                ErrorNote(error!!)
                Spacer(Modifier.height(12.dp))
                Pill("Try again", onClick = { attempt++ }, style = PillStyle.Ghost, compact = true)
            }
            else -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Ember)
            }
        }
    }
}
