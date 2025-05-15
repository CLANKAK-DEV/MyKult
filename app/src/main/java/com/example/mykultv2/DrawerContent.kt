package com.mykult.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DrawerContent(
    navController: NavHostController,
    onItemClick: () -> Unit,
    isLoggedIn: Boolean,
    username: String,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        DrawerHeader(navController, isLoggedIn, username, onItemClick, onLoginClick, onSignupClick)

        DrawerItem(
            icon = Icons.Default.Home,
            label = "Home",
            route = "Home",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.Movie,
            label = "Movies",
            route = "Films",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.Book,
            label = "Books",
            route = "Livres",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.MusicNote,
            label = "Music",
            route = "Musique",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.Favorite,
            label = "Favorites",
            route = "Favorites",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.History,
            label = "Recently Watched",
            route = "RecentlyWatched",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.StarRate,
            label = "Highest Rate",
            route = "HighestRate",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            route = "Settings",
            navController = navController,
            onItemClick = onItemClick
        )
        DrawerItem(
            icon = Icons.Default.Info,
            label = "About",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lahoucines.gitbook.io/mykultv2"))
                context.startActivity(intent)
                onItemClick()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isLoggedIn) {
            TextButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("Home") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                    onItemClick()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Logout",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DrawerHeader(
    navController: NavHostController,
    isLoggedIn: Boolean,
    username: String,
    onItemClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (isLoggedIn) {
                Text(
                    text = username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (!isLoggedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onLoginClick()
                        onItemClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        onSignupClick()
                        onItemClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "Signup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    route: String? = null,
    navController: NavHostController? = null,
    onItemClick: () -> Unit = {},
    onClick: () -> Unit = {} // Added onClick parameter for custom actions
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (route != null && navController != null) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
                onClick()
                onItemClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}