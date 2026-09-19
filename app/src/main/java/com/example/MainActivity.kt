package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.AppLanguage
import com.example.data.IptvRepository
import com.example.ui.EuroIptvScreen
import com.example.ui.screens.ActivationScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.IptvBackground
import com.example.ui.theme.MyApplicationTheme

/**
 * Screen destinations for the EURO IPTV navigation flow
 */
enum class IptvScreenDestination(val depth: Int) {
  SPLASH(0),
  ACTIVATION(1),
  HOME(2),
  PLAYER(3),
  ADMIN(3)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val currentLanguage by IptvRepository.currentLanguage.collectAsState()
      val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

      CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MyApplicationTheme(darkTheme = true) {
          Surface(
            modifier = Modifier
              .fillMaxSize()
              .safeDrawingPadding(),
            color = IptvBackground
          ) {
            EuroIptvAppNavigation()
          }
        }
      }
    }
  }
}

@Composable
fun EuroIptvAppNavigation() {
  var currentScreen by remember { mutableStateOf(IptvScreenDestination.SPLASH) }
  val isActivated by IptvRepository.isActivated.collectAsState()

  // Handle Android Back Navigation
  BackHandler(enabled = currentScreen != IptvScreenDestination.SPLASH && currentScreen != IptvScreenDestination.HOME) {
    currentScreen = when (currentScreen) {
      IptvScreenDestination.PLAYER -> IptvScreenDestination.HOME
      IptvScreenDestination.ADMIN -> IptvScreenDestination.HOME
      IptvScreenDestination.ACTIVATION -> IptvScreenDestination.ACTIVATION
      else -> IptvScreenDestination.HOME
    }
  }

  // Smooth & Lightweight Page / Section Transitions (Animations)
  // Sliding + fading transitions adapted to forward / backward movement
  AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
      val isForward = targetState.depth >= initialState.depth
      val animationDuration = 320

      if (targetState == IptvScreenDestination.SPLASH || initialState == IptvScreenDestination.SPLASH) {
        fadeIn(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
      } else if (isForward) {
        (slideInHorizontally(
          initialOffsetX = { fullWidth -> fullWidth / 4 },
          animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(animationDuration))) togetherWith
            (slideOutHorizontally(
              targetOffsetX = { fullWidth -> -fullWidth / 4 },
              animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(animationDuration / 2)))
      } else {
        (slideInHorizontally(
          initialOffsetX = { fullWidth -> -fullWidth / 4 },
          animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(animationDuration))) togetherWith
            (slideOutHorizontally(
              targetOffsetX = { fullWidth -> fullWidth / 4 },
              animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(animationDuration / 2)))
      }
    },
    label = "screen_motion_transition"
  ) { screen ->
    when (screen) {
      IptvScreenDestination.SPLASH -> {
        SplashScreen(
          onSplashFinished = {
            currentScreen = if (isActivated) {
              IptvScreenDestination.HOME
            } else {
              IptvScreenDestination.ACTIVATION
            }
          }
        )
      }

      IptvScreenDestination.ACTIVATION -> {
        ActivationScreen(
          onActivationSuccess = {
            currentScreen = IptvScreenDestination.HOME
          }
        )
      }

      IptvScreenDestination.HOME -> {
        EuroIptvScreen(
          onOpenPlayer = { _ ->
            currentScreen = IptvScreenDestination.PLAYER
          },
          onOpenAdminPanel = {
            currentScreen = IptvScreenDestination.ADMIN
          }
        )
      }

      IptvScreenDestination.PLAYER -> {
        VideoPlayerScreen(
          onBackClick = {
            currentScreen = IptvScreenDestination.HOME
          }
        )
      }

      IptvScreenDestination.ADMIN -> {
        AdminPanelScreen(
          onBackClick = {
            currentScreen = IptvScreenDestination.HOME
          }
        )
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 540)
@Composable
fun EuroIptvPreview() {
  MyApplicationTheme(darkTheme = true) {
    EuroIptvScreen()
  }
}


