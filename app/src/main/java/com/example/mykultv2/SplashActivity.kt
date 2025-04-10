package com.example.mykultv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin



class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedPrefs = getSharedPreferences("MyKultV2Prefs", MODE_PRIVATE)
            val isDarkMode = sharedPrefs.getBoolean("isDarkMode", false)
            val colorScheme = if (isDarkMode) DarkCultureColorScheme else LightCultureColorScheme

            MaterialTheme(colorScheme = colorScheme) {
                EnhancedSplashScreen()
            }
        }
    }

    @Composable
    fun EnhancedSplashScreen() {
        var showLogo by remember { mutableStateOf(false) }
        var showTitle by remember { mutableStateOf(false) }
        var showIcons by remember { mutableStateOf(false) }
        var showTagline by remember { mutableStateOf(false) }

        val logoScale = remember { Animatable(0.6f) }
        val logoRotation = remember { Animatable(0f) }
        val waveOffset = remember { Animatable(0f) }
        val waveAlpha = remember { Animatable(0f) }
        val iconScale = remember { Animatable(0f) }
        val iconOrbit = remember { Animatable(0f) }

        // Ensure colorScheme is accessed correctly
        val colorScheme = MaterialTheme.colorScheme
        val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }

        LaunchedEffect(Unit) {
            waveAlpha.animateTo(0.3f, animationSpec = tween(1000))

            launch {
                while (true) {
                    waveOffset.animateTo(1f, animationSpec = tween(2000))
                    waveOffset.snapTo(0f)
                }
            }

            delay(500)
            showLogo = true
            delay(300)
            logoScale.animateTo(1f, animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ))

            launch {
                while (true) {
                    logoScale.animateTo(1.05f, animationSpec = tween(1000))
                    logoScale.animateTo(1f, animationSpec = tween(1000))
                }
            }

            logoRotation.animateTo(360f, animationSpec = tween(1500))

            delay(300)
            showTitle = true
            delay(500)
            showIcons = true
            iconScale.animateTo(1f, animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            ))

            launch {
                iconOrbit.animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }

            delay(500)
            showTagline = true

            delay(1500)

            val sharedPrefs = getSharedPreferences("CulturePrefs", MODE_PRIVATE)
            val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)
            val intent = if (onboardingCompleted) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(waveAlpha.value)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val offset = waveOffset.value * 100

                for (i in 0 until 3) {
                    val path = Path()
                    val yOffset = (i * 200) + offset

                    path.moveTo(0f, canvasHeight * 0.2f + yOffset)

                    for (x in 0..canvasWidth.toInt() step 40) {
                        val y = canvasHeight * 0.2f + sin(x / 50f) * 30 + yOffset
                        path.lineTo(x.toFloat(), y)
                    }

                    path.lineTo(canvasWidth, canvasHeight * 0.2f + yOffset)

                    drawPath(
                        path = path,
                        color = colorScheme.secondary.copy(alpha = 0.1f - (i * 0.02f)),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(
                    visible = showLogo,
                    enter = fadeIn(animationSpec = tween(500)) +
                            slideInVertically(
                                initialOffsetY = { -200 },
                                animationSpec = tween(500)
                            )
                ) {
                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(logoScale.value)
                                .clip(CircleShape)
                                .background(colorScheme.primary.copy(alpha = 0.7f))
                        )

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .rotate(logoRotation.value)
                                .scale(logoScale.value)
                                .clip(CircleShape)
                                .background(colorScheme.surface.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img),
                                contentDescription = "MyKult Logo",
                                modifier = Modifier.size(100.dp)
                            )
                        }

                        if (showIcons) {
                            val radius = 110.dp
                            val iconSize = 34.dp
                            val iconCount = 4

                            for (i in 0 until iconCount) {
                                val angle = (i * (360f / iconCount) + iconOrbit.value) % 360
                                val density = LocalDensity.current
                                val radiusPx = with(density) { radius.toPx() }

                                val x = cos(Math.toRadians(angle.toDouble())).toFloat() * radiusPx
                                val y = sin(Math.toRadians(angle.toDouble())).toFloat() * radiusPx

                                val icon = when (i) {
                                    0 -> Icons.Default.MusicNote
                                    1 -> Icons.Default.Movie
                                    2 -> Icons.Default.Book
                                    else -> Icons.Default.ColorLens
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = x.dp, y = y.dp)
                                        .size(iconSize)
                                        .scale(iconScale.value)
                                        .clip(CircleShape)
                                        .background(colorScheme.secondary)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn(animationSpec = tween(800))
                ) {
                    Text(
                        text = "MyKult",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = showTagline,
                    enter = fadeIn(animationSpec = tween(800)),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Explore • Discover • Experience",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onBackground.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(
                    visible = showTagline,
                    enter = fadeIn(animationSpec = tween(800)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "",
                            fontSize = 12.sp,
                            color = colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

}