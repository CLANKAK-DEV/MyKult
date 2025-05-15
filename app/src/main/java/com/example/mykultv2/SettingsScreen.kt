package com.mykult.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    navController: NavHostController,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyKultV2Prefs", Context.MODE_PRIVATE)

    var showPrivacySecurity by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showEditPasswordDialog by remember { mutableStateOf(false) }

    // State for Edit Password form
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val user: FirebaseUser? = auth.currentUser

    fun clearAppData() {
        CoroutineScope(Dispatchers.IO).launch {
            // Clear SharedPreferences (e.g., favorites, recently watched)
            val prefsEditor = prefs.edit()
            prefsEditor.clear()
            prefsEditor.apply()

            // Clear in-memory data
            FavoritesManager.clearFavorites()
            RecentlyWatchedManager.clearRecentlyWatched()

            // Navigate ba
            //
            // ck to Home and clear back stack
            withContext(Dispatchers.Main) {
                navController.popBackStack("Home", inclusive = false)
            }
        }
    }

    suspend fun updatePassword() {
        if (user == null) {
            withContext(Dispatchers.Main) {
                errorMessage = "User not logged in."
            }
            return
        }

        if (newPassword != confirmNewPassword) {
            withContext(Dispatchers.Main) {
                errorMessage = "New passwords do not match."
            }
            return
        }

        if (newPassword.length < 6) {
            withContext(Dispatchers.Main) {
                errorMessage = "New password must be at least 6 characters."
            }
            return
        }

        withContext(Dispatchers.Main) {
            isLoading = true
            errorMessage = null
        }

        try {
            val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Password updated successfully.", Toast.LENGTH_LONG).show()
                showEditPasswordDialog = false
                currentPassword = ""
                newPassword = ""
                confirmNewPassword = ""
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                errorMessage = "Error updating password: ${e.message}"
            }
        } finally {
            withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        item {
            SettingsItem(
                icon = Icons.Outlined.Lock,
                title = "Privacy and Security",
                onClick = { showPrivacySecurity = !showPrivacySecurity },
                accentColor = MaterialTheme.colorScheme.primary,
                colorScheme = MaterialTheme.colorScheme
            )
            AnimatedVisibility(
                visible = showPrivacySecurity,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column {
                    SettingsSubItem(
                        icon = Icons.Outlined.Info,
                        title = "Privacy Policy",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy-policy"))
                            context.startActivity(intent)
                        },
                        accentColor = MaterialTheme.colorScheme.primary,
                        colorScheme = MaterialTheme.colorScheme
                    )
                    SettingsSubItem(
                        icon = Icons.Outlined.Lock,
                        title = "Change Password",
                        onClick = { showEditPasswordDialog = true },
                        accentColor = MaterialTheme.colorScheme.primary,
                        colorScheme = MaterialTheme.colorScheme
                    )
                    SettingsSubItem(
                        icon = Icons.Outlined.Delete,
                        title = "Clear Data",
                        onClick = { showClearDataDialog = true },
                        accentColor = MaterialTheme.colorScheme.primary,
                        colorScheme = MaterialTheme.colorScheme
                    )
                }
            }
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.DarkMode,
                title = "Theme",
                onClick = {
                    onThemeChange(!isDarkMode)
                },
                accentColor = MaterialTheme.colorScheme.primary,
                colorScheme = MaterialTheme.colorScheme
            ) {
                Text(
                    text = if (isDarkMode) "Dark Mode" else "Light Mode",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.HelpOutline,
                title = "Help and Support",
                onClick = { navController.navigate("HelpAndSupport") },
                accentColor = MaterialTheme.colorScheme.primary,
                colorScheme = MaterialTheme.colorScheme
            )
        }


    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Data", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to clear all app data? This will remove your favorites and history.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        clearAppData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearDataDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showEditPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditPasswordDialog = false
                currentPassword = ""
                newPassword = ""
                confirmNewPassword = ""
                errorMessage = null
            },
            title = { Text("Change Password", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password", color = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password", color = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirm New Password", color = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            updatePassword()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isLoading && currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmNewPassword.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showEditPasswordDialog = false
                        currentPassword = ""
                        newPassword = ""
                        confirmNewPassword = ""
                        errorMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    accentColor: Color,
    colorScheme: ColorScheme,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            content()
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Arrow",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsSubItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    accentColor: Color,
    colorScheme: ColorScheme,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surface.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        content()
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = "Arrow",
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
    }
}