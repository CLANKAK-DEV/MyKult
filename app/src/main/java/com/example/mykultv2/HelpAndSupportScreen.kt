package com.mykult.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
@Composable
fun HelpAndSupportScreen(navController: NavHostController) {
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

    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun sendSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("lahoucinechouker@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "From: $email\n\n$message")
        }
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    }

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
                    text = "Aide et Support",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CultureColorScheme.onBackground
                )
            }
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Votre Email", color = CultureColorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CultureColorScheme.primary,
                    unfocusedBorderColor = CultureColorScheme.onSurface,
                    focusedLabelColor = CultureColorScheme.primary,
                    cursorColor = CultureColorScheme.primary,
                    focusedTextColor = CultureColorScheme.onSurface,
                    unfocusedTextColor = CultureColorScheme.onSurface
                )
            )
        }

        item {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Sujet", color = CultureColorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CultureColorScheme.primary,
                    unfocusedBorderColor = CultureColorScheme.onSurface,
                    focusedLabelColor = CultureColorScheme.primary,
                    cursorColor = CultureColorScheme.primary,
                    focusedTextColor = CultureColorScheme.onSurface,
                    unfocusedTextColor = CultureColorScheme.onSurface
                )
            )
        }

        item {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message", color = CultureColorScheme.onSurface) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CultureColorScheme.primary,
                    unfocusedBorderColor = CultureColorScheme.onSurface,
                    focusedLabelColor = CultureColorScheme.primary,
                    cursorColor = CultureColorScheme.primary,
                    focusedTextColor = CultureColorScheme.onSurface,
                    unfocusedTextColor = CultureColorScheme.onSurface
                )
            )
        }

        item {
            Button(
                onClick = {
                    sendSupportEmail()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotEmpty() && subject.isNotEmpty() && message.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CultureColorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Envoyer",
                    fontSize = 16.sp,
                    color = CultureColorScheme.onPrimary
                )
            }
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