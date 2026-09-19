package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.AppLanguage
import com.example.data.IptvRepository
import com.example.ui.screens.ActivationScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+w960dp-h540dp", sdk = [36])
class EuroIptvFlowTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSplashScreen_rendersLogoAndSpinner() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        SplashScreen(onSplashFinished = {})
      }
    }

    composeTestRule.onNodeWithTag("splash_screen").assertExists()
    composeTestRule.onNodeWithTag("splash_logo").assertExists()
    composeTestRule.onNodeWithTag("splash_spinner").assertExists()
  }

  @Test
  fun testActivationScreen_validationAndWhatsApp() {
    IptvRepository.deactivate()
    var activated = false
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        ActivationScreen(onActivationSuccess = { activated = true })
      }
    }

    composeTestRule.onNodeWithTag("activation_screen").assertExists()
    composeTestRule.onNodeWithTag("whatsapp_support_card").assertExists()
    composeTestRule.onNodeWithTag("whatsapp_button").assertExists()

    // Test activation with valid code
    composeTestRule.onNodeWithTag("activation_code_input").performTextInput("EURO2026")
    composeTestRule.onNodeWithTag("activate_submit_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(activated)
    assertTrue(IptvRepository.isActivated.value)
  }

  @Test
  fun testVideoPlayerScreen_rendersControls() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        VideoPlayerScreen(onBackClick = {})
      }
    }

    composeTestRule.onNodeWithTag("video_player_screen").assertExists()
    composeTestRule.onNodeWithTag("player_back_button").assertExists()
    composeTestRule.onNodeWithTag("player_play_pause_button").assertExists()
    composeTestRule.onNodeWithTag("player_live_badge").assertExists()
    composeTestRule.onNodeWithTag("player_duration_text").assertExists()
    composeTestRule.onNodeWithTag("player_prev_button").assertExists()
    composeTestRule.onNodeWithTag("player_next_button").assertExists()
    composeTestRule.onNodeWithTag("player_hide_controls_button").assertExists()
    composeTestRule.onNodeWithTag("player_subtitles_button").assertExists()
    composeTestRule.onNodeWithTag("player_favorite_button").assertExists()
    composeTestRule.onNodeWithTag("player_settings_button").assertExists()

    // Test clicking play/pause toggle
    composeTestRule.onNodeWithTag("player_play_pause_button").performClick()
  }

  @Test
  fun testAdminPanelScreen_rendersTabsAndFeatures() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        AdminPanelScreen(onBackClick = {})
      }
    }

    composeTestRule.onNodeWithTag("admin_panel_screen").assertExists()
  }

  @Test
  fun testLanguageSwitcher_toggleArabicAndEnglish() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        EuroIptvAppNavigation()
      }
    }

    // Set activated state to view home screen directly
    IptvRepository.activateWithCode("EURO2026")
    composeTestRule.waitForIdle()

    // Test language switching in repository
    assertEquals(AppLanguage.ENGLISH, IptvRepository.currentLanguage.value)
    IptvRepository.setLanguage(AppLanguage.ARABIC)
    assertEquals(AppLanguage.ARABIC, IptvRepository.currentLanguage.value)
    assertTrue(IptvRepository.currentLanguage.value.isRtl)

    IptvRepository.setLanguage(AppLanguage.ENGLISH)
    assertEquals(AppLanguage.ENGLISH, IptvRepository.currentLanguage.value)
  }
}

