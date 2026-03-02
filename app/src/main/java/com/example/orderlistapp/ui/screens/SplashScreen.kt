package com.example.orderlistapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    // Animation states
    val iconScale = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Icon pops in
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(200)
        // Title fades in
        titleAlpha.animateTo(1f, animationSpec = tween(500))
        delay(150)
        // Subtitle fades in
        subtitleAlpha.animateTo(1f, animationSpec = tween(400))
        delay(150)
        // Tagline fades in
        taglineAlpha.animateTo(1f, animationSpec = tween(400))
        delay(1200)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B4B),  // deep navy top
                        Color(0xFF1A237E),  // indigo mid
                        Color(0xFF283593)   // slightly lighter bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App Icon / Logo area
            Box(
                modifier = Modifier
                    .scale(iconScale.value)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF42A5F5),
                                Color(0xFF1565C0)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📦",
                    fontSize = 52.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Title
            Text(
                text = "Order Manager",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Manage • Track • Dispatch",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF90CAF9),
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Divider line
            Box(
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .width(60.dp)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF90CAF9), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "Effortless order fulfillment",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}
