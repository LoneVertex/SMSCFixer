package io.github.lonevertex.smscguard

import android.telephony.SmsManager
import io.github.libxposed.api.XposedInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class SmscGuardHookInterceptorTest {

    private lateinit var logger: DiagnosticLogger
    private lateinit var resolver: RoutingSignalResolver
    private lateinit var config: SmscSelectionConfig

    @Before
    fun setUp() {
        logger = DiagnosticLogger("TestTag")
        resolver = RoutingSignalResolver(logger)
        config = SmscRuntimeConfig.buildDefaultConfig(
            SmscGuardModule.DEFAULT_SMSC_PRIMARY,
            SmscGuardModule.DEFAULT_SMSC_SECONDARY
        )
    }

    class DummySmsManager(val slot: Int, val subId: Int) {
        fun getSlotIndex(): Int = slot
        fun getSubscriptionId(): Int = subId
    }

    private fun getSendTextMessageMethod(): Method {
        for (m in SmsManager::class.java.declaredMethods) {
            if (m.name == "sendTextMessage" && m.parameterTypes.size == 5) {
                return m
            }
        }
        error("sendTextMessage not found")
    }

    private fun getNonRegisteredMethod(): Method {
        return SmsManager::class.java.getMethod("getSubscriptionId")
    }

    /**
     * Executes the interceptor logic extracted from SmscGuardModule.kt for unit verification.
     */
    private fun executeInterceptor(chain: XposedInterface.Chain, currentConfig: SmscSelectionConfig) {
        val sig = HookSignatureRegistry.match(chain.executable as Method)
        if (sig == null || chain.args.size <= sig.smscArgumentIndex) {
            chain.proceed()
            return
        }

        val signals = resolver.resolve(chain.thisObject)
        val selection = SmscSelector.selectSmscDetailed(
            signals.slotIndex,
            signals.mccMnc,
            signals.carrierName,
            currentConfig
        )

        if (!selection.replacementAuthorized || selection.smsc == null) {
            logger.throttledDiagnostic(
                "replacement_preserved",
                "reason=" + selection.reason,
                30_000L
            )
            chain.proceed()
            return
        }

        val original = chain.getArg(sig.smscArgumentIndex)
        if (selection.smsc == original) {
            chain.proceed()
            return
        }

        val newArgs = chain.args.toTypedArray()
        newArgs[sig.smscArgumentIndex] = selection.smsc
        logger.diagnostic("smsc_replaced", "reason=" + selection.reason)
        chain.proceed(newArgs)
    }

    @Test
    fun slotZeroReplacesSmscWithPrimary() {
        val method = getSendTextMessageMethod()
        val dummyManager = DummySmsManager(slot = 0, subId = 1)
        val initialArgs: Array<Any?> = arrayOf("123456", null, "Hello", null, null)

        val chain = TestHookChain(method, dummyManager, initialArgs)
        executeInterceptor(chain, config)

        assertFalse(chain.isProceedCalledWithoutArgs)
        assertNotNull(chain.proceedArgs)
        assertEquals(SmscGuardModule.DEFAULT_SMSC_PRIMARY, chain.proceedArgs[1])
        assertEquals("123456", chain.proceedArgs[0])
    }

    @Test
    fun slotOneReplacesSmscWithSecondary() {
        val method = getSendTextMessageMethod()
        val dummyManager = DummySmsManager(slot = 1, subId = 2)
        val initialArgs: Array<Any?> = arrayOf("123456", null, "Hello", null, null)

        val chain = TestHookChain(method, dummyManager, initialArgs)
        executeInterceptor(chain, config)

        assertFalse(chain.isProceedCalledWithoutArgs)
        assertNotNull(chain.proceedArgs)
        assertEquals(SmscGuardModule.DEFAULT_SMSC_SECONDARY, chain.proceedArgs[1])
    }

    @Test
    fun idempotentNoOpWhenSmscAlreadyMatchesTarget() {
        val method = getSendTextMessageMethod()
        val dummyManager = DummySmsManager(slot = 0, subId = 1)
        // Original already matches primary SMSC!
        val initialArgs: Array<Any?> = arrayOf("123456", SmscGuardModule.DEFAULT_SMSC_PRIMARY, "Hello", null, null)

        val chain = TestHookChain(method, dummyManager, initialArgs)
        executeInterceptor(chain, config)

        // Must proceed without argument replacement
        assertTrue(chain.isProceedCalledWithoutArgs)
        assertNull(chain.proceedArgs)
    }

    @Test
    fun failClosedPreservesOriginalWhenSignalsUnknown() {
        val method = getSendTextMessageMethod()
        // Unknown slot and no carrier signals
        val dummyManager = DummySmsManager(slot = -1, subId = -1)
        val initialArgs: Array<Any?> = arrayOf("123456", "+original_smsc", "Hello", null, null)

        val chain = TestHookChain(method, dummyManager, initialArgs)
        executeInterceptor(chain, config)

        // Must preserve original SMSC
        assertTrue(chain.isProceedCalledWithoutArgs)
        assertNull(chain.proceedArgs)
    }

    @Test
    fun unmatchedMethodProceedsWithoutModification() {
        val nonHookMethod = getNonRegisteredMethod()
        val dummyManager = DummySmsManager(slot = 0, subId = 1)
        val initialArgs: Array<Any?> = arrayOf()

        val chain = TestHookChain(nonHookMethod, dummyManager, initialArgs)
        executeInterceptor(chain, config)

        assertTrue(chain.isProceedCalledWithoutArgs)
        assertNull(chain.proceedArgs)
    }

    @Test
    fun shortArgumentsArrayProceedsWithoutModification() {
        val method = getSendTextMessageMethod()
        val dummyManager = DummySmsManager(slot = 0, subId = 1)
        // Less than or equal to smscArgumentIndex (1)
        val initialArgs: Array<Any?> = arrayOf("only_dest")

        val chain = TestHookChain(method, dummyManager, initialArgs)
        executeInterceptor(chain, config)

        assertTrue(chain.isProceedCalledWithoutArgs)
        assertNull(chain.proceedArgs)
    }
}
