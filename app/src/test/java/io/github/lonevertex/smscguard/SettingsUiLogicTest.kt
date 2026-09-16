package io.github.lonevertex.smscguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiLogicTest {

    private fun isStrictE164(value: String): Boolean {
        return SmscConfigSchema.isStrictE164(value)
    }

    @Test
    fun testStrictE164Validation() {
        // Valid E.164 with leading +
        assertTrue(isStrictE164("+20105996500"))
        assertTrue(isStrictE164("+20122000020"))
        assertTrue(isStrictE164("+12345"))
        assertTrue(isStrictE164("+12345678901234567890")) // 20 digits

        // Invalid: missing leading +
        assertFalse(isStrictE164("20105996500"))
        assertFalse(isStrictE164("0105996500"))

        // Invalid: too short or too long
        assertFalse(isStrictE164("+1234")) // 4 digits
        assertFalse(isStrictE164("+123456789012345678901")) // 21 digits

        // Invalid: non-digits
        assertFalse(isStrictE164("+2010599650a"))
        assertFalse(isStrictE164("+2010 599 6500"))
        assertFalse(isStrictE164("+2010-599-6500"))
        assertFalse(isStrictE164(""))
        assertFalse(isStrictE164("   "))
    }

    @Test
    fun testDefaultSmscConstantsPreserved() {
        assertEquals("+20105996500", SmscGuardModule.DEFAULT_SMSC_PRIMARY)
        assertEquals("+20122000020", SmscGuardModule.DEFAULT_SMSC_SECONDARY)
        assertEquals("+20105996500", SettingsViewModel.DEFAULT_PRIMARY)
        assertEquals("+20122000020", SettingsViewModel.DEFAULT_SECONDARY)
        assertTrue(SettingsViewModel.DEFAULT_PACKAGES.contains("com.google.android.apps.messaging"))
        assertTrue(SettingsViewModel.DEFAULT_PACKAGES.contains("com.android.mms"))
    }

    @Test
    fun testSanitizedRoutingSimulationDecisions() {
        val config = SmscRuntimeConfig.buildDefaultConfig(
            SmscGuardModule.DEFAULT_SMSC_PRIMARY,
            SmscGuardModule.DEFAULT_SMSC_SECONDARY
        )

        // Slot 0 -> Primary
        val result0 = SmscSelector.selectSmscDetailed(0, "60202", "Vodafone Egypt", config)
        assertTrue(result0.replacementAuthorized)
        assertEquals(SmscSelector.DecisionReason.SLOT_PRIMARY, result0.reason)
        assertEquals(SmscGuardModule.DEFAULT_SMSC_PRIMARY, result0.smsc)

        // Slot 1 -> Secondary
        val result1 = SmscSelector.selectSmscDetailed(1, "60201", "Orange Egypt", config)
        assertTrue(result1.replacementAuthorized)
        assertEquals(SmscSelector.DecisionReason.SLOT_SECONDARY, result1.reason)
        assertEquals(SmscGuardModule.DEFAULT_SMSC_SECONDARY, result1.smsc)

        // Ambiguous signals -> Preserved original
        val resultAmbiguous = SmscSelector.selectSmscDetailed(-1, "60201", "Vodafone Egypt", config)
        assertFalse(resultAmbiguous.replacementAuthorized)
        assertEquals(SmscSelector.DecisionReason.AMBIGUOUS_CARRIER_SIGNALS, resultAmbiguous.reason)
        assertNull(resultAmbiguous.smsc)
    }

    @Test
    fun testSelfCheckValidationLogic() {
        val validPrimary = "+20105996500"
        val validSecondary = "+20122000020"
        val validTargets = "com.google.android.apps.messaging,com.android.mms"

        val schemaOk = SmscConfigSchema.CURRENT_VERSION >= 1
        val smscOk = SmscConfigSchema.isSmscFormatValid(validPrimary) && SmscConfigSchema.isSmscFormatValid(validSecondary)
        val packagesOk = SmscConfigSchema.isTargetPackagesCsvAcceptable(validTargets)
        val parsedTargets = SmscConfigSchema.parseAndNormalizeTargetPackages(validTargets)

        assertTrue(schemaOk)
        assertTrue(smscOk)
        assertTrue(packagesOk)
        assertEquals(2, parsedTargets.size)
    }

    @Test
    fun testSettingsUiStateFrameworkTelemetryDefaults() {
        val state = SettingsUiState()
        assertFalse("isLsposedBound should default to false", state.isLsposedBound)
        assertNull("frameworkInfo should default to null", state.frameworkInfo)
        assertFalse("lsposedManagedPreferences should default to false", state.lsposedManagedPreferences)
    }

    @Test
    fun testSettingsUiStateFrameworkTelemetryStateTransitions() {
        val initialState = SettingsUiState()

        val boundState = initialState.copy(
            isLsposedBound = true,
            frameworkInfo = "LSPosed 2.2.0 (102)",
            lsposedManagedPreferences = true
        )
        assertTrue("isLsposedBound should be true when bound", boundState.isLsposedBound)
        assertEquals("LSPosed 2.2.0 (102)", boundState.frameworkInfo)
        assertTrue("lsposedManagedPreferences should be true when bound", boundState.lsposedManagedPreferences)

        val unboundState = boundState.copy(
            isLsposedBound = false,
            frameworkInfo = null,
            lsposedManagedPreferences = false
        )
        assertFalse("isLsposedBound should be false after unbinding", unboundState.isLsposedBound)
        assertNull("frameworkInfo should be null after unbinding", unboundState.frameworkInfo)
        assertFalse("lsposedManagedPreferences should be false after unbinding", unboundState.lsposedManagedPreferences)
    }

    @Test
    fun testPreferencesManagerFrameworkFlows() {
        PreferencesManager.updateFrameworkStatusForTesting(true, "LSPosed 2.2.0 (102)")
        assertTrue(PreferencesManager.isLsposedBound.value)
        assertEquals("LSPosed 2.2.0 (102)", PreferencesManager.frameworkInfo.value)

        PreferencesManager.updateFrameworkStatusForTesting(false)
        assertFalse(PreferencesManager.isLsposedBound.value)
        assertNull(PreferencesManager.frameworkInfo.value)
    }
}
