package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.example.ui.components.JoyTubeBrandHeader
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @get:Rule
  val composeRule: ComposeContentTestRule = createComposeRule()

  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
  }

  @Test
  fun brandHeaderKeepsSearchAndProfileButHidesThemeChip() {
    composeRule.setContent {
      JoyTubeBrandHeader(onSearchClick = {}, onAccountClick = {})
    }

    composeRule.onNodeWithTag("design_specs_button").assertDoesNotExist()
    composeRule.onNodeWithTag("header_search_button").assertExists()
    composeRule.onNodeWithTag("header_account_button").assertExists()
  }
}
