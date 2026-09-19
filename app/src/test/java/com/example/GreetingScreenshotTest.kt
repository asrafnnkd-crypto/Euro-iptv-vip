package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.example.ui.EuroIptvScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+w960dp-h540dp", sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun iptv_screen_renders_successfully() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        EuroIptvScreen()
      }
    }

    composeTestRule.onNodeWithTag("euro_iptv_screen").assertExists()
    composeTestRule.onNodeWithTag("app_logo").assertExists()
    composeTestRule.onNodeWithTag("card_live_tvs").assertExists()
    composeTestRule.onNodeWithTag("card_movies").assertExists()
    composeTestRule.onNodeWithTag("card_radios").assertExists()
    composeTestRule.onNodeWithTag("bottom_widget_area").assertExists()

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

