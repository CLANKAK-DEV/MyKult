package com.example.mykultv2


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CategoryChip(
    category: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category) },
        modifier = Modifier.border(width = 0.dp, color = Color.Transparent),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4A00E0),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFFE0E0E0),
            labelColor = Color.Black
        )
    )
}