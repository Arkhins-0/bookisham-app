package com.bookisham.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bookisham.app.ui.theme.Ink

enum class AdminSection { Readers, Books, Purchases }

/** The Readers / Books / Purchases switcher — the admin's own tab nav, mirroring the web layout. */
@Composable
fun AdminSectionTabs(current: AdminSection, onSelect: (AdminSection) -> Unit) {
    Row(
        Modifier
            .background(Color.Black.copy(alpha = 0.05f), CircleShape)
            .padding(3.dp),
    ) {
        SectionTab("Readers", Icons.Outlined.Group, current == AdminSection.Readers) { onSelect(AdminSection.Readers) }
        SectionTab("Books", Icons.AutoMirrored.Outlined.LibraryBooks, current == AdminSection.Books) { onSelect(AdminSection.Books) }
        SectionTab("Purchases", Icons.Outlined.CreditCard, current == AdminSection.Purchases) { onSelect(AdminSection.Purchases) }
    }
}

@Composable
private fun SectionTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Color.White else Color.Transparent,
        contentColor = Ink,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
