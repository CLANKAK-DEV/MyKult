package com.mykult.app

import androidx.compose.foundation.clickable
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
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SignupState(val username: String = "", val email: String = "", val password: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(SignupState()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sign Up") },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = { state = state.copy(username = it) },
                label = { Text("Username") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

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
                            if (state.username.isBlank()) throw Exception("Username is required")
                            if (state.email.isBlank() || !isEmailValid(state.email)) throw Exception("Invalid email")
                            if (state.password.isBlank()) throw Exception("Password is required")
                            if (state.password.length < 6) throw Exception("Password must be at least 6 characters")

                            val authResult = withContext(Dispatchers.IO) {
                                auth.createUserWithEmailAndPassword(state.email, state.password).await()
                            }
                            val user = authResult.user ?: throw Exception("User creation failed")

                            // Update the user's display name with the username
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(state.username)
                                .build()
                            withContext(Dispatchers.IO) {
                                user.updateProfile(profileUpdates).await()
                            }

                            snackbarHostState.showSnackbar("Signup successful!")
                            navController.navigate("Home") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        } catch (e: FirebaseAuthException) {
                            val errorMessage = when (e.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "Invalid email format."
                                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
                                "ERROR_WEAK_PASSWORD" -> "Password is too weak."
                                else -> "Signup failed: ${e.message ?: "Unknown error"}"
                            }
                            snackbarHostState.showSnackbar(errorMessage)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Signup failed: ${e.message ?: "Unknown error"}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Already have an account? Login",
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable {
                        navController.navigate("Login") {
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