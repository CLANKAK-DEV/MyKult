package com.mykult.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Data class for bottom navigation items
data class BottomNavItem(val label: String, val icon: Int, val route: String)

// Placeholder for SessionManager
object SessionManager {
    fun login(activity: MainActivity, userId: String) {
        Log.d("SessionManager", "User logged in: $userId")
    }
}

@Composable
fun CultureBottomNavigation(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", R.drawable.ic_home, "home"),
        BottomNavItem("Movies", R.drawable.ic_movie, "films"),
        BottomNavItem("Books", R.drawable.ic_book, "books"),
        BottomNavItem("Music", R.drawable.ic_music, "music")
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                    unselectedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "MainActivity"

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                showErrorMessage("Google sign-in failed: ${e.statusCode}")
                Log.e(TAG, "Google sign-in failed", e)
            }
        } else {
            showErrorMessage("Google sign-in cancelled")
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("256080357991-kcplqnkp0mgb399baoshjdr8ih7rndce.apps.googleusercontent.com")
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            val prefs = getSharedPreferences("MyKultV2Prefs", Context.MODE_PRIVATE)
            var isDarkMode by remember {
                mutableStateOf(prefs.getBoolean("isDarkMode", false))
            }

            val onThemeChange: (Boolean) -> Unit = { newDarkMode ->
                isDarkMode = newDarkMode
                with(prefs.edit()) {
                    putBoolean("isDarkMode", newDarkMode)
                    apply()
                }
            }

            CultureAppTheme(isDarkMode = isDarkMode) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                LaunchedEffect(Unit) {
                    auth.addAuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                }

                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        FavoritesManager.loadFavorites()
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            navController = navController,
                            onItemClick = { scope.launch { drawerState.close() } },
                            isLoggedIn = currentUser != null,
                            username = currentUser?.displayName ?: "Guest",
                            onLoginClick = {
                                navController.navigate("login")
                                scope.launch { drawerState.close() }
                            },
                            onSignupClick = {
                                navController.navigate("signup")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        "MyKult",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu",
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
                        bottomBar = {
                            if (currentRoute != "login" && currentRoute != "signup") {
                                CultureBottomNavigation(navController)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        MainScreen(
                            navController = navController,
                            isDarkMode = isDarkMode,
                            onThemeChange = onThemeChange,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val authResult = withContext(Dispatchers.IO) {
                    auth.signInWithCredential(credential).await()
                }
                val userId = authResult.user?.uid ?: throw Exception("User ID not found")
                val email = account.email ?: ""
                val username = account.displayName ?: "Google User"

                val userMap = hashMapOf(
                    "username" to username,
                    "email" to email,
                    "roomId" to "room_$userId",
                    "createdAt" to System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    firestore.collection("users").document(userId).set(userMap).await()
                }
                SessionManager.login(this@MainActivity, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-up failed: ${e.message}", e)
                showErrorMessage("Google sign-up failed: ${e.message ?: "Unknown error"}")
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: NavHostController,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(navController) }
            composable("films") { FilmsScreen(navController) }
            composable("books") { BooksScreen(navController) }
            composable("music") { MusiqueScreen(navController) }
            composable("filmDetail/{filmId}") { backStackEntry ->
                val filmId = backStackEntry.arguments?.getString("filmId") ?: ""
                FilmDetailScreen(navController = navController, filmId = filmId)
            }
            composable("bookDetail/{bookId}") { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                BookDetailScreen(navController = navController, bookId = bookId)
            }
            composable("musicDetail/{musicId}") { backStackEntry ->
                val musicId = backStackEntry.arguments?.getString("musicId") ?: ""
                MusicDetailScreen(navController = navController, musicId = musicId)
            }
            composable("albumDetail/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                MusicDetailScreen(navController = navController, musicId = albumId)
            }
            composable("artistProfile/{artistId}") { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                ArtistProfileScreen(artistId = artistId, navController = navController)
            }
            composable("authorProfile/{authorName}") { backStackEntry ->
                val authorName = backStackEntry.arguments?.getString("authorName") ?: ""
                AuthorProfileScreen(authorName = authorName, navController = navController)
            }
            composable("actorProfile/{actorId}") { backStackEntry ->
                val actorId = backStackEntry.arguments?.getString("actorId") ?: ""
                ActorProfileScreen(actorId = actorId, navController = navController)
            }
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignupScreen(navController) }
            composable("favorites") { FavoritesScreen(navController) }
            composable("recentlyWatched") { RecentlyWatchedScreen(navController) }
            composable("highestRate") { HighestRateScreen(navController) }
            composable("settings") {
                SettingsScreen(
                    navController = navController,
                    isDarkMode = isDarkMode,
                    onThemeChange = onThemeChange
                )
            }
            composable("about") { AboutScreen(navController) }
            composable("helpAndSupport") { HelpAndSupportScreen(navController) }
        }
    }
}

@Composable
fun EmptyPage(pageName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pageName,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}