package no.knoksen.i2pbrowser

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.RealAlphaStatus
import no.knoksen.i2pbrowser.ui.I2pEndpointSetupCard
import no.knoksen.i2pbrowser.ui.RealAlphaStatusCard
import no.knoksen.i2pbrowser.ui.SecurityBoundariesCard
import no.knoksen.i2pbrowser.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndroidRealAlphaUiCoreFlowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `endpoint setup UI rejects invalid host without saving unintended endpoint`() {
        var attemptedConfig: I2pEndpointConfig? = null

        compose.setContent {
            var validationError by remember { mutableStateOf("") }
            MyApplicationTheme {
                I2pEndpointSetupCard(
                    endpointConfig = I2pEndpointConfig.LOCAL_ANDROID_ROUTER,
                    validationError = validationError,
                    onSave = { config ->
                        attemptedConfig = config
                        val validation = config.validate()
                        validationError = validation.errorText
                        validation.isValid
                    }
                )
            }
        }

        compose.onNodeWithTag("i2p_endpoint_setup_card").assertIsDisplayed()
        compose.onNodeWithTag("endpoint_manual_button").performClick()
        compose.onNodeWithTag("endpoint_host_input").performTextReplacement("bad host")
        compose.onNodeWithTag("endpoint_manual_save_button").performClick()

        assertEquals("Router host cannot contain spaces.", textForTag("endpoint_validation_error"))
        compose.onNodeWithText("Router host cannot contain spaces.", useUnmergedTree = true)
            .fetchSemanticsNode()
        assertEquals("bad host", attemptedConfig?.host)
    }

    @Test
    fun `real alpha status and boundaries UI keep limitations visible`() {
        val status = RealAlphaStatus(
            endpoint = I2pEndpointConfig.LOCAL_ANDROID_ROUTER,
            diagnostics = null,
            lastDiagnosticsAtMillis = null,
            appMode = AppExperienceMode.RELEASE_REAL,
            versionName = "0.3.1-dev",
            versionCode = 4
        )

        compose.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    RealAlphaStatusCard(status = status)
                    SecurityBoundariesCard()
                }
            }
        }

        compose.onNodeWithTag("real_alpha_status_card").assertIsDisplayed()
        compose.onNodeWithText("REAL ALPHA STATUS").assertIsDisplayed()
        compose.onNodeWithText("Run diagnostics to verify readiness").assertIsDisplayed()
        compose.onNodeWithTag("security_boundaries_card").performScrollTo()
        compose.onNodeWithText("SECURITY BOUNDARIES").assertIsDisplayed()
        compose.onNodeWithText("No OS-level VPN tunneling is active in this release.").fetchSemanticsNode()
        compose.onNodeWithText("RELEASE_REAL means measured diagnostics/proxy/SAM behavior, not anonymity by itself.").fetchSemanticsNode()
    }

    private fun textForTag(tag: String): String {
        return compose.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }
    }
}
