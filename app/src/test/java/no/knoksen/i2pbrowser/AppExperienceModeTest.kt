package no.knoksen.i2pbrowser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppExperienceModeTest {
    @Test
    fun `release mode hides lab modules without real SAM identity`() {
        val tabs = visibleAppTabs(
            mode = AppExperienceMode.RELEASE_REAL,
            hasRealSamIdentity = false
        )

        assertEquals(listOf(AppTab.ROUTER, AppTab.BROWSER), tabs)
        assertFalse(tabs.contains(AppTab.VPN_VPS))
        assertFalse(tabs.contains(AppTab.COMMS))
        assertFalse(tabs.contains(AppTab.IDENTITY))
    }

    @Test
    fun `release mode shows identity only when backed by real SAM identity`() {
        val tabs = visibleAppTabs(
            mode = AppExperienceMode.RELEASE_REAL,
            hasRealSamIdentity = true
        )

        assertTrue(tabs.contains(AppTab.IDENTITY))
        assertFalse(tabs.contains(AppTab.VPN_VPS))
        assertFalse(tabs.contains(AppTab.COMMS))
    }

    @Test
    fun `lab mode shows all modules`() {
        val tabs = visibleAppTabs(
            mode = AppExperienceMode.LAB_SIMULATION,
            hasRealSamIdentity = false
        )

        assertEquals(AppTab.values().toList(), tabs)
    }

    @Test
    fun `lab-only tabs are clearly labeled`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertTrue(context.getString(AppTab.VPN_VPS.labelRes).startsWith("LAB"))
        assertTrue(context.getString(AppTab.COMMS.labelRes).startsWith("LAB"))
    }
}
