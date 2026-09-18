package com.example.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmanDarkSlate
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.AmanTealLight
import com.example.ui.theme.TextSecondary
import com.example.R

@Composable
fun AmanSplashScreen(
    isReturningUser: Boolean = false,
    onFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        started = true
        kotlinx.coroutines.delay(2500)
        onFinished()
    }
    val iconScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.78f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "aman_logo_scale"
    )
    val pulse by rememberInfiniteTransition(label = "aman_pulse").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "aman_pulse_scale"
    )

    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF007F72), Color(0xFF005A58), Color(0xFF003F46)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(300.dp).graphicsLayer { alpha = 0.11f; scaleX = pulse; scaleY = pulse }.clip(CircleShape).background(AmanTealLight))
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF002F39).copy(alpha = 0.48f)))
            )
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.aman_logo),
                contentDescription = "شعار أمان",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(128.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }.clip(RoundedCornerShape(36.dp))
            )
            Spacer(Modifier.height(18.dp))
            AnimatedVisibility(visible = started, enter = fadeIn(tween(550)) + scaleIn(tween(550))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AMAN | أمان", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("أمان حماية وضمان", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE0FFFA))
                    Spacer(Modifier.height(16.dp))
                    Text(if (isReturningUser) "أهلاً بعودتك" else "حماية وضمان لرقمك", fontSize = 12.sp, color = Color(0xFFB7EEE6))
                    Spacer(Modifier.height(28.dp))
                    Box(Modifier.width(72.dp).height(2.dp).background(AmanTealLight, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

@Composable
fun AmanSplashScreenLegacy(onLogin: () -> Unit, onRegister: () -> Unit, modifier: Modifier = Modifier) {
    AmanSplashScreen(isReturningUser = false, onFinished = onLogin, modifier = modifier)
}
