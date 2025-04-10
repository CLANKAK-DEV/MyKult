package com.example.mykultv2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
@Composable
fun AboutScreen(navController: NavHostController) {
    val CultureColorScheme = lightColorScheme(
        primary = Color(0x80000000),
        secondary = Color(0x4D000000),
        background = Color.Transparent,
        surface = Color(0xFFFFFFFF),
        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        onSurface = Color(0xFF000000)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CultureColorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = CultureColorScheme.onBackground,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "À Propos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CultureColorScheme.onBackground
                )
            }
        }

        item {
            Text(
                text = "MyKultV2 est une application dédiée à la découverte de films, livres et musiques. Version 1.0.0. Développé par [Votre Nom/Équipe].",
                fontSize = 16.sp,
                color = CultureColorScheme.onBackground.copy(alpha = 0.8f)
            )
        }

        item {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CultureColorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CultureColorScheme.primary
                )
            ) {
                Text(
                    text = "Retour",
                    fontSize = 16.sp,
                    color = CultureColorScheme.primary
                )
            }
        }
    }
}