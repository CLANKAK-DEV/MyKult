package com.mykult.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class LoginState(val email: String = "", val password: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(LoginState()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val auth = FirebaseAuth.getInstance()
    var clickCount by remember { mutableStateOf(0) } // Track the number of clicks

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Login") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .clickable(
                    onClick = {
                        clickCount++
                        if (clickCount >= 3) {
                            // Auto-fill the email and password fields
                            state = state.copy(
                                email = "test123456@gmail.com",
                                password = "test123456"
                            )
                            clickCount = 0 // Reset the counter after auto-filling
                        }
                    },
                    indication = null, // No ripple effect for the click
                    interactionSource = remember { MutableInteractionSource() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = state.email,
                onValueChange = { state = state.copy(email = it) },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = { state = state.copy(password = it) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    scope.launch {
                        try {
                            if (state.email.isBlank() || !isEmailValid(state.email)) throw Exception("Invalid email")
                            if (state.password.isBlank()) throw Exception("Password is required")

                            val authResult = withContext(Dispatchers.IO) {
                                auth.signInWithEmailAndPassword(state.email, state.password).await()
                            }
                            authResult.user?.uid ?: throw Exception("User ID not found")

                            snackbarHostState.showSnackbar("Login successful!")
                            navController.navigate("Home") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        } catch (e: FirebaseAuthException) {
                            val errorMessage = when (e.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "Invalid email format."
                                "ERROR_WRONG_PASSWORD" -> "Incorrect password."
                                "ERROR_USER_NOT_FOUND" -> "User not found."
                                else -> "Login failed: ${e.message ?: "Unknown error"}"
                            }
                            snackbarHostState.showSnackbar(errorMessage)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Login failed: ${e.message ?: "Unknown error"}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Don't have an account? Sign Up",
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable {
                        navController.navigate("Signup") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper function to validate email format
fun isEmailValid(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}